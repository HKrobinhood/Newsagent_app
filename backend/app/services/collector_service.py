"""Service orchestrating RSS news ingestion, deduplication, and persistence."""

import logging
from dataclasses import dataclass
from datetime import datetime, timezone

import httpx
from sqlalchemy.ext.asyncio import AsyncSession

from app.repositories.news_repository import NewsRepository
from app.schemas.news import ArticleMediaCreate, NewsArticleCreate, NewsSourceCreate
from app.services.collectors import ALL_COLLECTORS
from app.services.collectors.base import BaseCollector

logger = logging.getLogger(__name__)


@dataclass
class CollectorResult:
    """Outcome metrics for a single news source collection run."""

    source_id: str
    source_name: str
    success: bool
    fetched_count: int = 0
    inserted_count: int = 0
    duplicate_count: int = 0
    error_message: str | None = None


class NewsCollectorService:
    """Manages execution of RSS collectors with URL deduplication and state tracking."""

    def __init__(self, collectors: list[BaseCollector] | None = None) -> None:
        """Initialize with collectors list."""
        self.collectors = collectors or ALL_COLLECTORS

    async def ensure_source_registered(
        self, collector: BaseCollector, repo: NewsRepository
    ) -> None:
        """Verify the media source entity exists in the database or register it."""
        existing = await repo.get_source(collector.source_id)
        if not existing:
            await repo.create_source(
                NewsSourceCreate(
                    id=collector.source_id,
                    name=collector.source_name,
                    rss_url=collector.rss_url,
                    home_url=collector.home_url,
                    is_active=True,
                )
            )

    async def collect_from_source(
        self,
        collector: BaseCollector,
        db: AsyncSession,
        client: httpx.AsyncClient,
    ) -> CollectorResult:
        """Run single collector pipeline, deduplicate by URL, and persist articles."""
        repo = NewsRepository(db)
        await self.ensure_source_registered(collector, repo)

        try:
            raw_articles = await collector.collect(client)
        except Exception as e:
            logger.error(
                "Collection failed for source %s: %s",
                collector.source_name,
                e,
                exc_info=True,
            )
            return CollectorResult(
                source_id=collector.source_id,
                source_name=collector.source_name,
                success=False,
                error_message=str(e),
            )

        inserted = 0
        duplicates = 0

        for raw in raw_articles:
            # Check unique constraint by original_url
            existing = await repo.get_article_by_url(raw.original_url)
            if existing:
                duplicates += 1
                continue

            article_in = NewsArticleCreate(
                title=raw.title,
                source_id=raw.source_id,
                source_name=raw.source_name,
                published_at=raw.published_at,
                original_url=raw.original_url,
                category=raw.category,
                location_name=raw.location_name,
                latitude=raw.latitude,
                longitude=raw.longitude,
            )
            try:
                created_article = await repo.create_article(article_in)
                inserted += 1
                if raw.image_url:
                    try:
                        await repo.create_media(
                            created_article.id,
                            ArticleMediaCreate(
                                image_url=raw.image_url,
                                mime_type=raw.image_mime_type,
                                width=raw.image_width,
                                height=raw.image_height,
                                image_source=raw.source_id,
                                credit=raw.image_credit,
                            ),
                        )
                    except Exception as media_err:
                        logger.warning(
                            "Failed persisting media for article '%s': %s",
                            raw.title,
                            media_err,
                        )
            except Exception as e:
                logger.warning("Failed inserting article '%s': %s", raw.title, e)
                duplicates += 1

        # Update last_polled timestamp on source
        source = await repo.get_source(collector.source_id)
        if source:
            source.last_polled = datetime.now(timezone.utc)
            await db.commit()

        logger.info(
            "Source %s finished: %d fetched, %d inserted, %d duplicates",
            collector.source_name,
            len(raw_articles),
            inserted,
            duplicates,
        )

        return CollectorResult(
            source_id=collector.source_id,
            source_name=collector.source_name,
            success=True,
            fetched_count=len(raw_articles),
            inserted_count=inserted,
            duplicate_count=duplicates,
        )

    async def collect_all(
        self, db: AsyncSession, client: httpx.AsyncClient | None = None
    ) -> list[CollectorResult]:
        """Trigger collection across all registered media sources."""
        results: list[CollectorResult] = []

        if client is not None:
            for collector in self.collectors:
                res = await self.collect_from_source(collector, db, client)
                results.append(res)
        else:
            async with httpx.AsyncClient() as new_client:
                for collector in self.collectors:
                    res = await self.collect_from_source(collector, db, new_client)
                    results.append(res)

        return results
