"""Geographic entity resolution and disambiguation for news headlines."""

import re

# Verified major global cities with unambiguous coordinates
KNOWN_CITIES: dict[str, tuple[str, float, float]] = {
    "geneva": ("Geneva, Switzerland", 46.2044, 6.1432),
    "tokyo": ("Tokyo, Japan", 35.6762, 139.6503),
    "london": ("London, UK", 51.5074, -0.1278),
    "nairobi": ("Nairobi, Kenya", -1.2921, 36.8219),
    "munich": ("Munich, Germany", 48.1351, 11.5820),
    "berlin": ("Berlin, Germany", 52.5200, 13.4050),
    "paris": ("Paris, France", 48.8566, 2.3522),
    "washington": ("Washington, USA", 38.9072, -77.0369),
    "new york": ("New York, USA", 40.7128, -74.0060),
    "beijing": ("Beijing, China", 39.9042, 116.4074),
    "seoul": ("Seoul, South Korea", 37.5665, 126.9780),
    "kyiv": ("Kyiv, Ukraine", 50.4501, 30.5234),
    "sydney": ("Sydney, Australia", -33.8688, 151.2093),
    "cairo": ("Cairo, Egypt", 30.0444, 31.2357),
    "new delhi": ("New Delhi, India", 28.6139, 77.2090),
    "delhi": ("New Delhi, India", 28.6139, 77.2090),
    "rome": ("Rome, Italy", 41.9028, 12.4964),
    "madrid": ("Madrid, Spain", 40.4168, -3.7038),
    "singapore": ("Singapore", 1.3521, 103.8198),
    "brasilia": ("Brasilia, Brazil", -15.7975, -47.8919),
    "ottawa": ("Ottawa, Canada", 45.4215, -75.6972),
    "riyadh": ("Riyadh, Saudi Arabia", 24.7136, 46.6753),
    "jakarta": ("Jakarta, Indonesia", -6.2088, 106.8456),
}

# Country mappings only accepted for diplomatic/governmental action (e.g. "in Brazil")
# Broad country reports or non-capital regions must NOT be pinned to capital cities.
KNOWN_COUNTRIES: dict[str, tuple[str, float, float]] = {
    "brazil": ("Brasilia, Brazil", -15.7975, -47.8919),
    "switzerland": ("Geneva, Switzerland", 46.2044, 6.1432),
    "japan": ("Tokyo, Japan", 35.6762, 139.6503),
    "uk": ("London, UK", 51.5074, -0.1278),
    "britain": ("London, UK", 51.5074, -0.1278),
    "kenya": ("Nairobi, Kenya", -1.2921, 36.8219),
    "germany": ("Berlin, Germany", 52.5200, 13.4050),
    "france": ("Paris, France", 48.8566, 2.3522),
    "china": ("Beijing, China", 39.9042, 116.4074),
    "korea": ("Seoul, South Korea", 37.5665, 126.9780),
    "ukraine": ("Kyiv, Ukraine", 50.4501, 30.5234),
    "australia": ("Sydney, Australia", -33.8688, 151.2093),
    "egypt": ("Cairo, Egypt", 30.0444, 31.2357),
    "italy": ("Rome, Italy", 41.9028, 12.4964),
    "spain": ("Madrid, Spain", 40.4168, -3.7038),
    "canada": ("Ottawa, Canada", 45.4215, -75.6972),
    "saudi arabia": ("Riyadh, Saudi Arabia", 24.7136, 46.6753),
    "indonesia": ("Jakarta, Indonesia", -6.2088, 106.8456),
}

# Ambiguous phrases where geographic words do NOT refer to places
AMBIGUOUS_PATTERNS = [
    r"\bchina\s+(cup|plate|dish|cabinet|doll|porcelain|clay)\b",
    r"\bnew york\s+(strip|steak)\b",
    r"\bturkey\s+(sandwich|dinner|roast|meat|bacon)\b",
    r"\bgermany\s+(measles)\b",
]

# Regional, non-capital, or macro-economic modifiers indicating non-localized events
REGIONAL_OR_NON_CAPITAL_MODIFIERS = [
    r"\b(eastern|western|northern|southern|northeastern|northwestern|southeastern|southwestern)\b",
    r"\b(coast|coastline|coastal|border|borders|bordering|offshore|rural|mountains|valley|province|state|strait)\b",
    r"\b(poverty|inequality|nationwide|gdp|growth|economy|nation)\b",
]


def resolve_location_from_title(
    title: str,
) -> tuple[str | None, float | None, float | None]:
    """Extract reliable geographical coordinates from news title with disambiguation.

    Note:
        Strictly returns None if entity resolution is ambiguous, non-geographic,
        or absent. Fictitious coordinates are never produced.

    Args:
        title: Headline text of the news article.

    Returns:
        Tuple of (location_name, latitude, longitude) or (None, None, None).
    """
    if not title:
        return None, None, None

    lower_title = title.lower()

    # 1. Mask ambiguous homonym patterns so they do not trigger false positives
    sanitized_title = lower_title
    for pattern in AMBIGUOUS_PATTERNS:
        sanitized_title = re.sub(pattern, " ", sanitized_title)

    # 2. Prioritize explicit city matching (highest confidence)
    for city_keyword, (loc_name, lat, lon) in KNOWN_CITIES.items():
        pattern = r"\b" + re.escape(city_keyword) + r"\b"
        if re.search(pattern, sanitized_title):
            return loc_name, lat, lon

    # 3. Disambiguate regional or macro modifiers: do not pin to capitals
    for modifier in REGIONAL_OR_NON_CAPITAL_MODIFIERS:
        if re.search(modifier, sanitized_title):
            return None, None, None

    # 4. Fallback to country matching only when country is a national entity
    for country_keyword, (loc_name, lat, lon) in KNOWN_COUNTRIES.items():
        pattern = r"\b" + re.escape(country_keyword) + r"\b"
        if re.search(pattern, sanitized_title):
            return loc_name, lat, lon

    return None, None, None
