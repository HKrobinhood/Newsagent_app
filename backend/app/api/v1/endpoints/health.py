"""Health check endpoint."""

from datetime import datetime, timezone
from typing import Any

from fastapi import APIRouter

router = APIRouter()


@router.get("/health", summary="Service health probe")
async def health_check() -> dict[str, Any]:
    """Check health status of the application."""
    return {
        "status": "ok",
        "service": "GlobalNewsAI",
        "timestamp": datetime.now(timezone.utc).isoformat(),
    }
