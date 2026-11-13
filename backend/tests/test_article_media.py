"""Automated tests for ArticleMedia model, RSS extraction, and media API endpoints."""

from datetime import datetime, timezone

import pytest
from httpx import AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession

from app.repositories.news_repository import NewsRepository
from app.schemas.news import (
    ArticleMediaCreate,
    NewsArticleCreate,
    NewsSourceCreate,
)
from app.services.collector_service import NewsCollectorService
from app.services.collectors.base import BaseCollector


@pytest.mark.asyncio
async def test_article_media_model_and_cascade_delete(
    db_session: AsyncSession,
) -> None:
    """Test ArticleMedia persistence, primary_media property, and cascade deletion."""
    repo = NewsRepository(db_session)

    # 1. Setup Source and Article
    await repo.create_source(
        NewsSourceCreate(
            id="lemon_news",
            name="Lemon News",
            rss_url="https://example.com/rss.xml",
            home_url="https://example.com",
            is_active=True,
        )
    )

    now = datetime.now(timezone.utc)
    article = await repo.create_article(
        NewsArticleCreate(
            id="art-test-001",
            title="Editorial Redesign Launched with Modern Visual Hierarchy",
            source_id="lemon_news",
            source_name="Lemon News",
            published_at=now,
            original_url="https://example.com/editorial-redesign",
            category="design",
        )
    )

    # 2. Add two media records
    media1 = await repo.create_media(
        article.id,
        ArticleMediaCreate(
            id="media-001",
            image_url="https://images.example.com/editorial-cover.jpg",
            mime_type="image/jpeg",
            width=1600,
            height=900,
            image_source="lemon_news",
            credit="Jane Doe / Lemon News",
        ),
    )

    media2 = await repo.create_media(
        article.id,
        ArticleMediaCreate(
            id="media-002",
            image_url="https://images.example.com/editorial-detail.jpg",
            mime_type="image/jpeg",
            width=800,
            height=600,
            image_source="lemon_news",
            credit="Lemon Visuals",
        ),
    )

    # 3. Retrieve article and verify relationships
    fetched_article = await repo.get_article(article.id)
    assert fetched_article is not None
    assert len(fetched_article.media_items) == 2
    assert fetched_article.primary_media is not None
    assert fetched_article.primary_media.id == media1.id
    assert (
        fetched_article.primary_media.thumbnail_url
        == f"/api/v1/media/{media1.id}/thumbnail"
    )

    # 4. Verify cascade delete: removing article cascades to its media records
    await db_session.delete(fetched_article)
    await db_session.commit()

    media_check = await repo.get_media(media1.id)
    assert media_check is None
    media_check_2 = await repo.get_media(media2.id)
    assert media_check_2 is None


def test_rss_feed_media_extraction() -> None:
    """Test extracting media elements from RSS media tags."""

    class MockCollector(BaseCollector):
        source_id = "test_feed"
        source_name = "Test Feed"
        rss_url = "https://example.com/feed.xml"
        home_url = "https://example.com"

    sample_rss_xml = """<?xml version="1.0" encoding="UTF-8"?>
    <rss version="2.0" xmlns:media="http://search.yahoo.com/mrss/">
      <channel>
        <title>Test Feed Channel</title>
        <link>https://example.com</link>
        <item>
          <title>Media Content Article in London</title>
          <link>https://example.com/article-1</link>
          <pubDate>Mon, 12 Nov 2026 12:00:00 GMT</pubDate>
          <category>world</category>
          <media:content
            url="https://images.example.com/media1.jpg"
            type="image/jpeg"
            medium="image"
            width="1280"
            height="720">
            <media:credit>Reuters / Photo</media:credit>
          </media:content>
        </item>
        <item>
          <title>Thumbnail Article in Tokyo</title>
          <link>https://example.com/article-2</link>
          <pubDate>Mon, 12 Nov 2026 13:00:00 GMT</pubDate>
          <category>tech</category>
          <media:thumbnail
            url="https://images.example.com/thumb2.png"
            width="400"
            height="300" />
        </item>
        <item>
          <title>Enclosure Article in Paris</title>
          <link>https://example.com/article-3</link>
          <pubDate>Mon, 12 Nov 2026 14:00:00 GMT</pubDate>
          <enclosure
            url="https://images.example.com/enc3.jpg"
            type="image/jpeg"
            length="12345" />
        </item>
        <item>
          <title>Text Only Story in Berlin</title>
          <link>https://example.com/article-4</link>
          <pubDate>Mon, 12 Nov 2026 15:00:00 GMT</pubDate>
        </item>
      </channel>
    </rss>
    """

    collector = MockCollector()
    raw_articles = collector.parse_feed_content(sample_rss_xml)
    assert len(raw_articles) == 4

    # Article 1: media:content
    art1 = raw_articles[0]
    assert art1.image_url == "https://images.example.com/media1.jpg"
    assert art1.image_mime_type == "image/jpeg"
    assert art1.image_width == 1280
    assert art1.image_height == 720
    assert art1.image_credit == "Reuters / Photo"

    # Article 2: media:thumbnail
    art2 = raw_articles[1]
    assert art2.image_url == "https://images.example.com/thumb2.png"
    assert art2.image_width == 400
    assert art2.image_height == 300

    # Article 3: enclosure
    art3 = raw_articles[2]
    assert art3.image_url == "https://images.example.com/enc3.jpg"
    assert art3.image_mime_type == "image/jpeg"

    # Article 4: text-only
    art4 = raw_articles[3]
    assert art4.image_url is None
    assert art4.image_credit is None


