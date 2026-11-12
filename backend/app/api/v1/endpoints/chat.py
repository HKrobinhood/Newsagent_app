"""AI Agent conversational interaction and API key verification endpoints."""

import time
from collections import defaultdict

from fastapi import APIRouter, Depends, Header, HTTPException, Request, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.schemas.chat import (
    ChatRequest,
    ChatResponse,
    KeyTestRequest,
    KeyTestResponse,
)
from app.schemas.news import ErrorResponse
from app.services.agent_service import AgentService
from app.services.llm_client import verify_provider_key

router = APIRouter()

# Per-IP sliding window rate limiter for failed API key test attempts
_FAILED_ATTEMPT_WINDOW_SEC = 60.0
_MAX_FAILED_ATTEMPTS_PER_WINDOW = 5
_failed_key_test_attempts: dict[str, list[float]] = defaultdict(list)


def reset_key_test_rate_limits() -> None:
    """Clear rate limiter state (used by automated test suites)."""
    _failed_key_test_attempts.clear()


def _is_key_test_rate_limited(client_ip: str, now: float) -> bool:
    """Return True if client IP has exceeded failed key test threshold."""
    cutoff = now - _FAILED_ATTEMPT_WINDOW_SEC
    recent = [t for t in _failed_key_test_attempts[client_ip] if t >= cutoff]
    _failed_key_test_attempts[client_ip] = recent
    return len(recent) >= _MAX_FAILED_ATTEMPTS_PER_WINDOW


def _record_failed_key_test(client_ip: str, now: float) -> None:
    """Record a failed connection verification attempt for rate limiting."""
    _failed_key_test_attempts[client_ip].append(now)


@router.post(
    "/chat",
    response_model=ChatResponse,
    responses={
        400: {"model": ErrorResponse, "description": "Empty or invalid query"},
        504: {
            "model": ErrorResponse,
            "description": "Provider gateway timeout",
        },
    },
    summary="Submit user prompt to AI News Intelligence Agent",
)
async def chat_with_agent(
    request: ChatRequest,
    db: AsyncSession = Depends(get_db),
    x_llm_provider: str | None = Header(default=None, alias="X-LLM-Provider"),
    x_llm_api_key: str | None = Header(default=None, alias="X-LLM-Api-Key"),
) -> ChatResponse:
    """Process user prompt and return conversational answer with citations.

    - **message**: Non-empty user prompt.
    - **session_id**: Optional existing session UUID to maintain context.
    - **mode**: 'auto', 'title_search', or 'web_verification'.
    """
    cleaned_prompt = request.message.strip()
    if not cleaned_prompt:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Message content cannot be blank or whitespace only.",
        )

    agent_service = AgentService()
    return await agent_service.process_chat(
        request,
        db,
        llm_provider=x_llm_provider,
        llm_api_key=x_llm_api_key,
    )


@router.post(
    "/test-connection",
    response_model=KeyTestResponse,
    responses={
        429: {
            "model": ErrorResponse,
            "description": "Too many failed key verification attempts",
        },
    },
    summary="Test user-provided LLM API key connectivity safely",
)
async def test_llm_connection(
    payload: KeyTestRequest,
    http_request: Request,
    x_llm_api_key: str | None = Header(default=None, alias="X-LLM-Api-Key"),
) -> KeyTestResponse:
    """Verify LLM provider API key without storing or echoing the key."""
    client_ip = (
        http_request.client.host
        if http_request.client and http_request.client.host
        else "unknown"
    )
    now = time.monotonic()

    if _is_key_test_rate_limited(client_ip, now):
        raise HTTPException(
            status_code=status.HTTP_429_TOO_MANY_REQUESTS,
            detail=(
                "Too many failed API key test attempts. "
                "Please wait 60 seconds before trying again."
            ),
        )

    effective_key = x_llm_api_key if x_llm_api_key is not None else payload.api_key
    normalized_provider = (payload.provider or "server_default").strip().lower()

    status_label, message = await verify_provider_key(
        provider=normalized_provider,
        api_key=effective_key,
    )

    if status_label != "Success":
        _record_failed_key_test(client_ip, now)

    return KeyTestResponse(
        status=status_label,
        provider=normalized_provider,
        message=message,
    )
