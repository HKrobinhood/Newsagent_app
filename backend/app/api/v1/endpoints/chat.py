"""AI Agent conversational interaction endpoint."""

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.schemas.chat import ChatRequest, ChatResponse
from app.schemas.news import ErrorResponse
from app.services.agent_service import AgentService

router = APIRouter()


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
    return await agent_service.process_chat(request, db)
