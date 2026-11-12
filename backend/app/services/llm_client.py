"""LLM client integration providing tool calling and grounded generation."""

import logging
from abc import ABC, abstractmethod
from typing import Any

import httpx

from app.core.config import settings
from app.services.intent_parser import IntentParser

logger = logging.getLogger(__name__)

SYSTEM_PROMPT = (
    "You are GlobalNewsAI Intelligence Agent, an international news "
    "intelligence assistant.\n\n"
    "CRITICAL RULES:\n"
    "1. You must ONLY answer based on the real verified news articles "
    "retrieved from the search_news_database tool.\n"
    "2. NEVER hallucinate, invent, or extrapolate facts, quotes, or details "
    "beyond the headlines and metadata provided.\n"
    "3. If the search results are empty, you MUST explicitly state that no "
    "matching news was found in the database.\n"
    "4. Always accurately associate each stated event with its original "
    "source and headline.\n"
)


class BaseLLMClient(ABC):
    """Abstract interface for LLM providers supporting function calling."""

    @abstractmethod
    async def parse_and_call_tool(
        self, prompt: str, system_prompt: str, tools: list[dict]
    ) -> tuple[str, dict[str, Any]]:
        """Determine tool to invoke and extract its arguments."""

    @abstractmethod
    async def generate_grounded_answer(
        self, prompt: str, search_results: list[dict], system_prompt: str
    ) -> str:
        """Synthesize response grounded strictly in retrieved news articles."""


class MockLLMClient(BaseLLMClient):
    """Deterministic simulated LLM client for testing and offline execution."""

    async def parse_and_call_tool(
        self, prompt: str, system_prompt: str, tools: list[dict]
    ) -> tuple[str, dict[str, Any]]:
        """Parse natural language query into search_news_database tool call."""
        args = IntentParser.parse_intent(prompt)
        return "search_news_database", args

    async def generate_grounded_answer(
        self, prompt: str, search_results: list[dict], system_prompt: str
    ) -> str:
        """Generate strictly grounded answer from retrieved article metadata."""
        if not search_results:
            return "未检索到相关新闻，数据库中暂无匹配的报道。"

        count = len(search_results)
        lines = [
            f"根据本地数据库中检索到的 {count} 篇权威新闻报道，相关资讯归纳如下：\n"
        ]
        for idx, art in enumerate(search_results[:5], start=1):
            source = art.get("source_name", "未知媒体")
            title = art.get("title", "")
            loc = art.get("location_name")
            loc_str = f"（{loc}）" if loc else ""
            lines.append(f"{idx}. **{source}**{loc_str}: {title}")

        lines.append(
            "\n以上简报严格基于数据库收录的公开新闻标题与元数据归纳，"
            "未包含未经核实的主观推测。"
        )
        return "\n".join(lines)


