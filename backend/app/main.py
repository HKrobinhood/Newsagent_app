"""Main entrypoint for GlobalNewsAI FastAPI backend application."""

import logging
import time
from collections import defaultdict
from collections.abc import AsyncGenerator
from contextlib import asynccontextmanager
from datetime import datetime, timezone
from typing import Any

from fastapi import FastAPI, Request, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app.api.v1.api import api_router
from app.core.config import settings
from app.core.database import init_db

logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncGenerator[None, None]:
    """Lifespan event handler for startup and shutdown."""
    try:
        await init_db()
    except Exception as e:
        logger.info("Database initialization info: %s", e)
    yield


# Simple in-memory rate limiter per IP: max 180 requests per 60 seconds
RATE_LIMIT_WINDOW = 60.0
MAX_REQUESTS_PER_WINDOW = 180
ip_request_history: dict[str, list[float]] = defaultdict(list)


def create_app() -> FastAPI:
    """Create and configure FastAPI application."""
    app = FastAPI(
        title=settings.PROJECT_NAME,
        openapi_url=f"{settings.API_V1_STR}/openapi.json",
        version="0.1.0",
        lifespan=lifespan,
    )

    # 1. CORS Middleware
    app.add_middleware(
        CORSMiddleware,
        allow_origins=settings.BACKEND_CORS_ORIGINS,
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    # 2. Security Headers & Rate Limiting Middleware
    @app.middleware("http")
    async def security_and_rate_limit_middleware(
        request: Request, call_next: Any
    ) -> Any:
        client_ip = request.client.host if request.client else "127.0.0.1"
        now = time.time()

        # Clean history older than window
        timestamps = ip_request_history[client_ip]
        while timestamps and timestamps[0] < now - RATE_LIMIT_WINDOW:
            timestamps.pop(0)

        # Enforce rate limit (skip for test clients sending high-rate internal calls)
        if len(timestamps) >= MAX_REQUESTS_PER_WINDOW:
            return JSONResponse(
                status_code=status.HTTP_429_TOO_MANY_REQUESTS,
                content={
                    "error": {
                        "code": "RATE_LIMIT_EXCEEDED",
                        "message": "Too many requests. Please slow down.",
                    }
                },
            )

        timestamps.append(now)

        response = await call_next(request)
        response.headers["X-Content-Type-Options"] = "nosniff"
        response.headers["X-Frame-Options"] = "DENY"
        response.headers["X-XSS-Protection"] = "1; mode=block"
        return response

    # 3. Global Unhandled Exception Handler
    @app.exception_handler(Exception)
    async def global_exception_handler(
        request: Request, exc: Exception
    ) -> JSONResponse:
        logger.error("Unhandled server exception: %s", exc, exc_info=True)
        return JSONResponse(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            content={
                "error": {
                    "code": "INTERNAL_SERVER_ERROR",
                    "message": "An unexpected server error occurred.",
                }
            },
        )

    # 4. Root Health Check Endpoint
    @app.get("/health", tags=["Health"], summary="Root health check")
    async def root_health_check() -> dict[str, Any]:
        """Return current service health status."""
        return {
            "status": "ok",
            "service": "GlobalNewsAI",
            "timestamp": datetime.now(timezone.utc).isoformat(),
        }

    # 5. Include API V1 Router
    app.include_router(api_router, prefix=settings.API_V1_STR)

    return app


app = create_app()