@pytest.mark.asyncio
async def test_collector_service_persists_media(
    db_session: AsyncSession,
) -> None:
    """Test collector service saves ArticleMedia alongside NewsArticle."""

    class MockCollectorWithMedia(BaseCollector):
        source_id = "lemon_media"
        source_name = "Lemon Media"
        rss_url = "https://example.com/media.xml"
        home_url = "https://example.com"

        def parse_feed_content(self, xml_content: str):
            from app.services.collectors.base import RawArticleData

            return [
                RawArticleData(
                    title="Seaside Complex Architecture in Shanghai",
                    source_id=self.source_id,
                    source_name=self.source_name,
                    published_at=datetime.now(timezone.utc),
                    original_url="https://example.com/seaside-complex-arch",
                    category="culture",
                    location_name="Shanghai",
                    latitude=31.2304,
                    longitude=121.4737,
                    image_url="https://images.example.com/seaside.jpg",
                    image_credit="Lemon Photography",
                    image_width=1920,
                    image_height=1080,
                    image_mime_type="image/jpeg",
                )
            ]

    collector = MockCollectorWithMedia()
    service = NewsCollectorService(collectors=[collector])

    class DummyClient:
        async def get(self, *args, **kwargs):
            class DummyResponse:
                text = "<xml></xml>"

                def raise_for_status(self):
                    pass

            return DummyResponse()

    res = await service.collect_from_source(collector, db_session, DummyClient())  # type: ignore[arg-type]
    assert res.success is True
    assert res.inserted_count == 1

    repo = NewsRepository(db_session)
    art = await repo.get_article_by_url("https://example.com/seaside-complex-arch")
    assert art is not None
    assert len(art.media_items) == 1
    media = art.media_items[0]
    assert media.image_url == "https://images.example.com/seaside.jpg"
    assert media.credit == "Lemon Photography"
    assert media.width == 1920
    assert media.height == 1080


@pytest.mark.asyncio
async def test_news_api_media_presentation_and_backwards_compatibility(
    client: AsyncClient,
    db_session: AsyncSession,
) -> None:
    """Test GET /api/v1/news backwards compatibility and media format."""
    repo = NewsRepository(db_session)

    # 1. Create Source
    await repo.create_source(
        NewsSourceCreate(
            id="bbc_media",
            name="BBC Media",
            rss_url="https://feeds.bbci.co.uk/news/rss.xml",
            home_url="https://www.bbc.com/news",
            is_active=True,
        )
    )

    now = datetime.now(timezone.utc)

    # 2. Create Article with media
    art_with_media = await repo.create_article(
        NewsArticleCreate(
            id="art-with-media-1",
            title="Clean Energy Milestone Across Europe",
            source_id="bbc_media",
            source_name="BBC Media",
            published_at=now,
            original_url="https://www.bbc.com/news/clean-energy-1",
            category="climate",
        )
    )
    await repo.create_media(
        art_with_media.id,
        ArticleMediaCreate(
            id="media-item-1",
            image_url="https://images.bbci.co.uk/clean-energy.jpg",
            mime_type="image/jpeg",
            width=1200,
            height=675,
            credit="BBC Photo Library",
        ),
    )

    # 3. Create Article without media (Text-only degradation)
    await repo.create_article(
        NewsArticleCreate(
            id="art-without-media-2",
            title="Central Bank Policy Rate Statement",
            source_id="bbc_media",
            source_name="BBC Media",
            published_at=now,
            original_url="https://www.bbc.com/news/bank-statement-2",
            category="business",
        )
    )

    # 4. Fetch /api/v1/news
    response = await client.get("/api/v1/news")
    assert response.status_code == 200
    data = response.json()
    assert data["total"] == 2
    assert len(data["items"]) == 2

    # Verify Article with Media
    item_with_media = next(
        item for item in data["items"] if item["id"] == "art-with-media-1"
    )
    # Check backward compatibility fields exist
    assert "id" in item_with_media
    assert "title" in item_with_media
    assert "source_id" in item_with_media
    assert "source_name" in item_with_media
    assert "published_at" in item_with_media
    assert "original_url" in item_with_media
    assert "category" in item_with_media
    assert "location_name" in item_with_media
    assert "latitude" in item_with_media
    assert "longitude" in item_with_media
    assert "created_at" in item_with_media

    # Check N2 media payload
    media_obj = item_with_media["media"]
    assert media_obj is not None
    assert media_obj["id"] == "media-item-1"
    assert media_obj["article_id"] == "art-with-media-1"
    assert media_obj["image_url"] == "https://images.bbci.co.uk/clean-energy.jpg"
    assert media_obj["thumbnail_url"] == "/api/v1/media/media-item-1/thumbnail"
    assert media_obj["width"] == 1200
    assert media_obj["height"] == 675
    assert media_obj["credit"] == "BBC Photo Library"
    assert len(item_with_media["media_list"]) == 1

    # Verify Text-Only Article (Degradation)
    item_no_media = next(
        item for item in data["items"] if item["id"] == "art-without-media-2"
    )
    assert item_no_media["media"] is None
    assert item_no_media["media_list"] == []


