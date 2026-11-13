package com.example.newsagnet.ui.fragment;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.example.newsagnet.MainActivity;
import com.example.newsagnet.R;
import com.example.newsagnet.auth.GoogleAuthManager;
import com.example.newsagnet.concurrency.ResultCallback;
import com.example.newsagnet.data.prefs.FontScaleOption;
import com.example.newsagnet.data.prefs.LocalPreferencesRepository;
import com.example.newsagnet.data.prefs.ThemeMode;
import com.example.newsagnet.data.prefs.UserPreferences;
import com.example.newsagnet.data.prefs.UserPreferencesRepository;
import com.example.newsagnet.data.security.AiProviderOption;
import com.example.newsagnet.data.security.SecureApiKeyStore;
import com.example.newsagnet.databinding.FragmentSettingsBinding;
import com.example.newsagnet.model.KeyTestResult;
import com.example.newsagnet.model.NewsSourceItem;
import com.example.newsagnet.model.UserProfile;
import com.example.newsagnet.repository.AuthRepository;
import com.example.newsagnet.repository.AuthRepositoryImpl;
import com.example.newsagnet.repository.JavaChatRepository;
import com.example.newsagnet.repository.JavaChatRepositoryImpl;
import com.example.newsagnet.repository.JavaNewsRepository;
import com.example.newsagnet.repository.JavaNewsRepositoryImpl;
import com.example.newsagnet.repository.NewsCallback;
import com.example.newsagnet.repository.RemotePreferencesRepository;
import com.example.newsagnet.repository.RemotePreferencesRepositoryImpl;
import com.example.newsagnet.ui.theme.FontScaleManager;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * SettingsFragment hosts the unified User Settings screen with four sections: Account, News
 * Sources, Appearance (Text Size with live preview), and AI Agent API Key Configuration.
 *
 * <p>Conforms to COMP7506 Ch3 (Java OOP & Repository Pattern), Ch4 (Mobile App Lifecycle), Ch5 (XML
 * Layouts & Material Components), Ch6 (Fragment Lifecycle, SharedPreferences, State Restoration),
 * and Ch7 (Asynchronous Concurrency).
 */
public class SettingsFragment extends Fragment implements AuthRepository.AuthListener {

  private static final String TAG = "SettingsFragment";

  private static final String STATE_PROVIDER_CHECKED_ID = "state_provider_checked_id";

  public static final float SCALE_SMALL = FontScaleOption.SMALL.getScale();
  public static final float SCALE_MEDIUM = FontScaleOption.MEDIUM.getScale();
  public static final float SCALE_LARGE = FontScaleOption.LARGE.getScale();
  public static final float SCALE_EXTRA_LARGE = FontScaleOption.EXTRA_LARGE.getScale();

  private static final float BASE_PREVIEW_HEADLINE_SP = 16f;
  private static final float BASE_PREVIEW_BODY_SP = 14f;
  private static final float BASE_PREVIEW_META_SP = 12f;

  private FragmentSettingsBinding binding;
  private UserPreferencesRepository preferencesRepository;
  private SecureApiKeyStore secureApiKeyStore;
  private JavaNewsRepository newsRepository;
  private JavaChatRepository chatRepository;
  private AuthRepository authRepository;
  private GoogleAuthManager googleAuthManager;
  private RemotePreferencesRepository remotePreferencesRepository;
  private final Map<String, MaterialCheckBox> dynamicSourceCheckBoxes = new LinkedHashMap<>();
  private final Set<String> availableSourceIds =
      new LinkedHashSet<>(NewsSourceItem.getDefaultSourceIds());
  private boolean isUpdatingUi = false;
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
    Log.d(TAG, "onCreate: Initializing SettingsFragment repositories");
    preferencesRepository = LocalPreferencesRepository.getInstance(requireContext());
    secureApiKeyStore = SecureApiKeyStore.getInstance(requireContext());
    newsRepository = new JavaNewsRepositoryImpl();
    chatRepository = new JavaChatRepositoryImpl(requireContext());
    authRepository = AuthRepositoryImpl.getInstance(requireContext());
    googleAuthManager = GoogleAuthManager.getInstance();
    remotePreferencesRepository = RemotePreferencesRepositoryImpl.getInstance(requireContext());
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
    Log.d(TAG, "onViewCreated: Configuring Settings sections and loading persistent preferences");

