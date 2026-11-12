package com.example.newsagnet.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.newsagnet.MainActivity;
import com.example.newsagnet.R;
import com.example.newsagnet.databinding.FragmentSettingsBinding;

/**
 * SettingsFragment hosts the unified User Settings screen with four sections: Account, News
 * Sources, Appearance (Text Size with live preview), and AI Agent API Key Configuration.
 *
 * <p>Conforms to COMP7506 Ch4 (Mobile App Lifecycle), Ch5 (XML Layouts & Material Components), and
 * Ch6 (Fragment Lifecycle, Navigation, and State Restoration).
 */
public class SettingsFragment extends Fragment {

  private static final String TAG = "SettingsFragment";

  private static final String STATE_FONT_CHECKED_ID = "state_font_checked_id";
  private static final String STATE_PROVIDER_CHECKED_ID = "state_provider_checked_id";
  private static final String STATE_CB_BBC = "state_cb_bbc";
  private static final String STATE_CB_CNN = "state_cb_cnn";
  private static final String STATE_CB_ALJAZEERA = "state_cb_aljazeera";
  private static final String STATE_CB_FOX = "state_cb_fox";

  public static final float SCALE_SMALL = 0.90f;
  public static final float SCALE_MEDIUM = 1.00f;
  public static final float SCALE_LARGE = 1.15f;
  public static final float SCALE_EXTRA_LARGE = 1.30f;

  private static final float BASE_PREVIEW_HEADLINE_SP = 16f;
  private static final float BASE_PREVIEW_BODY_SP = 14f;
  private static final float BASE_PREVIEW_META_SP = 12f;

  private FragmentSettingsBinding binding;
  private float currentPreviewScale = SCALE_MEDIUM;

  /**
   * Factory method creating a new instance of {@link SettingsFragment}.
   *
   * @return A new instance of {@link SettingsFragment}.
   */
  @NonNull
  public static SettingsFragment newInstance() {
    return new SettingsFragment();
  }

  @Override
  public void onAttach(@NonNull Context context) {
    super.onAttach(context);
    Log.d(TAG, "onAttach: SettingsFragment attached to Activity");
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "onCreate: Initializing SettingsFragment");
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    Log.d(TAG, "onCreateView: Inflating fragment_settings layout with ViewBinding");
    binding = FragmentSettingsBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    Log.d(TAG, "onViewCreated: Configuring Settings sections and navigation callbacks");

    setupHeaderNavigation();
    setupNewsSourcesSection();
    setupAppearanceSection();
    setupAiAgentSection();

