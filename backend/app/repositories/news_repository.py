"""Repository for database operations on NewsArticle and NewsSource."""

from datetime import datetime

from sqlalchemy import desc, func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.news import NewsArticle, NewsSource
from app.schemas.news import NewsArticleCreate, NewsSourceCreate


class NewsRepository:
    """Encapsulates data persistence logic for news articles and sources."""

    def __init__(self, session: AsyncSession) -> None:
        """Initialize repository with database session.

        Args:
            session: Active SQLAlchemy AsyncSession.
        """
        self.session = session

    async def create_source(self, source_in: NewsSourceCreate) -> NewsSource:
        """Create and persist a new news source."""
        source = NewsSource(
            id=source_in.id,
            name=source_in.name,
            rss_url=source_in.rss_url,
            home_url=source_in.home_url,
            is_active=source_in.is_active,
        )
        self.session.add(source)
        await self.session.commit()
        await self.session.refresh(source)
        return source

    async def get_source(self, source_id: str) -> NewsSource | None:
        """Fetch a news source by ID."""
        stmt = select(NewsSource).where(NewsSource.id == source_id)
        result = await self.session.execute(stmt)
        return result.scalar_one_or_none()

    async def list_sources(self, active_only: bool = False) -> list[NewsSource]:
        """Retrieve list of news sources."""
        stmt = select(NewsSource)
        if active_only:
            stmt = stmt.where(NewsSource.is_active.is_(True))
        result = await self.session.execute(stmt)
        return list(result.scalars().all())

    async def create_article(self, article_in: NewsArticleCreate) -> NewsArticle:
        """Create and persist a news article metadata record."""
        article = NewsArticle(
            title=article_in.title,
            source_id=article_in.source_id,
            source_name=article_in.source_name,
            published_at=article_in.published_at,
            original_url=article_in.original_url,
            category=article_in.category.lower(),
            location_name=article_in.location_name,
            latitude=article_in.latitude,
            longitude=article_in.longitude,
        )
        if article_in.id:
            article.id = article_in.id

        self.session.add(article)
        await self.session.commit()
        await self.session.refresh(article)
        return article

    async def get_article(self, article_id: str) -> NewsArticle | None:
        """Fetch a single news article by unique ID."""
        stmt = select(NewsArticle).where(NewsArticle.id == article_id)
        result = await self.session.execute(stmt)
        return result.scalar_one_or_none()

    async def get_article_by_url(self, url: str) -> NewsArticle | None:
        """Fetch article by unique original URL."""
        stmt = select(NewsArticle).where(NewsArticle.original_url == url)
        result = await self.session.execute(stmt)
        return result.scalar_one_or_none()

    async def list_articles(
        self,
        page: int = 1,
        limit: int = 20,
        category: str | None = None,
        source: str | None = None,
        source_ids: list[str] | None = None,
        keyword: str | None = None,
        start_time: datetime | None = None,
        end_time: datetime | None = None,
    ) -> tuple[list[NewsArticle], int]:
        """List articles with pagination, filtering and sorting by published_at DESC."""
        if source_ids is not None:
            normalized_sources = [
                s.strip().lower() for s in source_ids if s and s.strip()
            ]
            if not normalized_sources:
                return [], 0
        else:
            normalized_sources = None

        stmt = select(NewsArticle)
        count_stmt = select(func.count(NewsArticle.id))

        if category and category.lower() != "all":
            stmt = stmt.where(func.lower(NewsArticle.category) == category.lower())
            count_stmt = count_stmt.where(
                func.lower(NewsArticle.category) == category.lower()
            )

        if source:
            stmt = stmt.where(func.lower(NewsArticle.source_id) == source.lower())
            count_stmt = count_stmt.where(
                func.lower(NewsArticle.source_id) == source.lower()
            )

        if normalized_sources is not None:
            stmt = stmt.where(
                func.lower(NewsArticle.source_id).in_(normalized_sources)
            )
            count_stmt = count_stmt.where(
                func.lower(NewsArticle.source_id).in_(normalized_sources)
            )

        if keyword:
            like_pattern = f"%{keyword}%"
            stmt = stmt.where(NewsArticle.title.ilike(like_pattern))
            count_stmt = count_stmt.where(NewsArticle.title.ilike(like_pattern))

        if start_time:
            stmt = stmt.where(NewsArticle.published_at >= start_time)
            count_stmt = count_stmt.where(NewsArticle.published_at >= start_time)

        if end_time:
            stmt = stmt.where(NewsArticle.published_at <= end_time)
            count_stmt = count_stmt.where(NewsArticle.published_at <= end_time)

        # Count total matching rows
        total_result = await self.session.execute(count_stmt)
        total = total_result.scalar_one()

        # Apply ordering and pagination
        stmt = (
            stmt.order_by(desc(NewsArticle.published_at))
            .offset((page - 1) * limit)
            .limit(limit)
        )
        articles_result = await self.session.execute(stmt)
        articles = list(articles_result.scalars().all())

        return articles, total

    async def list_geolocated_articles(
        self,
        category: str | None = None,
        source_ids: list[str] | None = None,
        limit: int = 100,
    ) -> list[NewsArticle]:
        """List articles that have valid non-null latitude and longitude."""
        if source_ids is not None:
            normalized_sources = [
                s.strip().lower() for s in source_ids if s and s.strip()
            ]
            if not normalized_sources:
                return []
        else:
            normalized_sources = None

        stmt = (
            select(NewsArticle)
            .where(NewsArticle.latitude.is_not(None))
            .where(NewsArticle.longitude.is_not(None))
        )
        if category and category.lower() != "all":
            stmt = stmt.where(func.lower(NewsArticle.category) == category.lower())

        if normalized_sources is not None:
            stmt = stmt.where(
                func.lower(NewsArticle.source_id).in_(normalized_sources)
            )

        stmt = stmt.order_by(desc(NewsArticle.published_at)).limit(limit)
        result = await self.session.execute(stmt)
        return list(result.scalars().all())
