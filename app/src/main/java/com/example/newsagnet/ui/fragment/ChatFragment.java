package com.example.newsagnet.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.newsagnet.MainActivity;
import com.example.newsagnet.concurrency.ResultCallback;
import com.example.newsagnet.data.prefs.LocalPreferencesRepository;
import com.example.newsagnet.data.prefs.UserPreferences;
import com.example.newsagnet.data.prefs.UserPreferencesRepository;
import com.example.newsagnet.databinding.FragmentChatBinding;
import com.example.newsagnet.history.HistoryRepository;
import com.example.newsagnet.model.ChatMessage;
import com.example.newsagnet.model.MessageRole;
import com.example.newsagnet.model.NewsArticle;
import com.example.newsagnet.repository.AgentChatResult;
import com.example.newsagnet.repository.JavaChatRepository;
import com.example.newsagnet.repository.JavaChatRepositoryImpl;
import com.example.newsagnet.ui.adapter.ChatAdapter;
import com.example.newsagnet.ui.adapter.NewsItemClickListener;
import com.example.newsagnet.ui.theme.FontScaleManager;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * ChatFragment hosts the AI news intelligence assistant interface with multi-type ViewHolders,
 * media source constraint propagation, and global font scaling.
 *
 * <p>Conforms to COMP7506 Ch3 (OOP Polymorphism), Ch4 (Mobile Lifecycle), Ch5 (XML Layouts,
 * RecyclerView, Adapter, Multi-Type ViewHolders), Ch6 (Intent), and Ch7 (Thread, Runnable,
 * ExecutorService, Handler, UI Thread).
 */
