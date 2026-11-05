"""Security utilities for Prompt Injection defense and content sanitization."""

import html
import re

# Malicious prompt injection patterns to disarm
INJECTION_PATTERNS = [
    r"(?i)ignore\s+(all\s+)?(previous|prior)\s+instructions?",
    r"(?i)you\s+are\s+now\s+(DAN|unfiltered|jailbroken)",
    r"(?i)system\s*prompt\s*:",
    r"(?i)<\|im_start\|>",
    r"(?i)<\|im_end\|>",
    r"(?i)\[system\]",
    r"(?i)new\s+system\s+instruction",
    r"(?i)override\s+system\s+directives?",
    r"(?i)disregard\s+the\s+above",
]


def sanitize_web_content(text: str, max_chars: int = 1000) -> str:
    """Sanitize retrieved external web text to defend against prompt injection.

    Args:
        text: Raw text or snippet from search results.
        max_chars: Maximum allowable character length to prevent context flooding.

    Returns:
        Sanitized, plain-text string with disarmed injection patterns.
    """
    if not text:
        return ""

    # 1. Unescape HTML and remove HTML/script tags
    cleaned = html.unescape(text)
    cleaned = re.sub(r"<[^>]+>", " ", cleaned)

    # 2. Neutralize known Prompt Injection attack signatures
    for pattern in INJECTION_PATTERNS:
        cleaned = re.sub(pattern, "[FILTERED_INSTRUCTION]", cleaned)

    # 3. Collapse multiple whitespace and newlines
    cleaned = re.sub(r"\s+", " ", cleaned).strip()

    # 4. Truncate to safe length
    if len(cleaned) > max_chars:
        cleaned = cleaned[:max_chars].rstrip() + "..."

    return cleaned
