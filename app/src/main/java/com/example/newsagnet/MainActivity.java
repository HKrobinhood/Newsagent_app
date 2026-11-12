package com.example.newsagnet;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.example.newsagnet.ui.fragment.ChatFragment;
import com.example.newsagnet.ui.fragment.GlobeFragment;
import com.example.newsagnet.ui.fragment.NewsFragment;
import com.example.newsagnet.ui.fragment.SettingsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

/**
 * MainActivity acts as the central controller hosting Fragment navigation and XML UI.
 *
 * <p>Conforms to COMP7506 Ch2 (Logcat/Debugging), Ch5 (XML Layouts / MVC), and Ch6 (Activity &
 * Fragment Lifecycle, Navigation, State Restoration).
 */
public class MainActivity extends AppCompatActivity
    implements NavigationBarView.OnItemSelectedListener,
        NavigationBarView.OnItemReselectedListener {

  private static final String TAG = "MainActivity";
  public static final String TAG_NEWS = "tag_news";
  public static final String TAG_GLOBE = "tag_globe";
  public static final String TAG_CHAT = "tag_chat";
  public static final String TAG_SETTINGS = "tag_settings";

  private static final String STATE_ACTIVE_TAG = "state_active_tag";
  private static final String STATE_PREVIOUS_TAB_TAG = "state_previous_tab_tag";

  private BottomNavigationView bottomNavigationView;
  private Fragment activeFragment;
  private NewsFragment newsFragment;
  private GlobeFragment globeFragment;
  private ChatFragment chatFragment;
  private SettingsFragment settingsFragment;
  private String previousTabTag = TAG_NEWS;

  private final OnBackPressedCallback settingsBackCallback =
      new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
          Log.d(TAG, "handleOnBackPressed: Returning from Settings to previous tab: " + previousTabTag);
          closeSettings();
        }
      };

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "onCreate: Initializing MainActivity with XML layout");

    setContentView(R.layout.activity_main);

    getOnBackPressedDispatcher().addCallback(this, settingsBackCallback);

    bottomNavigationView = findViewById(R.id.bottom_navigation);
    bottomNavigationView.setOnItemSelectedListener(this);
    bottomNavigationView.setOnItemReselectedListener(this);

    FragmentManager fm = getSupportFragmentManager();

    if (savedInstanceState == null) {
      Log.d(TAG, "onCreate: First launch, initializing fragments");
      newsFragment = NewsFragment.newInstance();
      globeFragment = GlobeFragment.newInstance();
      chatFragment = ChatFragment.newInstance();

      fm.beginTransaction()
          .add(R.id.fragment_container, chatFragment, TAG_CHAT)
          .hide(chatFragment)
          .add(R.id.fragment_container, globeFragment, TAG_GLOBE)
          .hide(globeFragment)
          .add(R.id.fragment_container, newsFragment, TAG_NEWS)
          .commit();

      activeFragment = newsFragment;
      previousTabTag = TAG_NEWS;
      settingsBackCallback.setEnabled(false);
    } else {
      Log.d(TAG, "onCreate: Restoring fragments from saved instance state");
      newsFragment = (NewsFragment) fm.findFragmentByTag(TAG_NEWS);
      globeFragment = (GlobeFragment) fm.findFragmentByTag(TAG_GLOBE);
      chatFragment = (ChatFragment) fm.findFragmentByTag(TAG_CHAT);
      settingsFragment = (SettingsFragment) fm.findFragmentByTag(TAG_SETTINGS);

      if (newsFragment == null) {
        newsFragment = NewsFragment.newInstance();
      }
      if (globeFragment == null) {
        globeFragment = GlobeFragment.newInstance();
      }
      if (chatFragment == null) {
        chatFragment = ChatFragment.newInstance();
      }

      previousTabTag = savedInstanceState.getString(STATE_PREVIOUS_TAB_TAG, TAG_NEWS);
      String activeTag = savedInstanceState.getString(STATE_ACTIVE_TAG, TAG_NEWS);

      if (TAG_SETTINGS.equals(activeTag) && settingsFragment == null) {
        settingsFragment = SettingsFragment.newInstance();
      }

      activeFragment = getFragmentForTag(activeTag);
      syncFragmentVisibility(fm, activeFragment);
      settingsBackCallback.setEnabled(TAG_SETTINGS.equals(activeTag));
    }
  }

  private void syncFragmentVisibility(
      @NonNull FragmentManager fm, @NonNull Fragment targetVisible) {
    FragmentTransaction ft = fm.beginTransaction();
    if (newsFragment != null && newsFragment.isAdded()) {
      if (newsFragment == targetVisible) {
        ft.show(newsFragment);
      } else {
        ft.hide(newsFragment);
      }
    }
    if (globeFragment != null && globeFragment.isAdded()) {
      if (globeFragment == targetVisible) {
        ft.show(globeFragment);
      } else {
        ft.hide(globeFragment);
      }
    }
    if (chatFragment != null && chatFragment.isAdded()) {
      if (chatFragment == targetVisible) {
        ft.show(chatFragment);
      } else {
        ft.hide(chatFragment);
      }
    }
    if (settingsFragment != null) {
      if (settingsFragment == targetVisible) {
        if (!settingsFragment.isAdded()) {
          ft.add(R.id.fragment_container, settingsFragment, TAG_SETTINGS);
        } else {
          ft.show(settingsFragment);
        }
      } else if (settingsFragment.isAdded()) {
        ft.hide(settingsFragment);
      }
    }
    ft.commit();
  }

  /**
   * Opens the unified {@link SettingsFragment} while recording the current tab so back navigation
   * returns to the exact origin screen (News, Globe, or AI Intel).
   */
  public void openSettings() {
    if (isSettingsVisible()) {
      Log.d(TAG, "openSettings: SettingsFragment is already visible");
      return;
    }

    String currentTag = getTagForFragment(activeFragment);
    if (!TAG_SETTINGS.equals(currentTag)) {
      previousTabTag = currentTag;
    }
    Log.d(TAG, "openSettings: Navigating to Settings from " + previousTabTag);

    FragmentManager fm = getSupportFragmentManager();
    if (settingsFragment == null) {
      settingsFragment = (SettingsFragment) fm.findFragmentByTag(TAG_SETTINGS);
      if (settingsFragment == null) {
        settingsFragment = SettingsFragment.newInstance();
      }
    }

    FragmentTransaction ft = fm.beginTransaction();
    if (activeFragment != null) {
      ft.hide(activeFragment);
    }
    if (!settingsFragment.isAdded()) {
      ft.add(R.id.fragment_container, settingsFragment, TAG_SETTINGS);
    } else {
      ft.show(settingsFragment);
    }
    ft.commit();

    activeFragment = settingsFragment;
    settingsBackCallback.setEnabled(true);
  }

  /**
   * Closes {@link SettingsFragment} and restores the tab that was active prior to opening Settings.
   */
  public void closeSettings() {
    if (!isSettingsVisible()) {
      return;
    }

    Fragment targetFragment = getFragmentForTag(previousTabTag);
    if (targetFragment == settingsFragment) {
      targetFragment = newsFragment;
    }
    Log.d(TAG, "closeSettings: Restoring previous fragment: " + previousTabTag);

    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
    if (settingsFragment != null && settingsFragment.isAdded()) {
      ft.hide(settingsFragment);
    }
    ft.show(targetFragment);
    ft.commit();

    activeFragment = targetFragment;
    settingsBackCallback.setEnabled(false);
  }

  /**
   * Checks whether the Settings screen is currently displayed.
   *
   * @return True if {@link SettingsFragment} is the active fragment.
   */
  public boolean isSettingsVisible() {
    return activeFragment != null && activeFragment == settingsFragment;
  }

  /**
   * Returns the tag of the currently active fragment.
   *
   * @return Active fragment tag (`tag_news`, `tag_globe`, `tag_chat`, or `tag_settings`).
   */
  @NonNull
  public String getActiveFragmentTag() {
    return getTagForFragment(activeFragment);
  }

  /**
   * Returns the tag of the tab that was active before entering Settings.
   *
   * @return Previous tab tag.
   */
  @NonNull
  public String getPreviousTabTag() {
    return previousTabTag;
  }

  @NonNull
  private String getTagForFragment(@Nullable Fragment fragment) {
    if (fragment == globeFragment) {
      return TAG_GLOBE;
    } else if (fragment == chatFragment) {
      return TAG_CHAT;
    } else if (fragment == settingsFragment && settingsFragment != null) {
      return TAG_SETTINGS;
    } else {
      return TAG_NEWS;
    }
  }

  @NonNull
  private Fragment getFragmentForTag(@Nullable String tag) {
    if (TAG_GLOBE.equals(tag)) {
      return globeFragment;
    } else if (TAG_CHAT.equals(tag)) {
      return chatFragment;
    } else if (TAG_SETTINGS.equals(tag) && settingsFragment != null) {
      return settingsFragment;
    } else {
      return newsFragment;
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.main_toolbar_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    if (item.getItemId() == R.id.action_settings) {
      openSettings();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public boolean onNavigationItemSelected(@NonNull MenuItem item) {
    int itemId = item.getItemId();
    Fragment targetFragment = null;

    if (itemId == R.id.nav_news) {
      Log.d(TAG, "onNavigationItemSelected: Selected News Feed");
      targetFragment = newsFragment;
      previousTabTag = TAG_NEWS;
    } else if (itemId == R.id.nav_globe) {
      Log.d(TAG, "onNavigationItemSelected: Selected 3D Globe Situation Map");
      targetFragment = globeFragment;
      previousTabTag = TAG_GLOBE;
    } else if (itemId == R.id.nav_chat) {
      Log.d(TAG, "onNavigationItemSelected: Selected AI News Intel");
      targetFragment = chatFragment;
      previousTabTag = TAG_CHAT;
    }

    if (targetFragment != null && targetFragment != activeFragment) {
      FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
      ft.hide(activeFragment);
      ft.show(targetFragment);
      ft.commit();
      activeFragment = targetFragment;
      settingsBackCallback.setEnabled(false);
      return true;
    }

    return false;
  }

  @Override
  public void onNavigationItemReselected(@NonNull MenuItem item) {
    if (isSettingsVisible()) {
      Log.d(TAG, "onNavigationItemReselected while in Settings: closing Settings");
      onNavigationItemSelected(item);
    }
  }

  @Override
  protected void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putString(STATE_ACTIVE_TAG, getTagForFragment(activeFragment));
    outState.putString(STATE_PREVIOUS_TAB_TAG, previousTabTag);
  }

  @Override
  protected void onStart() {
    super.onStart();
    Log.d(TAG, "onStart: Activity becoming visible");
  }

  @Override
  protected void onResume() {
    super.onResume();
    Log.d(TAG, "onResume: Activity interacting with user");
  }

  @Override
  protected void onPause() {
    super.onPause();
    Log.d(TAG, "onPause: Activity pausing");
  }

  @Override
  protected void onStop() {
    super.onStop();
    Log.d(TAG, "onStop: Activity no longer visible");
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    Log.d(TAG, "onDestroy: Activity destroyed");
  }
}
