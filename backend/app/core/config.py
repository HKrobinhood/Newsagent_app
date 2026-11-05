"""Application configuration settings."""

import os


class Settings:
    """Project configuration container."""

    PROJECT_NAME: str = "GlobalNewsAI Backend"
    API_V1_STR: str = "/api/v1"
    DEBUG: bool = os.getenv("DEBUG", "False").lower() in ("true", "1", "yes")

    # Database
    DATABASE_URL: str = os.getenv(
        "DATABASE_URL",
        "postgresql+asyncpg://postgres:postgres@localhost:5432/globalnewsai",
    )

    # CORS
    BACKEND_CORS_ORIGINS: list[str] = ["*"]

    # LLM Settings
    LLM_API_KEY: str = os.getenv("LLM_API_KEY", "")
    LLM_MODEL: str = os.getenv("LLM_MODEL", "gemini-1.5-pro")

    # Web Search
    SEARCH_API_KEY: str = os.getenv("SEARCH_API_KEY", "")


settings = Settings()
