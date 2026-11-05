"""Fox News RSS collector adapter."""

from app.services.collectors.base import BaseCollector


class FoxNewsCollector(BaseCollector):
    """Collector for Fox News World RSS feed."""

    source_id = "fox"
    source_name = "Fox News"
    rss_url = "https://moxie.foxnews.com/google-publisher/world.xml"
    home_url = "https://www.foxnews.com"
    default_category = "world"
