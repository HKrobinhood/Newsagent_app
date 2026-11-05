"""Background scheduler for periodic RSS ingestion using APScheduler."""

import logging

from apscheduler.schedulers.asyncio import AsyncIOScheduler

from app.core.database import AsyncSessionLocal
from app.services.collector_service import NewsCollectorService

logger = logging.getLogger(__name__)


class CollectorScheduler:
    """Manages scheduled background execution of news collectors."""

    def __init__(self, interval_minutes: int = 15) -> None:
        """Initialize scheduler with configurable polling interval."""
        self.scheduler = AsyncIOScheduler()
        self.interval_minutes = interval_minutes
        self.service = NewsCollectorService()

    async def _scheduled_job(self) -> None:
        """Job executed on each timer trigger."""
        logger.info("Executing scheduled periodic news collection job...")
        async with AsyncSessionLocal() as session:
            try:
                results = await self.service.collect_all(session)
                for res in results:
                    if res.success:
                        logger.info(
                            "Periodic collection [%s]: +%d new",
                            res.source_name,
                            res.inserted_count,
                        )
                    else:
                        logger.error(
                            "Periodic collection [%s] failed: %s",
                            res.source_name,
                            res.error_message,
                        )
            except Exception as e:
                logger.error("Error during scheduled collection: %s", e)

    def start(self) -> None:
        """Start the background scheduler."""
        if not self.scheduler.running:
            self.scheduler.add_job(
                self._scheduled_job,
                "interval",
                minutes=self.interval_minutes,
                id="periodic_rss_collection",
                replace_existing=True,
            )
            self.scheduler.start()
            logger.info(
                "CollectorScheduler started (interval: %d minutes)",
                self.interval_minutes,
            )

    def shutdown(self) -> None:
        """Stop the background scheduler."""
        if self.scheduler.running:
            self.scheduler.shutdown(wait=False)
            logger.info("CollectorScheduler shut down.")
