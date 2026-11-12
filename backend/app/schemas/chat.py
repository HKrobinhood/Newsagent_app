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
    source_ids: list[str] | None = Field(
        None,
        description="Optional list of allowed news source IDs",
    )
    llm_provider: str | None = Field(
        None,
        description="Optional LLM provider: server_default, gemini, or openai",
    )


class KeyTestRequest(BaseModel):
    """Request payload for testing LLM provider connectivity."""

    provider: str = Field(
        default="gemini",
        description="Target LLM provider: server_default, gemini, or openai",
    )
    api_key: str | None = Field(
        default=None,
        description="Optional API key if not supplied via X-LLM-Api-Key header",
    )


class KeyTestResponse(BaseModel):
    """Sanitized response from LLM API key connectivity verification."""

    status: str = Field(
        ...,
        description=(
            "Connection test status: Success, Authentication Failed, "
            "Timeout, or Provider Error"
        ),
    )
    provider: str = Field(..., description="Normalized provider identifier")
    message: str = Field(
        ..., description="Sanitized status message (never contains API key)"
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
