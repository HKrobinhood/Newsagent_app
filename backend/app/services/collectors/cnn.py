"""CNN News RSS collector adapter."""

from app.services.collectors.base import BaseCollector


class CNNCollector(BaseCollector):
    """Collector for CNN World News RSS feed."""

    source_id = "cnn"
    source_name = "CNN News"
    rss_url = "http://rss.cnn.com/rss/edition_world.rss"
    home_url = "https://edition.cnn.com"
    default_category = "world"
