package com.example.newsagnet.ui.fragment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.example.newsagnet.MainActivity;
import com.example.newsagnet.R;
import org.junit.Test;

/**
 * Unit tests for {@link SettingsFragment} and {@link MainActivity} settings navigation constants.
 */
public class SettingsFragmentTest {

  @Test
  public void newInstance_createsValidFragmentWithDefaultMediumScale() {
    SettingsFragment fragment = SettingsFragment.newInstance();
    assertNotNull(fragment);
    assertEquals(SettingsFragment.SCALE_MEDIUM, fragment.getCurrentPreviewScale(), 0.001f);
  }

  @Test
  public void resolveScaleForRadioId_mapsAllFourFontScalesAccurately() {
    assertEquals(
        0.90f, SettingsFragment.resolveScaleForRadioId(R.id.rb_font_small), 0.001f);
    assertEquals(
        1.00f, SettingsFragment.resolveScaleForRadioId(R.id.rb_font_medium), 0.001f);
    assertEquals(
        1.15f, SettingsFragment.resolveScaleForRadioId(R.id.rb_font_large), 0.001f);
    assertEquals(
        1.30f, SettingsFragment.resolveScaleForRadioId(R.id.rb_font_extra_large), 0.001f);
    assertEquals(
        1.00f, SettingsFragment.resolveScaleForRadioId(-1), 0.001f);
  }

  @Test
  public void mainActivityTagConstants_remainStable() {
    assertEquals("tag_news", MainActivity.TAG_NEWS);
    assertEquals("tag_globe", MainActivity.TAG_GLOBE);
    assertEquals("tag_chat", MainActivity.TAG_CHAT);
    assertEquals("tag_settings", MainActivity.TAG_SETTINGS);
  }
}
