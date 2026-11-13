"""Media retrieval and thumbnail serving REST API endpoints."""

from fastapi import APIRouter, Depends, HTTPException, Response, status
from fastapi.responses import RedirectResponse
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.repositories.news_repository import NewsRepository
from app.schemas.news import ArticleMediaResponse

router = APIRouter()


@router.get(
    "/{media_id}",
    response_model=ArticleMediaResponse,
    summary="Get media metadata by ID",
)
async def get_media_metadata(
    media_id: str,
    db: AsyncSession = Depends(get_db),
) -> ArticleMediaResponse:
    """Fetch metadata for a single news article media attachment."""
    repo = NewsRepository(db)
    media = await repo.get_media(media_id)
    if not media:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Media item with ID '{media_id}' not found",
        )
    return ArticleMediaResponse.model_validate(media)


@router.get(
    "/{media_id}/thumbnail",
    summary="Serve thumbnail or redirect to upstream image URL",
)
async def get_media_thumbnail(
    media_id: str,
    db: AsyncSession = Depends(get_db),
) -> Response:
    """Return stored thumbnail bytes if available, or redirect to image URL."""
    repo = NewsRepository(db)
    media = await repo.get_media(media_id)
    if not media:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Media item with ID '{media_id}' not found",
        )

    if media.thumbnail_data:
        return Response(
            content=media.thumbnail_data,
            media_type=media.mime_type or "image/jpeg",
        )

    return RedirectResponse(
        url=str(media.image_url),
        status_code=status.HTTP_307_TEMPORARY_REDIRECT,
    )
