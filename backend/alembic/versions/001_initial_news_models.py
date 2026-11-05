"""initial news models

Revision ID: 001_initial_news_models
Revises:
Create Date: 2026-11-03 19:26:00.000000

"""

from typing import Sequence, Union
from alembic import op
import sqlalchemy as sa

# revision identifiers, used by Alembic.
revision: str = "001_initial_news_models"
down_revision: Union[str, None] = None
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "news_sources",
        sa.Column("id", sa.String(length=50), nullable=False),
        sa.Column("name", sa.String(length=100), nullable=False),
        sa.Column("rss_url", sa.String(length=500), nullable=False),
        sa.Column("home_url", sa.String(length=500), nullable=False),
        sa.Column("is_active", sa.Boolean(), nullable=False, server_default=sa.true()),
        sa.Column("last_polled", sa.DateTime(timezone=True), nullable=True),
        sa.PrimaryKeyConstraint("id"),
    )

    op.create_table(
        "news_articles",
        sa.Column("id", sa.String(length=36), nullable=False),
        sa.Column("title", sa.String(length=500), nullable=False),
        sa.Column("source_id", sa.String(length=50), nullable=False),
        sa.Column("source_name", sa.String(length=100), nullable=False),
        sa.Column("published_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("original_url", sa.String(length=1000), nullable=False),
        sa.Column("category", sa.String(length=50), nullable=False),
        sa.Column("location_name", sa.String(length=100), nullable=True),
        sa.Column("latitude", sa.Float(), nullable=True),
        sa.Column("longitude", sa.Float(), nullable=True),
        sa.Column(
            "created_at",
            sa.DateTime(timezone=True),
            server_default=sa.text("CURRENT_TIMESTAMP"),
            nullable=False,
        ),
        sa.ForeignKeyConstraint(
            ["source_id"], ["news_sources.id"], ondelete="CASCADE"
        ),
        sa.PrimaryKeyConstraint("id"),
    )

    op.create_index(
        op.f("ix_news_articles_category"),
        "news_articles",
        ["category"],
        unique=False,
    )
    op.create_index(
        op.f("ix_news_articles_original_url"),
        "news_articles",
        ["original_url"],
        unique=True,
    )
    op.create_index(
        op.f("ix_news_articles_published_at"),
        "news_articles",
        ["published_at"],
        unique=False,
    )
    op.create_index(
        op.f("ix_news_articles_source_id"),
        "news_articles",
        ["source_id"],
        unique=False,
    )
    op.create_index(
        op.f("ix_news_articles_title"),
        "news_articles",
        ["title"],
        unique=False,
    )


def downgrade() -> None:
    op.drop_index(
        op.f("ix_news_articles_title"), table_name="news_articles"
    )
    op.drop_index(
        op.f("ix_news_articles_source_id"), table_name="news_articles"
    )
    op.drop_index(
        op.f("ix_news_articles_published_at"), table_name="news_articles"
    )
    op.drop_index(
        op.f("ix_news_articles_original_url"), table_name="news_articles"
    )
    op.drop_index(
        op.f("ix_news_articles_category"), table_name="news_articles"
    )
    op.drop_table("news_articles")
    op.drop_table("news_sources")