public class ChatFragment extends Fragment
    implements NewsItemClickListener, UserPreferencesRepository.OnPreferencesChangedListener {

  private static final String TAG = "ChatFragment";

  private FragmentChatBinding binding;
  private JavaChatRepository chatRepository;
  private UserPreferencesRepository preferencesRepository;
  private ChatAdapter chatAdapter;
  private LinearLayoutManager layoutManager;

  private final List<ChatMessage> messageList = new ArrayList<>();
  private String currentSessionId = null;
  private boolean isSending = false;
  private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

  public static ChatFragment newInstance() {
    return new ChatFragment();
  }

  @Override
  public void onAttach(@NonNull Context context) {
    super.onAttach(context);
    Log.d(TAG, "onAttach: ChatFragment attached to Activity");
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "onCreate: Initializing ChatRepository");
    chatRepository = new JavaChatRepositoryImpl();
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    Log.d(TAG, "onCreateView: Inflating fragment_chat with ViewBinding");
    binding = FragmentChatBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    Log.d(TAG, "onViewCreated: Initializing Chat RecyclerView and Input listeners");

    preferencesRepository = LocalPreferencesRepository.getInstance(requireContext());
    preferencesRepository.addOnPreferencesChangedListener(this);

    setupRecyclerView();
    setupInputActions();
    setupHeaderActions();

    applyUserFontScale(preferencesRepository.getFontScale());

    if (messageList.isEmpty()) {
      initWelcomeMessage();
    }
  }

  private void applyUserFontScale(float fontScale) {
    if (binding != null) {
      FontScaleManager.applyFontScale(binding.getRoot(), fontScale);
    }
    if (chatAdapter != null) {
      chatAdapter.setFontScale(fontScale);
    }
  }

  @Override
  public void onPreferencesChanged(@NonNull UserPreferences updatedPreferences) {
    applyUserFontScale(updatedPreferences.getFontScale());
  }

  @Override
  public void onHiddenChanged(boolean hidden) {
    super.onHiddenChanged(hidden);
    if (!hidden && preferencesRepository != null) {
      applyUserFontScale(preferencesRepository.getFontScale());
    }
  }

  private void setupRecyclerView() {
    layoutManager = new LinearLayoutManager(requireContext());
    layoutManager.setStackFromEnd(true);
    chatAdapter = new ChatAdapter(this);

    binding.recyclerChat.setLayoutManager(layoutManager);
    binding.recyclerChat.setAdapter(chatAdapter);
  }

  private void setupInputActions() {
    binding.btnSend.setOnClickListener(v -> handleSendPrompt());

    binding.etChatInput.setOnEditorActionListener(
        (v, actionId, event) -> {
          if (actionId == EditorInfo.IME_ACTION_SEND) {
            handleSendPrompt();
            return true;
          }
          return false;
        });
  }

  private void setupHeaderActions() {
    binding.btnChatClear.setOnClickListener(
        v -> {
          Log.d(TAG, "Resetting conversation session");
          currentSessionId = null;
          messageList.clear();
          initWelcomeMessage();
        });

    binding.btnChatSettings.setOnClickListener(
        v -> {
          Log.d(TAG, "Chat header settings clicked");
          if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).openSettings();
          }
        });
  }

  private void initWelcomeMessage() {
    ChatMessage welcomeMessage =
        new ChatMessage.Builder()
            .setRole(MessageRole.ASSISTANT)
            .setContent(
                "Hello! I am GlobalNewsAI Intelligence Agent.\n\n"
                    + "You can ask me about recent international news, specific political statements, "
                    + "or cross-source verification (e.g. 'What did the climate summit conclude today?').")
            .setTimestamp(timeFormat.format(new Date()))
            .build();

    messageList.add(welcomeMessage);
    chatAdapter.submitList(new ArrayList<>(messageList));
  }

  private void handleSendPrompt() {
    if (isSending || binding == null) {
      return;
    }

    String prompt = binding.etChatInput.getText().toString().trim();
    if (prompt.isEmpty()) {
      return;
    }

    binding.etChatInput.setText("");
    setSendingState(true);

    ChatMessage userMessage =
        new ChatMessage.Builder()
            .setRole(MessageRole.USER)
            .setContent(prompt)
            .setTimestamp(timeFormat.format(new Date()))
            .build();
    messageList.add(userMessage);

    ChatMessage thinkingMessage =
        new ChatMessage.Builder()
            .setRole(MessageRole.ASSISTANT)
            .setContent("Analyzing intelligence streams...")
            .setThinking(true)
            .setTimestamp(timeFormat.format(new Date()))
            .build();
    messageList.add(thinkingMessage);

    chatAdapter.submitList(new ArrayList<>(messageList));
    binding.recyclerChat.smoothScrollToPosition(messageList.size() - 1);

    Set<String> enabledSourceIds =
        preferencesRepository != null ? preferencesRepository.getEnabledSourceIds() : null;

    Log.d(
        TAG,
        "Submitting prompt via background executor: "
            + prompt
            + ", allowedSources="
            + enabledSourceIds);

    chatRepository.sendMessage(
        prompt,
        currentSessionId,
        "hybrid",
        enabledSourceIds,
        new ResultCallback<AgentChatResult>() {
          @Override
          public void onSuccess(@NonNull AgentChatResult result) {
            Log.d(
                TAG,
                "AgentChatResult received on UI thread: " + result.getMessage().getContent());

            currentSessionId = result.getSessionId();
            setSendingState(false);

            if (!messageList.isEmpty() && messageList.get(messageList.size() - 1).isThinking()) {
              messageList.remove(messageList.size() - 1);
            }

            messageList.add(result.getMessage());

            if (binding != null) {
              chatAdapter.submitList(new ArrayList<>(messageList));
              binding.recyclerChat.smoothScrollToPosition(messageList.size() - 1);
            }
          }

          @Override
          public void onError(@NonNull Exception exception) {
            Log.e(TAG, "Agent error received: " + exception.getMessage(), exception);
            setSendingState(false);

            if (!messageList.isEmpty() && messageList.get(messageList.size() - 1).isThinking()) {
              messageList.remove(messageList.size() - 1);
            }

            ChatMessage errorMessage =
                new ChatMessage.Builder()
                    .setRole(MessageRole.ASSISTANT)
                    .setContent(
                        "Could not reach intelligence agent: "
                            + exception.getMessage()
                            + ". Please check backend connection.")
                    .setTimestamp(timeFormat.format(new Date()))
                    .setError(true)
                    .build();

            messageList.add(errorMessage);

            if (binding != null) {
              chatAdapter.submitList(new ArrayList<>(messageList));
              binding.recyclerChat.smoothScrollToPosition(messageList.size() - 1);
            }
          }
        });
  }

  private void setSendingState(boolean sending) {
    this.isSending = sending;
    if (binding != null) {
      binding.btnSend.setVisibility(sending ? View.GONE : View.VISIBLE);
      binding.progressSending.setVisibility(sending ? View.VISIBLE : View.GONE);
    }
  }

  @Override
  public void onNewsItemClick(@NonNull NewsArticle article) {
    String url = article.getOriginalUrl();
    Log.d(TAG, "Citation clicked, opening URL via Intent: " + url);
    if (url != null && !url.trim().isEmpty()) {
      try {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
        HistoryRepository.recordOpened(requireContext(), article);
      } catch (Exception e) {
        Log.e(TAG, "Failed to launch web browser intent: " + url, e);
      }
    }
  }

  @Override
  public void onStart() {
    super.onStart();
    Log.d(TAG, "onStart: ChatFragment becoming visible");
  }

  @Override
  public void onResume() {
    super.onResume();
    Log.d(TAG, "onResume: ChatFragment active");
    if (preferencesRepository != null) {
      applyUserFontScale(preferencesRepository.getFontScale());
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    Log.d(TAG, "onPause: ChatFragment paused");
  }

  @Override
  public void onStop() {
    super.onStop();
    Log.d(TAG, "onStop: ChatFragment no longer visible");
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    Log.d(TAG, "onDestroyView: Clearing binding");
    if (preferencesRepository != null) {
      preferencesRepository.removeOnPreferencesChangedListener(this);
    }
    binding = null;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    Log.d(TAG, "onDestroy: ChatFragment destroyed");
  }

  @Override
  public void onDetach() {
    super.onDetach();
    Log.d(TAG, "onDetach: ChatFragment detached");
  }
}
