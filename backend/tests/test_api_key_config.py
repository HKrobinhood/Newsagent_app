"""Tests for S2 user-provided LLM API key configuration and test-connection."""

from unittest.mock import AsyncMock, MagicMock, patch

import httpx
import pytest
from httpx import AsyncClient

from app.api.v1.endpoints.chat import reset_key_test_rate_limits
from app.services.llm_client import (
    GeminiLLMClient,
    MockLLMClient,
    OpenAILLMClient,
    get_llm_client,
)


@pytest.fixture(autouse=True)
def clear_rate_limits():
    """Ensure rate limit state is clean before and after each test."""
    reset_key_test_rate_limits()
    yield
    reset_key_test_rate_limits()


def test_get_llm_client_provider_routing():
    """Verify get_llm_client routes server_default, gemini, and openai."""
    default_client = get_llm_client(provider="server_default", api_key=None)
    assert isinstance(default_client, MockLLMClient)

    gemini_client = get_llm_client(
        provider="gemini", api_key="AIzaSyDummyGeminiTestKey123456"
    )
    assert isinstance(gemini_client, GeminiLLMClient)
    assert gemini_client.api_key == "AIzaSyDummyGeminiTestKey123456"

    openai_client = get_llm_client(
        provider="openai", api_key="sk-proj-DummyOpenAITestKey123456"
    )
    assert isinstance(openai_client, OpenAILLMClient)
    assert openai_client.api_key == "sk-proj-DummyOpenAITestKey123456"


@pytest.mark.asyncio
async def test_connection_server_default_returns_success(client: AsyncClient):
    """Verify server_default connectivity check succeeds immediately."""
    resp = await client.post(
        "/api/v1/ai/test-connection",
        json={"provider": "server_default"},
    )
    assert resp.status_code == 200
    body = resp.json()
    assert body["status"] == "Success"
    assert body["provider"] == "server_default"


@pytest.mark.asyncio
async def test_connection_valid_gemini_key_succeeds_without_echoing_key(
    client: AsyncClient,
):
    """Verify valid Gemini key returns Success and never echoes key."""
    secret_key = "AIzaSySecretKeyNeverEcho987654321"
    mock_response = MagicMock()
    mock_response.status_code = 200

    with patch("httpx.AsyncClient.get", new_callable=AsyncMock) as mock_get:
        mock_get.return_value = mock_response
        resp = await client.post(
            "/api/v1/ai/test-connection",
            headers={"X-LLM-Api-Key": secret_key},
            json={"provider": "gemini"},
        )

    assert resp.status_code == 200
    body = resp.json()
    assert body["status"] == "Success"
    assert body["provider"] == "gemini"
    assert secret_key not in resp.text

    # Verify key was sent in header, not URL query parameter
    called_url = mock_get.call_args[0][0]
    called_headers = mock_get.call_args[1].get("headers", {})
    assert secret_key not in called_url
    assert called_headers.get("x-goog-api-key") == secret_key


@pytest.mark.asyncio
async def test_connection_invalid_key_returns_auth_failed(client: AsyncClient):
    """Verify 401 from provider returns Authentication Failed without key leak."""
    bad_key = "sk-invalid-openai-secret-key-9999"
    mock_response = MagicMock()
    mock_response.status_code = 401

    with patch("httpx.AsyncClient.get", new_callable=AsyncMock) as mock_get:
        mock_get.return_value = mock_response
        resp = await client.post(
            "/api/v1/ai/test-connection",
            headers={"X-LLM-Api-Key": bad_key},
            json={"provider": "openai"},
        )

    assert resp.status_code == 200
    body = resp.json()
    assert body["status"] == "Authentication Failed"
    assert bad_key not in resp.text


@pytest.mark.asyncio
async def test_connection_timeout_and_provider_error(client: AsyncClient):
    """Verify TimeoutException maps to Timeout and 503 maps to Provider Error."""
    test_key = "AIzaSyTimeoutTestKey123456789"

    with patch("httpx.AsyncClient.get", new_callable=AsyncMock) as mock_get:
        mock_get.side_effect = httpx.ReadTimeout("Timed out")
        resp_timeout = await client.post(
            "/api/v1/ai/test-connection",
            headers={"X-LLM-Api-Key": test_key},
            json={"provider": "gemini"},
        )

    assert resp_timeout.status_code == 200
    assert resp_timeout.json()["status"] == "Timeout"
    assert test_key not in resp_timeout.text

    mock_503 = MagicMock()
    mock_503.status_code = 503
    with patch("httpx.AsyncClient.get", new_callable=AsyncMock) as mock_get:
        mock_get.return_value = mock_503
        resp_err = await client.post(
            "/api/v1/ai/test-connection",
            headers={"X-LLM-Api-Key": test_key},
            json={"provider": "gemini"},
        )

    assert resp_err.status_code == 200
    assert resp_err.json()["status"] == "Provider Error"
    assert test_key not in resp_err.text


@pytest.mark.asyncio
async def test_connection_rate_limits_repeated_failures(client: AsyncClient):
    """Verify 5 failed attempts trigger 429 rate limiting on the 6th attempt."""
    for _ in range(5):
        resp = await client.post(
            "/api/v1/ai/test-connection",
            headers={"X-LLM-Api-Key": "short"},
            json={"provider": "gemini"},
        )
        assert resp.status_code == 200
        assert resp.json()["status"] == "Authentication Failed"

    # 6th attempt within window must be rate limited with HTTP 429
    blocked_resp = await client.post(
        "/api/v1/ai/test-connection",
        headers={"X-LLM-Api-Key": "short"},
        json={"provider": "gemini"},
    )
    assert blocked_resp.status_code == 429


@pytest.mark.asyncio
async def test_chat_with_ephemeral_user_key_never_leaks_secret(
    client: AsyncClient,
):
    """Verify /api/v1/ai/chat accepts X-LLM-Provider and X-LLM-Api-Key safely."""
    ephemeral_key = "sk-proj-EphemeralUserSecretKey777888999"
    resp = await client.post(
        "/api/v1/ai/chat",
        headers={
            "X-LLM-Provider": "openai",
            "X-LLM-Api-Key": ephemeral_key,
        },
        json={
            "message": "What did the minister say about global trade?",
            "mode": "web_verification",
        },
    )
    assert resp.status_code == 200
    assert ephemeral_key not in resp.text
