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
    is_commercial_affiliate_url,
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
    image_url: str | None = None
    image_credit: str | None = None
    image_width: int | None = None
    image_height: int | None = None
    image_mime_type: str | None = None


def extract_media_from_entry(
    entry: object,
) -> tuple[str | None, str | None, int | None, int | None, str | None]:
    """Extract primary image URL, credit, dimensions, and mime_type.

    Strictly inspects standard RSS media elements: media:content, media:thumbnail,
    and enclosure. Never scrapes full-text web pages.
    """
    image_url: str | None = None
    credit: str | None = None
    width: int | None = None
    height: int | None = None
    mime_type: str | None = None

    # 1. media:credit or credit attribute
    raw_credit = getattr(entry, "media_credit", None)
    if isinstance(raw_credit, list) and raw_credit:
        first = raw_credit[0]
        if isinstance(first, dict):
            credit = first.get("content") or first.get("label")
        elif isinstance(first, str) and first.strip():
            credit = first.strip()
    elif isinstance(raw_credit, dict):
        credit = raw_credit.get("content") or raw_credit.get("label")
    elif isinstance(raw_credit, str) and raw_credit.strip():
        credit = raw_credit.strip()

    if not credit:
        direct_credit = getattr(entry, "credit", None)
        if isinstance(direct_credit, str) and direct_credit.strip():
            credit = direct_credit.strip()

    # 2. media_content list
    media_content = getattr(entry, "media_content", [])
    if isinstance(media_content, list):
        for item in media_content:
            if not isinstance(item, dict):
                continue
            url = item.get("url", "")
            m_type = item.get("type", "")
            medium = item.get("medium", "")
            is_image = (
                medium == "image"
                or (isinstance(m_type, str) and m_type.startswith("image/"))
                or any(
                    url.lower().endswith(ext)
                    for ext in (".jpg", ".jpeg", ".png", ".webp", ".gif")
                )
            )
            if url and is_image:
                image_url = url
                mime_type = m_type or "image/jpeg"
                w = item.get("width")
                h = item.get("height")
                try:
                    width = int(w) if w is not None else None
                except (ValueError, TypeError):
                    width = None
                try:
                    height = int(h) if h is not None else None
                except (ValueError, TypeError):
                    height = None
                if not credit and item.get("credit"):
                    credit = str(item.get("credit")).strip()
                break

    # 3. media_thumbnail list (if media_content did not yield an image)
    if not image_url:
        media_thumbnails = getattr(entry, "media_thumbnail", [])
        if isinstance(media_thumbnails, list):
            for thumb in media_thumbnails:
                if isinstance(thumb, dict) and thumb.get("url"):
                    image_url = thumb["url"]
                    mime_type = "image/jpeg"
                    w = thumb.get("width")
                    h = thumb.get("height")
                    try:
                        width = int(w) if w is not None else None
                    except (ValueError, TypeError):
                        width = None
                    try:
                        height = int(h) if h is not None else None
                    except (ValueError, TypeError):
                        height = None
                    break

    # 4. enclosures
    if not image_url:
        enclosures = getattr(entry, "enclosures", [])
        if isinstance(enclosures, list):
            for enc in enclosures:
                if not isinstance(enc, dict):
                    continue
                href = enc.get("href") or enc.get("url")
                enc_type = enc.get("type", "")
                if href and (
                    (isinstance(enc_type, str) and enc_type.startswith("image/"))
                    or any(
                        href.lower().endswith(ext)
                        for ext in (".jpg", ".jpeg", ".png", ".webp", ".gif")
                    )
                ):
                    image_url = href
                    mime_type = enc_type or "image/jpeg"
                    break

    return image_url, credit, width, height, mime_type


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
            if not canonical_url or is_commercial_affiliate_url(canonical_url):
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

            # Media metadata extraction
            img_url, img_credit, img_w, img_h, img_mime = extract_media_from_entry(
                entry
            )

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
                    image_url=img_url,
                    image_credit=img_credit,
                    image_width=img_w,
                    image_height=img_h,
                    image_mime_type=img_mime,
                )
            )

        logger.info("Parsed %d valid articles from %s", len(articles), self.source_name)
        return articles

    async def collect(self, client: httpx.AsyncClient) -> list[RawArticleData]:
        """Fetch and parse feed in one pipeline."""
        xml = await self.fetch_feed_xml(client)
        return self.parse_feed_content(xml)
