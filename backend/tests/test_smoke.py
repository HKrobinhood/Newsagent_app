"""Smoke tests for GlobalNewsAI backend core modules."""

from app import __version__
from app.core.config import settings


def test_app_version():
    """Verify package version is defined."""
    assert __version__ == "0.1.0"


def test_settings_initialization():
    """Verify settings defaults are populated."""
    assert settings.PROJECT_NAME == "GlobalNewsAI Backend"
    assert settings.API_V1_STR == "/api/v1"
