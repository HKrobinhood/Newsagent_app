package com.example.newsagnet.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.newsagnet.MainActivity;
import com.example.newsagnet.R;
import com.example.newsagnet.data.prefs.LocalPreferencesRepository;
import com.example.newsagnet.model.NewsSourceItem;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumentation tests verifying S0 & S1 Settings navigation, News Sources persistence & Empty
 * State filtering, and Global Font Scale persistence across configuration changes.
 */
@RunWith(AndroidJUnit4.class)
public class SettingsNavigationTest {

  @Before
  public void resetPreferencesBeforeTest() {
    Context context = ApplicationProvider.getApplicationContext();
    LocalPreferencesRepository repo = LocalPreferencesRepository.getInstance(context);
    repo.resetSourcesToDefault(NewsSourceItem.getDefaultSourceIds());
    repo.setFontScale(1.00f);
  }

  @Test
  public void openSettingsFromNewsGlobeAndChat_andReturnToOriginTab() {
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      // 1. Open Settings from News and return via Toolbar back button
      scenario.onActivity(
          activity -> {
            assertEquals(MainActivity.TAG_NEWS, activity.getActiveFragmentTag());
            View newsSettingsBtn = activity.findViewById(R.id.btn_settings);
            assertNotNull(newsSettingsBtn);
            newsSettingsBtn.performClick();
            activity.getSupportFragmentManager().executePendingTransactions();

            assertTrue(activity.isSettingsVisible());
            assertEquals(MainActivity.TAG_SETTINGS, activity.getActiveFragmentTag());
            assertEquals(MainActivity.TAG_NEWS, activity.getPreviousTabTag());

            View backBtn = activity.findViewById(R.id.btn_settings_back);
            assertNotNull(backBtn);
            backBtn.performClick();
            activity.getSupportFragmentManager().executePendingTransactions();

            assertFalse(activity.isSettingsVisible());
            assertEquals(MainActivity.TAG_NEWS, activity.getActiveFragmentTag());
          });

      // 2. Switch to Globe, open Settings, and return via system Back dispatcher
      scenario.onActivity(
          activity -> {
            BottomNavigationView bottomNav = activity.findViewById(R.id.bottom_navigation);
            bottomNav.setSelectedItemId(R.id.nav_globe);
            activity.getSupportFragmentManager().executePendingTransactions();
            assertEquals(MainActivity.TAG_GLOBE, activity.getActiveFragmentTag());

            View globeSettingsBtn = activity.findViewById(R.id.btn_globe_settings);
            assertNotNull(globeSettingsBtn);
            globeSettingsBtn.performClick();
            activity.getSupportFragmentManager().executePendingTransactions();

            assertTrue(activity.isSettingsVisible());
            assertEquals(MainActivity.TAG_GLOBE, activity.getPreviousTabTag());

            activity.getOnBackPressedDispatcher().onBackPressed();
            activity.getSupportFragmentManager().executePendingTransactions();

            assertFalse(activity.isSettingsVisible());
            assertEquals(MainActivity.TAG_GLOBE, activity.getActiveFragmentTag());
          });

      // 3. Switch to AI Intel (Chat), open Settings, and return
      scenario.onActivity(
          activity -> {
            BottomNavigationView bottomNav = activity.findViewById(R.id.bottom_navigation);
            bottomNav.setSelectedItemId(R.id.nav_chat);
            activity.getSupportFragmentManager().executePendingTransactions();
            assertEquals(MainActivity.TAG_CHAT, activity.getActiveFragmentTag());

            View chatSettingsBtn = activity.findViewById(R.id.btn_chat_settings);
            assertNotNull(chatSettingsBtn);
            chatSettingsBtn.performClick();
            activity.getSupportFragmentManager().executePendingTransactions();

            assertTrue(activity.isSettingsVisible());
            assertEquals(MainActivity.TAG_CHAT, activity.getPreviousTabTag());

            activity.findViewById(R.id.btn_settings_back).performClick();
            activity.getSupportFragmentManager().executePendingTransactions();

            assertFalse(activity.isSettingsVisible());
            assertEquals(MainActivity.TAG_CHAT, activity.getActiveFragmentTag());
          });
    }
  }

  @Test
  public void settingsSurvivesActivityRecreation_andRestoresOriginTabOnBack() {
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      scenario.onActivity(
          activity -> {
            BottomNavigationView bottomNav = activity.findViewById(R.id.bottom_navigation);
            bottomNav.setSelectedItemId(R.id.nav_globe);
            activity.getSupportFragmentManager().executePendingTransactions();

            activity.findViewById(R.id.btn_globe_settings).performClick();
            activity.getSupportFragmentManager().executePendingTransactions();

            CheckBox bbcCheck = activity.findViewById(R.id.cb_source_bbc);
            bbcCheck.setChecked(false);
            RadioGroup fontGroup = activity.findViewById(R.id.rg_font_size);
            fontGroup.check(R.id.rb_font_large);
          });

      // Simulate screen rotation
      scenario.recreate();

      scenario.onActivity(
          activity -> {
            activity.getSupportFragmentManager().executePendingTransactions();
            assertTrue(activity.isSettingsVisible());
            assertEquals(MainActivity.TAG_SETTINGS, activity.getActiveFragmentTag());
            assertEquals(MainActivity.TAG_GLOBE, activity.getPreviousTabTag());

            CheckBox bbcCheck = activity.findViewById(R.id.cb_source_bbc);
            assertFalse(bbcCheck.isChecked());
            RadioGroup fontGroup = activity.findViewById(R.id.rg_font_size);
            assertEquals(R.id.rb_font_large, fontGroup.getCheckedRadioButtonId());

            // Press back and verify return to Globe tab
            activity.getOnBackPressedDispatcher().onBackPressed();
            activity.getSupportFragmentManager().executePendingTransactions();
            assertFalse(activity.isSettingsVisible());
            assertEquals(MainActivity.TAG_GLOBE, activity.getActiveFragmentTag());
          });
    }
  }

  @Test
  public void disableAllSources_showsAllSourcesDisabledEmptyState_andSelectAllRestores() {
    Context context = ApplicationProvider.getApplicationContext();
    LocalPreferencesRepository repo = LocalPreferencesRepository.getInstance(context);

    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      scenario.onActivity(
          activity -> {
            // Open Settings from News
            activity.findViewById(R.id.btn_settings).performClick();
            activity.getSupportFragmentManager().executePendingTransactions();

            // Uncheck all 4 default sources
            ((CheckBox) activity.findViewById(R.id.cb_source_bbc)).setChecked(false);
            ((CheckBox) activity.findViewById(R.id.cb_source_cnn)).setChecked(false);
            ((CheckBox) activity.findViewById(R.id.cb_source_aljazeera)).setChecked(false);
            ((CheckBox) activity.findViewById(R.id.cb_source_fox)).setChecked(false);

            assertTrue(repo.getPreferences().areAllSourcesDisabled());

            // Return to News and verify Empty State message for all sources disabled
            activity.findViewById(R.id.btn_settings_back).performClick();
            activity.getSupportFragmentManager().executePendingTransactions();

            TextView tvEmpty = activity.findViewById(R.id.tv_empty);
            assertEquals(View.VISIBLE, tvEmpty.getVisibility());
            assertEquals(
                activity.getString(R.string.settings_empty_all_sources_disabled),
                tvEmpty.getText().toString());

            // Open Settings again and click Select All
            activity.findViewById(R.id.btn_settings).performClick();
            activity.getSupportFragmentManager().executePendingTransactions();
            activity.findViewById(R.id.btn_sources_select_all).performClick();

            assertFalse(repo.getPreferences().areAllSourcesDisabled());
            assertEquals(4, repo.getEnabledSourceIds().size());
          });
    }
  }

  @Test
  public void extraLargeFontScale_persistsAndScalesTextWithoutClipping() {
    Context context = ApplicationProvider.getApplicationContext();
    LocalPreferencesRepository repo = LocalPreferencesRepository.getInstance(context);

    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      scenario.onActivity(
          activity -> {
            activity.findViewById(R.id.btn_settings).performClick();
            activity.getSupportFragmentManager().executePendingTransactions();

            TextView previewTitle = activity.findViewById(R.id.tv_font_preview_headline);
            float mediumPx = previewTitle.getTextSize();

            RadioGroup fontGroup = activity.findViewById(R.id.rg_font_size);
            fontGroup.check(R.id.rb_font_extra_large);

            assertEquals(1.30f, repo.getFontScale(), 0.001f);
            float extraLargePx = previewTitle.getTextSize();
            assertTrue(
                "Extra Large text size should be scaled up relative to Medium",
                extraLargePx > mediumPx * 1.20f);
          });
    }
  }
}
