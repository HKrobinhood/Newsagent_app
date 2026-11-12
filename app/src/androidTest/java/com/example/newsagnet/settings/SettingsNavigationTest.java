package com.example.newsagnet.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.newsagnet.MainActivity;
import com.example.newsagnet.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumentation tests verifying S0 Settings navigation from News, Globe, and AI Intel, back
 * navigation, screen rotation state survival, and coexistence with BottomNavigation and Reading
 * History.
 */
@RunWith(AndroidJUnit4.class)
public class SettingsNavigationTest {

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
}
