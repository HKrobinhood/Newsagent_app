"""News collectors package."""

from app.services.collectors.aljazeera import AlJazeeraCollector
from app.services.collectors.base import BaseCollector, RawArticleData
from app.services.collectors.bbc import BBCCollector
from app.services.collectors.cnn import CNNCollector
from app.services.collectors.fox import FoxNewsCollector

ALL_COLLECTORS: list[BaseCollector] = [
    BBCCollector(),
    CNNCollector(),
    AlJazeeraCollector(),
    FoxNewsCollector(),
]

__all__ = [
    "ALL_COLLECTORS",
    "AlJazeeraCollector",
    "BBCCollector",
    "BaseCollector",
    "CNNCollector",
    "FoxNewsCollector",
    "RawArticleData",
]
