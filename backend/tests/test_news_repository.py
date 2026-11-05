"""Unit tests for NewsRepository database operations."""

from datetime import datetime, timezone

import pytest
from sqlalchemy.exc import IntegrityError
from sqlalchemy.ext.asyncio import AsyncSession

from app.repositories.news_repository import NewsRepository
from app.schemas.news import NewsArticleCreate, NewsSourceCreate


@pytest.mark.asyncio
async def test_create_and_get_news_source(db_session: AsyncSession) -> None:
    """Test creating and retrieving a news source entity."""
    repo = NewsRepository(db_session)
    source_in = NewsSourceCreate(
        id="bbc",
        name="BBC News",
        rss_url="http://feeds.bbci.co.uk/news/rss.xml",
        home_url="https://www.bbc.com/news",
        is_active=True,
    )
    source = await repo.create_source(source_in)
    assert source.id == "bbc"
    assert source.name == "BBC News"

    fetched = await repo.get_source("bbc")
    assert fetched is not None
    assert fetched.id == "bbc"


@pytest.mark.asyncio
async def test_create_and_list_news_articles(
    db_session: AsyncSession,
) -> None:
    """Test creating news articles and querying with filters and pagination."""
    repo = NewsRepository(db_session)

    # First insert source
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

    # Insert 3 articles with different categories and sources
    a1 = await repo.create_article(
        NewsArticleCreate(
            title="Global Climate Treaty Approved in Geneva",
            source_id="bbc",
            source_name="BBC News",
            published_at=now,
            original_url="https://bbc.com/news/1",
            category="world",
            location_name="Geneva, Switzerland",
            latitude=46.2044,
            longitude=6.1432,
        )
    )

    a2 = await repo.create_article(
        NewsArticleCreate(
            title="Quantum Processor Reaches Commercial Scale",
            source_id="cnn",
            source_name="CNN Tech",
            published_at=now,
            original_url="https://cnn.com/tech/1",
            category="technology",
            location_name="Tokyo, Japan",
            latitude=35.6762,
            longitude=139.6503,
        )
    )

    # List all
    items, total = await repo.list_articles(page=1, limit=10)
    assert total == 2
    assert len(items) == 2

    # Filter by category
    tech_items, tech_total = await repo.list_articles(
        page=1, limit=10, category="technology"
    )
    assert tech_total == 1
    assert tech_items[0].id == a2.id

    # Filter by source
    bbc_items, bbc_total = await repo.list_articles(page=1, limit=10, source="bbc")
    assert bbc_total == 1
    assert bbc_items[0].id == a1.id

    # Keyword search
    search_items, search_total = await repo.list_articles(
        page=1, limit=10, keyword="Climate"
    )
    assert search_total == 1
    assert search_items[0].id == a1.id


@pytest.mark.asyncio
async def test_duplicate_url_rejected(db_session: AsyncSession) -> None:
    """Verify that duplicate original_url triggers an IntegrityError."""
    repo = NewsRepository(db_session)
    await repo.create_source(
        NewsSourceCreate(
            id="bbc",
            name="BBC",
            rss_url="https://bbc.com/rss",
            home_url="https://bbc.com",
        )
    )

    article = NewsArticleCreate(
        title="Breaking News Duplicate Test",
        source_id="bbc",
        source_name="BBC News",
        published_at=datetime.now(timezone.utc),
        original_url="https://bbc.com/news/unique-test-1",
        category="world",
    )
    await repo.create_article(article)

    # Attempt inserting identical URL
    with pytest.raises(IntegrityError):
        await repo.create_article(article)
