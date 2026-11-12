"""Tests for S1 media source filtering across news, geo, and AI agent endpoints."""

from datetime import datetime, timezone

import pytest
from httpx import AsyncClient


@pytest.mark.asyncio
async def test_list_news_sources_returns_supported_media(client: AsyncClient) -> None:
    """Verify GET /api/v1/news/sources returns the supported news media sources."""
    response = await client.get("/api/v1/news/sources")
    assert response.status_code == 200
    data = response.json()
    source_ids = {item["id"] for item in data}
    assert {"bbc", "cnn", "aljazeera", "fox"}.issubset(source_ids)


@pytest.mark.asyncio
async def test_news_and_geo_filtering_by_source_ids(client: AsyncClient) -> None:
    """Verify source_ids query parameter filters articles and pagination."""
    for src_id, src_name in [
        ("bbc", "BBC News"),
        ("cnn", "CNN"),
        ("aljazeera", "Al Jazeera"),
    ]:
        await client.post(
            "/api/v1/news/sources",
            json={
                "id": src_id,
                "name": src_name,
                "rss_url": f"https://{src_id}.example.com/rss",
                "home_url": f"https://{src_id}.example.com",
                "is_active": True,
            },
        )

    now_iso = datetime.now(timezone.utc).isoformat()
    await client.post(
        "/api/v1/news",
        json={
            "title": "BBC Artificial Intelligence Summit in London",
            "source_id": "bbc",
            "source_name": "BBC News",
            "published_at": now_iso,
            "original_url": "https://bbc.example.com/ai-london",
            "category": "technology",
            "location_name": "London, UK",
            "latitude": 51.5074,
            "longitude": -0.1278,
        },
    )
    await client.post(
        "/api/v1/news",
        json={
            "title": "CNN Artificial Intelligence Breakthrough in New York",
            "source_id": "cnn",
            "source_name": "CNN",
            "published_at": now_iso,
            "original_url": "https://cnn.example.com/ai-ny",
            "category": "technology",
            "location_name": "New York, USA",
            "latitude": 40.7128,
            "longitude": -74.0060,
        },
    )

    # 1. Exclude BBC -> only CNN returned, total == 1
    res_cnn = await client.get("/api/v1/news?source_ids=cnn,aljazeera")
    assert res_cnn.status_code == 200
    body_cnn = res_cnn.json()
    assert body_cnn["total"] == 1
    assert len(body_cnn["items"]) == 1
    assert body_cnn["items"][0]["source_id"] == "cnn"

    # 2. Deselect all sources (__none__) -> empty list, total == 0
    res_none = await client.get("/api/v1/news?source_ids=__none__")
    assert res_none.status_code == 200
    body_none = res_none.json()
    assert body_none["total"] == 0
    assert body_none["items"] == []

    # 3. Geo endpoint filtered by source_ids=cnn -> excludes BBC London marker
    res_geo = await client.get("/api/v1/news/geo?source_ids=cnn")
    assert res_geo.status_code == 200
    features = res_geo.json()["features"]
    assert len(features) == 1
    assert features[0]["properties"]["source_id"] == "cnn"


@pytest.mark.asyncio
async def test_ai_chat_respects_source_ids_in_title_and_web_modes(
    client: AsyncClient,
) -> None:
    """Verify AI Title Search and Web Verification honor allowed source_ids."""
    now_iso = datetime.now(timezone.utc).isoformat()
    await client.post(
        "/api/v1/news/sources",
        json={
            "id": "bbc",
            "name": "BBC News",
            "rss_url": "https://bbc.example.com/rss",
            "home_url": "https://bbc.example.com",
            "is_active": True,
        },
    )
    await client.post(
        "/api/v1/news/sources",
        json={
            "id": "cnn",
            "name": "CNN",
            "rss_url": "https://cnn.example.com/rss",
            "home_url": "https://cnn.example.com",
            "is_active": True,
        },
    )
    await client.post(
        "/api/v1/news",
        json={
            "title": "BBC Exclusive Semiconductor Report",
            "source_id": "bbc",
            "source_name": "BBC News",
            "published_at": now_iso,
            "original_url": "https://bbc.example.com/semi-report",
            "category": "technology",
        },
    )
    await client.post(
        "/api/v1/news",
        json={
            "title": "CNN Global Semiconductor Outlook",
            "source_id": "cnn",
            "source_name": "CNN",
            "published_at": now_iso,
            "original_url": "https://cnn.example.com/semi-outlook",
            "category": "technology",
        },
    )

    # 1. Title search with BBC excluded -> only CNN returned in sources
    res_title = await client.post(
        "/api/v1/ai/chat",
        json={
            "message": "Latest semiconductor technology news",
            "mode": "title_search",
            "source_ids": ["cnn"],
        },
    )
    assert res_title.status_code == 200
    title_payload = res_title.json()
    assert len(title_payload["sources"]) == 1
    assert title_payload["sources"][0]["source_name"] == "CNN"

    # 2. Web verification for a Geneva climate statement (BBC News in MockWebSearch)
    # when BBC is excluded -> must NOT leak BBC card and must explain constraint
    res_web = await client.post(
        "/api/v1/ai/chat",
        json={
            "message": "What did delegates say at the Geneva climate accord?",
            "mode": "web_verification",
            "source_ids": ["fox"],
        },
    )
    assert res_web.status_code == 200
    web_payload = res_web.json()
    assert web_payload["sources"] == []
    assert "Settings" in web_payload["answer"]
