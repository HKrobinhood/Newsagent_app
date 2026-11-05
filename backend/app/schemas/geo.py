"""GeoJSON data models for spatial news hotspot representation."""

from typing import Literal

from pydantic import BaseModel, Field


class GeoGeometry(BaseModel):
    """GeoJSON Point geometry with [longitude, latitude] coordinates."""

    type: Literal["Point"] = "Point"
    coordinates: list[float] = Field(
        ...,
        description="Coordinates in [longitude, latitude] order per RFC 7946",
        min_length=2,
        max_length=2,
    )


class GeoProperties(BaseModel):
    """Properties payload for geolocated news article feature."""

    id: str
    title: str
    source_id: str
    source_name: str
    published_at: str
    original_url: str
    category: str
    location_name: str | None = None


class GeoFeature(BaseModel):
    """GeoJSON Feature representing a single geolocated news event."""

    type: Literal["Feature"] = "Feature"
    geometry: GeoGeometry
    properties: GeoProperties


class GeoFeatureCollection(BaseModel):
    """Standard GeoJSON FeatureCollection."""

    type: Literal["FeatureCollection"] = "FeatureCollection"
    features: list[GeoFeature]
