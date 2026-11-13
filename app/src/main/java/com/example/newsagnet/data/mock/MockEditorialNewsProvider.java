package com.example.newsagnet.data.mock;

import androidx.annotation.NonNull;
import com.example.newsagnet.R;
import com.example.newsagnet.data.prefs.UserPreferences;
import com.example.newsagnet.model.NewsArticle;
import com.example.newsagnet.model.NewsCategory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Provides curated editorial mock news articles with image assets, category tags, and publisher
 * metadata for the N1 Editorial UI Prototype.
 *
 * <p>Conforms to COMP7506 Ch3 (Java OOP & Factory pattern).
 */
public final class MockEditorialNewsProvider {

  private MockEditorialNewsProvider() {
    // Utility class
  }

  /**
   * Generates a realistic set of editorial news articles representing an editorial magazine feed.
   *
   * @return List of mock news articles with image and text metadata.
   */
  @NonNull
  public static List<NewsArticle> getEditorialMockArticles() {
    List<NewsArticle> list = new ArrayList<>();

    // 1. Cover Story / Featured
    NewsArticle featured =
        new NewsArticle.Builder()
            .setId("mock-editorial-featured")
            .setTitle(
                "Global AI & Climate Summit Adopts Landmark Treaty on Planetary Resilience and Digital"
                    + " Ethics")
            .setSourceId("bbc")
            .setSourceName("BBC News")
            .setPublishedAt("2026-11-12T08:30:00Z")
            .setOriginalUrl("https://www.bbc.com/news/world")
            .setCategory("world")
            .setLocationName("Geneva, Switzerland")
            .setLatitude(46.2044)
            .setLongitude(6.1432)
            .setMockDrawableRes(R.drawable.mock_img_cover_summit)
            .setImageCredit("Photo: Global Summit Media Centre / Reuters")
            .build();
    list.add(featured);

    // 2. Recommended Article 1
    NewsArticle rec1 =
        new NewsArticle.Builder()
            .setId("mock-editorial-rec-1")
            .setTitle(
                "After disappearing for 365 days, polar science expedition returns with 36 key"
                    + " ocean records")
            .setSourceId("aljazeera")
            .setSourceName("Al Jazeera")
            .setPublishedAt("2026-11-12T07:15:00Z")
            .setOriginalUrl("https://www.aljazeera.com/news")
            .setCategory("science")
            .setLocationName("Tromsø, Norway")
            .setLatitude(69.6492)
            .setLongitude(18.9553)
            .setMockDrawableRes(R.drawable.mock_img_coastal_architecture)
            .setImageCredit("Photo: Polar Heritage Foundation")
            .build();
    list.add(rec1);

    // 3. Recommended Article 2
    NewsArticle rec2 =
        new NewsArticle.Builder()
            .setId("mock-editorial-rec-2")
            .setTitle(
                "Quantum Computing Lab Unveils 10,000-Qubit Fault-Tolerant Processor for Materials"
                    + " Design")
            .setSourceId("bbc")
            .setSourceName("BBC News")
            .setPublishedAt("2026-11-12T06:45:00Z")
            .setOriginalUrl("https://www.bbc.com/news/technology")
            .setCategory("technology")
            .setLocationName("Cambridge, UK")
            .setLatitude(52.2053)
            .setLongitude(0.1218)
            .setMockDrawableRes(R.drawable.mock_img_tech_quantum)
            .setImageCredit("Photo: Quantum Computing Consortium")
            .build();
    list.add(rec2);

    // 4. Recommended Article 3
    NewsArticle rec3 =
        new NewsArticle.Builder()
            .setId("mock-editorial-rec-3")
            .setTitle(
                "Renewable Energy Investment Hits Record $1.8 Trillion Across Pacific Rim Markets")
            .setSourceId("cnn")
            .setSourceName("CNN")
            .setPublishedAt("2026-11-12T05:20:00Z")
            .setOriginalUrl("https://edition.cnn.com/business")
            .setCategory("business")
            .setLocationName("Singapore")
            .setLatitude(1.3521)
            .setLongitude(103.8198)
            .setMockDrawableRes(R.drawable.mock_img_renewable_energy)
            .setImageCredit("Photo: Clean Energy Council")
            .build();
    list.add(rec3);

    // 5. Recommended Article 4
    NewsArticle rec4 =
        new NewsArticle.Builder()
            .setId("mock-editorial-rec-4")
            .setTitle(
                "Commercial Space Station Orbital Tourism Complex Welcomes First International"
                    + " Research Crew")
            .setSourceId("fox")
            .setSourceName("Fox News")
            .setPublishedAt("2026-11-12T04:10:00Z")
            .setOriginalUrl("https://www.foxnews.com/science")
            .setCategory("world")
            .setLocationName("Cape Canaveral, USA")
            .setLatitude(28.3922)
            .setLongitude(-80.6077)
            .setMockDrawableRes(R.drawable.mock_img_space_economy)
            .setImageCredit("Photo: Orbital Station Media")
            .build();
    list.add(rec4);

    // 6. Latest News: Medium 1
    NewsArticle latMed1 =
        new NewsArticle.Builder()
            .setId("mock-editorial-latest-med-1")
            .setTitle("North Sea Offshore Wind Megagrid Commences Multi-Nation Electricity Export")
            .setSourceId("bbc")
            .setSourceName("BBC News")
            .setPublishedAt("2026-11-12T03:50:00Z")
            .setOriginalUrl("https://www.bbc.com/news/business")
            .setCategory("business")
            .setLocationName("Oslo, Norway")
            .setLatitude(59.9139)
            .setLongitude(10.7522)
            .setMockDrawableRes(R.drawable.mock_img_renewable_energy)
            .build();
    list.add(latMed1);

    // 7. Latest News: Medium 2
    NewsArticle latMed2 =
        new NewsArticle.Builder()
            .setId("mock-editorial-latest-med-2")
            .setTitle(
                "Deep Ocean Autonomous Submersible Maps Uncharted Mariana Trench Biospheres")
            .setSourceId("aljazeera")
            .setSourceName("Al Jazeera")
            .setPublishedAt("2026-11-12T03:20:00Z")
            .setOriginalUrl("https://www.aljazeera.com/economy")
            .setCategory("science")
            .setLocationName("Suva, Fiji")
            .setLatitude(-18.1416)
            .setLongitude(178.4419)
            .setMockDrawableRes(R.drawable.mock_img_coastal_architecture)
            .build();
    list.add(latMed2);

    // 8. Latest News: Large 1
    NewsArticle latLrg1 =
        new NewsArticle.Builder()
            .setId("mock-editorial-latest-lrg-1")
            .setTitle(
                "Trans-Eurasian High-Speed Maglev Rail Corridor Completes Final Automated Tunnel"
                    + " Testing")
            .setSourceId("cnn")
            .setSourceName("CNN")
            .setPublishedAt("2026-11-12T02:40:00Z")
            .setOriginalUrl("https://edition.cnn.com/travel")
            .setCategory("world")
            .setLocationName("Vienna, Austria")
            .setLatitude(48.2082)
            .setLongitude(16.3738)
            .setMockDrawableRes(R.drawable.mock_img_cover_summit)
            .setImageCredit("Photo: Trans-Rail Infrastructure Corp")
            .build();
    list.add(latLrg1);

    // 9. Latest News: Compact 1
    NewsArticle latCmp1 =
        new NewsArticle.Builder()
            .setId("mock-editorial-latest-cmp-1")
            .setTitle("Global Semiconductor Alliance Unifies 1.4nm Photolithography Standards")
            .setSourceId("fox")
            .setSourceName("Fox News")
            .setPublishedAt("2026-11-12T02:10:00Z")
            .setOriginalUrl("https://www.foxnews.com/tech")
            .setCategory("technology")
            .setLocationName("Hsinchu, Taiwan")
            .setLatitude(24.8138)
            .setLongitude(120.9675)
            .setMockDrawableRes(R.drawable.mock_img_tech_quantum)
            .build();
    list.add(latCmp1);

    // 10. Latest News: Compact 2
    NewsArticle latCmp2 =
        new NewsArticle.Builder()
            .setId("mock-editorial-latest-cmp-2")
            .setTitle(
                "World Health Organization Declares Global Eradication of Wild Polio Strain 2")
            .setSourceId("bbc")
            .setSourceName("BBC News")
            .setPublishedAt("2026-11-12T01:30:00Z")
            .setOriginalUrl("https://www.bbc.com/news/health")
            .setCategory("health")
            .setLocationName("Geneva, Switzerland")
            .setLatitude(46.2044)
            .setLongitude(6.1432)
            .setMockDrawableRes(R.drawable.mock_img_renewable_energy)
            .build();
    list.add(latCmp2);

    // 11. Latest News: Text Only 1 (Degradation verification)
    NewsArticle latTxt1 =
        new NewsArticle.Builder()
            .setId("mock-editorial-latest-txt-1")
            .setTitle(
                "G20 Central Banking Advisory Group Finalizes Multi-Currency Cross-Border Liquidity"
                    + " Protocols")
            .setSourceId("aljazeera")
            .setSourceName("Al Jazeera")
            .setPublishedAt("2026-11-12T01:00:00Z")
            .setOriginalUrl("https://www.aljazeera.com/economy")
            .setCategory("business")
            .setLocationName("Riyadh, Saudi Arabia")
            .setLatitude(24.7136)
            .setLongitude(46.6753)
            .build(); // No image to verify Type D text-only card

    list.add(latTxt1);

    // 12. Latest News: Text Only 2
    NewsArticle latTxt2 =
        new NewsArticle.Builder()
            .setId("mock-editorial-latest-txt-2")
            .setTitle(
                "International Cyber Peacekeeping Framework Formally Ratified by 85 Member Nations")
            .setSourceId("cnn")
            .setSourceName("CNN")
            .setPublishedAt("2026-11-12T00:30:00Z")
            .setOriginalUrl("https://edition.cnn.com/world")
            .setCategory("world")
            .setLocationName("New York, USA")
            .setLatitude(40.7128)
            .setLongitude(-74.0060)
            .build(); // No image
    list.add(latTxt2);

    return list;
  }

