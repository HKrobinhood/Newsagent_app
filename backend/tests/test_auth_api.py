"""Integration tests for Google Sign-In, token authentication, and user preferences."""

from unittest.mock import AsyncMock, MagicMock, patch

import pytest
from httpx import AsyncClient

from app.services.auth_service import clear_revoked_tokens_for_testing


@pytest.fixture(autouse=True)
def clean_auth_state():
    """Ensure in-memory token revocation cache is clear between tests."""
    clear_revoked_tokens_for_testing()
    yield
    clear_revoked_tokens_for_testing()


@pytest.mark.asyncio
async def test_google_login_success_creates_user_and_default_preferences(
    client: AsyncClient,
):
    """Verify valid Google ID Token creates user and default preferences."""
    fake_token = "valid-google-id-token-user-1"
    mock_tokeninfo = {
        "sub": "google-sub-12345678",
        "email": "alice@example.com",
        "name": "Alice Innovator",
        "exp": 2000000000,
    }
    mock_resp = MagicMock()
    mock_resp.status_code = 200
    mock_resp.json.return_value = mock_tokeninfo

    with patch("httpx.AsyncClient.get", new_callable=AsyncMock) as mock_get:
        mock_get.return_value = mock_resp
        resp = await client.post(
            "/api/v1/auth/google",
            json={"id_token": fake_token},
        )

    assert resp.status_code == 200
    body = resp.json()
    assert "access_token" in body
    assert body["token_type"] == "bearer"
    user = body["user"]
    assert user["email"] == "alice@example.com"
    assert user["display_name"] == "Alice Innovator"
    assert user["auth_provider"] == "google"

    token = body["access_token"]

    # Verify GET /api/v1/users/me returns authenticated profile
    me_resp = await client.get(
        "/api/v1/users/me",
        headers={"Authorization": f"Bearer {token}"},
    )
    assert me_resp.status_code == 200
    assert me_resp.json()["id"] == user["id"]

    # Verify GET /api/v1/users/me/preferences returns default preferences
    prefs_resp = await client.get(
        "/api/v1/users/me/preferences",
        headers={"Authorization": f"Bearer {token}"},
    )
    assert prefs_resp.status_code == 200
    prefs_body = prefs_resp.json()
    assert prefs_body["user_id"] == user["id"]
    assert "bbc" in prefs_body["enabled_source_ids"]
    assert prefs_body["font_scale"] == 1.00


@pytest.mark.asyncio
async def test_google_login_invalid_token_rejected_with_401(client: AsyncClient):
    """Verify invalid or rejected Google token returns HTTP 401."""
    mock_resp = MagicMock()
    mock_resp.status_code = 400
    mock_resp.json.return_value = {"error_description": "Invalid Value"}

    with patch("httpx.AsyncClient.get", new_callable=AsyncMock) as mock_get:
        mock_get.return_value = mock_resp
        resp = await client.post(
            "/api/v1/auth/google",
            json={"id_token": "bad-or-spoofed-token"},
        )

    assert resp.status_code == 401
    assert "Invalid or expired Google ID Token" in resp.json()["detail"]


@pytest.mark.asyncio
async def test_unauthenticated_requests_to_users_api_rejected(client: AsyncClient):
    """Verify protected user endpoints require valid Bearer token."""
    resp1 = await client.get("/api/v1/users/me")
    assert resp1.status_code == 401

    resp2 = await client.get("/api/v1/users/me/preferences")
    assert resp2.status_code == 401

    resp3 = await client.put(
        "/api/v1/users/me/preferences",
        json={"enabled_source_ids": ["bbc"], "font_scale": 1.15},
    )
    assert resp3.status_code == 401


@pytest.mark.asyncio
async def test_update_preferences_and_user_isolation(client: AsyncClient):
    """Verify user preferences update and user isolation between accounts."""
    # 1. Login User A
    mock_a = MagicMock(
        status_code=200,
        json=lambda: {
            "sub": "sub-user-a-111",
            "email": "user.a@example.com",
            "name": "User A",
            "exp": 2000000000,
        },
    )
    with patch("httpx.AsyncClient.get", new_callable=AsyncMock) as mock_get:
        mock_get.return_value = mock_a
        resp_a = await client.post(
            "/api/v1/auth/google",
            json={"id_token": "valid-token-user-a-12345"},
        )
    assert resp_a.status_code == 200
    token_a = resp_a.json()["access_token"]
    user_a_id = resp_a.json()["user"]["id"]

    # 2. Login User B
    mock_b = MagicMock(
        status_code=200,
        json=lambda: {
            "sub": "sub-user-b-222",
            "email": "user.b@example.com",
            "name": "User B",
            "exp": 2000000000,
        },
    )
    with patch("httpx.AsyncClient.get", new_callable=AsyncMock) as mock_get:
        mock_get.return_value = mock_b
        resp_b = await client.post(
            "/api/v1/auth/google",
            json={"id_token": "valid-token-user-b-12345"},
        )
    assert resp_b.status_code == 200
    token_b = resp_b.json()["access_token"]
    user_b_id = resp_b.json()["user"]["id"]

    assert user_a_id != user_b_id

    # 3. User A updates preferences: only bbc and cnn, font_scale 1.30
    put_a = await client.put(
        "/api/v1/users/me/preferences",
        headers={"Authorization": f"Bearer {token_a}"},
        json={
            "enabled_source_ids": ["bbc", "cnn"],
            "font_scale": 1.30,
            "schema_version": 2,
        },
    )
    assert put_a.status_code == 200
    prefs_a = put_a.json()
    assert prefs_a["enabled_source_ids"] == ["bbc", "cnn"]
    assert prefs_a["font_scale"] == 1.30

    # 4. User B checks preferences: must NOT inherit User A's changes
    get_b = await client.get(
        "/api/v1/users/me/preferences",
        headers={"Authorization": f"Bearer {token_b}"},
    )
    assert get_b.status_code == 200
    prefs_b = get_b.json()
    assert prefs_b["user_id"] == user_b_id
    assert prefs_b["font_scale"] == 1.00
    assert len(prefs_b["enabled_source_ids"]) == 4


@pytest.mark.asyncio
async def test_logout_revokes_token(client: AsyncClient):
    """Verify logout revokes active session token."""
    mock_token = MagicMock(
        status_code=200,
        json=lambda: {
            "sub": "sub-logout-test",
            "email": "logout@example.com",
            "name": "Logout Tester",
            "exp": 2000000000,
        },
    )
    with patch("httpx.AsyncClient.get", new_callable=AsyncMock) as mock_get:
        mock_get.return_value = mock_token
        login_resp = await client.post(
            "/api/v1/auth/google",
            json={"id_token": "token-to-logout"},
        )
    token = login_resp.json()["access_token"]

    # Valid before logout
    me_resp = await client.get(
        "/api/v1/users/me",
        headers={"Authorization": f"Bearer {token}"},
    )
    assert me_resp.status_code == 200

    # Execute logout
    logout_resp = await client.post(
        "/api/v1/auth/logout",
        headers={"Authorization": f"Bearer {token}"},
    )
    assert logout_resp.status_code == 200
    assert logout_resp.json()["status"] == "success"

    # Subsequent request with revoked token must fail with 401
    revoked_resp = await client.get(
        "/api/v1/users/me",
        headers={"Authorization": f"Bearer {token}"},
    )
    assert revoked_resp.status_code == 401
    assert "revoked" in revoked_resp.json()["detail"].lower()
