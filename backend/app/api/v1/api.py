"""V1 API router aggregator."""

from fastapi import APIRouter

from app.api.v1.endpoints import auth, chat, health, media, news, users

api_router = APIRouter()
api_router.include_router(health.router, tags=["Health"])
api_router.include_router(auth.router, prefix="/auth", tags=["Auth"])
api_router.include_router(users.router, prefix="/users", tags=["Users"])
api_router.include_router(news.router, prefix="/news", tags=["News"])
api_router.include_router(media.router, prefix="/media", tags=["Media"])
api_router.include_router(chat.router, prefix="/ai", tags=["AI Agent"])
