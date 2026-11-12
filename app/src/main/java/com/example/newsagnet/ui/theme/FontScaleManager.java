package com.example.newsagnet.ui.theme;

import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.Nullable;
import com.example.newsagnet.R;
import com.example.newsagnet.data.prefs.FontScaleOption;

/**
 * Applies the user's preferred font scale (`0.90x`, `1.00x`, `1.15x`, `1.30x`) across XML View
 * hierarchies without overriding Android system Accessibility `Configuration.fontScale` or
 * mutating layout `dp` dimensions.
 *
 * <p>Conforms to COMP7506 Ch5 (Android Layouts, TextAppearance & Typography) and Ch6
 * (SharedPreferences-driven UI adaptation).
 */
public final class FontScaleManager {

  private FontScaleManager() {}

  /**
   * Recursively scales all {@link TextView} instances (including {@code Button}, {@code CheckBox},
   * {@code RadioButton}, {@code EditText}, {@code Chip}) within {@code rootView} relative to their
   * original XML baseline pixel size (which already incorporates Android system accessibility font
   * scaling).
   *
   * @param rootView Root view or ViewHolder itemView.
   * @param fontScale User font scale factor (0.90f to 1.30f).
   */
  public static void applyFontScale(@Nullable View rootView, float fontScale) {
    if (rootView == null) {
      return;
    }
    float clampedScale = FontScaleOption.fromScale(fontScale).getScale();
    applyRecursive(rootView, clampedScale);
  }

  private static void applyRecursive(View view, float scale) {
    if (view instanceof TextView) {
      TextView textView = (TextView) view;
      Object tagObj = textView.getTag(R.id.tag_base_text_size_px);
      float basePx;
      if (tagObj instanceof Float) {
        basePx = (Float) tagObj;
      } else {
        basePx = textView.getTextSize();
        textView.setTag(R.id.tag_base_text_size_px, basePx);
      }
      if (basePx > 0f) {
        float targetPx = basePx * scale;
        if (Math.abs(textView.getTextSize() - targetPx) > 0.25f) {
          textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, targetPx);
        }
      }
    }

    if (view instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) view;
      int childCount = group.getChildCount();
      for (int i = 0; i < childCount; i++) {
        View child = group.getChildAt(i);
        if (child != null) {
          applyRecursive(child, scale);
        }
      }
    }
  }
}
