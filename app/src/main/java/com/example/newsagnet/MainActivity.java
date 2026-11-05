package com.example.newsagnet;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.example.newsagnet.ui.fragment.ChatFragment;
import com.example.newsagnet.ui.fragment.GlobeFragment;
import com.example.newsagnet.ui.fragment.NewsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

/**
 * MainActivity acts as the central controller hosting Fragment navigation and XML UI.
 * Conforms to COMP7506 Ch2 (Logcat/Debugging), Ch5 (XML Layouts / MVC), and Ch6 (Activity Lifecycle).
 */
public class MainActivity extends AppCompatActivity
    implements NavigationBarView.OnItemSelectedListener {

  private static final String TAG = "MainActivity";
  private static final String TAG_NEWS = "tag_news";
  private static final String TAG_GLOBE = "tag_globe";
  private static final String TAG_CHAT = "tag_chat";

  private BottomNavigationView bottomNavigationView;
  private Fragment activeFragment;
  private NewsFragment newsFragment;
  private GlobeFragment globeFragment;
  private ChatFragment chatFragment;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "onCreate: Initializing MainActivity with XML layout");

    setContentView(R.layout.activity_main);

    bottomNavigationView = findViewById(R.id.bottom_navigation);
    bottomNavigationView.setOnItemSelectedListener(this);

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
    } else {
      Log.d(TAG, "onCreate: Restoring fragments from saved instance state");
      newsFragment = (NewsFragment) fm.findFragmentByTag(TAG_NEWS);
      globeFragment = (GlobeFragment) fm.findFragmentByTag(TAG_GLOBE);
      chatFragment = (ChatFragment) fm.findFragmentByTag(TAG_CHAT);

      if (newsFragment == null) newsFragment = NewsFragment.newInstance();
      if (globeFragment == null) globeFragment = GlobeFragment.newInstance();
      if (chatFragment == null) chatFragment = ChatFragment.newInstance();

      activeFragment = newsFragment;
    }
  }

  @Override
  public boolean onNavigationItemSelected(@NonNull MenuItem item) {
    int itemId = item.getItemId();
    Fragment targetFragment = null;

    if (itemId == R.id.nav_news) {
      Log.d(TAG, "onNavigationItemSelected: Selected News Feed");
      targetFragment = newsFragment;
    } else if (itemId == R.id.nav_globe) {
      Log.d(TAG, "onNavigationItemSelected: Selected 3D Globe Situation Map");
      targetFragment = globeFragment;
    } else if (itemId == R.id.nav_chat) {
      Log.d(TAG, "onNavigationItemSelected: Selected AI News Intel");
      targetFragment = chatFragment;
    }

    if (targetFragment != null && targetFragment != activeFragment) {
      FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
      ft.hide(activeFragment);
      ft.show(targetFragment);
      ft.commit();
      activeFragment = targetFragment;
      return true;
    }

    return false;
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
