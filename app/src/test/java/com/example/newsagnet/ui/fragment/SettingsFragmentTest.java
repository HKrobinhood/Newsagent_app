package com.example.newsagnet.ui.fragment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.example.newsagnet.MainActivity;
import com.example.newsagnet.R;
import com.example.newsagnet.data.security.AiProviderOption;
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
  public void resolveRadioIdForScale_mapsAllFourScalesToRadioButtons() {
    assertEquals(R.id.rb_font_small, SettingsFragment.resolveRadioIdForScale(0.90f));
    assertEquals(R.id.rb_font_medium, SettingsFragment.resolveRadioIdForScale(1.00f));
    assertEquals(R.id.rb_font_large, SettingsFragment.resolveRadioIdForScale(1.15f));
    assertEquals(R.id.rb_font_extra_large, SettingsFragment.resolveRadioIdForScale(1.30f));
  }

  @Test
  public void resolveProviderAndRadioId_mapsAllProvidersBidirectionally() {
    assertEquals(
        AiProviderOption.SERVER_DEFAULT,
        SettingsFragment.resolveProviderForRadioId(R.id.rb_provider_server_default));
    assertEquals(
        AiProviderOption.GEMINI,
        SettingsFragment.resolveProviderForRadioId(R.id.rb_provider_gemini));
    assertEquals(
        AiProviderOption.OPENAI,
        SettingsFragment.resolveProviderForRadioId(R.id.rb_provider_openai));

    assertEquals(
        R.id.rb_provider_server_default,
        SettingsFragment.resolveRadioIdForProvider(AiProviderOption.SERVER_DEFAULT));
    assertEquals(
        R.id.rb_provider_gemini,
        SettingsFragment.resolveRadioIdForProvider(AiProviderOption.GEMINI));
    assertEquals(
        R.id.rb_provider_openai,
        SettingsFragment.resolveRadioIdForProvider(AiProviderOption.OPENAI));
  }

  @Test
  public void mainActivityTagConstants_remainStable() {
    assertEquals("tag_news", MainActivity.TAG_NEWS);
    assertEquals("tag_globe", MainActivity.TAG_GLOBE);
    assertEquals("tag_chat", MainActivity.TAG_CHAT);
    assertEquals("tag_settings", MainActivity.TAG_SETTINGS);
  }
}
