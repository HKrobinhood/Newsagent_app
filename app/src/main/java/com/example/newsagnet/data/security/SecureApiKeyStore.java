package com.example.newsagnet.data.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Objects;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Hardware-backed Android Keystore + AES-GCM encrypted storage for personal LLM API keys.
 *
 * <p>Security Guarantees:
 *
 * <ul>
 *   <li>Plaintext API keys are NEVER written to SharedPreferences, files, or logs.
 *   <li>Encryption uses 256-bit AES-GCM with 128-bit authentication tag (`AES/GCM/NoPadding`).
 *   <li>Storage file (`global_news_ai_secure_keystore.xml`) is excluded from Android Cloud Backup
 *       and device-to-device transfer, and is never included in cross-device account sync.
 * </ul>
 */
public class SecureApiKeyStore {

  public static final String PREF_FILE_NAME = "global_news_ai_secure_keystore";
  public static final String KEY_SELECTED_PROVIDER = "selected_ai_provider";
  public static final String KEY_CONFIGURED_KEY_PROVIDER = "configured_key_provider";
  public static final String KEY_CIPHERTEXT_B64 = "encrypted_api_key_ciphertext_b64";
  public static final String KEY_IV_B64 = "encrypted_api_key_iv_b64";
  public static final String KEY_MASKED_SUFFIX = "masked_key_suffix";

  private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
  private static final String KEYSTORE_ALIAS = "global_news_ai_llm_key_aes_v1";
  private static final String AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding";
  private static final int GCM_TAG_LENGTH_BITS = 128;

  private static volatile SecureApiKeyStore instance;

  /** Pluggable AES-GCM encryption engine interface. */
  public interface CipherEngine {
    @NonNull
    EncryptedPayload encrypt(@NonNull byte[] plaintext) throws Exception;

    @NonNull
    byte[] decrypt(@NonNull byte[] iv, @NonNull byte[] ciphertext) throws Exception;
  }

  /** Immutable holder for AES-GCM IV and ciphertext bytes. */
  public static final class EncryptedPayload {
    private final byte[] iv;
    private final byte[] ciphertext;

    public EncryptedPayload(@NonNull byte[] iv, @NonNull byte[] ciphertext) {
      this.iv = iv.clone();
      this.ciphertext = ciphertext.clone();
    }

    @NonNull
    public byte[] getIv() {
      return iv.clone();
    }

    @NonNull
    public byte[] getCiphertext() {
      return ciphertext.clone();
    }
  }

  private final SharedPreferences securePrefs;
  private final CipherEngine cipherEngine;

