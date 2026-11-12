"""Pydantic schemas for news and error responses."""

from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field, model_validator


class ArticleMediaBase(BaseModel):
    """Base fields for an article media attachment."""

    media_type: str = Field(default="image", max_length=50, description="Media type")
    image_url: str = Field(..., max_length=1000, description="Canonical image URL")
    thumbnail_url: str | None = Field(
        default=None, max_length=500, description="Endpoint to load thumbnail"
    )
    mime_type: str | None = Field(
        default=None, max_length=100, description="MIME content type"
    )
    width: int | None = Field(default=None, ge=1, description="Image pixel width")
    height: int | None = Field(default=None, ge=1, description="Image pixel height")
    credit: str | None = Field(
        default=None, max_length=255, description="Image photo credit/attribution"
    )


class ArticleMediaCreate(ArticleMediaBase):
    """Schema for inserting a media record."""

    id: str | None = Field(default=None, description="Optional custom UUID")
    thumbnail_data: bytes | None = Field(
        default=None, description="Optional processed thumbnail binary"
    )
    storage_key: str | None = Field(
        default=None, max_length=255, description="Storage object key"
    )
    image_source: str | None = Field(
        default=None, max_length=100, description="Origin publisher domain"
    )
    license_status: str | None = Field(
        default="rss_fair_use", max_length=100, description="Content license status"
    )


class ArticleMediaResponse(ArticleMediaBase):
    """Response schema for article media metadata."""

    id: str
    article_id: str
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)

    @model_validator(mode="before")
    @classmethod
    def set_thumbnail_url(cls, data: object) -> object:
        """Ensure thumbnail_url is populated from id if missing."""
        if isinstance(data, dict):
            if not data.get("thumbnail_url") and data.get("id"):
                data["thumbnail_url"] = f"/api/v1/media/{data['id']}/thumbnail"
        return data


class ErrorDetail(BaseModel):
    """Detailed error object."""

    code: str = Field(..., description="Machine-readable error code")
    message: str = Field(..., description="Human-readable error description")


class ErrorResponse(BaseModel):
    """Standardized API error envelope."""

    error: ErrorDetail


class NewsSourceBase(BaseModel):
    """Base fields for a news source."""

    id: str = Field(..., max_length=50, description="Unique source identifier")
    name: str = Field(..., max_length=100, description="Display name")
    rss_url: str = Field(..., max_length=500, description="RSS feed URL")
    home_url: str = Field(..., max_length=500, description="Media homepage URL")
    is_active: bool = Field(default=True, description="Active polling flag")


class NewsSourceCreate(NewsSourceBase):
    """Schema for creating a news source."""

    pass


class NewsSourceResponse(NewsSourceBase):
    """Response schema for news source."""

    last_polled: datetime | None = None

    model_config = ConfigDict(from_attributes=True)


class NewsArticleBase(BaseModel):
    """Base fields for news article metadata."""

    title: str = Field(..., max_length=500, description="Headline of the article")
    source_id: str = Field(..., max_length=50, description="Source identifier")
    source_name: str = Field(..., max_length=100, description="Source display name")
    published_at: datetime = Field(..., description="Publication time in UTC")
    original_url: str = Field(..., max_length=1000, description="Original article URL")
    category: str = Field(
        ..., max_length=50, description="Category (world, tech, etc.)"
    )
    location_name: str | None = Field(
        None, max_length=100, description="Geographic entity"
    )
    latitude: float | None = Field(None, ge=-90.0, le=90.0, description="Latitude")
    longitude: float | None = Field(None, ge=-180.0, le=180.0, description="Longitude")


class NewsArticleCreate(NewsArticleBase):
    """Schema for inserting a new article metadata record."""

    id: str | None = Field(None, description="Optional custom UUID")


class NewsArticleResponse(NewsArticleBase):
    """Response schema for a single news article with optional media attachments."""

    id: str
    created_at: datetime
    media: ArticleMediaResponse | None = Field(
        default=None, description="Primary featured editorial media"
    )
    media_list: list[ArticleMediaResponse] = Field(
        default_factory=list, description="All associated media attachments"
    )

    model_config = ConfigDict(from_attributes=True)

    @model_validator(mode="before")
    @classmethod
    def populate_media_from_orm(cls, data: object) -> object:
        """Resolve media_items relationship from ORM entity into responses."""
        if hasattr(data, "media_items"):
            media_items = getattr(data, "media_items", []) or []
            res_list: list[dict] = []
            for item in media_items:
                thumb_ep = (
                    f"/api/v1/media/{getattr(item, 'id', '')}/thumbnail"
                    if getattr(item, "id", None)
                    else None
                )
                res_dict = {
                    "id": getattr(item, "id", None),
                    "article_id": getattr(item, "article_id", None),
                    "media_type": getattr(item, "media_type", "image"),
                    "image_url": getattr(item, "image_url", None),
                    "thumbnail_url": thumb_ep,
                    "mime_type": getattr(item, "mime_type", None),
                    "width": getattr(item, "width", None),
                    "height": getattr(item, "height", None),
                    "credit": getattr(item, "credit", None),
                    "created_at": getattr(item, "created_at", None),
                }
                res_list.append(res_dict)

            # Convert to dictionary or mutate data dictionary
            if isinstance(data, dict):
                data["media_list"] = res_list
                data["media"] = res_list[0] if res_list else None
                return data
            else:
                # ORM Object
                orm_dict = {
                    "id": getattr(data, "id", None),
                    "title": getattr(data, "title", None),
                    "source_id": getattr(data, "source_id", None),
                    "source_name": getattr(data, "source_name", None),
                    "published_at": getattr(data, "published_at", None),
                    "original_url": getattr(data, "original_url", None),
                    "category": getattr(data, "category", None),
                    "location_name": getattr(data, "location_name", None),
                    "latitude": getattr(data, "latitude", None),
                    "longitude": getattr(data, "longitude", None),
                    "created_at": getattr(data, "created_at", None),
                    "media_list": res_list,
                    "media": res_list[0] if res_list else None,
                }
                return orm_dict
        return data


class NewsListResponse(BaseModel):
    """Paginated response for news articles list."""

    items: list[NewsArticleResponse]
    total: int
    page: int
    limit: int
    has_more: bool
