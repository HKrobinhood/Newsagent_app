"""User account and user preferences management endpoints."""

from datetime import datetime, timezone

from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.models.user import User, UserPreferences
from app.schemas.auth import (
    PreferencesResponse,
    PreferencesUpdateRequest,
    UserResponse,
)
from app.services.auth_service import get_current_user

router = APIRouter()


@router.get(
    "/me",
    response_model=UserResponse,
    summary="Get current authenticated user profile",
)
async def get_me(
    current_user: User = Depends(get_current_user),
) -> UserResponse:
    """Return identity profile of the currently signed-in user."""
    return UserResponse(
        id=current_user.id,
        email=current_user.email,
        display_name=current_user.display_name,
        auth_provider=current_user.auth_provider,
        created_at=current_user.created_at.isoformat(),
        updated_at=current_user.updated_at.isoformat(),
    )


@router.get(
    "/me/preferences",
    response_model=PreferencesResponse,
    summary="Get current user cloud preferences",
)
async def get_my_preferences(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> PreferencesResponse:
    """Retrieve personal preferences for the authenticated user."""
    stmt = select(UserPreferences).where(UserPreferences.user_id == current_user.id)
    result = await db.execute(stmt)
    prefs = result.scalar_one_or_none()

    if prefs is None:
        # Create default preferences record if missing
        prefs = UserPreferences(
            user_id=current_user.id,
            enabled_source_ids="bbc,cnn,aljazeera,fox",
            font_scale=1.00,
            schema_version=1,
        )
        db.add(prefs)
        await db.commit()
        await db.refresh(prefs)

    sources_list = [
        s.strip().lower()
        for s in prefs.enabled_source_ids.split(",")
        if s and s.strip()
    ]

    return PreferencesResponse(
        user_id=current_user.id,
        enabled_source_ids=sources_list,
        font_scale=prefs.font_scale,
        schema_version=prefs.schema_version,
        updated_at=prefs.updated_at.isoformat(),
    )


@router.put(
    "/me/preferences",
    response_model=PreferencesResponse,
    summary="Update current user cloud preferences",
)
async def update_my_preferences(
    payload: PreferencesUpdateRequest,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> PreferencesResponse:
    """Update personal preferences for the authenticated user only."""
    stmt = select(UserPreferences).where(UserPreferences.user_id == current_user.id)
    result = await db.execute(stmt)
    prefs = result.scalar_one_or_none()

    clean_sources = [
        s.strip().lower() for s in payload.enabled_source_ids if s and s.strip()
    ]
    sources_str = ",".join(clean_sources)

    if prefs is None:
        prefs = UserPreferences(
            user_id=current_user.id,
            enabled_source_ids=sources_str,
            font_scale=payload.font_scale,
            schema_version=payload.schema_version,
        )
        db.add(prefs)
    else:
        prefs.enabled_source_ids = sources_str
        prefs.font_scale = payload.font_scale
        prefs.schema_version = payload.schema_version
        prefs.updated_at = datetime.now(timezone.utc)

    await db.commit()
    await db.refresh(prefs)

    return PreferencesResponse(
        user_id=current_user.id,
        enabled_source_ids=clean_sources,
        font_scale=prefs.font_scale,
        schema_version=prefs.schema_version,
        updated_at=prefs.updated_at.isoformat(),
    )
