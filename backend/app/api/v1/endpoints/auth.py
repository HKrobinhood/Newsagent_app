"""Authentication endpoints for Google Sign-In and logout."""

from fastapi import APIRouter, Depends, Header, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.models.user import User, UserPreferences
from app.schemas.auth import (
    AuthResponse,
    GoogleAuthRequest,
    LogoutResponse,
    UserResponse,
)
from app.services.auth_service import AuthService

router = APIRouter()


@router.post(
    "/google",
    response_model=AuthResponse,
    status_code=status.HTTP_200_OK,
    summary="Sign in with Google OAuth ID Token",
)
async def login_with_google(
    request: GoogleAuthRequest,
    db: AsyncSession = Depends(get_db),
) -> AuthResponse:
    """Authenticate or create user using verified Google ID Token."""
    claims = await AuthService.verify_google_id_token(request.id_token)

    provider_subject = claims["sub"]
    email = claims["email"]
    display_name = claims["name"]

    # Lookup or create user by provider_subject
    stmt = select(User).where(User.provider_subject == provider_subject)
    result = await db.execute(stmt)
    user = result.scalar_one_or_none()

    if user is None:
        user = User(
            auth_provider="google",
            provider_subject=provider_subject,
            email=email,
            display_name=display_name,
        )
        db.add(user)
        await db.flush()

        # Initialize default user preferences
        prefs = UserPreferences(
            user_id=user.id,
            enabled_source_ids="bbc,cnn,aljazeera,fox",
            font_scale=1.00,
            schema_version=1,
        )
        db.add(prefs)
        await db.commit()
        await db.refresh(user)
    else:
        # Update profile info if changed
        modified = False
        if user.email != email:
            user.email = email
            modified = True
        if display_name and user.display_name != display_name:
            user.display_name = display_name
            modified = True
        if modified:
            await db.commit()
            await db.refresh(user)

    token_str, expires_at_iso = AuthService.create_access_token(user.id, user.email)

    user_response = UserResponse(
        id=user.id,
        email=user.email,
        display_name=user.display_name,
        auth_provider=user.auth_provider,
        created_at=user.created_at.isoformat(),
        updated_at=user.updated_at.isoformat(),
    )

    return AuthResponse(
        access_token=token_str,
        token_type="bearer",
        expires_at=expires_at_iso,
        user=user_response,
    )


@router.post(
    "/logout",
    response_model=LogoutResponse,
    status_code=status.HTTP_200_OK,
    summary="Terminate active session",
)
async def logout(
    authorization: str | None = Header(None),
) -> LogoutResponse:
    """Revoke active session token."""
    if authorization and authorization.lower().startswith("bearer "):
        token = authorization[7:].strip()
        AuthService.revoke_token(token)

    return LogoutResponse(
        status="success",
        message="Logged out successfully",
    )