@pytest.mark.asyncio
async def test_media_endpoints(
    client: AsyncClient,
    db_session: AsyncSession,
) -> None:
    """Test standalone GET /api/v1/media endpoints and thumbnail serving."""
    repo = NewsRepository(db_session)

    # Setup source and article
    await repo.create_source(
        NewsSourceCreate(
            id="src_media_ep",
            name="Source Media",
            rss_url="https://example.com/rss",
            home_url="https://example.com",
            is_active=True,
        )
    )
    art = await repo.create_article(
        NewsArticleCreate(
            id="art-ep-1",
            title="Test Media Endpoints Article",
            source_id="src_media_ep",
            source_name="Source Media",
            published_at=datetime.now(timezone.utc),
            original_url="https://example.com/art-ep-1",
            category="tech",
        )
    )

    # 1. Media item with URL only (no local binary thumbnail)
    media_url_only = await repo.create_media(
        art.id,
        ArticleMediaCreate(
            id="med-ep-url",
            image_url="https://images.example.com/test-url.jpg",
            mime_type="image/jpeg",
            width=1000,
            height=600,
        ),
    )

    # 2. Media item with thumbnail_data binary
    dummy_bytes = b"\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR"
    media_binary = await repo.create_media(
        art.id,
        ArticleMediaCreate(
            id="med-ep-bin",
            image_url="https://images.example.com/test-bin.png",
            thumbnail_data=dummy_bytes,
            mime_type="image/png",
            width=200,
            height=200,
        ),
    )

    # Test GET /api/v1/media/{media_id}
    res = await client.get(f"/api/v1/media/{media_url_only.id}")
    assert res.status_code == 200
    meta = res.json()
    assert meta["id"] == "med-ep-url"
    assert meta["image_url"] == "https://images.example.com/test-url.jpg"
    assert meta["thumbnail_url"] == f"/api/v1/media/{media_url_only.id}/thumbnail"

    # Test GET /api/v1/media/{media_id} for unknown ID
    res_404 = await client.get("/api/v1/media/non-existent-media-id")
    assert res_404.status_code == 404

    # Test GET /api/v1/media/{media_id}/thumbnail with URL (307 redirect)
    res_thumb_redirect = await client.get(
        f"/api/v1/media/{media_url_only.id}/thumbnail",
        follow_redirects=False,
    )
    assert res_thumb_redirect.status_code == 307
    assert (
        res_thumb_redirect.headers["location"]
        == "https://images.example.com/test-url.jpg"
    )

    # Test GET /api/v1/media/{media_id}/thumbnail with binary bytes
    res_thumb_bin = await client.get(
        f"/api/v1/media/{media_binary.id}/thumbnail",
        follow_redirects=False,
    )
    assert res_thumb_bin.status_code == 200
    assert res_thumb_bin.headers["content-type"] == "image/png"
    assert res_thumb_bin.content == dummy_bytes

    # Test GET /api/v1/media/{media_id}/thumbnail for unknown ID
    res_thumb_404 = await client.get(
        "/api/v1/media/non-existent-media-id/thumbnail",
        follow_redirects=False,
    )
    assert res_thumb_404.status_code == 404
