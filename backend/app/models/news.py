"""SQLAlchemy models for NewsArticle and NewsSource."""

import uuid
from datetime import datetime

from sqlalchemy import Boolean, DateTime, Float, ForeignKey, String, func
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.core.database import Base


class NewsSource(Base):
    """Media source definition entity."""

    __tablename__ = "news_sources"

    id: Mapped[str] = mapped_column(String(50), primary_key=True)
    name: Mapped[str] = mapped_column(String(100), nullable=False)
    rss_url: Mapped[str] = mapped_column(String(500), nullable=False)
    home_url: Mapped[str] = mapped_column(String(500), nullable=False)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)
    last_polled: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )

    articles: Mapped[list["NewsArticle"]] = relationship(
        "NewsArticle", back_populates="source", cascade="all, delete-orphan"
    )

    def __repr__(self) -> str:
        """String representation of NewsSource."""
        return f"<NewsSource(id={self.id}, name={self.name})>"


class NewsArticle(Base):
    """News article metadata entity.

    Note:
        Full article text is strictly not persisted to honor copyright rules.
    """

    __tablename__ = "news_articles"

    id: Mapped[str] = mapped_column(
        String(36), primary_key=True, default=lambda: str(uuid.uuid4())
    )
    title: Mapped[str] = mapped_column(String(500), nullable=False, index=True)
    source_id: Mapped[str] = mapped_column(
        String(50),
        ForeignKey("news_sources.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )
    source_name: Mapped[str] = mapped_column(String(100), nullable=False)
    published_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, index=True
    )
    original_url: Mapped[str] = mapped_column(
        String(1000), nullable=False, unique=True, index=True
    )
    category: Mapped[str] = mapped_column(String(50), nullable=False, index=True)
    location_name: Mapped[str | None] = mapped_column(String(100), nullable=True)
    latitude: Mapped[float | None] = mapped_column(Float, nullable=True)
    longitude: Mapped[float | None] = mapped_column(Float, nullable=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )

    source: Mapped[NewsSource] = relationship("NewsSource", back_populates="articles")

    def __repr__(self) -> str:
        """String representation of NewsArticle."""
        return f"<NewsArticle(id={self.id}, title={self.title[:30]}...)>"
