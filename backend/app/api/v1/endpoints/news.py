"""News metadata REST API endpoints."""

from fastapi import APIRouter, Depends, HTTPException, Query, status
from fastapi.responses import JSONResponse
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.repositories.news_repository import NewsRepository
from app.schemas.geo import (
    GeoFeature,
    GeoFeatureCollection,
    GeoGeometry,
    GeoProperties,
)
from app.schemas.news import (
    ErrorResponse,
    NewsArticleCreate,
    NewsArticleResponse,
    NewsListResponse,
    NewsSourceCreate,
    NewsSourceResponse,
)
from app.services.collector_service import NewsCollectorService

router = APIRouter()


@router.post(
    "/collect",
    summary="Trigger on-demand collection across all RSS sources",
)
async def trigger_collection(
    db: AsyncSession = Depends(get_db),
) -> dict:
    """Manually trigger RSS feed collection and deduplication."""
    collector_service = NewsCollectorService()
    results = await collector_service.collect_all(db)
    return {
        "status": "completed",
        "sources": [
            {
                "source_id": r.source_id,
                "source_name": r.source_name,
                "success": r.success,
                "fetched_count": r.fetched_count,
                "inserted_count": r.inserted_count,
                "duplicate_count": r.duplicate_count,
                "error_message": r.error_message,
            }
            for r in results
        ],
    }


@router.get(
    "",
    response_model=NewsListResponse,
    summary="List news articles with pagination and filtering",
)
async def list_news(
    page: int = Query(1, ge=1, description="Page number"),
    limit: int = Query(20, ge=1, le=100, description="Items per page"),
    category: str | None = Query(None, description="Category filter"),
    source: str | None = Query(None, description="Source ID filter"),
    keyword: str | None = Query(None, description="Keyword search in title"),
    db: AsyncSession = Depends(get_db),
) -> NewsListResponse:
    """Retrieve paginated list of news articles."""
    repo = NewsRepository(db)
    items, total = await repo.list_articles(
        page=page,
        limit=limit,
        category=category,
        source=source,
        keyword=keyword,
    )
    has_more = (page * limit) < total
    return NewsListResponse(
        items=[NewsArticleResponse.model_validate(item) for item in items],
        total=total,
        page=page,
        limit=limit,
        has_more=has_more,
    )


@router.get(
    "/geo",
    response_model=GeoFeatureCollection,
    summary="Get geolocated news hotspots in GeoJSON format",
)
async def get_geolocated_news(
    category: str | None = Query(None, description="Category filter"),
    limit: int = Query(100, ge=1, le=500, description="Max items to return"),
    db: AsyncSession = Depends(get_db),
) -> GeoFeatureCollection:
    """Retrieve geolocated news articles as standard GeoJSON FeatureCollection."""
    repo = NewsRepository(db)
    articles = await repo.list_geolocated_articles(category=category, limit=limit)
    features = [
        GeoFeature(
            geometry=GeoGeometry(coordinates=[a.longitude, a.latitude]),
            properties=GeoProperties(
                id=a.id,
                title=a.title,
                source_id=a.source_id,
                source_name=a.source_name,
                published_at=a.published_at.isoformat(),
                original_url=a.original_url,
                category=a.category,
                location_name=a.location_name,
            ),
        )
        for a in articles
        if a.longitude is not None and a.latitude is not None
    ]
    return GeoFeatureCollection(features=features)


@router.get(
    "/{id}",
    response_model=NewsArticleResponse,
    responses={404: {"model": ErrorResponse}},
    summary="Get single news article by ID",
)
async def get_news_article(
    id: str,
    db: AsyncSession = Depends(get_db),
) -> NewsArticleResponse:
    """Fetch article metadata by unique ID."""
    repo = NewsRepository(db)
    article = await repo.get_article(id)
    if not article:
        return JSONResponse(
            status_code=status.HTTP_404_NOT_FOUND,
            content={
                "error": {
                    "code": "RESOURCE_NOT_FOUND",
                    "message": "The requested news item does not exist.",
                }
            },
        )
    return NewsArticleResponse.model_validate(article)


@router.post(
    "",
    response_model=NewsArticleResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Create news article metadata (internal/collector/seed)",
)
async def create_news_article(
    article_in: NewsArticleCreate,
    db: AsyncSession = Depends(get_db),
) -> NewsArticleResponse:
    """Insert a new article metadata record."""
    repo = NewsRepository(db)
    existing = await repo.get_article_by_url(article_in.original_url)
    if existing:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Article with this URL already exists.",
        )
    article = await repo.create_article(article_in)
    return NewsArticleResponse.model_validate(article)


@router.post(
    "/sources",
    response_model=NewsSourceResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Create news source definition",
)
async def create_news_source(
    source_in: NewsSourceCreate,
    db: AsyncSession = Depends(get_db),
) -> NewsSourceResponse:
    """Insert a new news source."""
    repo = NewsRepository(db)
    existing = await repo.get_source(source_in.id)
    if existing:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail=f"Source with id '{source_in.id}' already exists.",
        )
    source = await repo.create_source(source_in)
    return NewsSourceResponse.model_validate(source)
