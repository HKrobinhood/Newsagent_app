"""Tests for Milestone M8: 3D Globe Spatial GeoJSON API and Location Disambiguation."""

from datetime import datetime, timezone

import httpx
import pytest
from sqlalchemy.ext.asyncio import AsyncSession

from app.repositories.news_repository import NewsRepository
from app.schemas.news import NewsArticleCreate, NewsSourceCreate
from app.services.location_resolver import resolve_location_from_title


def test_location_resolver_valid_entities() -> None:
    """Verify known cities and nations accurately resolve to coordinates."""
    test_cases = [
        (
            "Global Climate Accord Finalized in Geneva",
            "Geneva, Switzerland",
            46.2044,
            6.1432,
        ),
        (
            "Quantum Computing Demonstration in Tokyo",
            "Tokyo, Japan",
            35.6762,
            139.6503,
        ),
        (
            "Tech Summit Concluded in Munich",
            "Munich, Germany",
            48.1351,
            11.5820,
        ),
        (
            "New Environmental Standards Ratified in Brazil",
            "Brasilia, Brazil",
            -15.7975,
            -47.8919,
        ),
        (
            "Financial Forum Hosted in Singapore",
            "Singapore",
            1.3521,
            103.8198,
        ),
    ]

    for title, expected_name, expected_lat, expected_lon in test_cases:
        loc_name, lat, lon = resolve_location_from_title(title)
        assert loc_name == expected_name
        assert lat is not None and abs(lat - expected_lat) < 0.001
        assert lon is not None and abs(lon - expected_lon) < 0.001


def test_location_resolver_disambiguation_rejection() -> None:
    """Verify non-geographic homonyms are safely rejected (coordinates=None)."""
    ambiguous_titles = [
        "Chef prepares tender New York strip steak with fresh herbs",
        "Rare antique china cup and saucer auctioned in London gallery",
        "Family enjoys traditional turkey dinner on holiday",
        "Quarterly earnings exceed expectations amid economic headwinds",
    ]

    for title in ambiguous_titles[:3]:
        # Strip steak, china cup, turkey dinner shouldn't resolve to location
        loc_name, lat, lon = resolve_location_from_title(title)
        if "London" in title:
            # gallery has London which is valid
            assert loc_name == "London, UK"
        else:
            assert loc_name is None
            assert lat is None
            assert lon is None

    # Title with zero location
    loc_name, lat, lon = resolve_location_from_title(ambiguous_titles[3])
    assert loc_name is None
    assert lat is None
    assert lon is None

    # Regional modifiers and macro reports must NOT be falsely pinned to capital cities
    regional_title = "Cyclone Murasu makes landfall on India's eastern coast"
    loc_name, lat, lon = resolve_location_from_title(regional_title)
    assert loc_name is None
    assert lat is None
    assert lon is None

    macro_title = (
        "How India pulled 500 million people out of poverty as inequality deepens"
    )
    loc_name, lat, lon = resolve_location_from_title(macro_title)
    assert loc_name is None
    assert lat is None
    assert lon is None

    # Explicit city within India still resolves accurately to New Delhi
    delhi_title = "Diplomatic representatives meet in New Delhi for bilateral talks"
    loc_name, lat, lon = resolve_location_from_title(delhi_title)
    assert loc_name == "New Delhi, India"
    assert lat is not None and abs(lat - 28.6139) < 0.001
    assert lon is not None and abs(lon - 77.2090) < 0.001


@pytest.mark.asyncio
async def test_get_geolocated_news_empty(client: httpx.AsyncClient) -> None:
    """Verify GET /api/v1/news/geo returns valid empty GeoJSON FeatureCollection."""
    response = await client.get("/api/v1/news/geo")
    assert response.status_code == 200
    data = response.json()

    assert data["type"] == "FeatureCollection"
    assert isinstance(data["features"], list)
    assert len(data["features"]) == 0


@pytest.mark.asyncio
async def test_get_geolocated_news_with_data(
    client: httpx.AsyncClient, db_session: AsyncSession
) -> None:
    """Verify GeoJSON format compliance and that non-geolocated news are excluded."""
    repo = NewsRepository(db_session)
    await repo.create_source(
        NewsSourceCreate(
            id="bbc",
            name="BBC News",
            rss_url="https://bbc.com/rss",
            home_url="https://bbc.com",
        )
    )

    now = datetime.now(timezone.utc)

    # 1. Geolocated Article A (Geneva)
    await repo.create_article(
        NewsArticleCreate(
            title="Climate Framework in Geneva",
            source_id="bbc",
            source_name="BBC News",
            published_at=now,
            original_url="https://bbc.com/news/geneva-1",
            category="science",
            location_name="Geneva, Switzerland",
            latitude=46.2044,
            longitude=6.1432,
        )
    )

    # 2. Geolocated Article B (Tokyo)
    await repo.create_article(
        NewsArticleCreate(
            title="Tokyo AI Exhibition",
            source_id="bbc",
            source_name="BBC News",
            published_at=now,
            original_url="https://bbc.com/news/tokyo-2",
            category="technology",
            location_name="Tokyo, Japan",
            latitude=35.6762,
            longitude=139.6503,
        )
    )

    # 3. Non-geolocated Article C (Null coordinates)
    await repo.create_article(
        NewsArticleCreate(
            title="Macroeconomic Trends and Global Markets",
            source_id="bbc",
            source_name="BBC News",
            published_at=now,
            original_url="https://bbc.com/news/macro-3",
            category="business",
            location_name=None,
            latitude=None,
            longitude=None,
        )
    )

    # Fetch all geolocated news
    response = await client.get("/api/v1/news/geo")
    assert response.status_code == 200
    data = response.json()

    assert data["type"] == "FeatureCollection"
    # Exactly 2 features; Article C without coordinates must be excluded
    assert len(data["features"]) == 2

    first_feat = data["features"][0]
    assert first_feat["type"] == "Feature"
    assert first_feat["geometry"]["type"] == "Point"
    # GeoJSON coordinates specification: [longitude, latitude]
    coords = first_feat["geometry"]["coordinates"]
    assert len(coords) == 2
    assert isinstance(coords[0], float)
    assert isinstance(coords[1], float)

    props = first_feat["properties"]
    assert "title" in props
    assert "source_name" in props
    assert "original_url" in props
    assert "location_name" in props

    # Filter by category = technology
    tech_resp = await client.get("/api/v1/news/geo?category=technology")
    assert tech_resp.status_code == 200
    tech_data = tech_resp.json()
    assert len(tech_data["features"]) == 1
    assert tech_data["features"][0]["properties"]["title"] == "Tokyo AI Exhibition"
    assert tech_data["features"][0]["geometry"]["coordinates"] == [139.6503, 35.6762]
