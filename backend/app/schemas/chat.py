"""Pydantic schemas for AI Agent chat communication."""

from datetime import datetime, timezone

from pydantic import BaseModel, Field


class NewsSourceRef(BaseModel):
    """Citation reference for verified news sources."""

    title: str = Field(..., description="Referenced news headline")
    source_name: str = Field(..., description="Media publisher name")
    original_url: str = Field(..., description="Canonical source URL")
    published_at: str = Field(..., description="Publication time ISO string")
    location_name: str | None = Field(
        None, description="Geographic location if applicable"
    )


class ChatRequest(BaseModel):
    """Chat message request payload."""

    message: str = Field(..., min_length=1, description="User prompt message text")
    session_id: str | None = Field(None, description="Optional existing session UUID")
    mode: str = Field(
        default="auto",
        description="Execution mode: auto, title_search, or web_verification",
    )


class ChatResponse(BaseModel):
    """Structured response from the AI News Agent."""

    session_id: str = Field(..., description="Active session UUID")
    answer: str = Field(..., description="Agent markdown response text")
    mode_used: str = Field(
        ..., description="Execution mode: title_search or web_verification"
    )
    sources: list[NewsSourceRef] = Field(
        default_factory=list, description="Verified citation sources"
    )
    confidence: float = Field(
        default=0.95, ge=0.0, le=1.0, description="Agent confidence score"
    )
    timestamp: str = Field(
        default_factory=lambda: datetime.now(timezone.utc).isoformat(),
        description="Response timestamp UTC",
    )
