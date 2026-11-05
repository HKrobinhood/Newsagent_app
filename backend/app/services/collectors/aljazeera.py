"""Al Jazeera News RSS collector adapter."""

from app.services.collectors.base import BaseCollector


class AlJazeeraCollector(BaseCollector):
    """Collector for Al Jazeera English News RSS feed."""

    source_id = "aljazeera"
    source_name = "Al Jazeera"
    rss_url = "https://www.aljazeera.com/xml/rss/all.xml"
    home_url = "https://www.aljazeera.com"
    default_category = "world"