  public SecureApiKeyStore(@NonNull Context context) {
    this(
        context.getApplicationContext().getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE),
        new AndroidKeystoreCipherEngine());
  }

  public SecureApiKeyStore(
      @NonNull SharedPreferences securePrefs, @NonNull CipherEngine cipherEngine) {
    this.securePrefs = Objects.requireNonNull(securePrefs);
    this.cipherEngine = Objects.requireNonNull(cipherEngine);
  }

  @NonNull
  public static SecureApiKeyStore getInstance(@NonNull Context context) {
    if (instance == null) {
      synchronized (SecureApiKeyStore.class) {
        if (instance == null) {
          instance = new SecureApiKeyStore(context.getApplicationContext());
        }
      }
    }
    return instance;
  }

  public static SecureApiKeyStore replaceForTesting(@Nullable SecureApiKeyStore replacement) {
    synchronized (SecureApiKeyStore.class) {
      SecureApiKeyStore previous = instance;
      instance = replacement;
      return previous;
    }
  }

  /**
   * Returns the currently active LLM provider selection.
   *
   * @return Active {@link AiProviderOption}.
   */
  @NonNull
  public AiProviderOption getSelectedProvider() {
    String raw =
        securePrefs.getString(KEY_SELECTED_PROVIDER, AiProviderOption.SERVER_DEFAULT.getId());
    return AiProviderOption.fromId(raw);
  }

  /**
   * Updates the active LLM provider selection without altering stored ciphertext.
   *
   * @param provider Selected {@link AiProviderOption}.
   */
  public void setSelectedProvider(@NonNull AiProviderOption provider) {
    securePrefs.edit().putString(KEY_SELECTED_PROVIDER, provider.getId()).apply();
  }

  /**
   * Returns the provider associated with the currently saved personal API key, if any.
   *
   * @return Configured key's {@link AiProviderOption}, or {@link AiProviderOption#SERVER_DEFAULT}.
   */
  @NonNull
  public AiProviderOption getConfiguredKeyProvider() {
    String raw =
        securePrefs.getString(
            KEY_CONFIGURED_KEY_PROVIDER, AiProviderOption.SERVER_DEFAULT.getId());
    return AiProviderOption.fromId(raw);
  }

  /**
   * Encrypts and persists a personal LLM API key using AES-GCM.
   *
   * @param provider Target LLM provider (`GEMINI` or `OPENAI`).
   * @param rawApiKey User-supplied API key (never logged or stored in plaintext).
   * @throws IllegalArgumentException if key is blank or provider is `SERVER_DEFAULT`.
   */
  public void saveApiKey(@NonNull AiProviderOption provider, @NonNull String rawApiKey) {
    if (provider == AiProviderOption.SERVER_DEFAULT) {
      throw new IllegalArgumentException("Please select Google Gemini or OpenAI to save a key.");
    }
    String trimmed = rawApiKey.trim();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException("API Key cannot be empty.");
    }

    try {
      byte[] plaintextBytes = trimmed.getBytes(StandardCharsets.UTF_8);
      EncryptedPayload encrypted = cipherEngine.encrypt(plaintextBytes);
      String ciphertextHex = toHex(encrypted.getCiphertext());
      String ivHex = toHex(encrypted.getIv());
      String masked = buildMaskedSummary(trimmed);

      securePrefs
          .edit()
          .putString(KEY_SELECTED_PROVIDER, provider.getId())
          .putString(KEY_CONFIGURED_KEY_PROVIDER, provider.getId())
          .putString(KEY_CIPHERTEXT_B64, ciphertextHex)
          .putString(KEY_IV_B64, ivHex)
          .putString(KEY_MASKED_SUFFIX, masked)
          .apply();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to encrypt API key in secure keystore.");
    }
  }

  /**
   * Returns true if an encrypted personal API key is currently stored on this device.
   *
   * @return True if ciphertext and IV are present.
   */
  public boolean hasConfiguredApiKey() {
    String ciphertext = securePrefs.getString(KEY_CIPHERTEXT_B64, null);
    String iv = securePrefs.getString(KEY_IV_B64, null);
    return ciphertext != null && !ciphertext.isEmpty() && iv != null && !iv.isEmpty();
  }

  /**
   * Decrypts and returns the stored personal API key for in-flight request headers only.
   *
   * @return Decrypted API key string, or null if not configured or if decryption fails.
   */
  @Nullable
  public String getDecryptedApiKey() {
    String ciphertextHex = securePrefs.getString(KEY_CIPHERTEXT_B64, null);
    String ivHex = securePrefs.getString(KEY_IV_B64, null);
    if (ciphertextHex == null || ivHex == null || ciphertextHex.isEmpty() || ivHex.isEmpty()) {
      return null;
    }
    try {
      byte[] ciphertext = fromHex(ciphertextHex);
      byte[] iv = fromHex(ivHex);
      byte[] decrypted = cipherEngine.decrypt(iv, ciphertext);
      return new String(decrypted, StandardCharsets.UTF_8);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Returns the active API key to attach to an outbound AI request, or null if `SERVER_DEFAULT` is
   * selected.
   *
   * @return Ephemeral API key for outbound request, or null when using Server Default.
   */
  @Nullable
  public String getActiveApiKeyForRequest() {
    AiProviderOption selected = getSelectedProvider();
    if (selected == AiProviderOption.SERVER_DEFAULT) {
      return null;
    }
    return getDecryptedApiKey();
  }

  /**
   * Returns a non-sensitive masked representation of the saved key (never full plaintext).
   *
   * @return Masked indicator (e.g., `"••••6789"`), or empty string if no key is saved.
   */
  @NonNull
  public String getMaskedKeySummary() {
    if (!hasConfiguredApiKey()) {
      return "";
    }
    String masked = securePrefs.getString(KEY_MASKED_SUFFIX, null);
    return masked != null ? masked : "••••••••";
  }

  /** Permanently deletes the stored personal API key and resets mode to Server Default. */
  public void removeApiKey() {
    securePrefs
        .edit()
        .remove(KEY_CIPHERTEXT_B64)
        .remove(KEY_IV_B64)
        .remove(KEY_MASKED_SUFFIX)
        .remove(KEY_CONFIGURED_KEY_PROVIDER)
        .putString(KEY_SELECTED_PROVIDER, AiProviderOption.SERVER_DEFAULT.getId())
        .apply();
  }

  /** Switches active provider to Server Default without exposing any key material. */
  public void useServerDefault() {
    securePrefs
        .edit()
        .putString(KEY_SELECTED_PROVIDER, AiProviderOption.SERVER_DEFAULT.getId())
        .apply();
  }

  @NonNull
  static String buildMaskedSummary(@NonNull String rawKey) {
    String trimmed = rawKey.trim();
    if (trimmed.length() <= 8) {
      return "••••••••";
    }
    String tail = trimmed.substring(trimmed.length() - 4);
    return "••••" + tail;
  }

  @NonNull
  private static String toHex(@NonNull byte[] bytes) {
    StringBuilder sb = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      sb.append(Character.forDigit((b >> 4) & 0xF, 16));
      sb.append(Character.forDigit(b & 0xF, 16));
    }
    return sb.toString();
  }

  @NonNull
  private static byte[] fromHex(@NonNull String hex) {
    int len = hex.length();
    byte[] out = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      int hi = Character.digit(hex.charAt(i), 16);
      int lo = Character.digit(hex.charAt(i + 1), 16);
      out[i / 2] = (byte) ((hi << 4) + lo);
    }
    return out;
  }

  /** Production Android Keystore AES-GCM 256-bit encryption engine. */
  public static final class AndroidKeystoreCipherEngine implements CipherEngine {

    private SecretKey getOrCreateSecretKey() throws Exception {
      KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
      keyStore.load(null);
      KeyStore.Entry existing = keyStore.getEntry(KEYSTORE_ALIAS, null);
      if (existing instanceof KeyStore.SecretKeyEntry) {
        return ((KeyStore.SecretKeyEntry) existing).getSecretKey();
      }

      KeyGenerator keyGenerator =
          KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
      KeyGenParameterSpec spec =
          new KeyGenParameterSpec.Builder(
                  KEYSTORE_ALIAS,
                  KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
              .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
              .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
              .setKeySize(256)
              .build();
      keyGenerator.init(spec);
      return keyGenerator.generateKey();
    }

    @NonNull
    @Override
    public EncryptedPayload encrypt(@NonNull byte[] plaintext) throws Exception {
      Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
      cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey());
      byte[] iv = cipher.getIV();
      byte[] ciphertext = cipher.doFinal(plaintext);
      return new EncryptedPayload(iv, ciphertext);
    }

    @NonNull
    @Override
    public byte[] decrypt(@NonNull byte[] iv, @NonNull byte[] ciphertext) throws Exception {
      Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
      GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
      cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), spec);
      return cipher.doFinal(ciphertext);
    }
  }

  /** Standard JCE AES-GCM 256-bit engine for JVM unit tests outside Android runtime. */
  public static final class JvmAesGcmCipherEngine implements CipherEngine {
    private final SecretKey secretKey;

    public JvmAesGcmCipherEngine() {
      try {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        this.secretKey = keyGen.generateKey();
      } catch (Exception e) {
        throw new IllegalStateException("Failed to initialize JVM AES-GCM key", e);
      }
    }

    @NonNull
    @Override
    public EncryptedPayload encrypt(@NonNull byte[] plaintext) throws Exception {
      Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
      cipher.init(Cipher.ENCRYPT_MODE, secretKey);
      return new EncryptedPayload(cipher.getIV(), cipher.doFinal(plaintext));
    }

    @NonNull
    @Override
    public byte[] decrypt(@NonNull byte[] iv, @NonNull byte[] ciphertext) throws Exception {
      Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
      GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
      cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
      return cipher.doFinal(ciphertext);
    }
  }
}