class GeminiLLMClient(BaseLLMClient):
    """Google Gemini LLM client utilizing REST API for function calling."""

    def __init__(self, api_key: str, model: str = "gemini-1.5-pro") -> None:
        """Initialize client with API credentials."""
        self.api_key = api_key
        self.model = model
        self.endpoint = (
            "https://generativelanguage.googleapis.com/v1beta/models/"
            f"{model}:generateContent"
        )
        self.fallback_mock = MockLLMClient()

    async def parse_and_call_tool(
        self, prompt: str, system_prompt: str, tools: list[dict]
    ) -> tuple[str, dict[str, Any]]:
        """Call Gemini API to determine tool invocation with mock fallback."""
        if not self.api_key:
            return await self.fallback_mock.parse_and_call_tool(
                prompt, system_prompt, tools
            )

        payload = {
            "system_instruction": {"parts": [{"text": system_prompt}]},
            "contents": [{"role": "user", "parts": [{"text": prompt}]}],
            "tools": [{"function_declarations": tools}],
            "tool_config": {"function_calling_config": {"mode": "AUTO"}},
        }

        try:
            async with httpx.AsyncClient(timeout=15.0) as client:
                resp = await client.post(
                    self.endpoint,
                    headers={"x-goog-api-key": self.api_key},
                    json=payload,
                )
                if resp.status_code == 200:
                    data = resp.json()
                    candidates = data.get("candidates", [])
                    if candidates:
                        parts = candidates[0].get("content", {}).get("parts", [])
                        for part in parts:
                            if "functionCall" in part:
                                fn = part["functionCall"]
                                return (
                                    fn.get("name", "search_news_database"),
                                    fn.get("args", {}),
                                )
        except Exception as e:
            logger.warning(
                "Gemini tool call failed (%s), falling back to mock",
                type(e).__name__,
            )

        return await self.fallback_mock.parse_and_call_tool(
            prompt, system_prompt, tools
        )

    async def generate_grounded_answer(
        self, prompt: str, search_results: list[dict], system_prompt: str
    ) -> str:
        """Synthesize answer using Gemini or fallback to grounded mock."""
        if not self.api_key or not search_results:
            return await self.fallback_mock.generate_grounded_answer(
                prompt, search_results, system_prompt
            )

        context_lines = []
        for r in search_results:
            source = r.get("source_name")
            title = r.get("title")
            loc = r.get("location_name", "Global")
            url = r.get("original_url")
            context_lines.append(f"- [{source}] {title} (Location: {loc}, URL: {url})")
        context_bullets = "\n".join(context_lines)

        user_content = (
            f"User Query: {prompt}\n\n"
            f"Retrieved Verified News Database Articles:\n{context_bullets}\n\n"
            "Synthesize a factual summary strictly answering the user query "
            "based ONLY on the above articles."
        )

        payload = {
            "system_instruction": {"parts": [{"text": system_prompt}]},
            "contents": [{"role": "user", "parts": [{"text": user_content}]}],
        }

        try:
            async with httpx.AsyncClient(timeout=20.0) as client:
                resp = await client.post(
                    self.endpoint,
                    headers={"x-goog-api-key": self.api_key},
                    json=payload,
                )
                if resp.status_code == 200:
                    data = resp.json()
                    candidates = data.get("candidates", [])
                    if candidates:
                        parts = candidates[0].get("content", {}).get("parts", [])
                        texts = [p.get("text", "") for p in parts if "text" in p]
                        if texts:
                            return "".join(texts).strip()
        except Exception as e:
            logger.warning(
                "Gemini synthesis failed (%s), falling back to mock",
                type(e).__name__,
            )

        return await self.fallback_mock.generate_grounded_answer(
            prompt, search_results, system_prompt
        )


class OpenAILLMClient(BaseLLMClient):
    """OpenAI LLM client utilizing fixed official REST API endpoint."""

    ENDPOINT = "https://api.openai.com/v1/chat/completions"

    def __init__(self, api_key: str, model: str = "gpt-4o-mini") -> None:
        """Initialize OpenAI client with ephemeral user/server API key."""
        self.api_key = api_key
        self.model = model
        self.fallback_mock = MockLLMClient()

    async def parse_and_call_tool(
        self, prompt: str, system_prompt: str, tools: list[dict]
    ) -> tuple[str, dict[str, Any]]:
        """Parse intent via IntentParser with deterministic tool mapping."""
        return await self.fallback_mock.parse_and_call_tool(
            prompt, system_prompt, tools
        )

    async def generate_grounded_answer(
        self, prompt: str, search_results: list[dict], system_prompt: str
    ) -> str:
        """Synthesize grounded answer via OpenAI Chat Completions or mock fallback."""
        if not self.api_key or not search_results:
            return await self.fallback_mock.generate_grounded_answer(
                prompt, search_results, system_prompt
            )

        context_lines = []
        for r in search_results:
            source = r.get("source_name")
            title = r.get("title")
            loc = r.get("location_name", "Global")
            url = r.get("original_url")
            context_lines.append(f"- [{source}] {title} (Location: {loc}, URL: {url})")
        context_bullets = "\n".join(context_lines)

        user_content = (
            f"User Query: {prompt}\n\n"
            f"Retrieved Verified News Database Articles:\n{context_bullets}\n\n"
            "Synthesize a factual summary strictly answering the user query "
            "based ONLY on the above articles."
        )

        payload = {
            "model": self.model,
            "messages": [
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_content},
            ],
            "temperature": 0.2,
            "max_tokens": 400,
        }

        try:
            async with httpx.AsyncClient(timeout=20.0) as client:
                resp = await client.post(
                    self.ENDPOINT,
                    headers={"Authorization": f"Bearer {self.api_key}"},
                    json=payload,
                )
                if resp.status_code == 200:
                    data = resp.json()
                    choices = data.get("choices", [])
                    if choices:
                        content = choices[0].get("message", {}).get("content", "")
                        if content and content.strip():
                            return content.strip()
        except Exception as e:
            logger.warning(
                "OpenAI synthesis failed (%s), falling back to mock",
                type(e).__name__,
            )

        return await self.fallback_mock.generate_grounded_answer(
            prompt, search_results, system_prompt
        )


