"""Abstract base class for news collectors."""

import logging
from abc import ABC
from dataclasses import dataclass
from datetime import datetime

import feedparser
import httpx

from app.services.location_resolver import resolve_location_from_title
from app.services.rss_normalizer import (
    canonicalize_url,
    clean_title,
    infer_category,
    parse_published_date,
)

logger = logging.getLogger(__name__)


@dataclass
class RawArticleData:
    """Standardized news metadata extracted from feed entry."""

    title: str
    source_id: str
    source_name: str
    published_at: datetime
    original_url: str
    category: str
    location_name: str | None = None
    latitude: float | None = None
    longitude: float | None = None


class BaseCollector(ABC):
    """Abstract collector defining RSS pulling, parsing, and normalization."""

    source_id: str
    source_name: str
    rss_url: str
    home_url: str
    default_category: str = "world"

    # User-agent header to avoid bot blockers
    headers = {
        "User-Agent": (
            "Mozilla/5.0 (compatible; GlobalNewsAIBot/1.0; "
            "+https://github.com/GlobalNewsAI)"
        ),
        "Accept": "application/rss+xml, application/xml, text/xml, */*",
    }

    async def fetch_feed_xml(self, client: httpx.AsyncClient) -> str:
        """Fetch raw RSS XML content with timeout and retry handling."""
        logger.info("Fetching feed from %s (%s)", self.source_name, self.rss_url)
        response = await client.get(
            self.rss_url,
            headers=self.headers,
            timeout=15.0,
            follow_redirects=True,
        )
        response.raise_for_status()
        return response.text

    def parse_feed_content(self, xml_content: str) -> list[RawArticleData]:
        """Parse RSS XML content into standardized RawArticleData objects."""
        parsed = feedparser.parse(xml_content)
        articles: list[RawArticleData] = []

        for entry in parsed.entries:
            raw_title = getattr(entry, "title", "")
            cleaned_title = clean_title(raw_title)
            if not cleaned_title:
                continue

            raw_link = getattr(entry, "link", "")
            canonical_url = canonicalize_url(raw_link)
            if not canonical_url:
                continue

            pub_date = parse_published_date(entry)

            # Category inference
            tags = [
                getattr(tag, "term", "")
                for tag in getattr(entry, "tags", [])
                if getattr(tag, "term", "")
            ]
            category = infer_category(cleaned_title, tags)
            if category == "world" and self.default_category != "world":
                category = self.default_category

            # Location extraction
            loc_name, lat, lon = resolve_location_from_title(cleaned_title)

            articles.append(
                RawArticleData(
                    title=cleaned_title,
                    source_id=self.source_id,
                    source_name=self.source_name,
                    published_at=pub_date,
                    original_url=canonical_url,
                    category=category,
                    location_name=loc_name,
                    latitude=lat,
                    longitude=lon,
                )
            )

        logger.info("Parsed %d valid articles from %s", len(articles), self.source_name)
        return articles

    async def collect(self, client: httpx.AsyncClient) -> list[RawArticleData]:
        """Fetch and parse feed in one pipeline."""
        xml = await self.fetch_feed_xml(client)
        return self.parse_feed_content(xml)
