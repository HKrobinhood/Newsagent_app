"""Intent parser extracting search parameters from natural language."""

import re
from datetime import datetime, timedelta, timezone
from typing import Any

SEARCH_NEWS_TOOL = {
    "name": "search_news_database",
    "description": (
        "Search internal news database metadata by query keywords, "
        "time range thresholds, category, and source."
    ),
    "parameters": {
        "type": "object",
        "properties": {
            "query": {
                "type": "string",
                "description": ("Core search keyword or topic (e.g. 'AI', 'climate')."),
            },
            "start_time": {
                "type": "string",
                "description": "ISO 8601 UTC start datetime filter.",
            },
            "end_time": {
                "type": "string",
                "description": "ISO 8601 UTC end datetime filter.",
            },
            "category": {
                "type": "string",
                "description": (
                    "Category: 'world', 'business', 'technology', 'science', 'general'."
                ),
            },
            "source": {
                "type": "string",
                "description": "Source ID: 'bbc', 'cnn', 'aljazeera', 'fox'.",
            },
        },
        "required": ["query"],
    },
}


class IntentParser:
    """Parses conversational user prompts into structured tool arguments."""

    @staticmethod
    def parse_intent(
        prompt: str, reference_time: datetime | None = None
    ) -> dict[str, Any]:
        """Extract tool arguments from user prompt.

        Args:
            prompt: Raw user input text.
            reference_time: Base time for calculations (defaults to UTC now).

        Returns:
            Dictionary conforming to SEARCH_NEWS_TOOL arguments.
        """
        now = reference_time or datetime.now(timezone.utc)
        lower_prompt = prompt.lower().strip()
        # Normalize whitespace between numbers and units (e.g. "24 小时" -> "24小时")
        normalized = re.sub(r"(\d+)\s+(小时|hours|h|天|days)", r"\1\2", lower_prompt)

        # 1. Parse time range
        start_time: str | None = None
        end_time: str | None = None

        time_24h_keys = [
            "24小时",
            "24hours",
            "24h",
            "past day",
            "last day",
        ]
        time_48h_keys = ["48小时", "48hours", "48h"]
        time_today_keys = ["今天", "today"]
        time_week_keys = [
            "7天",
            "7days",
            "一周",
            "past week",
            "last week",
        ]

        if any(p in normalized for p in time_24h_keys):
            start_time = (now - timedelta(hours=24)).isoformat()
        elif any(p in normalized for p in time_48h_keys):
            start_time = (now - timedelta(hours=48)).isoformat()
        elif any(p in normalized for p in time_today_keys):
            start_of_day = now.replace(hour=0, minute=0, second=0, microsecond=0)
            start_time = start_of_day.isoformat()
        elif any(p in normalized for p in time_week_keys):
            start_time = (now - timedelta(days=7)).isoformat()

        # 2. Parse category
        category: str | None = None
        tech_keys = [
            "科技",
            "tech",
            "technology",
            "ai",
            "人工智能",
            "芯片",
            "chip",
            "semiconductor",
            "半导体",
            "quantum",
            "量子",
        ]
        biz_keys = [
            "财经",
            "商业",
            "business",
            "economy",
            "market",
            "finance",
            "financial",
            "银行",
            "bank",
            "inflation",
            "通胀",
            "central bank",
            "央行",
        ]
        world_keys = [
            "国际",
            "world",
            "外交",
            "diplomacy",
            "summit",
            "峰会",
        ]
        sci_keys = ["科学", "science", "climate", "气候", "space", "太空"]

        if any(c in normalized for c in tech_keys):
            category = "technology"
        elif any(c in normalized for c in biz_keys):
            category = "business"
        elif any(c in normalized for c in world_keys):
            category = "world"
        elif any(c in normalized for c in sci_keys):
            category = "science"

        # 3. Parse source
        source: str | None = None
        aljazeera_keys = ["al jazeera", "aljazeera", "半岛", "半岛电视台"]
        if "bbc" in normalized or "英国广播公司" in normalized:
            source = "bbc"
        elif "cnn" in normalized or "有线电视新闻网" in normalized:
            source = "cnn"
        elif any(s in normalized for s in aljazeera_keys):
            source = "aljazeera"
        elif "fox" in normalized or "福克斯" in normalized:
            source = "fox"

        # 4. Clean and extract query keyword
        cleaned = normalized
        cleaned = re.sub(r"[?!.,;:\"'()\[\]{}#$]+", " ", cleaned)

        noise_phrases = [
            "最近24小时",
            "24小时内",
            "最近48小时",
            "最近7天",
            "过去一周",
            "今天",
            "last 24 hours",
            "past 24 hours",
            "last 24h",
            "past week",
            "last 7 days",
            "today",
            "24小时",
            "48小时",
            "bbc news",
            "bbc",
            "cnn news",
            "cnn",
            "al jazeera",
            "aljazeera",
            "fox news",
            "fox",
            "半岛电视台",
            "半岛",
            "英国广播公司",
            "有线电视新闻网",
            "福克斯",
            "tell me about",
            "what did",
            "what is",
            "what are",
            "search for",
            "show me",
            "find",
            "report about",
            "reports on",
            "give me",
            "can you tell me",
            "what happened with",
            "请问",
            "查询",
            "查找",
            "总结",
            "告诉我",
            "有哪些",
            "有什么",
            "最新报道",
            "新闻",
            "报道",
            "news",
            "reports",
            "headlines",
            "recent",
            "latest",
            "update",
            "updates",
            "关于",
            "的",
        ]
        noise_phrases.sort(key=len, reverse=True)
        for phrase in noise_phrases:
            cleaned = re.sub(r"\b" + re.escape(phrase) + r"\b", " ", cleaned)
            cleaned = cleaned.replace(phrase, " ")

        query_tokens = [w.strip() for w in cleaned.split() if len(w.strip()) > 1]
        query = " ".join(query_tokens).strip()

        # Handle cross-lingual synonyms for query
        if "人工智能" in query:
            query = query.replace("人工智能", "AI").strip()

        if not query:
            if category == "technology":
                query = "AI"
            elif category:
                query = category
            else:
                query = prompt.strip()

        result: dict[str, Any] = {"query": query}
        if start_time:
            result["start_time"] = start_time
        if end_time:
            result["end_time"] = end_time
        if category:
            result["category"] = category
        if source:
            result["source"] = source

        return result
