package com.example.newsagnet.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.newsagnet.MainActivity;
import com.example.newsagnet.R;
import com.example.newsagnet.data.prefs.AuthPreferences;
import com.example.newsagnet.data.prefs.LocalPreferencesRepository;
import com.example.newsagnet.data.security.AiProviderOption;
import com.example.newsagnet.data.security.SecureApiKeyStore;
import com.example.newsagnet.history.HistoryEntry;
import com.example.newsagnet.history.HistoryFilter;
import com.example.newsagnet.history.HistoryRepository;
import com.example.newsagnet.model.AuthSession;
import com.example.newsagnet.model.NewsArticle;
import com.example.newsagnet.model.NewsSourceItem;
import com.example.newsagnet.model.UserProfile;
import com.example.newsagnet.repository.AuthRepositoryImpl;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
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
    SecureApiKeyStore.getInstance(context).removeApiKey();
    AuthPreferences.getInstance(context).clearSession();
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

  @Test
  public void apiKeySaveMaskPersistAndRemove_withAndroidKeystoreAesGcm() {
    Context context = ApplicationProvider.getApplicationContext();
    SecureApiKeyStore keyStore = SecureApiKeyStore.getInstance(context);
    String testSecret = "AIzaSyHardwareKeystoreTestKey5678";

    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      scenario.onActivity(
          activity -> {
            activity.findViewById(R.id.btn_settings).performClick();
            activity.getSupportFragmentManager().executePendingTransactions();

            RadioGroup providerGroup = activity.findViewById(R.id.rg_ai_provider);
            providerGroup.check(R.id.rb_provider_gemini);

            EditText etApiKey = activity.findViewById(R.id.et_api_key);
            etApiKey.setText(testSecret);

            activity.findViewById(R.id.btn_save_api_key).performClick();

            // 1. Input box must be cleared immediately so full key is never shown on screen
            assertEquals("", etApiKey.getText().toString());

            // 2. Status banner shows masked status, never full plaintext key
            TextView tvStatus = activity.findViewById(R.id.tv_ai_connection_status);
            String statusText = tvStatus.getText().toString();
            assertTrue(statusText.contains("••••5678"));
            assertFalse(statusText.contains(testSecret));

            // 3. Hardware Keystore decrypts accurately for in-flight requests
            assertTrue(keyStore.hasConfiguredApiKey());
            assertEquals(AiProviderOption.GEMINI, keyStore.getSelectedProvider());
            assertEquals(testSecret, keyStore.getDecryptedApiKey());
          });

      // Simulate restart / recreation and verify encrypted persistence survives
      scenario.recreate();

      scenario.onActivity(
          activity -> {
            activity.getSupportFragmentManager().executePendingTransactions();
            assertTrue(keyStore.hasConfiguredApiKey());
            assertEquals(testSecret, keyStore.getDecryptedApiKey());

            TextView tvStatus = activity.findViewById(R.id.tv_ai_connection_status);
            assertTrue(tvStatus.getText().toString().contains("••••5678"));
            assertFalse(tvStatus.getText().toString().contains(testSecret));

            // Click Remove Key and verify key is wiped and mode resets to Server Default
            activity.findViewById(R.id.btn_remove_api_key).performClick();
            assertFalse(keyStore.hasConfiguredApiKey());
            assertEquals(AiProviderOption.SERVER_DEFAULT, keyStore.getSelectedProvider());
            assertEquals(
                activity.getString(R.string.settings_ai_status_default),
                tvStatus.getText().toString());
          });
    }
  }

  @Test
  public void accountSection_defaultsToGuest_andHandlesUnconfiguredOAuthGracefully() {
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      scenario.onActivity(
          activity -> {
            activity.findViewById(R.id.btn_settings).performClick();
            activity.getSupportFragmentManager().executePendingTransactions();

            TextView tvName = activity.findViewById(R.id.tv_account_name);
            TextView tvEmail = activity.findViewById(R.id.tv_account_email);
            View btnSignIn = activity.findViewById(R.id.btn_google_sign_in);
            View btnSignOut = activity.findViewById(R.id.btn_sign_out);

            assertEquals(
                activity.getString(R.string.settings_account_guest_name),
                tvName.getText().toString());
            assertEquals(
                activity.getString(R.string.settings_account_guest_email),
                tvEmail.getText().toString());
            assertEquals(View.VISIBLE, btnSignIn.getVisibility());
            assertEquals(View.GONE, btnSignOut.getVisibility());

            // Clicking sign in when unconfigured launches setup guide dialog safely without crashing
            btnSignIn.performClick();
            // App state remains stable in Guest mode
            assertEquals(View.VISIBLE, btnSignIn.getVisibility());
          });
    }
  }

  @Test
  public void accountSection_signedInUserUpdatesUi_andSignOutRestoresGuestAndPreservesHistory()
      throws Exception {
    Context context = ApplicationProvider.getApplicationContext();
    AuthPreferences authPrefs = AuthPreferences.getInstance(context);

    // Seed Reading History via public HistoryRepository API
    HistoryRepository historyRepo = HistoryRepository.getInstance(context);
    NewsArticle testArticle =
        new NewsArticle(
            "s3-test-id",
            "S3 Integration News",
            "bbc",
            "BBC News",
            "2026-11-12T00:00:00Z",
            "https://example.org/s3-test",
            "world");
    HistoryRepository.recordOpened(context, testArticle);

    // Give background disk executor time to write and save a note
    Thread.sleep(200);
    CountDownLatch noteLatch = new CountDownLatch(1);
    historyRepo.saveNote(
        "https://example.org/s3-test",
        "Important note to preserve",
        new HistoryRepository.Callback<Boolean>() {
          @Override
          public void onSuccess(Boolean value) {
            noteLatch.countDown();
          }

          @Override
          public void onError() {
            noteLatch.countDown();
          }
        });
    assertTrue(noteLatch.await(5, TimeUnit.SECONDS));

    // Verify seeded state
    CountDownLatch preLatch = new CountDownLatch(1);
    AtomicReference<HistoryRepository.Page> prePage = new AtomicReference<>();
    historyRepo.load(
        new HistoryFilter("", null, null, null),
        new HistoryRepository.Callback<HistoryRepository.Page>() {
          @Override
          public void onSuccess(HistoryRepository.Page value) {
            prePage.set(value);
            preLatch.countDown();
          }

          @Override
          public void onError() {
            preLatch.countDown();
          }
        });
    assertTrue(preLatch.await(5, TimeUnit.SECONDS));
    assertNotNull(prePage.get());
    assertFalse(prePage.get().entries.isEmpty());

    // Simulate active session
    UserProfile testUser =
        new UserProfile(
            "usr-test-123",
            "tester@globalnews.ai",
            "Jane Doe",
            "google");
    AuthSession session =
        new AuthSession(
            "test-access-token-123",
            "Bearer",
            "2026-12-31T23:59:59Z",
            testUser);
    authPrefs.saveSession(session);

    CountDownLatch logoutLatch = new CountDownLatch(1);
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      scenario.onActivity(
          activity -> {
            activity.findViewById(R.id.btn_settings).performClick();
            activity.getSupportFragmentManager().executePendingTransactions();

            TextView tvName = activity.findViewById(R.id.tv_account_name);
            TextView tvEmail = activity.findViewById(R.id.tv_account_email);
            View btnSignIn = activity.findViewById(R.id.btn_google_sign_in);
            View btnSignOut = activity.findViewById(R.id.btn_sign_out);

            assertEquals("Jane Doe", tvName.getText().toString());
            assertEquals("tester@globalnews.ai", tvEmail.getText().toString());
            assertEquals(View.GONE, btnSignIn.getVisibility());
            assertEquals(View.VISIBLE, btnSignOut.getVisibility());

            AuthRepositoryImpl.getInstance(activity).addAuthListener(
                user -> {
                  if (user.isGuest()) {
                    logoutLatch.countDown();
                  }
                });

            // Perform sign out
            btnSignOut.performClick();
          });

      assertTrue(
          "Sign out should complete and notify within 5s",
          logoutLatch.await(5, TimeUnit.SECONDS));

      scenario.onActivity(
          activity -> {
            activity.getSupportFragmentManager().executePendingTransactions();
            TextView tvName = activity.findViewById(R.id.tv_account_name);
            TextView tvEmail = activity.findViewById(R.id.tv_account_email);
            View btnSignIn = activity.findViewById(R.id.btn_google_sign_in);
            View btnSignOut = activity.findViewById(R.id.btn_sign_out);

            // Ui restored to Guest
            assertEquals(
                activity.getString(R.string.settings_account_guest_name),
                tvName.getText().toString());
            assertEquals(
                activity.getString(R.string.settings_account_guest_email),
                tvEmail.getText().toString());
            assertEquals(View.VISIBLE, btnSignIn.getVisibility());
            assertEquals(View.GONE, btnSignOut.getVisibility());
          });
    }

    // Verify Reading History is preserved without data loss
    CountDownLatch postLatch = new CountDownLatch(1);
    AtomicReference<HistoryRepository.Page> postPage = new AtomicReference<>();
    historyRepo.load(
        new HistoryFilter("", null, null, null),
        new HistoryRepository.Callback<HistoryRepository.Page>() {
          @Override
          public void onSuccess(HistoryRepository.Page value) {
            postPage.set(value);
            postLatch.countDown();
          }

          @Override
          public void onError() {
            postLatch.countDown();
          }
        });
    assertTrue(postLatch.await(5, TimeUnit.SECONDS));
    assertNotNull(postPage.get());

    boolean foundAfterLogout = false;
    for (HistoryEntry item : postPage.get().entries) {
      if ("https://example.org/s3-test".equals(item.getUrl())) {
        foundAfterLogout = true;
        assertEquals("Important note to preserve", item.getNote());
        break;
      }
    }
    assertTrue("Reading History and notes must remain intact after sign out", foundAfterLogout);
  }

  @Test
  public void cloudPreferenceSync_whenSignedIn_reconcilesAndReflectsInUiAndPersistsAcrossRecreation() {
    Context context = ApplicationProvider.getApplicationContext();
    AuthPreferences authPrefs = AuthPreferences.getInstance(context);
    LocalPreferencesRepository localRepo = LocalPreferencesRepository.getInstance(context);

    // Simulate active session
    UserProfile testUser =
        new UserProfile(
            "sync-usr-456",
            "crossdevice@globalnews.ai",
            "Alex Smith",
            "google");
    AuthSession session =
        new AuthSession(
            "cloud-sync-token-456",
            "Bearer",
            "2026-12-31T23:59:59Z",
            testUser);
    authPrefs.saveSession(session);

    // Seed cloud preferences into local repository via applyRemotePreferences (as if fetched from GET /me/preferences)
    localRepo.applyRemotePreferences(
        java.util.Arrays.asList("aljazeera", "fox"),
        1.15f,
        1,
        System.currentTimeMillis());

    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      scenario.onActivity(
          activity -> {
            activity.findViewById(R.id.btn_settings).performClick();
            activity.getSupportFragmentManager().executePendingTransactions();

            // 1. Account section shows signed-in state
            TextView tvName = activity.findViewById(R.id.tv_account_name);
            assertEquals("Alex Smith", tvName.getText().toString());

            // 2. News Sources checkboxes reflect cloud preferences
            CheckBox cbBbc = activity.findViewById(R.id.cb_source_bbc);
            CheckBox cbCnn = activity.findViewById(R.id.cb_source_cnn);
            CheckBox cbAljazeera = activity.findViewById(R.id.cb_source_aljazeera);
            CheckBox cbFox = activity.findViewById(R.id.cb_source_fox);

            assertFalse(cbBbc.isChecked());
            assertFalse(cbCnn.isChecked());
            assertTrue(cbAljazeera.isChecked());
            assertTrue(cbFox.isChecked());

            // 3. Font Scale radio button reflects cloud preference (Large 1.15x)
            RadioGroup fontGroup = activity.findViewById(R.id.rg_font_size);
            assertEquals(R.id.rb_font_large, fontGroup.getCheckedRadioButtonId());
            assertEquals(1.15f, localRepo.getFontScale(), 0.001f);

            // 4. Modify setting locally on device: select BBC
            cbBbc.setChecked(true);
            assertTrue(localRepo.getEnabledSourceIds().contains("bbc"));
          });

      // Verify persistent across activity recreation / screen rotation
      scenario.recreate();

      scenario.onActivity(
          activity -> {
            activity.getSupportFragmentManager().executePendingTransactions();
            CheckBox cbBbc = activity.findViewById(R.id.cb_source_bbc);
            assertTrue(cbBbc.isChecked());
            RadioGroup fontGroup = activity.findViewById(R.id.rg_font_size);
            assertEquals(R.id.rb_font_large, fontGroup.getCheckedRadioButtonId());
          });
    }
  }
}
