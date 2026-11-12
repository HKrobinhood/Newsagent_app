"""Authentication and authorization services with Google ID Token validation."""

import base64
import hashlib
import hmac
import json
import logging
import time
import uuid
from datetime import datetime, timezone
from typing import Any

import httpx
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import settings
from app.core.database import get_db
from app.models.user import User

logger = logging.getLogger(__name__)

security_bearer = HTTPBearer(auto_error=False)

# In-memory revocation cache for revoked JWT/HMAC tokens
_revoked_tokens: set[str] = set()


def clear_revoked_tokens_for_testing() -> None:
    """Clear revoked tokens set for unit test isolation."""
    _revoked_tokens.clear()


class AuthService:
    """Service handling Google OAuth verification and session token lifecycle."""

    TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo"

    @classmethod
    async def verify_google_id_token(cls, id_token: str) -> dict[str, Any]:
        """Validate Google ID Token with Google tokeninfo endpoint.

        Args:
            id_token: Raw Google OAuth ID Token string.

        Returns:
            Dictionary containing verified user claims ('sub', 'email', 'name').

        Raises:
            HTTPException: 401 if invalid, expired, or audience mismatch.
        """
        clean_token = id_token.strip()
        if not clean_token:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Google ID Token cannot be blank.",
            )

        try:
            async with httpx.AsyncClient(timeout=10.0) as client:
                resp = await client.get(
                    cls.TOKENINFO_URL, params={"id_token": clean_token}
                )
                if resp.status_code != 200:
                    raise HTTPException(
                        status_code=status.HTTP_401_UNAUTHORIZED,
                        detail="Invalid or expired Google ID Token.",
                    )
                data = resp.json()
        except httpx.RequestError as exc:
            logger.error("Failed to connect to Google OAuth tokeninfo: %s", exc)
            raise HTTPException(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                detail="Google authentication service is currently unreachable.",
            ) from exc

        sub = data.get("sub")
        email = data.get("email")
        if not sub or not email:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Google ID Token lacks mandatory subject or email claims.",
            )

        # Enforce audience validation if configured
        configured_client_id = settings.GOOGLE_CLIENT_ID.strip()
        if configured_client_id:
            aud = data.get("aud")
            if aud != configured_client_id:
                raise HTTPException(
                    status_code=status.HTTP_401_UNAUTHORIZED,
                    detail="Google ID Token audience mismatch.",
                )

        # Check expiration
        exp = int(data.get("exp", 0))
        if exp < time.time():
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Google ID Token has expired.",
            )

        return {
            "sub": str(sub),
            "email": str(email).lower().strip(),
            "name": data.get("name") or data.get("given_name") or email.split("@")[0],
        }

    @classmethod
    def create_access_token(cls, user_id: str, email: str) -> tuple[str, str]:
        """Generate HMAC-SHA256 signed bearer access token.

        Args:
            user_id: Unique user UUID.
            email: User email.

        Returns:
            Tuple of (token_string, expires_at_iso_string).
        """
        now = time.time()
        expires_at_epoch = now + settings.ACCESS_TOKEN_EXPIRE_SECONDS
        expires_at_iso = datetime.fromtimestamp(
            expires_at_epoch, timezone.utc
        ).isoformat()

        payload = {
            "sub": user_id,
            "email": email,
            "exp": int(expires_at_epoch),
            "iat": int(now),
            "jti": uuid.uuid4().hex,
        }

        payload_bytes = json.dumps(payload, separators=(",", ":")).encode("utf-8")
        payload_b64 = (
            base64.urlsafe_b64encode(payload_bytes).decode("ascii").rstrip("=")
        )

        signature = hmac.new(
            settings.SECRET_KEY.encode("utf-8"),
            payload_b64.encode("ascii"),
            hashlib.sha256,
        ).digest()
        sig_b64 = base64.urlsafe_b64encode(signature).decode("ascii").rstrip("=")

        token = f"{payload_b64}.{sig_b64}"
        return token, expires_at_iso

    @classmethod
    def decode_access_token(cls, token: str) -> dict[str, Any]:
        """Decode and verify HMAC-SHA256 signature and expiration of bearer token.

        Args:
            token: Raw bearer token string.

        Returns:
            Decoded payload dictionary.

        Raises:
            HTTPException: 401 if invalid, expired, or revoked.
        """
        if token in _revoked_tokens:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Session token has been revoked. Please sign in again.",
            )

        parts = token.split(".")
        if len(parts) != 2:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Malformed authorization token.",
            )

        payload_b64, sig_b64 = parts

        expected_sig = hmac.new(
            settings.SECRET_KEY.encode("utf-8"),
            payload_b64.encode("ascii"),
            hashlib.sha256,
        ).digest()
        expected_sig_b64 = (
            base64.urlsafe_b64encode(expected_sig).decode("ascii").rstrip("=")
        )

        if not hmac.compare_digest(sig_b64, expected_sig_b64):
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid token signature.",
            )

        padding = 4 - (len(payload_b64) % 4)
        if padding != 4:
            payload_b64 += "=" * padding

        try:
            payload_json = base64.urlsafe_b64decode(payload_b64.encode("ascii")).decode(
                "utf-8"
            )
            payload = json.loads(payload_json)
        except Exception as exc:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid token payload structure.",
            ) from exc

        if payload.get("exp", 0) < time.time():
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Authorization token has expired.",
            )

        return payload

    @classmethod
    def revoke_token(cls, token: str) -> None:
        """Mark access token as revoked in memory."""
        _revoked_tokens.add(token)


async def get_current_user(
    credentials: HTTPAuthorizationCredentials | None = Depends(security_bearer),
    db: AsyncSession = Depends(get_db),
) -> User:
    """Dependency verifying Bearer authentication and returning the User entity."""
    if not credentials or not credentials.credentials:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Missing Authorization Bearer token.",
            headers={"WWW-Authenticate": "Bearer"},
        )

    payload = AuthService.decode_access_token(credentials.credentials)
    user_id = payload.get("sub")
    if not user_id:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Token subject missing.",
        )

    stmt = select(User).where(User.id == user_id)
    result = await db.execute(stmt)
    user = result.scalar_one_or_none()
    if not user:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="User account no longer exists.",
        )

    return user
