"""Tests for RSS collector engine, normalizer, and deduplication."""

from unittest.mock import AsyncMock, patch

import httpx
import pytest
from sqlalchemy.ext.asyncio import AsyncSession

from app.repositories.news_repository import NewsRepository
from app.services.collector_service import NewsCollectorService
from app.services.collectors.base import BaseCollector
from app.services.location_resolver import resolve_location_from_title
from app.services.rss_normalizer import (
    canonicalize_url,
    clean_title,
    infer_category,
)

SAMPLE_RSS_XML = """<?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0">
  <channel>
    <title>Global Test News</title>
    <link>https://example.com</link>
    <description>Test news feed</description>
    <item>
      <title><![CDATA[<b>Historic</b> Climate Accord Signed in Geneva]]></title>
      <link>https://example.com/news/climate-accord?utm_source=rss#body</link>
      <pubDate>Tue, 03 Nov 2026 15:30:00 GMT</pubDate>
      <category>world</category>
    </item>
    <item>
      <title>Quantum Chip Breakthrough in Tokyo Achieves Coherence</title>
      <link>https://example.com/news/quantum-tokyo</link>
      <pubDate>2026-11-03T14:00:00Z</pubDate>
      <category>technology</category>
    </item>
  </channel>
</rss>
"""


class MockCollector(BaseCollector):
    """Test collector fixture with predefined RSS content."""

    source_id = "mock_source"
    source_name = "Mock News Service"
    rss_url = "https://example.com/mock.xml"
    home_url = "https://example.com"
    default_category = "world"


def test_clean_title():
    """Verify HTML markup removal and entity unescaping."""
    dirty = "   <b>Breaking:</b> Stock rally in London &amp; New York!   "
    clean = clean_title(dirty)
    assert clean == "Breaking: Stock rally in London & New York!"


def test_canonicalize_url():
    """Verify stripping of UTM tracking query params and fragments."""
    url = "https://example.com/article?utm_source=twitter&id=123&utm_medium=social#comments"
    clean = canonicalize_url(url)
    assert clean == "https://example.com/article?id=123"


def test_infer_category():
    """Verify category classification heuristic."""
    assert infer_category("New AI and quantum processors announced") == "technology"
    assert infer_category("Central banks announce interest rate policy") == "business"
    assert infer_category("Deep space telescope captures exoplanet image") == "science"
    assert infer_category("New hospital pandemic guidance issued by WHO") == "health"
    assert infer_category("Diplomats meet for bilateral summit") == "world"


def test_location_resolver():
    """Verify entity geocoding and strict None fallback for unknown entities."""
    name, lat, lon = resolve_location_from_title(
        "Delegates assemble in Geneva for treaty negotiations"
    )
    assert name == "Geneva, Switzerland"
    assert lat == 46.2044
    assert lon == 6.1432

    # Unlisted or ambiguous location
    name_none, lat_none, lon_none = resolve_location_from_title(
        "General assembly debates budget"
    )
    assert name_none is None
    assert lat_none is None
    assert lon_none is None


def test_parse_feed_content():
    """Verify parsing valid RSS XML into standardized RawArticleData."""
    collector = MockCollector()
    items = collector.parse_feed_content(SAMPLE_RSS_XML)

    assert len(items) == 2

    first = items[0]
    assert first.title == "Historic Climate Accord Signed in Geneva"

    assert first.original_url == "https://example.com/news/climate-accord"
    assert first.location_name == "Geneva, Switzerland"
    assert first.latitude == 46.2044
    assert first.published_at.year == 2026

    second = items[1]
    assert second.category == "technology"
    assert second.location_name == "Tokyo, Japan"


@pytest.mark.asyncio
async def test_collector_service_deduplication(db_session: AsyncSession):
    """Verify that duplicate original_url items are not re-inserted."""
    collector = MockCollector()
    service = NewsCollectorService(collectors=[collector])

    # Mock HTTP client returning SAMPLE_RSS_XML
    mock_response = httpx.Response(
        200,
        text=SAMPLE_RSS_XML,
        request=httpx.Request("GET", collector.rss_url),
    )

    with patch.object(httpx.AsyncClient, "get", new_callable=AsyncMock) as mock_get:
        mock_get.return_value = mock_response

        async with httpx.AsyncClient() as client:
            # 1. First run: should insert 2 items
            result1 = await service.collect_from_source(collector, db_session, client)
            assert result1.success is True
            assert result1.inserted_count == 2
            assert result1.duplicate_count == 0

            # 2. Second run: identical items should be identified as duplicates
            result2 = await service.collect_from_source(collector, db_session, client)
            assert result2.success is True
            assert result2.inserted_count == 0
            assert result2.duplicate_count == 2

    # Verify database state
    repo = NewsRepository(db_session)
    items, total = await repo.list_articles(page=1, limit=10)
    assert total == 2


@pytest.mark.asyncio
async def test_collector_network_error_isolation(db_session: AsyncSession):
    """Verify that network failures on a collector do not crash the runner."""
    collector = MockCollector()
    service = NewsCollectorService(collectors=[collector])

    with patch.object(httpx.AsyncClient, "get", new_callable=AsyncMock) as mock_get:
        mock_get.side_effect = httpx.ConnectTimeout("Connection timed out")

        async with httpx.AsyncClient() as client:
            result = await service.collect_from_source(collector, db_session, client)
            assert result.success is False
            assert result.inserted_count == 0
            assert "Connection timed out" in (result.error_message or "")


def test_malformed_xml_handling():
    """Verify that completely broken XML returns empty list rather than crashing."""
    collector = MockCollector()
    items = collector.parse_feed_content("<not valid xml at all <<>>")
    assert items == []


@pytest.mark.asyncio
async def test_manual_collect_api(client: httpx.AsyncClient):
    """Test POST /api/v1/news/collect triggers ingestion and returns status."""
    mock_response = httpx.Response(
        200,
        text=SAMPLE_RSS_XML,
        request=httpx.Request("GET", "https://example.com/rss"),
    )

    with patch.object(httpx.AsyncClient, "get", new_callable=AsyncMock) as mock_get:
        mock_get.return_value = mock_response

        response = await client.post("/api/v1/news/collect")
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "completed"
        assert "sources" in data
        assert len(data["sources"]) > 0