SUPPORTED_PROVIDERS = {"server_default", "gemini", "openai"}


def get_llm_client(
    provider: str | None = None,
    api_key: str | None = None,
) -> BaseLLMClient:
    """Factory creating configured LLM provider instance.

    Supports both ephemeral user-supplied provider keys and server default.
    Never persists or logs user-provided API keys.
    """
    normalized_provider = (provider or "server_default").strip().lower()
    cleaned_key = (api_key or "").strip()

    if normalized_provider == "gemini" and cleaned_key:
        return GeminiLLMClient(api_key=cleaned_key, model=settings.LLM_MODEL)
    if normalized_provider == "openai" and cleaned_key:
        return OpenAILLMClient(api_key=cleaned_key)

    if settings.LLM_API_KEY.strip():
        return GeminiLLMClient(api_key=settings.LLM_API_KEY, model=settings.LLM_MODEL)
    return MockLLMClient()


async def verify_provider_key(
    provider: str,
    api_key: str | None,
) -> tuple[str, str]:
    """Verify LLM provider API key connectivity without wasting tokens.

    Returns:
        Tuple of (status_code_label, sanitized_message) where status_code_label
        is one of: 'Success', 'Authentication Failed', 'Timeout', 'Provider Error'.
    """
    normalized_provider = (provider or "server_default").strip().lower()
    if normalized_provider not in SUPPORTED_PROVIDERS:
        return (
            "Provider Error",
            "Unsupported LLM provider. Allowed: server_default, gemini, openai.",
        )

    if normalized_provider == "server_default":
        return (
            "Success",
            "Server Default AI Agent is active and ready.",
        )

    cleaned_key = (api_key or "").strip()
    if not cleaned_key or len(cleaned_key) < 8 or any(c.isspace() for c in cleaned_key):
        return (
            "Authentication Failed",
            "API key is missing or has an invalid format.",
        )

    if normalized_provider == "gemini":
        url = (
            "https://generativelanguage.googleapis.com/v1beta/models/"
            f"{settings.LLM_MODEL}"
        )
        headers = {"x-goog-api-key": cleaned_key}
    else:
        url = "https://api.openai.com/v1/models"
        headers = {"Authorization": f"Bearer {cleaned_key}"}

    try:
        async with httpx.AsyncClient(timeout=8.0) as client:
            resp = await client.get(url, headers=headers)
            if resp.status_code == 200:
                return (
                    "Success",
                    f"Connected to {normalized_provider.upper()} successfully.",
                )
            if resp.status_code in (400, 401, 403):
                return (
                    "Authentication Failed",
                    "API key was rejected by the provider. Please check your key.",
                )
            return (
                "Provider Error",
                f"Provider returned HTTP {resp.status_code}.",
            )
    except httpx.TimeoutException:
        return (
            "Timeout",
            "Connection to LLM provider timed out.",
        )
    except Exception:
        return (
            "Provider Error",
            "Failed to reach LLM provider endpoint.",
        )
