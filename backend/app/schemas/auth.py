"""Pydantic schemas for Authentication and User management."""

from datetime import datetime, timezone

from pydantic import BaseModel, Field


class GoogleAuthRequest(BaseModel):
    """Payload for Google Sign-In authentication."""

    id_token: str = Field(
        ...,
        min_length=10,
        description="Google OAuth 2.0 ID Token from Android Credential Manager",
    )


class UserResponse(BaseModel):
    """Public user identity representation."""

    id: str = Field(..., description="Unique internal user UUID")
    email: str = Field(..., description="User email address")
    display_name: str | None = Field(None, description="User full display name")
    auth_provider: str = Field(default="google", description="Identity provider")
    created_at: str = Field(..., description="Account creation timestamp ISO-8601")
    updated_at: str = Field(..., description="Account update timestamp ISO-8601")


class AuthResponse(BaseModel):
    """Authentication token and profile response."""

    access_token: str = Field(..., description="Signed Bearer access token")
    token_type: str = Field(default="bearer", description="Token type")
    expires_at: str = Field(..., description="Token expiration timestamp ISO-8601")
    user: UserResponse = Field(..., description="Authenticated user profile")


class LogoutResponse(BaseModel):
    """Response returned upon session termination."""

    status: str = Field(default="success", description="Logout status")
    message: str = Field(
        default="Logged out successfully", description="Status message"
    )


class PreferencesResponse(BaseModel):
    """User cloud preferences representation."""

    user_id: str = Field(..., description="Owner user ID")
    enabled_source_ids: list[str] = Field(
        default_factory=lambda: ["bbc", "cnn", "aljazeera", "fox"],
        description="List of enabled news media source IDs",
    )
    font_scale: float = Field(default=1.00, description="Global font scale ratio")
    schema_version: int = Field(default=1, description="Preferences schema version")
    updated_at: str = Field(
        default_factory=lambda: datetime.now(timezone.utc).isoformat(),
        description="Last updated timestamp ISO-8601",
    )


class PreferencesUpdateRequest(BaseModel):
    """Payload for updating user preferences."""

    enabled_source_ids: list[str] = Field(
        ...,
        description="Updated list of enabled news media source IDs",
    )
    font_scale: float = Field(
        ...,
        ge=0.5,
        le=2.0,
        description="Updated font scale ratio (0.90, 1.00, 1.15, 1.30)",
    )
    schema_version: int = Field(default=1, description="Preferences schema version")
