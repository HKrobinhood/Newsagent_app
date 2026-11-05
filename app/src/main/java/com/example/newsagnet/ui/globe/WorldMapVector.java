package com.example.newsagnet.ui.globe;

import androidx.annotation.NonNull;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Domain entity holding vectorized geographic continents and sovereign borders (Natural Earth 110m).
 *
 * <p>Conforms to COMP7506 Ch3 (Java OOP Encapsulation and Data Structures).
 */
public class WorldMapVector implements Serializable {

  private static final long serialVersionUID = 1L;

  private final List<float[]> landRings;
  private final List<float[]> borderLines;

  public WorldMapVector() {
    this.landRings = Collections.emptyList();
    this.borderLines = Collections.emptyList();
  }

  public WorldMapVector(@NonNull List<float[]> landRings, @NonNull List<float[]> borderLines) {
    this.landRings = Collections.unmodifiableList(new ArrayList<>(landRings));
    this.borderLines = Collections.unmodifiableList(new ArrayList<>(borderLines));
  }

  @NonNull
  public List<float[]> getLandRings() {
    return landRings;
  }

  @NonNull
  public List<float[]> getBorderLines() {
    return borderLines;
  }

  public boolean isEmpty() {
    return landRings.isEmpty() && borderLines.isEmpty();
  }
}
