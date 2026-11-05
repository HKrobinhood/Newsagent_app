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
                    params={"key": self.api_key},
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
            logger.warning("Gemini tool call failed (%s), falling back to mock", e)

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
                    params={"key": self.api_key},
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
            logger.warning("Gemini synthesis failed (%s), falling back to mock", e)

        return await self.fallback_mock.generate_grounded_answer(
            prompt, search_results, system_prompt
        )


def get_llm_client() -> BaseLLMClient:
    """Factory creating configured LLM provider instance."""
    if settings.LLM_API_KEY.strip():
        return GeminiLLMClient(api_key=settings.LLM_API_KEY, model=settings.LLM_MODEL)
    return MockLLMClient()
