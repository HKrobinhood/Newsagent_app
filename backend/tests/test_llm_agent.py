"""Tests for Milestone M6: LLM-based News Semantic Retrieval Agent."""

from datetime import datetime, timedelta, timezone

import httpx
import pytest
from sqlalchemy.ext.asyncio import AsyncSession

from app.repositories.news_repository import NewsRepository
from app.schemas.news import NewsArticleCreate, NewsSourceCreate
from app.services.intent_parser import IntentParser
from app.services.llm_client import SYSTEM_PROMPT, MockLLMClient


def test_intent_parser_relative_24h_ai() -> None:
    """Verify intent parser extracts 24h threshold and AI category."""
    now = datetime(2026, 11, 3, 12, 0, 0, tzinfo=timezone.utc)
    prompt = "最近 24 小时 AI 新闻"

    args = IntentParser.parse_intent(prompt, reference_time=now)
    assert args["category"] == "technology"
    assert "start_time" in args
    expected_start = (now - timedelta(hours=24)).isoformat()
    assert args["start_time"] == expected_start
    assert "ai" in args["query"].lower()


def test_intent_parser_source_and_world_category() -> None:
    """Verify intent parser detects media source (BBC) and category (world)."""
    now = datetime(2026, 11, 3, 12, 0, 0, tzinfo=timezone.utc)
    prompt = "今天 BBC 关于日内瓦峰会的国际新闻"

    args = IntentParser.parse_intent(prompt, reference_time=now)
    assert args["source"] == "bbc"
    assert args["category"] == "world"
    assert "日内瓦峰会" in args["query"]
    assert "start_time" in args


def test_intent_parser_business_and_fox() -> None:
    """Verify business category and fox source extraction."""
    prompt = "What did Fox report on inflation and central banks?"
    args = IntentParser.parse_intent(prompt)
    assert args["source"] == "fox"
    assert args["category"] == "business"
    assert "inflation" in args["query"] or "central banks" in args["query"]


@pytest.mark.asyncio
async def test_mock_llm_tool_calling() -> None:
    """Verify MockLLMClient parses prompt into search_news_database tool call."""
    client = MockLLMClient()
    tool_name, tool_args = await client.parse_and_call_tool(
        "查询最近24小时科技芯片新闻", SYSTEM_PROMPT, []
    )
    assert tool_name == "search_news_database"
    assert tool_args["category"] == "technology"
    assert "start_time" in tool_args


@pytest.mark.asyncio
async def test_grounded_answer_synthesis_with_articles() -> None:
    """Verify grounded answer includes exact headlines and source attribution."""
    client = MockLLMClient()
    search_results = [
        {
            "title": "Quantum Photonics Breakthrough in Tokyo",
            "source_name": "CNN Tech",
            "location_name": "Tokyo, Japan",
            "original_url": "https://cnn.com/tech/quantum",
        }
    ]
    answer = await client.generate_grounded_answer(
        "Quantum progress", search_results, SYSTEM_PROMPT
    )
    assert "Quantum Photonics Breakthrough in Tokyo" in answer
    assert "CNN Tech" in answer
    assert "Tokyo, Japan" in answer
    assert "严格基于数据库收录" in answer


@pytest.mark.asyncio
async def test_grounded_answer_synthesis_empty_results() -> None:
    """Verify strict refusal when search results are empty."""
    client = MockLLMClient()
    answer = await client.generate_grounded_answer("Random topic", [], SYSTEM_PROMPT)
    assert answer == "未检索到相关新闻，数据库中暂无匹配的报道。"


@pytest.mark.asyncio
async def test_repository_time_filtering(db_session: AsyncSession) -> None:
    """Verify NewsRepository filters articles by published_at time bounds."""
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
    # Article 1: published 2 hours ago
    await repo.create_article(
        NewsArticleCreate(
            title="Recent AI Chip Innovation",
            source_id="bbc",
            source_name="BBC News",
            published_at=now - timedelta(hours=2),
            original_url="https://bbc.com/news/chip-2h",
            category="technology",
            location_name="London",
        )
    )
    # Article 2: published 36 hours ago
    await repo.create_article(
        NewsArticleCreate(
            title="Old Technology Review",
            source_id="bbc",
            source_name="BBC News",
            published_at=now - timedelta(hours=36),
            original_url="https://bbc.com/news/old-36h",
            category="technology",
            location_name="London",
        )
    )

    # Filter with start_time = now - 24h
    articles_24h, total_24h = await repo.list_articles(
        category="technology",
        start_time=now - timedelta(hours=24),
    )
    assert total_24h == 1
    assert articles_24h[0].title == "Recent AI Chip Innovation"


@pytest.mark.asyncio
async def test_chat_api_m6_end_to_end_search(
    client: httpx.AsyncClient, db_session: AsyncSession
) -> None:
    """Verify end-to-end API call resolves time, category, and grounds answer."""
    repo = NewsRepository(db_session)
    await repo.create_source(
        NewsSourceCreate(
            id="cnn",
            name="CNN Tech",
            rss_url="https://cnn.com/rss",
            home_url="https://cnn.com",
        )
    )

    now = datetime.now(timezone.utc)
    seeded_url = "https://edition.cnn.com/tech/ai-datacenter-munich"
    await repo.create_article(
        NewsArticleCreate(
            title="Next-Generation AI Datacenter Deployed in Munich",
            source_id="cnn",
            source_name="CNN Tech",
            published_at=now - timedelta(hours=4),
            original_url=seeded_url,
            category="technology",
            location_name="Munich, Germany",
        )
    )

    payload = {
        "message": "最近24小时关于人工智能的报道",
        "mode": "title_search",
    }
    response = await client.post("/api/v1/ai/chat", json=payload)
    assert response.status_code == 200
    data = response.json()

    assert data["mode_used"] == "title_search"
    assert len(data["sources"]) == 1
    assert data["sources"][0]["original_url"] == seeded_url
    assert "Next-Generation AI Datacenter" in data["sources"][0]["title"]
    assert "Munich" in data["answer"]


@pytest.mark.asyncio
async def test_chat_api_m6_unmatched_returns_strict_refusal(
    client: httpx.AsyncClient,
) -> None:
    """Verify searching for nonexistent news returns '未检索到相关新闻'."""
    payload = {
        "message": "查询关于火星地下冰川开采的新闻报道",
        "mode": "title_search",
    }
    response = await client.post("/api/v1/ai/chat", json=payload)
    assert response.status_code == 200
    data = response.json()

    assert data["mode_used"] == "title_search"
    assert data["sources"] == []
    assert "未检索到相关新闻" in data["answer"]