    authRepository.addAuthListener(this);

    setupHeaderNavigation();
    setupNewsSourcesSection();
    setupAppearanceSection();
    setupAiAgentSection();

    syncUiFromPreferences();

    if (authRepository.isLoggedIn()) {
      remotePreferencesRepository.syncFromRemote(
          new ResultCallback<UserPreferences>() {
            @Override
            public void onSuccess(@NonNull UserPreferences preferences) {
              if (binding != null) {
                syncUiFromPreferences();
              }
            }

            @Override
            public void onError(@NonNull Exception exception) {
              Log.d(TAG, "syncFromRemote fallback to local: " + exception.getMessage());
            }
          });
    }

    if (savedInstanceState != null) {
      int providerCheckedId =
          savedInstanceState.getInt(STATE_PROVIDER_CHECKED_ID, R.id.rb_provider_server_default);
      binding.rgAiProvider.check(providerCheckedId);
    }

    fetchBackendNewsSources();
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

  @Override
  public void onAuthChanged(@NonNull UserProfile userProfile) {
    if (!userProfile.isGuest() && remotePreferencesRepository != null) {
      remotePreferencesRepository.syncFromRemote(
          new ResultCallback<UserPreferences>() {
            @Override
            public void onSuccess(@NonNull UserPreferences preferences) {
              if (binding != null) {
                syncUiFromPreferences();
              }
            }

            @Override
            public void onError(@NonNull Exception exception) {
              Log.d(TAG, "onAuthChanged sync error: " + exception.getMessage());
            }
          });
    }
  }

  private void setupNewsSourcesSection() {
    CompoundButton.OnCheckedChangeListener sourceToggleListener =
        (buttonView, isChecked) -> {
          if (isUpdatingUi) {
            return;
          }
          String sourceId = resolveSourceIdForCheckBoxId(buttonView.getId());
          if (sourceId != null) {
            Log.d(TAG, "Media source toggled: " + sourceId + " -> " + isChecked);
            preferencesRepository.setSourceEnabled(sourceId, isChecked);
          }
        };

    binding.cbSourceBbc.setOnCheckedChangeListener(sourceToggleListener);
    binding.cbSourceCnn.setOnCheckedChangeListener(sourceToggleListener);
    binding.cbSourceAljazeera.setOnCheckedChangeListener(sourceToggleListener);
    binding.cbSourceFox.setOnCheckedChangeListener(sourceToggleListener);

    binding.btnSourcesSelectAll.setOnClickListener(
        v -> {
          Log.d(TAG, "Select All news sources clicked");
          preferencesRepository.selectAllSources(availableSourceIds);
          syncUiFromPreferences();
        });

    binding.btnSourcesReset.setOnClickListener(
        v -> {
          Log.d(TAG, "Reset news sources to default clicked");
          preferencesRepository.resetSourcesToDefault(availableSourceIds);
          syncUiFromPreferences();
        });
  }

  @Nullable
  private String resolveSourceIdForCheckBoxId(int viewId) {
    if (viewId == R.id.cb_source_bbc) {
      return NewsSourceItem.SOURCE_BBC;
    } else if (viewId == R.id.cb_source_cnn) {
      return NewsSourceItem.SOURCE_CNN;
    } else if (viewId == R.id.cb_source_aljazeera) {
      return NewsSourceItem.SOURCE_ALJAZEERA;
    } else if (viewId == R.id.cb_source_fox) {
      return NewsSourceItem.SOURCE_FOX;
    }
    return null;
  }

  private void fetchBackendNewsSources() {
    newsRepository.getNewsSources(
        new NewsCallback<List<NewsSourceItem>>() {
          @Override
          public void onSuccess(@NonNull List<NewsSourceItem> sources) {
            if (binding == null || !isAdded()) {
              return;
            }
            List<String> ids = NewsSourceItem.extractIds(sources);
            availableSourceIds.addAll(ids);
            preferencesRepository.reconcileAvailableSources(availableSourceIds);
            renderDynamicSourcesIfAny(sources);
            syncUiFromPreferences();
          }

          @Override
          public void onError(@NonNull Throwable throwable) {
            Log.w(TAG, "Using default news sources fallback: " + throwable.getMessage());
          }
        });
  }

