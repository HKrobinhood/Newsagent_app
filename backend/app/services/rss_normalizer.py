"""Normalizer utilities for RSS article titles, dates, URLs, and categories."""

import html
import re
import time
from datetime import datetime, timezone
from typing import Any
from urllib.parse import parse_qsl, urlencode, urlparse, urlunparse

from bs4 import BeautifulSoup


def clean_title(raw_title: str) -> str:
    """Strip HTML tags and unescape HTML entities from article titles.

    Args:
        raw_title: Raw title string potentially containing HTML markup.

    Returns:
        Cleaned, plain-text title string.
    """
    if not raw_title:
        return ""

    # Parse with BeautifulSoup to strip HTML markup
    soup = BeautifulSoup(raw_title, "html.parser")
    text = soup.get_text()

    # Unescape HTML entities (&amp; -> &, &quot; -> ", etc.)
    text = html.unescape(text)

    # Collapse repeated whitespace
    text = re.sub(r"\s+", " ", text)
    return text.strip()


def parse_published_date(entry: Any) -> datetime:
    """Extract and normalize publication date to UTC datetime.

    Args:
        entry: feedparser entry dictionary/object.

    Returns:
        datetime object with UTC timezone.
    """
    # 1. Check feedparser structured time tuple
    parsed_time = getattr(entry, "published_parsed", None) or getattr(
        entry, "updated_parsed", None
    )
    if parsed_time:
        try:
            return datetime.fromtimestamp(time.mktime(parsed_time), tz=timezone.utc)
        except Exception:
            pass

    # 2. Check string date representations
    date_str = getattr(entry, "published", None) or getattr(entry, "pubDate", None)
    if isinstance(date_str, str):
        # Try ISO format
        try:
            dt = datetime.fromisoformat(date_str.replace("Z", "+00:00"))
            return dt.astimezone(timezone.utc)
        except Exception:
            pass

    # Fallback to current UTC time if no valid timestamp found
    return datetime.now(timezone.utc)


def canonicalize_url(raw_url: str) -> str:
    """Clean and standardize article original URL by stripping tracking parameters.

    Args:
        raw_url: Raw URL string.

    Returns:
        Canonicalized URL.
    """
    if not raw_url:
        return ""

    parsed = urlparse(raw_url.strip())

    # Tracking query parameter prefixes to strip
    tracking_prefixes = ("utm_", "fbclid", "gclid", "ref", "feed")
    filtered_query = [
        (k, v)
        for k, v in parse_qsl(parsed.query, keep_blank_values=True)
        if not any(k.lower().startswith(p) for p in tracking_prefixes)
    ]

    new_query = urlencode(filtered_query)
    clean_parts = (
        parsed.scheme,
        parsed.netloc,
        parsed.path.rstrip("/") if parsed.path != "/" else "/",
        parsed.params,
        new_query,
        "",  # Strip fragment identifier (#...)
    )
    return urlunparse(clean_parts)


def infer_category(title: str, tags: list[str] | None = None) -> str:
    """Infer standard category from title and feed tags.

    Standard categories: world, technology, business, science, health.

    Args:
        title: Cleaned article headline.
        tags: Optional list of raw category tags from RSS feed.

    Returns:
        One of the 5 standard categories (default: 'world').
    """
    search_space = (title + " " + " ".join(tags or [])).lower()

    if any(
        kw in search_space
        for kw in (
            "tech",
            "ai",
            "quantum",
            "software",
            "chip",
            "semiconductor",
            "apple",
            "google",
            "microsoft",
            "cyber",
            "robot",
        )
    ):
        return "technology"

    if any(
        kw in search_space
        for kw in (
            "business",
            "market",
            "economy",
            "inflation",
            "bank",
            "stock",
            "trade",
            "tariff",
            "oil",
            "fed",
        )
    ):
        return "business"

    if any(
        kw in search_space
        for kw in (
            "science",
            "space",
            "nasa",
            "telescope",
            "physics",
            "climate",
            "planet",
            "renewable",
            "energy",
        )
    ):
        return "science"

    if any(
        kw in search_space
        for kw in (
            "health",
            "medical",
            "vaccine",
            "hospital",
            "doctor",
            "virus",
            "disease",
            "pandemic",
            "who",
        )
    ):
        return "health"

    return "world"