  /**
   * Supplies the N1 editorial magazine feed by enriching server articles with mock artwork or
   * falling back to curated mock editorial stories, filtered by user media and category settings.
   */
  @NonNull
  public static List<NewsArticle> provideEditorialFeed(
      @NonNull List<NewsArticle> serverArticles,
      @NonNull UserPreferences prefs,
      @NonNull NewsCategory category) {
    List<NewsArticle> baseList = new ArrayList<>();
    if (serverArticles.isEmpty()) {
      baseList.addAll(getEditorialMockArticles());
    } else {
      List<Integer> mockDrawables =
          Arrays.asList(
              R.drawable.mock_img_cover_summit,
              R.drawable.mock_img_coastal_architecture,
              R.drawable.mock_img_tech_quantum,
              R.drawable.mock_img_renewable_energy,
              R.drawable.mock_img_space_economy);

      for (int i = 0; i < serverArticles.size(); i++) {
        NewsArticle orig = serverArticles.get(i);
        NewsArticle.Builder b =
            new NewsArticle.Builder()
                .setId(orig.getId())
                .setTitle(orig.getTitle())
                .setSourceId(orig.getSourceId())
                .setSourceName(orig.getSourceName())
                .setPublishedAt(orig.getPublishedAt())
                .setOriginalUrl(orig.getOriginalUrl())
                .setCategory(orig.getCategory())
                .setLocationName(orig.getLocationName())
                .setLatitude(orig.getLatitude())
                .setLongitude(orig.getLongitude());

        // For N1 prototype demonstration:
        // Indices 10 and 11 keep null image to test Type D Text Only degradation
        if (i != 10 && i != 11) {
          int dIndex = i % mockDrawables.size();
          b.setMockDrawableRes(mockDrawables.get(dIndex));
          if (i == 0) {
            b.setImageCredit("Photo: Global News Editorial Media");
          }
        }
        baseList.add(b.build());
      }
    }

    // Filter by category
    List<NewsArticle> categoryFiltered = new ArrayList<>();
    for (NewsArticle a : baseList) {
      if (category == NewsCategory.ALL || category.getKey().equalsIgnoreCase(a.getCategory())) {
        categoryFiltered.add(a);
      }
    }

    // Filter by user media preferences
    return prefs.filterArticles(categoryFiltered);
  }
}
