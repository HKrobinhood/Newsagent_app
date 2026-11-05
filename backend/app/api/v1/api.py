"""V1 API router aggregator."""

from fastapi import APIRouter

from app.api.v1.endpoints import chat, health, news

api_router = APIRouter()
api_router.include_router(health.router, tags=["Health"])
api_router.include_router(news.router, prefix="/news", tags=["News"])
api_router.include_router(chat.router, prefix="/ai", tags=["AI Agent"])
