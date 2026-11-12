package com.example.newsagnet.data.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.SharedPreferences;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link SecureApiKeyStore} verifying AES-GCM encryption, zero plaintext storage in
 * SharedPreferences, masked summary generation, key removal, and Server Default switching.
 */
public class SecureApiKeyStoreTest {

  private FakeSharedPreferences backingPrefs;
  private SecureApiKeyStore keyStore;

  @Before
  public void setUp() {
    backingPrefs = new FakeSharedPreferences();
    keyStore =
        new SecureApiKeyStore(backingPrefs, new SecureApiKeyStore.JvmAesGcmCipherEngine());
  }

  @Test
  public void saveApiKey_encryptsWithAesGcm_neverStoresPlaintextInSharedPreferences() {
    String secretKey = "AIzaSySuperSecretPersonalGeminiKey123456";

    keyStore.saveApiKey(AiProviderOption.GEMINI, secretKey);

    assertTrue(keyStore.hasConfiguredApiKey());
    assertEquals(AiProviderOption.GEMINI, keyStore.getSelectedProvider());
    assertEquals(AiProviderOption.GEMINI, keyStore.getConfiguredKeyProvider());

    // Verify raw plaintext key is NEVER stored in SharedPreferences
    for (Map.Entry<String, ?> entry : backingPrefs.getAll().entrySet()) {
      String valStr = String.valueOf(entry.getValue());
      assertFalse(
          "Plaintext API key must never appear in SharedPreferences entry: " + entry.getKey(),
          valStr.contains(secretKey));
    }

    String ciphertext = backingPrefs.getString(SecureApiKeyStore.KEY_CIPHERTEXT_B64, null);
    String iv = backingPrefs.getString(SecureApiKeyStore.KEY_IV_B64, null);
    assertNotNull(ciphertext);
    assertNotNull(iv);
    assertNotEquals(secretKey, ciphertext);

    // Verify decryption round-trip for outbound request
    assertEquals(secretKey, keyStore.getDecryptedApiKey());
    assertEquals(secretKey, keyStore.getActiveApiKeyForRequest());

    // Verify masked summary only shows last 4 chars, never full key
    String masked = keyStore.getMaskedKeySummary();
    assertEquals("••••3456", masked);
    assertFalse(masked.contains("AIzaSySuperSecret"));
  }

  @Test
  public void removeApiKey_deletesCiphertextAndResetsToServerDefault() {
    keyStore.saveApiKey(AiProviderOption.OPENAI, "sk-proj-PersonalOpenAIKey987654321");
    assertTrue(keyStore.hasConfiguredApiKey());

    keyStore.removeApiKey();

    assertFalse(keyStore.hasConfiguredApiKey());
    assertNull(keyStore.getDecryptedApiKey());
    assertNull(keyStore.getActiveApiKeyForRequest());
    assertEquals(AiProviderOption.SERVER_DEFAULT, keyStore.getSelectedProvider());
    assertEquals("", keyStore.getMaskedKeySummary());
  }

  @Test
  public void useServerDefault_disablesPersonalKeyForOutboundRequests() {
    keyStore.saveApiKey(AiProviderOption.GEMINI, "AIzaSyPersonalGeminiKey99998888");
    assertNotNull(keyStore.getActiveApiKeyForRequest());

    keyStore.useServerDefault();

    assertEquals(AiProviderOption.SERVER_DEFAULT, keyStore.getSelectedProvider());
    assertNull(
        "Outbound request must not use personal key when Server Default is active",
        keyStore.getActiveApiKeyForRequest());
  }

  /** In-memory SharedPreferences test double. */
  private static final class FakeSharedPreferences implements SharedPreferences {
    private final Map<String, Object> map = new HashMap<>();

    @Override
    public Map<String, ?> getAll() {
      return new HashMap<>(map);
    }

    @Override
    public String getString(String key, String defValue) {
      Object val = map.get(key);
      return val instanceof String ? (String) val : defValue;
    }

    @Override
    public Set<String> getStringSet(String key, Set<String> defValues) {
      return defValues;
    }

    @Override
    public int getInt(String key, int defValue) {
      return defValue;
    }

    @Override
    public long getLong(String key, long defValue) {
      return defValue;
    }

    @Override
    public float getFloat(String key, float defValue) {
      return defValue;
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
      return defValue;
    }

    @Override
    public boolean contains(String key) {
      return map.containsKey(key);
    }

    @Override
    public Editor edit() {
      return new Editor() {
        private final Map<String, Object> staged = new HashMap<>();
        private final Set<String> removals = new java.util.HashSet<>();

        @Override
        public Editor putString(String key, String value) {
          staged.put(key, value);
          return this;
        }

        @Override
        public Editor putStringSet(String key, Set<String> values) {
          return this;
        }

        @Override
        public Editor putInt(String key, int value) {
          return this;
        }

        @Override
        public Editor putLong(String key, long value) {
          return this;
        }

        @Override
        public Editor putFloat(String key, float value) {
          return this;
        }

        @Override
        public Editor putBoolean(String key, boolean value) {
          return this;
        }

        @Override
        public Editor remove(String key) {
          removals.add(key);
          staged.remove(key);
          return this;
        }

        @Override
        public Editor clear() {
          map.clear();
          return this;
        }

        @Override
        public boolean commit() {
          apply();
          return true;
        }

        @Override
        public void apply() {
          for (String k : removals) {
            map.remove(k);
          }
          map.putAll(staged);
        }
      };
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(
        OnSharedPreferenceChangeListener listener) {}

    @Override
    public void unregisterOnSharedPreferenceChangeListener(
        OnSharedPreferenceChangeListener listener) {}
  }
}
