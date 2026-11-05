"""Pydantic schemas for news and error responses."""

from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field


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
    """Response schema for a single news article."""

    id: str
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)


class NewsListResponse(BaseModel):
    """Paginated response for news articles list."""

    items: list[NewsArticleResponse]
    total: int
    page: int
    limit: int
    has_more: bool
