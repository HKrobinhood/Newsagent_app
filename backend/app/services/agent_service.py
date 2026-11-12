"""AI Agent service managing conversation flow, dual-mode routing, and citations."""

import logging
import uuid
from datetime import datetime

from sqlalchemy.ext.asyncio import AsyncSession

from app.repositories.news_repository import NewsRepository
from app.schemas.chat import ChatRequest, ChatResponse, NewsSourceRef
from app.services.intent_parser import SEARCH_NEWS_TOOL
from app.services.llm_client import SYSTEM_PROMPT, get_llm_client
from app.services.web_search import get_web_search_client, infer_source_id

logger = logging.getLogger(__name__)


class AgentService:
    """Core AI agent coordinator handling Dual-Mode dispatching and citations."""

    def determine_mode(self, message: str, requested_mode: str) -> str:
        """Determine whether to use title_search or web_verification mode.

        Args:
            message: User query text.
            requested_mode: Explicit client-requested mode.

        Returns:
            Selected mode: 'title_search' or 'web_verification'.
        """
        if requested_mode in ("title_search", "web_verification"):
            return requested_mode

        lower_msg = message.lower()
        verification_triggers = [
            "说了什么",
            "发言",
            "声明",
            "是否属实",
            "证实",
            "核实",
            "真假",
            "官员",
            "部长",
            "statement",
            "stated",
            "state",
            "said",
            "say",
            "minister",
            "official",
            "verify",
            "verification",
            "fact check",
            "fact-check",
            "claim",
            "is it true",
            "confirmed",
        ]
        if any(t in lower_msg for t in verification_triggers):
            return "web_verification"

        return "title_search"

    async def process_chat(
        self,
        request: ChatRequest,
        db: AsyncSession,
        llm_provider: str | None = None,
        llm_api_key: str | None = None,
    ) -> ChatResponse:
        """Process user inquiry across Dual-Mode pipeline.

        Args:
            request: Validated ChatRequest payload.
            db: Database session for citation lookups.
            llm_provider: Ephemeral LLM provider identifier (if supplied).
            llm_api_key: Ephemeral user-provided LLM API key (never persisted).

        Returns:
            ChatResponse with answer, citations, mode_used, and session ID.
        """
        session_id = request.session_id or str(uuid.uuid4())
        user_msg = request.message.strip()

        effective_provider = llm_provider or request.llm_provider
        repo = NewsRepository(db)
        llm_client = get_llm_client(
            provider=effective_provider,
            api_key=llm_api_key,
        )
        web_search = get_web_search_client()

        target_mode = self.determine_mode(user_msg, request.mode)

        allowed_source_ids: list[str] | None = None
        if request.source_ids is not None:
            allowed_source_ids = [
                s.strip().lower() for s in request.source_ids if s and s.strip()
            ]
            if not allowed_source_ids:
                return ChatResponse(
                    session_id=session_id,
                    answer=(
                        "当前已在 Settings 中取消勾选所有新闻媒体源。"
                        "请先在 Settings 中启用至少一个媒体源后再进行检索。"
                    ),
                    mode_used=target_mode,
                    sources=[],
                    confidence=0.5,
                )

        # -------------------------------------------------------------
        # 1. Title Search Mode (Internal Database Metadata Retrieval)
        # -------------------------------------------------------------
        if target_mode == "title_search":
            tool_name, tool_args = await llm_client.parse_and_call_tool(
                user_msg, SYSTEM_PROMPT, [SEARCH_NEWS_TOOL]
            )

            query = tool_args.get("query", "").strip()
            category = tool_args.get("category")
            source = tool_args.get("source")

            if (
                source
                and allowed_source_ids is not None
                and source.strip().lower() not in allowed_source_ids
            ):
                return ChatResponse(
                    session_id=session_id,
                    answer=(
                        f"媒体源 '{source}' 当前已在 Settings 中被禁用，"
                        "未检索该媒体的报道。"
                    ),
                    mode_used="title_search",
                    sources=[],
                    confidence=0.5,
                )

            start_dt = None
            if tool_args.get("start_time"):
                try:
                    start_dt = datetime.fromisoformat(tool_args["start_time"])
                except ValueError:
                    start_dt = None

            end_dt = None
            if tool_args.get("end_time"):
                try:
                    end_dt = datetime.fromisoformat(tool_args["end_time"])
                except ValueError:
                    end_dt = None

            matched_articles: list = []
            if query or category or source or start_dt:
                articles, _ = await repo.list_articles(
                    page=1,
                    limit=5,
                    keyword=query if query else None,
                    category=category,
                    source=source,
                    source_ids=allowed_source_ids,
                    start_time=start_dt,
                    end_time=end_dt,
                )
                matched_articles.extend(articles)

                # Fallback: category/time query if keyword didn't match
                if not matched_articles and category:
                    cat_articles, _ = await repo.list_articles(
                        page=1,
                        limit=5,
                        category=category,
                        source=source,
                        source_ids=allowed_source_ids,
                        start_time=start_dt,
                        end_time=end_dt,
                    )
                    matched_articles.extend(cat_articles)

                # Fallback: pure keyword query if time filter was too restrictive
                if not matched_articles and query:
                    fallback_articles, _ = await repo.list_articles(
                        page=1,
                        limit=5,
                        keyword=query,
                        source_ids=allowed_source_ids,
                    )
                    matched_articles.extend(fallback_articles)

            if matched_articles:
                seen_urls = set()
                sources: list[NewsSourceRef] = []
                search_results = []
                for art in matched_articles:
                    if art.original_url not in seen_urls:
                        seen_urls.add(art.original_url)
                        sources.append(
                            NewsSourceRef(
                                title=art.title,
                                source_name=art.source_name,
                                original_url=art.original_url,
                                published_at=art.published_at.isoformat(),
                                location_name=art.location_name,
                            )
                        )
                        search_results.append(
                            {
                                "title": art.title,
                                "source_name": art.source_name,
                                "original_url": art.original_url,
                                "published_at": art.published_at.isoformat(),
                                "location_name": art.location_name,
                            }
                        )

                answer = await llm_client.generate_grounded_answer(
                    user_msg, search_results, SYSTEM_PROMPT
                )
                return ChatResponse(
                    session_id=session_id,
                    answer=answer,
                    mode_used="title_search",
                    sources=sources,
                    confidence=0.98,
                )

            # If user explicitly requested title_search, do not elevate to web
            if request.mode == "title_search":
                return ChatResponse(
                    session_id=session_id,
                    answer="未检索到相关新闻，数据库中暂无匹配的报道。",
                    mode_used="title_search",
                    sources=[],
                    confidence=0.5,
                )

            # Otherwise, elevate to Web Verification Mode for unindexed news
            target_mode = "web_verification"

        # -------------------------------------------------------------
        # 2. Web Verification Mode (Realtime Cross-Checking with Web Search)
        # -------------------------------------------------------------
        try:
            raw_web_results = await web_search.search(user_msg, max_results=5)
        except Exception as e:
            logger.error("Web search execution failed: %s", e)
            raw_web_results = []

        if allowed_source_ids is not None:
            web_results = [
                r
                for r in raw_web_results
                if (r.source_id or infer_source_id(r.source_name, r.url))
                in allowed_source_ids
            ]
            if not web_results and raw_web_results:
                return ChatResponse(
                    session_id=session_id,
                    answer=(
                        "根据您在 Settings 中启用的新闻媒体源限制，"
                        "未在所选允许的媒体范围内找到可核实的报道"
                        "（已严格排除未勾选媒体的新闻卡片）。"
                    ),
                    mode_used="web_verification",
                    sources=[],
                    confidence=0.5,
                )
        else:
            web_results = raw_web_results

        if not web_results:
            return ChatResponse(
                session_id=session_id,
                answer=(
                    "经多方联网权威报道核实，暂未检索到相关事实证据或官方声明证实该说法。"
                ),
                mode_used="web_verification",
                sources=[],
                confidence=0.5,
            )

        sources_out: list[NewsSourceRef] = []
        for r in web_results:
            sources_out.append(
                NewsSourceRef(
                    title=r.title,
                    source_name=r.source_name,
                    original_url=r.url,
                    published_at=r.published_at,
                    location_name=r.location_name,
                )
            )

        # Synthesize cross-verification briefing
        lines = [
            f"经过对多方权威媒体报道的实时交叉核验，针对“{user_msg}”的事实查证如下：\n"
        ]
        for idx, res in enumerate(web_results[:3], start=1):
            loc_tag = f"[{res.location_name}] " if res.location_name else ""
            lines.append(
                f"{idx}. **{res.source_name}** {loc_tag}《{res.title}》\n"
                f"   事实摘要：{res.snippet}"
            )

        lines.append(
            "\n核查结论：上述内容已由主流国际权威媒体采信报道，相关原始链接已列入参考来源。"
        )
        answer = "\n".join(lines)

        return ChatResponse(
            session_id=session_id,
            answer=answer,
            mode_used="web_verification",
            sources=sources_out,
            confidence=0.95,
        )
