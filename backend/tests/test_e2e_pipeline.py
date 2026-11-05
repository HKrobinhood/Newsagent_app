"""End-to-End integration tests covering pipeline from ingestion to Agent."""

from datetime import datetime, timezone
from unittest.mock import patch

import httpx
import pytest
from sqlalchemy.ext.asyncio import AsyncSession

from app.repositories.news_repository import NewsRepository
from app.schemas.news import NewsArticleCreate, NewsSourceCreate
from app.services.collector_service import CollectorResult


@pytest.mark.asyncio
async def test_e2e_full_news_ingestion_to_api_and_geo(
    client: httpx.AsyncClient, db_session: AsyncSession
) -> None:
    """Validate full lifecycle: ingestion -> database -> /news -> /news/geo."""
    repo = NewsRepository(db_session)

    # 1. Register sources
    await repo.create_source(
        NewsSourceCreate(
            id="bbc",
            name="BBC News",
            rss_url="https://bbc.com/rss",
            home_url="https://bbc.com",
        )
    )
    await repo.create_source(
        NewsSourceCreate(
            id="cnn",
            name="CNN News",
            rss_url="https://cnn.com/rss",
            home_url="https://cnn.com",
        )
    )

    now = datetime.now(timezone.utc)

    # 2. Simulate multi-source ingestion
    article_a = NewsArticleCreate(
        title="Diplomatic Climate Accords Concluded in Geneva",
        source_id="bbc",
        source_name="BBC News",
        published_at=now,
        original_url="https://bbc.com/news/climate-geneva-e2e",
        category="science",
        location_name="Geneva, Switzerland",
        latitude=46.2044,
        longitude=6.1432,
    )
    article_b = NewsArticleCreate(
        title="Breakthrough in Quantum Processing Architecture in Tokyo",
        source_id="cnn",
        source_name="CNN News",
        published_at=now,
        original_url="https://cnn.com/news/quantum-tokyo-e2e",
        category="technology",
        location_name="Tokyo, Japan",
        latitude=35.6762,
        longitude=139.6503,
    )
    article_c = NewsArticleCreate(
        title="Global Macroeconomic Liquidity Analysis",
        source_id="bbc",
        source_name="BBC News",
        published_at=now,
        original_url="https://bbc.com/news/macro-global-e2e",
        category="business",
        location_name=None,
        latitude=None,
        longitude=None,
    )

    await repo.create_article(article_a)
    await repo.create_article(article_b)
    await repo.create_article(article_c)

    # 3. Verify News List REST API
    news_resp = await client.get("/api/v1/news?page=1&limit=10")
    assert news_resp.status_code == 200
    news_data = news_resp.json()
    assert news_data["total"] >= 3
    assert len(news_data["items"]) >= 3

    # 4. Verify GeoJSON Endpoint (Only A & B have valid coordinates)
    geo_resp = await client.get("/api/v1/news/geo")
    assert geo_resp.status_code == 200
    geo_data = geo_resp.json()
    assert geo_data["type"] == "FeatureCollection"
    assert len(geo_data["features"]) >= 2

    # Check coordinate order [longitude, latitude]
    coords = [f["geometry"]["coordinates"] for f in geo_data["features"]]
    assert any(c == [6.1432, 46.2044] for c in coords)
    assert any(c == [139.6503, 35.6762] for c in coords)


@pytest.mark.asyncio
async def test_e2e_ai_agent_dual_mode_flow(
    client: httpx.AsyncClient, db_session: AsyncSession
) -> None:
    """Validate full conversational agent dual-mode routing and grounded citations."""
    repo = NewsRepository(db_session)
    await repo.create_source(
        NewsSourceCreate(
            id="bbc",
            name="BBC News",
            rss_url="https://bbc.com/rss",
            home_url="https://bbc.com",
        )
    )

    now = datetime.now(timezone.utc)
    seeded_url = "https://bbc.com/news/ai-semiconductor-munich-e2e"
    await repo.create_article(
        NewsArticleCreate(
            title="European Semiconductor Initiative Signed in Munich",
            source_id="bbc",
            source_name="BBC News",
            published_at=now,
            original_url=seeded_url,
            category="technology",
            location_name="Munich, Germany",
            latitude=48.1351,
            longitude=11.5820,
        )
    )

    # 1. Title Search Mode (Internal DB query)
    title_search_payload = {
        "message": "查询最近24小时关于半导体芯片的报道",
        "mode": "title_search",
    }
    chat_resp = await client.post("/api/v1/ai/chat", json=title_search_payload)
    assert chat_resp.status_code == 200
    chat_data = chat_resp.json()
    assert chat_data["mode_used"] == "title_search"
    assert len(chat_data["sources"]) > 0
    assert any("semiconductor" in s["title"].lower() for s in chat_data["sources"])
    assert any(s["original_url"] == seeded_url for s in chat_data["sources"])

    # 2. Web Verification Mode (Live search and cross check)
    web_verif_payload = {
        "message": "财政部长关于金融稳定与通胀的发言说了什么",
        "mode": "auto",
    }
    web_resp = await client.post("/api/v1/ai/chat", json=web_verif_payload)
    assert web_resp.status_code == 200
    web_data = web_resp.json()
    assert web_data["mode_used"] == "web_verification"
    assert len(web_data["sources"]) > 0
    assert "核查结论" in web_data["answer"]


@pytest.mark.asyncio
async def test_e2e_security_headers_and_error_handling(
    client: httpx.AsyncClient,
) -> None:
    """Verify security hardening headers and standardized RFC error responses."""
    # 1. Health check & security headers
    health_resp = await client.get("/health")
    assert health_resp.status_code == 200
    assert health_resp.headers.get("X-Content-Type-Options") == "nosniff"
    assert health_resp.headers.get("X-Frame-Options") == "DENY"
    assert "1" in health_resp.headers.get("X-XSS-Protection", "")

    # 2. Non-existent resource returns structured 404
    missing_resp = await client.get("/api/v1/news/non-existent-uuid-99999")
    assert missing_resp.status_code == 404
    missing_json = missing_resp.json()
    assert "error" in missing_json
    assert missing_json["error"]["code"] == "RESOURCE_NOT_FOUND"


@pytest.mark.asyncio
async def test_e2e_collector_mock_pipeline(
    client: httpx.AsyncClient,
) -> None:
    """Verify trigger_collection endpoint returns structured report."""
    with patch(
        "app.services.collector_service.NewsCollectorService.collect_all",
        return_value=[
            CollectorResult(
                source_id="bbc",
                source_name="BBC News",
                success=True,
                fetched_count=10,
                inserted_count=5,
                duplicate_count=5,
                error_message=None,
            )
        ],
    ):
        resp = await client.post("/api/v1/news/collect")
        assert resp.status_code == 200
        data = resp.json()
        assert data["status"] == "completed"
        assert len(data["sources"]) == 1
        assert data["sources"][0]["source_id"] == "bbc"
        assert data["sources"][0]["success"] is True
