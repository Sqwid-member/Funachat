package com.funa.chat;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Сховище токенів: AES-256-GCM ключем із Android Keystore.
 *
 * Ключ генерується в системному сховищі (апаратний бекенд, якщо є) і ніколи
 * не потрапляє в застосунок чи резервні копії. У налаштуваннях лежить лише
 * шифротекст у вигляді "v1:<iv>:<cipher>".
 */
final class Vault {
    private static final String STORE = "AndroidKeyStore";
    private static final String ALIAS = "funa.token.vault";
    private static final String PREFIX = "v1:";

    private Vault() {}

    private static SecretKey key(boolean create) {
        try {
            KeyStore ks = KeyStore.getInstance(STORE);
            ks.load(null);
            if (ks.containsAlias(ALIAS)) {
                java.security.Key k = ks.getKey(ALIAS, null);
                if (k instanceof SecretKey) return (SecretKey) k;
            }
            if (!create) return null;
            KeyGenerator kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, STORE);
            kg.init(new KeyGenParameterSpec.Builder(ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build());
            return kg.generateKey();
        } catch (Exception e) {
            Log.e("vault", "keystore unavailable", e);
            return null;
        }
    }

    /** Шифрує токен. null — якщо Keystore недоступний (тоді токен не зберігаємо). */
    static String seal(String plain) {
        if (plain == null || plain.isEmpty()) return null;
        try {
            SecretKey k = key(true);
            if (k == null) return null;
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, k);
            byte[] iv = c.getIV();
            byte[] out = c.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return PREFIX + Base64.encodeToString(iv, Base64.NO_WRAP) + ":"
                    + Base64.encodeToString(out, Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e("vault", "seal failed", e);
            return null;
        }
    }

    /** Розшифровує. Порожній рядок — якщо ключ втрачено або дані зіпсовані. */
    static String open(String stored) {
        if (stored == null || stored.isEmpty()) return "";
        // Дані старої версії (без префікса) — звичайний текст, їх перешифрує Store.
        if (!stored.startsWith(PREFIX)) return stored;
        try {
            int a = stored.indexOf(':', PREFIX.length());
            if (a < 0) return "";
            byte[] iv = Base64.decode(stored.substring(PREFIX.length(), a), Base64.NO_WRAP);
            byte[] ct = Base64.decode(stored.substring(a + 1), Base64.NO_WRAP);
            SecretKey k = key(false);
            if (k == null) return "";
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, k, new GCMParameterSpec(128, iv));
            return new String(c.doFinal(ct), StandardCharsets.UTF_8);
        } catch (Exception e) {
            Log.add("vault", "open failed: " + e.getClass().getSimpleName());
            return "";
        }
    }
}