    if (savedInstanceState != null) {
      restoreUiState(savedInstanceState);
    } else {
      applyPreviewScale(SCALE_MEDIUM);
    }
  }

  private void setupHeaderNavigation() {
    binding.btnSettingsBack.setOnClickListener(
        v -> {
          Log.d(TAG, "Toolbar back button clicked: returning to previous screen");
          if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).closeSettings();
          } else if (getActivity() != null) {
            getActivity().getOnBackPressedDispatcher().onBackPressed();
          }
        });
  }

  private void setupNewsSourcesSection() {
    binding.btnSourcesSelectAll.setOnClickListener(
        v -> {
          Log.d(TAG, "Select All news sources clicked");
          setAllSourcesChecked(true);
        });

    binding.btnSourcesReset.setOnClickListener(
        v -> {
          Log.d(TAG, "Reset news sources to default clicked");
          setAllSourcesChecked(true);
        });
  }

  private void setAllSourcesChecked(boolean checked) {
    if (binding == null) {
      return;
    }
    binding.cbSourceBbc.setChecked(checked);
    binding.cbSourceCnn.setChecked(checked);
    binding.cbSourceAljazeera.setChecked(checked);
    binding.cbSourceFox.setChecked(checked);
  }

  private void setupAppearanceSection() {
    binding.rgFontSize.setOnCheckedChangeListener(
        (group, checkedId) -> {
          float scale = resolveScaleForRadioId(checkedId);
          Log.d(TAG, "Font size preference changed: scale=" + scale);
          applyPreviewScale(scale);
        });
  }

  private void setupAiAgentSection() {
    binding.btnUseServerDefault.setOnClickListener(
        v -> {
          if (binding != null) {
            binding.rbProviderServerDefault.setChecked(true);
            binding.etApiKey.setText("");
            binding.tvAiConnectionStatus.setText(R.string.settings_ai_status_default);
          }
        });
  }

  /**
   * Maps a radio button ID to its corresponding font scale factor.
   *
   * @param checkedId Checked RadioButton resource ID.
   * @return Scale factor (0.90x to 1.30x).
   */
  public static float resolveScaleForRadioId(int checkedId) {
    if (checkedId == R.id.rb_font_small) {
      return SCALE_SMALL;
    } else if (checkedId == R.id.rb_font_large) {
      return SCALE_LARGE;
    } else if (checkedId == R.id.rb_font_extra_large) {
      return SCALE_EXTRA_LARGE;
    } else {
      return SCALE_MEDIUM;
    }
  }

  private void applyPreviewScale(float scale) {
    this.currentPreviewScale = scale;
    if (binding == null) {
      return;
    }
    binding.tvFontPreviewHeadline.setTextSize(
        TypedValue.COMPLEX_UNIT_SP, BASE_PREVIEW_HEADLINE_SP * scale);
    binding.tvFontPreviewBody.setTextSize(
        TypedValue.COMPLEX_UNIT_SP, BASE_PREVIEW_BODY_SP * scale);
    binding.tvFontPreviewMeta.setTextSize(
        TypedValue.COMPLEX_UNIT_SP, BASE_PREVIEW_META_SP * scale);
  }

  private void restoreUiState(@NonNull Bundle savedInstanceState) {
    binding.cbSourceBbc.setChecked(savedInstanceState.getBoolean(STATE_CB_BBC, true));
    binding.cbSourceCnn.setChecked(savedInstanceState.getBoolean(STATE_CB_CNN, true));
    binding.cbSourceAljazeera.setChecked(savedInstanceState.getBoolean(STATE_CB_ALJAZEERA, true));
    binding.cbSourceFox.setChecked(savedInstanceState.getBoolean(STATE_CB_FOX, true));

    int fontCheckedId = savedInstanceState.getInt(STATE_FONT_CHECKED_ID, R.id.rb_font_medium);
    binding.rgFontSize.check(fontCheckedId);
    applyPreviewScale(resolveScaleForRadioId(fontCheckedId));

    int providerCheckedId =
        savedInstanceState.getInt(STATE_PROVIDER_CHECKED_ID, R.id.rb_provider_server_default);
    binding.rgAiProvider.check(providerCheckedId);
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    if (binding != null) {
      outState.putBoolean(STATE_CB_BBC, binding.cbSourceBbc.isChecked());
      outState.putBoolean(STATE_CB_CNN, binding.cbSourceCnn.isChecked());
      outState.putBoolean(STATE_CB_ALJAZEERA, binding.cbSourceAljazeera.isChecked());
      outState.putBoolean(STATE_CB_FOX, binding.cbSourceFox.isChecked());
      outState.putInt(STATE_FONT_CHECKED_ID, binding.rgFontSize.getCheckedRadioButtonId());
      outState.putInt(STATE_PROVIDER_CHECKED_ID, binding.rgAiProvider.getCheckedRadioButtonId());
    }
  }

  /**
   * Returns the current live preview font scale factor.
   *
   * @return Current font scale factor.
   */
  public float getCurrentPreviewScale() {
    return currentPreviewScale;
  }

  @Override
  public void onStart() {
    super.onStart();
    Log.d(TAG, "onStart: SettingsFragment becoming visible");
  }

  @Override
  public void onResume() {
    super.onResume();
    Log.d(TAG, "onResume: SettingsFragment active in foreground");
  }

  @Override
  public void onPause() {
    super.onPause();
    Log.d(TAG, "onPause: SettingsFragment paused");
  }

  @Override
  public void onStop() {
    super.onStop();
    Log.d(TAG, "onStop: SettingsFragment stopped");
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    Log.d(TAG, "onDestroyView: Clearing ViewBinding reference");
    binding = null;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    Log.d(TAG, "onDestroy: SettingsFragment destroyed");
  }

  @Override
  public void onDetach() {
    super.onDetach();
    Log.d(TAG, "onDetach: SettingsFragment detached from Activity");
  }
}
