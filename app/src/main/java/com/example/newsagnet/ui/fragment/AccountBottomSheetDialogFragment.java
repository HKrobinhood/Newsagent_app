package com.example.newsagnet.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.newsagnet.MainActivity;
import com.example.newsagnet.R;
import com.example.newsagnet.auth.GoogleAuthManager;
import com.example.newsagnet.concurrency.ResultCallback;
import com.example.newsagnet.data.prefs.UserPreferences;
import com.example.newsagnet.databinding.BottomSheetAccountBinding;
import com.example.newsagnet.model.UserProfile;
import com.example.newsagnet.repository.AuthRepository;
import com.example.newsagnet.repository.AuthRepositoryImpl;
import com.example.newsagnet.repository.RemotePreferencesRepository;
import com.example.newsagnet.repository.RemotePreferencesRepositoryImpl;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Bottom Sheet DialogFragment displaying user account identity, settings shortcut, reading
 * history shortcut, and safe sign-out action (inspired by BBC News account UI).
 *
 * <p>Conforms to COMP7506 Ch3 (Java OOP Encapsulation) and Material 3 Bottom Sheet design.
 */
public class AccountBottomSheetDialogFragment extends BottomSheetDialogFragment
    implements AuthRepository.AuthListener {

  public static final String TAG = "AccountBottomSheet";

  private BottomSheetAccountBinding binding;
  private AuthRepository authRepository;
  private GoogleAuthManager googleAuthManager;
  private RemotePreferencesRepository remotePreferencesRepository;

  @NonNull
  public static AccountBottomSheetDialogFragment newInstance() {
    return new AccountBottomSheetDialogFragment();
  }

  @Override
  public void onAttach(@NonNull Context context) {
    super.onAttach(context);
    Log.d(TAG, "onAttach: AccountBottomSheet attached to Activity");
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
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
    binding = BottomSheetAccountBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    authRepository.addAuthListener(this);

    setupAccountHeader();
    setupMenuActions();
    setupSignOutAction();

    syncAccountUi();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    if (authRepository != null) {
      authRepository.removeAuthListener(this);
    }
    binding = null;
  }

  private void setupAccountHeader() {
    binding.layoutSheetAccount.setOnClickListener(
        v -> {
          if (!authRepository.isLoggedIn()) {
            handleSignInClick();
          } else {
            showAccountDetailsDialog();
          }
        });
  }

  private void handleSignInClick() {
    if (!googleAuthManager.isGoogleOAuthConfigured(requireContext())) {
      String guide = googleAuthManager.getConfigurationGuideMessage(requireContext());
      new MaterialAlertDialogBuilder(requireContext())
          .setTitle(R.string.account_title)
          .setMessage(guide)
          .setPositiveButton(android.R.string.ok, null)
          .show();
      return;
    }

    googleAuthManager.startGoogleSignIn(
        requireActivity(),
        new ResultCallback<String>() {
          @Override
          public void onSuccess(@NonNull String idToken) {
            authRepository.loginWithGoogle(
                idToken,
                new ResultCallback<UserProfile>() {
                  @Override
                  public void onSuccess(@NonNull UserProfile user) {
                    remotePreferencesRepository.reconcileOnLogin(
                        new ResultCallback<UserPreferences>() {
                          @Override
                          public void onSuccess(@NonNull UserPreferences preferences) {
                            syncAccountUi();
                          }

                          @Override
                          public void onError(@NonNull Exception exception) {
                            syncAccountUi();
                          }
                        });
                  }

                  @Override
                  public void onError(@NonNull Exception exception) {
                    if (getContext() != null) {
                      new MaterialAlertDialogBuilder(requireContext())
                          .setTitle(R.string.account_title)
                          .setMessage(exception.getMessage())
                          .setPositiveButton(android.R.string.ok, null)
                          .show();
                    }
                  }
                });
          }

          @Override
          public void onError(@NonNull Exception exception) {
            if (getContext() != null) {
              new MaterialAlertDialogBuilder(requireContext())
                  .setTitle(R.string.account_title)
                  .setMessage(exception.getMessage())
                  .setPositiveButton(android.R.string.ok, null)
                  .show();
            }
          }
        });
  }

  private void showAccountDetailsDialog() {
    UserProfile profile = authRepository.getCurrentUser();
    String message =
        getString(R.string.settings_account_signed_in_hint)
            + "\n\n"
            + profile.getFormattedDisplayName()
            + "\n"
            + profile.getEmail();

    new MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.account_title)
        .setMessage(message)
        .setPositiveButton(android.R.string.ok, null)
        .show();
  }

  private void setupMenuActions() {
    // 1. Settings shortcut
    binding.itemSheetSettings.setOnClickListener(
        v -> {
          dismissAllowingStateLoss();
          if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).openSettings();
          }
        });

    // 2. Reading History shortcut
    binding.itemSheetHistory.setOnClickListener(
        v -> {
          dismissAllowingStateLoss();
          if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).openReadingHistory();
          }
        });
  }

  private void setupSignOutAction() {
    binding.btnSheetSignOut.setOnClickListener(
        v -> {
          new MaterialAlertDialogBuilder(requireContext())
              .setTitle(R.string.account_sign_out_confirm_title)
              .setMessage(R.string.account_sign_out_confirm_message)
              .setPositiveButton(
                  R.string.account_sign_out_confirm_positive,
                  (dialog, which) -> performSignOut())
              .setNegativeButton(R.string.account_sign_out_confirm_negative, null)
              .show();
        });
  }

  private void performSignOut() {
    authRepository.logout(
        new ResultCallback<Void>() {
          @Override
          public void onSuccess(Void data) {
            syncAccountUi();
          }

          @Override
          public void onError(@NonNull Exception exception) {
            syncAccountUi();
          }
        });
  }

  public void syncAccountUi() {
    if (binding == null || authRepository == null) {
      return;
    }
    if (authRepository.isLoggedIn()) {
      UserProfile profile = authRepository.getCurrentUser();
      binding.tvSheetAccountName.setText(profile.getFormattedDisplayName());
      binding.tvSheetAccountSubtitle.setText(profile.getEmail());
      binding.dividerSheetSignOut.setVisibility(View.VISIBLE);
      binding.btnSheetSignOut.setVisibility(View.VISIBLE);
    } else {
      binding.tvSheetAccountName.setText(R.string.account_guest_title);
      binding.tvSheetAccountSubtitle.setText(R.string.account_guest_subtitle);
      binding.dividerSheetSignOut.setVisibility(View.GONE);
      binding.btnSheetSignOut.setVisibility(View.GONE);
    }
  }

  @Override
  public void onAuthChanged(@NonNull UserProfile userProfile) {
    syncAccountUi();
  }
}
