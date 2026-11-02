package com.funa.chat;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Вбудовані навички ШІ: короткі інструкції, які модель читає ПЕРЕД тим,
 * як створювати DOCX/PDF, HTML-артефакт або документацію.
 *
 * Файли лежать в assets/skills/<name>.<lang>.md (uk/en/ru). Читання ліниве,
 * результат кешується; якщо мови немає — віддаємо en, потім uk. Без Android
 * контексту (JVM-тести) {@link #get} повертає null, а не падає.
 */
public final class Skills {
    private Skills() {}

    public static final String[] NAMES = {"docx", "pdf", "design", "docs"};
    private static final String DIR = "skills";
    private static final int MAX_BYTES = 200000;

    private static Context app;
    private static final Map<String, String> CACHE = new HashMap<>();

    /** Викликати один раз у MainActivity.onCreate до старту агента/API. */
    public static synchronized void init(Context c) {
        if (c != null) app = c.getApplicationContext();
    }

    /** Аліаси, які моделі інколи вигадують, → канонічні назви. */
    public static String norm(String name) {
        if (name == null) return "";
        String n = name.trim().toLowerCase(Locale.US);
        switch (n) {
            case "word":
            case "doc":
            case "docx": return "docx";
            case "pdf": return "pdf";
            case "html":
            case "ui":
            case "artifact":
            case "design": return "design";
            case "documentation":
            case "report":
            case "docs": return "docs";
            default: return n;
        }
    }

    public static boolean exists(String name) {
        String n = norm(name);
        for (String s : NAMES) if (s.equals(n)) return true;
        return false;
    }

    public static String names() {
        StringBuilder sb = new StringBuilder();
        for (String s : NAMES) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(s);
        }
        return sb.toString();
    }

    /**
     * Текст навички або null, якщо назва невідома/файл недоступний.
     * lang: uk/en/ru; невідома мова трактується як uk.
     */
    public static synchronized String get(String name, String lang) {
        String n = norm(name);
        if (!exists(n)) return null;
        // До init() кеш не заповнюємо: інакше перший виклик без контексту
        // назавжди «закешував» би порожню навичку.
        if (app == null) return null;
        String l = "en".equals(lang) ? "en" : ("ru".equals(lang) ? "ru" : "uk");
        String key = n + "." + l;
        if (CACHE.containsKey(key)) return emptyToNull(CACHE.get(key));

        String text = read(n + "." + l + ".md");
        if (text == null && !"en".equals(l)) text = read(n + ".en.md");
        if (text == null && !"uk".equals(l)) text = read(n + ".uk.md");
        // Порожній файл теж кешуємо: інакше кожен виклик знову відкриває assets.
        CACHE.put(key, text == null ? "" : text);
        return text;
    }

    private static String emptyToNull(String s) {
        return s == null || s.isEmpty() ? null : s;
    }

    private static String read(String file) {
        Context c = app;
        if (c == null) return null;
        InputStream in = null;
        try {
            AssetManager am = c.getAssets();
            in = am.open(DIR + "/" + file);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
                if (out.size() > MAX_BYTES) break;
            }
            String s = new String(out.toByteArray(), StandardCharsets.UTF_8);
            if (!s.isEmpty() && s.charAt(0) == '\uFEFF') s = s.substring(1);
            return s.trim().isEmpty() ? null : s;
        } catch (Exception e) {
            Log.add("skills", "read " + file + ": " + e.getClass().getSimpleName());
            return null;
        } finally {
            if (in != null) try { in.close(); } catch (Exception ignored) {}
        }
    }
}
