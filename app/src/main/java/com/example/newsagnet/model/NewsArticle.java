package com.example.newsagnet.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Domain entity representing an international news article item.
 *
 * <p>Full article body text is intentionally not persisted in compliance with copyright
 * constraints.
 *
 * <p>Conforms to COMP7506 Ch3 (Java OOP: Encapsulation, Constructor Overloading, JavaBean, and
 * Builder Pattern).
 */
public class NewsArticle implements Serializable {

  private static final long serialVersionUID = 1L;

  private String id;
  private String title;
  private String sourceId;
  private String sourceName;
  private String publishedAt;
  private String originalUrl;
  private String category;
  private String locationName;
  private Double latitude;
  private Double longitude;
  private String imageUrl;
  private String imageCredit;
  private Integer mockDrawableRes;

  /** Default constructor for JavaBean serialization frameworks. */
  public NewsArticle() {}

  /**
   * Overloaded constructor for articles without geographical coordinate data.
   *
   * @param id Unique article identifier.
   * @param title Headline title.
   * @param sourceId Identifier of the news publisher.
   * @param sourceName Human-readable publisher name.
   * @param publishedAt ISO-8601 publication timestamp.
   * @param originalUrl Canonical URL to media source article.
   * @param category Classification category key.
   */
  public NewsArticle(
      @NonNull String id,
      @NonNull String title,
      @NonNull String sourceId,
      @NonNull String sourceName,
      @NonNull String publishedAt,
      @NonNull String originalUrl,
      @NonNull String category) {
    this(id, title, sourceId, sourceName, publishedAt, originalUrl, category, null, null, null);
  }

