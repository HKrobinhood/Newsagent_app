"""Pydantic schemas package."""

from app.schemas.chat import ChatRequest, ChatResponse, NewsSourceRef
from app.schemas.geo import (
    GeoFeature,
    GeoFeatureCollection,
    GeoGeometry,
    GeoProperties,
)
from app.schemas.news import (
    ErrorDetail,
    ErrorResponse,
    NewsArticleCreate,
    NewsArticleResponse,
    NewsListResponse,
    NewsSourceCreate,
    NewsSourceResponse,
)

__all__ = [
    "ChatRequest",
    "ChatResponse",
    "ErrorDetail",
    "ErrorResponse",
    "GeoFeature",
    "GeoFeatureCollection",
    "GeoGeometry",
    "GeoProperties",
    "NewsArticleCreate",
    "NewsArticleResponse",
    "NewsListResponse",
    "NewsSourceCreate",
    "NewsSourceRef",
    "NewsSourceResponse",
]
