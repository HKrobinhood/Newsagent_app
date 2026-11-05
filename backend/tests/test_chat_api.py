"""Integration tests for AI Agent conversational chat REST endpoint."""

from datetime import datetime, timezone

import httpx
import pytest
from sqlalchemy.ext.asyncio import AsyncSession

from app.repositories.news_repository import NewsRepository
from app.schemas.news import NewsArticleCreate, NewsSourceCreate


@pytest.mark.asyncio
async def test_chat_valid_prompt(client: httpx.AsyncClient) -> None:
    """Verify chat prompt returns structured response with session and sources."""
    payload = {
        "message": "What is the latest status of the global climate summit?",
        "mode": "auto",
    }
    response = await client.post("/api/v1/ai/chat", json=payload)
    assert response.status_code == 200
    data = response.json()

    assert "session_id" in data
    assert len(data["session_id"]) > 0
    assert "answer" in data
    assert "Geneva" in data["answer"]
    assert data["mode_used"] in ("title_search", "web_verification")
    assert isinstance(data["sources"], list)
    assert len(data["sources"]) > 0
    assert data["confidence"] > 0.8
    assert "timestamp" in data


@pytest.mark.asyncio
async def test_chat_session_continuity(client: httpx.AsyncClient) -> None:
    """Verify that providing session_id preserves session identity across turns."""
    custom_session_id = "test-session-uuid-12345"
    payload = {
        "message": "Tell me about quantum computing advancements",
        "session_id": custom_session_id,
        "mode": "auto",
    }
    response = await client.post("/api/v1/ai/chat", json=payload)
    assert response.status_code == 200
    data = response.json()

    assert data["session_id"] == custom_session_id
    assert "quantum" in data["answer"].lower()


@pytest.mark.asyncio
async def test_chat_blank_message_rejected(client: httpx.AsyncClient) -> None:
    """Verify that empty or whitespace prompts are rejected with 400."""
    response = await client.post("/api/v1/ai/chat", json={"message": "    "})
    assert response.status_code == 400
    assert "cannot be blank" in response.text


@pytest.mark.asyncio
async def test_chat_retrieves_database_articles(
    client: httpx.AsyncClient, db_session: AsyncSession
) -> None:
    """Verify that agent detects database items and includes them in citations."""
    repo = NewsRepository(db_session)
    await repo.create_source(
        NewsSourceCreate(
            id="bbc",
            name="BBC News",
            rss_url="https://bbc.com/rss",
            home_url="https://bbc.com",
        )
    )
    await repo.create_article(
        NewsArticleCreate(
            title="Global Semiconductor Alliance Formed in Munich",
            source_id="bbc",
            source_name="BBC News",
            published_at=datetime.now(timezone.utc),
            original_url="https://bbc.com/news/semiconductor-munich",
            category="technology",
            location_name="Munich, Germany",
            latitude=48.1351,
            longitude=11.5820,
        )
    )

    payload = {"message": "Tell me about the semiconductor alliance"}
    response = await client.post("/api/v1/ai/chat", json=payload)
    assert response.status_code == 200
    data = response.json()

    assert data["mode_used"] == "title_search"
    assert any("semiconductor" in s["title"].lower() for s in data["sources"])
