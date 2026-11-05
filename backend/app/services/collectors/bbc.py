"""BBC News RSS collector adapter."""

from app.services.collectors.base import BaseCollector


class BBCCollector(BaseCollector):
    """Collector for BBC World News RSS feed."""

    source_id = "bbc"
    source_name = "BBC News"
    rss_url = "http://feeds.bbci.co.uk/news/world/rss.xml"
    home_url = "https://www.bbc.com/news"
    default_category = "world"
