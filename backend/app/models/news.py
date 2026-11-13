"""SQLAlchemy models for NewsArticle and NewsSource."""

import uuid
from datetime import datetime

from sqlalchemy import (
    Boolean,
    DateTime,
    Float,
    ForeignKey,
    Integer,
    LargeBinary,
    String,
    func,
)
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
    media_items: Mapped[list["ArticleMedia"]] = relationship(
        "ArticleMedia",
        back_populates="article",
        cascade="all, delete-orphan",
        lazy="selectin",
    )

    @property
    def primary_media(self) -> "ArticleMedia | None":
        """Return the primary media attachment if available."""
        return self.media_items[0] if self.media_items else None

    def __repr__(self) -> str:
        """String representation of NewsArticle."""
        return f"<NewsArticle(id={self.id}, title={self.title[:30]}...)>"


class ArticleMedia(Base):
    """Article media metadata and thumbnail entity.

    Conforms to N2 Editorial News specification.
    """

    __tablename__ = "article_media"

    id: Mapped[str] = mapped_column(
        String(36), primary_key=True, default=lambda: str(uuid.uuid4())
    )
    article_id: Mapped[str] = mapped_column(
        String(36),
        ForeignKey("news_articles.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )
    media_type: Mapped[str] = mapped_column(String(50), nullable=False, default="image")
    image_url: Mapped[str] = mapped_column(String(1000), nullable=False)
    thumbnail_data: Mapped[bytes | None] = mapped_column(LargeBinary, nullable=True)
    storage_key: Mapped[str | None] = mapped_column(String(255), nullable=True)
    mime_type: Mapped[str | None] = mapped_column(String(100), nullable=True)
    width: Mapped[int | None] = mapped_column(Integer, nullable=True)
    height: Mapped[int | None] = mapped_column(Integer, nullable=True)
    image_source: Mapped[str | None] = mapped_column(String(100), nullable=True)
    credit: Mapped[str | None] = mapped_column(String(255), nullable=True)
    license_status: Mapped[str | None] = mapped_column(
        String(100), nullable=True, default="rss_fair_use"
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )

    article: Mapped[NewsArticle] = relationship(
        "NewsArticle", back_populates="media_items", lazy="selectin"
    )

    @property
    def thumbnail_url(self) -> str:
        """Dynamic endpoint URL to serve this media's thumbnail."""
        return f"/api/v1/media/{self.id}/thumbnail"

    def __repr__(self) -> str:
        """String representation of ArticleMedia."""
        return (
            f"<ArticleMedia(id={self.id}, article_id={self.article_id}, "
            f"url={self.image_url[:30]}...)>"
        )
