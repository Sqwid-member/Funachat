package com.funa.chat;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Чиста логіка акаунта: хешування пароля, нормалізація і перевірка ідентифікаторів. */
public final class AuthCore {
    public static final int ITERATIONS = 120000;
    public static final int KEY_BITS = 256;
    private static final int SALT_BYTES = 16;
    private static final char[] ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final SecureRandom RND = new SecureRandom();

    private AuthCore() {}

    public static byte[] hash(String password, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }

    public static String newSalt() {
        byte[] b = new byte[SALT_BYTES];
        RND.nextBytes(b);
        return b64(b);
    }

    public static String b64(byte[] b) {
        return Base64.getEncoder().encodeToString(b);
    }

    public static byte[] unb64(String s) {
        try { return Base64.getDecoder().decode(s); }
        catch (Exception e) { return new byte[0]; }
    }

    public static boolean same(byte[] a, byte[] b) {
        return a != null && b != null && MessageDigest.isEqual(a, b);
    }

    /** Код відновлення у форматі XXXX-XXXX-XXXX. */
    public static String newRecovery() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            if (i > 0 && i % 4 == 0) sb.append('-');
            sb.append(ALPHABET[RND.nextInt(ALPHABET.length)]);
        }
        return sb.toString();
    }

    public static String normEmail(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase(Locale.ROOT);
    }

    /** Тільки цифри; провідний + відкидаємо для порівняння. */
    public static String normPhone(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') sb.append(c);
        }
        return sb.toString();
    }

    public static boolean validEmail(String s) {
        String v = normEmail(s);
        int at = v.indexOf('@');
        if (at <= 0 || at != v.lastIndexOf('@')) return false;
        String host = v.substring(at + 1);
        int dot = host.lastIndexOf('.');
        return dot > 0 && dot < host.length() - 1 && host.length() >= 4
                && v.length() <= 160 && !v.startsWith(".") && !v.contains("..");
    }

    public static boolean validPhone(String s) {
        String v = normPhone(s);
        return v.length() >= 7 && v.length() <= 15;
    }

    /** Для входу: email або номер у будь-якому форматі. */
    public static String normIdent(String s) {
        if (s == null) return "";
        String v = s.trim();
        if (v.contains("@")) return normEmail(v);
        String d = normPhone(v);
        return d.isEmpty() ? v.toLowerCase(Locale.ROOT) : d;
    }

    public static boolean looksEmail(String s) {
        return s != null && s.contains("@");
    }

    public static String mask(String s) {
        if (s == null || s.isEmpty()) return "";
        int at = s.indexOf('@');
        if (at > 1) return s.charAt(0) + "•••" + s.substring(at);
        if (s.length() > 4) return "•••" + s.substring(s.length() - 3);
        return "•••";
    }

    public static byte[] utf8(String s) {
        return s == null ? new byte[0] : s.getBytes(StandardCharsets.UTF_8);
    }
}