  /**
   * Comprehensive constructor initializing all properties including geocoding coordinates.
   *
   * @param id Unique article identifier.
   * @param title Headline title.
   * @param sourceId Identifier of the news publisher.
   * @param sourceName Human-readable publisher name.
   * @param publishedAt ISO-8601 publication timestamp.
   * @param originalUrl Canonical URL to media source article.
   * @param category Classification category key.
   * @param locationName Optional resolved geographical place name.
   * @param latitude Optional WGS-84 latitude.
   * @param longitude Optional WGS-84 longitude.
   */
  public NewsArticle(
      @NonNull String id,
      @NonNull String title,
      @NonNull String sourceId,
      @NonNull String sourceName,
      @NonNull String publishedAt,
      @NonNull String originalUrl,
      @NonNull String category,
      @Nullable String locationName,
      @Nullable Double latitude,
      @Nullable Double longitude) {
    this.id = id;
    this.title = title;
    this.sourceId = sourceId;
    this.sourceName = sourceName;
    this.publishedAt = publishedAt;
    this.originalUrl = originalUrl;
    this.category = category;
    this.locationName = locationName;
    this.latitude = latitude;
    this.longitude = longitude;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getSourceId() {
    return sourceId;
  }

  public void setSourceId(String sourceId) {
    this.sourceId = sourceId;
  }

  public String getSourceName() {
    return sourceName;
  }

  public void setSourceName(String sourceName) {
    this.sourceName = sourceName;
  }

  public String getPublishedAt() {
    return publishedAt;
  }

  public void setPublishedAt(String publishedAt) {
    this.publishedAt = publishedAt;
  }

  public String getOriginalUrl() {
    return originalUrl;
  }

  public void setOriginalUrl(String originalUrl) {
    this.originalUrl = originalUrl;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  @Nullable
  public String getLocationName() {
    return locationName;
  }

  public void setLocationName(@Nullable String locationName) {
    this.locationName = locationName;
  }

  @Nullable
  public Double getLatitude() {
    return latitude;
  }

  public void setLatitude(@Nullable Double latitude) {
    this.latitude = latitude;
  }

  @Nullable
  public Double getLongitude() {
    return longitude;
  }

  public void setLongitude(@Nullable Double longitude) {
    this.longitude = longitude;
  }

  @Nullable
  public String getImageUrl() {
    return imageUrl;
  }

  public void setImageUrl(@Nullable String imageUrl) {
    this.imageUrl = imageUrl;
  }

  @Nullable
  public String getImageCredit() {
    return imageCredit;
  }

  public void setImageCredit(@Nullable String imageCredit) {
    this.imageCredit = imageCredit;
  }

  @Nullable
  public Integer getMockDrawableRes() {
    return mockDrawableRes;
  }

  public void setMockDrawableRes(@Nullable Integer mockDrawableRes) {
    this.mockDrawableRes = mockDrawableRes;
  }

  public boolean hasImage() {
    return (imageUrl != null && !imageUrl.trim().isEmpty())
        || (mockDrawableRes != null && mockDrawableRes != 0);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof NewsArticle)) {
      return false;
    }
    NewsArticle that = (NewsArticle) o;
    return Objects.equals(id, that.id)
        && Objects.equals(title, that.title)
        && Objects.equals(sourceId, that.sourceId)
        && Objects.equals(sourceName, that.sourceName)
        && Objects.equals(publishedAt, that.publishedAt)
        && Objects.equals(originalUrl, that.originalUrl)
        && Objects.equals(category, that.category)
        && Objects.equals(locationName, that.locationName)
        && Objects.equals(latitude, that.latitude)
        && Objects.equals(longitude, that.longitude);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        id,
        title,
        sourceId,
        sourceName,
        publishedAt,
        originalUrl,
        category,
        locationName,
        latitude,
        longitude);
  }

  @NonNull
  @Override
  public String toString() {
    return "NewsArticle{"
        + "id='"
        + id
        + '\''
        + ", title='"
        + title
        + '\''
        + ", sourceName='"
        + sourceName
        + '\''
        + ", publishedAt='"
        + publishedAt
        + '\''
        + ", category='"
        + category
        + '\''
        + ", locationName='"
        + locationName
        + '\''
        + '}';
  }

  /**
   * Builder pattern for constructing {@link NewsArticle} instances fluently.
   *
   * <p>COMP7506 Ch3 OOP Design Patterns.
   */
  public static class Builder {
    private String id;
    private String title;
    private String sourceId;
    private String sourceName;
    private String publishedAt;
    private String originalUrl;
    private String category;
    private String locationName;
    private Double latitude;
    private Double longitude;
    private String imageUrl;
    private String imageCredit;
    private Integer mockDrawableRes;

    public Builder setId(String id) {
      this.id = id;
      return this;
    }

    public Builder setTitle(String title) {
      this.title = title;
      return this;
    }

    public Builder setSourceId(String sourceId) {
      this.sourceId = sourceId;
      return this;
    }

    public Builder setSourceName(String sourceName) {
      this.sourceName = sourceName;
      return this;
    }

    public Builder setPublishedAt(String publishedAt) {
      this.publishedAt = publishedAt;
      return this;
    }

    public Builder setOriginalUrl(String originalUrl) {
      this.originalUrl = originalUrl;
      return this;
    }

    public Builder setCategory(String category) {
      this.category = category;
      return this;
    }

    public Builder setLocationName(String locationName) {
      this.locationName = locationName;
      return this;
    }

    public Builder setLatitude(Double latitude) {
      this.latitude = latitude;
      return this;
    }

    public Builder setLongitude(Double longitude) {
      this.longitude = longitude;
      return this;
    }

    public Builder setImageUrl(String imageUrl) {
      this.imageUrl = imageUrl;
      return this;
    }

    public Builder setImageCredit(String imageCredit) {
      this.imageCredit = imageCredit;
      return this;
    }

    public Builder setMockDrawableRes(Integer mockDrawableRes) {
      this.mockDrawableRes = mockDrawableRes;
      return this;
    }

    public NewsArticle build() {
      NewsArticle article =
          new NewsArticle(
              id != null ? id : "",
              title != null ? title : "",
              sourceId != null ? sourceId : "",
              sourceName != null ? sourceName : "",
              publishedAt != null ? publishedAt : "",
              originalUrl != null ? originalUrl : "",
              category != null ? category : "",
              locationName,
              latitude,
              longitude);
      article.setImageUrl(imageUrl);
      article.setImageCredit(imageCredit);
      article.setMockDrawableRes(mockDrawableRes);
      return article;
    }
  }
}