  private void renderDynamicSourcesIfAny(@NonNull List<NewsSourceItem> sources) {
    if (binding == null) {
      return;
    }
    Set<String> builtInIds = NewsSourceItem.getDefaultSourceIds();
    for (NewsSourceItem item : sources) {
      String sourceId = item.getId();
      if (builtInIds.contains(sourceId) || dynamicSourceCheckBoxes.containsKey(sourceId)) {
        continue;
      }
      MaterialCheckBox checkBox = new MaterialCheckBox(requireContext());
      checkBox.setText(item.getName());
      checkBox.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
      checkBox.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
      checkBox.setButtonTintList(
          ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primary_cyan)));
      checkBox.setChecked(preferencesRepository.getPreferences().isSourceEnabled(sourceId));
      checkBox.setOnCheckedChangeListener(
          (btn, isChecked) -> {
            if (!isUpdatingUi) {
              preferencesRepository.setSourceEnabled(sourceId, isChecked);
            }
          });
      dynamicSourceCheckBoxes.put(sourceId, checkBox);
      binding.layoutSourcesContainer.addView(checkBox);
    }
  }

  private void setupAppearanceSection() {
    View.OnClickListener themeClickListener =
        v -> {
          int checkedId = v.getId();
          ThemeMode selectedMode;
          if (checkedId == R.id.rb_theme_light) {
            selectedMode = ThemeMode.LIGHT;
          } else if (checkedId == R.id.rb_theme_dark) {
            selectedMode = ThemeMode.DARK;
          } else {
            selectedMode = ThemeMode.SYSTEM;
          }
          Log.d(TAG, "Theme mode user selected: " + selectedMode.getKey());
          preferencesRepository.setThemeMode(selectedMode);
          AppCompatDelegate.setDefaultNightMode(selectedMode.getNightMode());
        };

    binding.rbThemeLight.setOnClickListener(themeClickListener);
    binding.rbThemeDark.setOnClickListener(themeClickListener);
    binding.rbThemeSystem.setOnClickListener(themeClickListener);

    binding.rgFontSize.setOnCheckedChangeListener(
        (group, checkedId) -> {
          float scale = resolveScaleForRadioId(checkedId);
          Log.d(TAG, "Font size preference changed: scale=" + scale);
          if (!isUpdatingUi) {
            preferencesRepository.setFontScale(scale);
          }
          applyPreviewScale(scale);
          if (binding != null) {
            FontScaleManager.applyFontScale(binding.getRoot(), scale);
            applyPreviewScale(scale);
          }
        });
  }

  private void setupAiAgentSection() {
    binding.rgAiProvider.setOnCheckedChangeListener(
        (group, checkedId) -> {
          if (isUpdatingUi || secureApiKeyStore == null) {
            return;
          }
          AiProviderOption selected = resolveProviderForRadioId(checkedId);
          secureApiKeyStore.setSelectedProvider(selected);
          updateAiStatusBanner(null);
        });

    binding.btnSaveApiKey.setOnClickListener(
        v -> {
          if (binding == null || secureApiKeyStore == null) {
            return;
          }
          String rawInput =
              binding.etApiKey.getText() != null
                  ? binding.etApiKey.getText().toString().trim()
                  : "";
          if (rawInput.isEmpty()) {
            binding.tilApiKey.setError(getString(R.string.settings_ai_error_empty_key));
            return;
          }
          binding.tilApiKey.setError(null);

          AiProviderOption targetProvider =
              resolveProviderForRadioId(binding.rgAiProvider.getCheckedRadioButtonId());
          if (targetProvider == AiProviderOption.SERVER_DEFAULT) {
            targetProvider = AiProviderOption.GEMINI;
          }

          secureApiKeyStore.saveApiKey(targetProvider, rawInput);
          // Immediately clear input field so full plaintext key is never displayed on screen
          binding.etApiKey.setText("");
          syncAiAgentUi();
        });

    binding.btnRemoveApiKey.setOnClickListener(
        v -> {
          if (binding == null || secureApiKeyStore == null) {
            return;
          }
          secureApiKeyStore.removeApiKey();
          binding.etApiKey.setText("");
          binding.tilApiKey.setError(null);
          syncAiAgentUi();
        });

    binding.btnUseServerDefault.setOnClickListener(
        v -> {
          if (binding == null || secureApiKeyStore == null) {
            return;
          }
          secureApiKeyStore.useServerDefault();
          binding.etApiKey.setText("");
          binding.tilApiKey.setError(null);
          syncAiAgentUi();
        });

    binding.btnTestConnection.setOnClickListener(
        v -> {
          if (binding == null || chatRepository == null || secureApiKeyStore == null) {
            return;
          }
          binding.tilApiKey.setError(null);
          AiProviderOption provider =
              resolveProviderForRadioId(binding.rgAiProvider.getCheckedRadioButtonId());
          String typedKey =
              binding.etApiKey.getText() != null
                  ? binding.etApiKey.getText().toString().trim()
                  : "";
          String keyToTest =
              !typedKey.isEmpty() ? typedKey : secureApiKeyStore.getDecryptedApiKey();

          binding.btnTestConnection.setEnabled(false);
          binding.tvAiConnectionStatus.setText(R.string.settings_ai_status_testing);

          chatRepository.testApiKeyConnection(
              provider,
              keyToTest,
              new ResultCallback<KeyTestResult>() {
                @Override
                public void onSuccess(@NonNull KeyTestResult result) {
                  if (binding == null || !isAdded()) {
                    return;
                  }
                  binding.btnTestConnection.setEnabled(true);
                  updateAiStatusBanner(result);
                }

                @Override
                public void onError(@NonNull Exception exception) {
                  if (binding == null || !isAdded()) {
                    return;
                  }
                  binding.btnTestConnection.setEnabled(true);
                  updateAiStatusBanner(
                      new KeyTestResult(
                          KeyTestResult.STATUS_PROVIDER_ERROR,
                          provider.getId(),
                          "Unable to reach verification service."));
                }
              });
        });
  }

  private void syncAiAgentUi() {
    if (binding == null || secureApiKeyStore == null) {
      return;
    }
    boolean prevUpdating = isUpdatingUi;
    isUpdatingUi = true;
    try {
      AiProviderOption selected = secureApiKeyStore.getSelectedProvider();
      int targetRadioId = resolveRadioIdForProvider(selected);
      if (binding.rgAiProvider.getCheckedRadioButtonId() != targetRadioId) {
        binding.rgAiProvider.check(targetRadioId);
      }

      if (secureApiKeyStore.hasConfiguredApiKey()) {
        AiProviderOption keyProvider = secureApiKeyStore.getConfiguredKeyProvider();
        String masked = secureApiKeyStore.getMaskedKeySummary();
        binding.tilApiKey.setHelperText(
            getString(R.string.settings_ai_helper_saved, keyProvider.getDisplayName(), masked));
      } else {
        binding.tilApiKey.setHelperText(null);
      }

      updateAiStatusBanner(null);
    } finally {
      isUpdatingUi = prevUpdating;
    }
  }

  private void updateAiStatusBanner(@Nullable KeyTestResult testResult) {
    if (binding == null || secureApiKeyStore == null) {
      return;
    }
    if (testResult != null) {
      binding.tvAiConnectionStatus.setText(
          getString(
              R.string.settings_ai_status_result,
              testResult.getStatus(),
              testResult.getMessage()));
      return;
    }

    AiProviderOption selected = secureApiKeyStore.getSelectedProvider();
    if (selected != AiProviderOption.SERVER_DEFAULT && secureApiKeyStore.hasConfiguredApiKey()) {
      String masked = secureApiKeyStore.getMaskedKeySummary();
      binding.tvAiConnectionStatus.setText(
          getString(
              R.string.settings_ai_status_configured, selected.getDisplayName(), masked));
    } else {
      binding.tvAiConnectionStatus.setText(R.string.settings_ai_status_default);
    }
  }

  /**
   * Maps a provider RadioButton ID to its {@link AiProviderOption}.
   *
   * @param checkedId Checked RadioButton resource ID.
   * @return Corresponding {@link AiProviderOption}.
   */
  @NonNull
  public static AiProviderOption resolveProviderForRadioId(int checkedId) {
    if (checkedId == R.id.rb_provider_gemini) {
      return AiProviderOption.GEMINI;
    } else if (checkedId == R.id.rb_provider_openai) {
      return AiProviderOption.OPENAI;
    } else {
      return AiProviderOption.SERVER_DEFAULT;
    }
  }

  /**
   * Maps an {@link AiProviderOption} to its RadioButton resource ID.
   *
   * @param provider {@link AiProviderOption}.
   * @return RadioButton resource ID.
   */
  public static int resolveRadioIdForProvider(@NonNull AiProviderOption provider) {
    if (provider == AiProviderOption.GEMINI) {
      return R.id.rb_provider_gemini;
    } else if (provider == AiProviderOption.OPENAI) {
      return R.id.rb_provider_openai;
    } else {
      return R.id.rb_provider_server_default;
    }
  }

  /**
   * Synchronizes checkboxes, font radio selection, and font scaling from {@link
   * UserPreferencesRepository}.
   */
  public void syncUiFromPreferences() {
    if (binding == null || preferencesRepository == null) {
      return;
    }
    isUpdatingUi = true;
    try {
      UserPreferences prefs = preferencesRepository.getPreferences();
      binding.cbSourceBbc.setChecked(prefs.isSourceEnabled(NewsSourceItem.SOURCE_BBC));
      binding.cbSourceCnn.setChecked(prefs.isSourceEnabled(NewsSourceItem.SOURCE_CNN));
      binding.cbSourceAljazeera.setChecked(prefs.isSourceEnabled(NewsSourceItem.SOURCE_ALJAZEERA));
      binding.cbSourceFox.setChecked(prefs.isSourceEnabled(NewsSourceItem.SOURCE_FOX));

      for (Map.Entry<String, MaterialCheckBox> entry : dynamicSourceCheckBoxes.entrySet()) {
        entry.getValue().setChecked(prefs.isSourceEnabled(entry.getKey()));
      }

      ThemeMode themeMode = preferencesRepository.getThemeMode();
      int themeRadioId;
      if (themeMode == ThemeMode.LIGHT) {
        themeRadioId = R.id.rb_theme_light;
      } else if (themeMode == ThemeMode.DARK) {
        themeRadioId = R.id.rb_theme_dark;
      } else {
        themeRadioId = R.id.rb_theme_system;
      }
      if (binding.rgThemeMode.getCheckedRadioButtonId() != themeRadioId) {
        binding.rgThemeMode.check(themeRadioId);
      }

      float scale = prefs.getFontScale();
      int radioId = resolveRadioIdForScale(scale);
      if (binding.rgFontSize.getCheckedRadioButtonId() != radioId) {
        binding.rgFontSize.check(radioId);
      }
      FontScaleManager.applyFontScale(binding.getRoot(), scale);
      applyPreviewScale(scale);
      syncAiAgentUi();
    } finally {
      isUpdatingUi = false;
    }
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

  /**
   * Maps a numeric font scale factor to its RadioButton resource ID.
   *
   * @param scale Font scale factor.
   * @return RadioButton ID in `fragment_settings.xml`.
   */
  public static int resolveRadioIdForScale(float scale) {
    FontScaleOption option = FontScaleOption.fromScale(scale);
    if (option == FontScaleOption.SMALL) {
      return R.id.rb_font_small;
    } else if (option == FontScaleOption.LARGE) {
      return R.id.rb_font_large;
    } else if (option == FontScaleOption.EXTRA_LARGE) {
      return R.id.rb_font_extra_large;
    } else {
      return R.id.rb_font_medium;
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

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    if (binding != null) {
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
  public void onHiddenChanged(boolean hidden) {
    super.onHiddenChanged(hidden);
    if (!hidden) {
      syncUiFromPreferences();
    }
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
    syncUiFromPreferences();
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
    if (authRepository != null) {
      authRepository.removeAuthListener(this);
    }
    dynamicSourceCheckBoxes.clear();
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
