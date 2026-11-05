"""Integration tests for FastAPI news REST endpoints."""

from datetime import datetime, timezone

import pytest
from httpx import AsyncClient


@pytest.mark.asyncio
async def test_health_endpoints(client: AsyncClient) -> None:
    """Test root and v1 health check endpoints."""
    # Root health
    response = await client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "ok"
    assert data["service"] == "GlobalNewsAI"
    assert "timestamp" in data

    # V1 health
    v1_response = await client.get("/api/v1/health")
    assert v1_response.status_code == 200
    assert v1_response.json()["status"] == "ok"


@pytest.mark.asyncio
async def test_news_empty_list(client: AsyncClient) -> None:
    """Test GET /api/v1/news with an empty database."""
    response = await client.get("/api/v1/news")
    assert response.status_code == 200
    data = response.json()
    assert data["items"] == []
    assert data["total"] == 0
    assert data["page"] == 1
    assert data["limit"] == 20
    assert data["has_more"] is False


@pytest.mark.asyncio
async def test_create_and_fetch_news(client: AsyncClient) -> None:
    """Test creating sources and articles via API, then querying with pagination."""
    # 1. Create Source
    source_payload = {
        "id": "bbc",
        "name": "BBC News",
        "rss_url": "https://feeds.bbci.co.uk/news/rss.xml",
        "home_url": "https://www.bbc.com/news",
        "is_active": True,
    }
    src_res = await client.post("/api/v1/news/sources", json=source_payload)
    assert src_res.status_code == 201
    assert src_res.json()["id"] == "bbc"

    # 2. Create Article
    now_str = datetime.now(timezone.utc).isoformat()
    article_payload = {
        "title": "Global Accord on Renewables Signed in Geneva",
        "source_id": "bbc",
        "source_name": "BBC News",
        "published_at": now_str,
        "original_url": "https://www.bbc.com/news/world-climate-2026",
        "category": "world",
        "location_name": "Geneva, Switzerland",
        "latitude": 46.2044,
        "longitude": 6.1432,
    }
    art_res = await client.post("/api/v1/news", json=article_payload)
    assert art_res.status_code == 201
    article_data = art_res.json()
    article_id = article_data["id"]
    assert article_data["title"] == article_payload["title"]

    # 3. Fetch single article by ID
    get_res = await client.get(f"/api/v1/news/{article_id}")
    assert get_res.status_code == 200
    assert get_res.json()["id"] == article_id
    assert get_res.json()["category"] == "world"

    # 4. List news
    list_res = await client.get("/api/v1/news?page=1&limit=10")
    assert list_res.status_code == 200
    list_data = list_res.json()
    assert list_data["total"] == 1
    assert len(list_data["items"]) == 1
    assert list_data["items"][0]["id"] == article_id


@pytest.mark.asyncio
async def test_news_not_found(client: AsyncClient) -> None:
    """Test fetching nonexistent article returns standard 404 error envelope."""
    response = await client.get("/api/v1/news/nonexistent-uuid-12345")
    assert response.status_code == 404
    data = response.json()
    assert "error" in data
    assert data["error"]["code"] == "RESOURCE_NOT_FOUND"
    assert "does not exist" in data["error"]["message"]


@pytest.mark.asyncio
async def test_news_invalid_parameters(client: AsyncClient) -> None:
    """Test input validation for invalid query parameters (e.g. page=0)."""
    response = await client.get("/api/v1/news?page=0")
    assert response.status_code == 422
