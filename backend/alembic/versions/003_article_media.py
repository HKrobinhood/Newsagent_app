"""create article_media table

Revision ID: 003_article_media
Revises: 002_user_and_preferences
Create Date: 2026-11-12 20:07:00.000000

"""

from collections.abc import Sequence

import sqlalchemy as sa
from alembic import op

# revision identifiers, used by Alembic.
revision: str = "003_article_media"
down_revision: str | None = "002_user_and_preferences"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    """Create article_media table."""
    op.create_table(
        "article_media",
        sa.Column("id", sa.String(length=36), nullable=False),
        sa.Column("article_id", sa.String(length=36), nullable=False),
        sa.Column(
            "media_type",
            sa.String(length=50),
            nullable=False,
            server_default="image",
        ),
        sa.Column("image_url", sa.String(length=1000), nullable=False),
        sa.Column("thumbnail_data", sa.LargeBinary(), nullable=True),
        sa.Column("storage_key", sa.String(length=255), nullable=True),
        sa.Column("mime_type", sa.String(length=100), nullable=True),
        sa.Column("width", sa.Integer(), nullable=True),
        sa.Column("height", sa.Integer(), nullable=True),
        sa.Column("image_source", sa.String(length=100), nullable=True),
        sa.Column("credit", sa.String(length=255), nullable=True),
        sa.Column(
            "license_status",
            sa.String(length=100),
            nullable=True,
            server_default="rss_fair_use",
        ),
        sa.Column(
            "created_at",
            sa.DateTime(timezone=True),
            server_default=sa.text("CURRENT_TIMESTAMP"),
            nullable=False,
        ),
        sa.ForeignKeyConstraint(
            ["article_id"], ["news_articles.id"], ondelete="CASCADE"
        ),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(
        op.f("ix_article_media_article_id"),
        "article_media",
        ["article_id"],
        unique=False,
    )
    op.create_index(
        op.f("ix_article_media_created_at"),
        "article_media",
        ["created_at"],
        unique=False,
    )


def downgrade() -> None:
    """Drop article_media table."""
    op.drop_index(op.f("ix_article_media_created_at"), table_name="article_media")
    op.drop_index(op.f("ix_article_media_article_id"), table_name="article_media")
    op.drop_table("article_media")
