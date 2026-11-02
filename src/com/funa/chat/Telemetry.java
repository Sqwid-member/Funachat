package com.funa.chat;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

/**
 * Знеособлений діагностичний знімок для добровільної телеметрії.
 *
 * Сюди потрапляє лише те, що потрібно для пошуку помилок: версія, модель
 * пристрою, стан дозволів, лічильники подій. Немає токенів, ключів, пошти,
 * тексту чатів, шляхів до файлів та IP. Файл кладеться в Download/FunaFiles,
 * а в Telegram його відправляє окремий скрипт у Termux — токен бота не
 * потрапляє в APK.
 */
public final class Telemetry {
    public static final String DIR = Tools.ROOT + "/telemetry";

    /**
     * HTTPS-приймач (Cloudflare Worker). Адреса не показується в UI і не
     * лежить у DEX відкритим рядком: байти закодовані XOR-ом і збираються
     * під час роботи. Це не криптографія, але прибирає адресу з `strings`.
     */
    private static final int[] ENDPOINT = {
            50, 46, 46, 42, 41, 96, 117, 117, 60, 47, 52, 59,
            56, 53, 46, 116, 56, 47, 56, 59, 54, 53, 44, 54,
            59, 62, 104, 109, 104, 116, 45, 53, 40, 49, 63, 40,
            41, 116, 62, 63, 44, 117, 44, 107, 117, 46, 63, 54,
            63, 55, 63, 46, 40, 35
    };

    public static String defaultUrl() {
        byte[] out = new byte[ENDPOINT.length];
        for (int i = 0; i < ENDPOINT.length; i++) out[i] = (byte) (ENDPOINT[i] ^ 0x5A);
        return new String(out, StandardCharsets.UTF_8);
    }

    private static final int KEEP = 40;
    private static final String[] USAGE = {
            "q", "done", "stop", "ms", "tin", "tout", "edit", "regen", "tool", "err", "last"
    };

    private Telemetry() {}

    public static JSONObject snapshot(Context c, Store st, JSONObject usage) {
        JSONObject o = new JSONObject();
        try {
            o.put("schema", 1);
            o.put("id", st.telemetryId());
            o.put("ts", System.currentTimeMillis());

            JSONObject app = new JSONObject();
            app.put("version", MainActivity.VERSION);
            app.put("code", MainActivity.CODE);
            o.put("app", app);

            JSONObject d = new JSONObject();
            d.put("manufacturer", safe(Build.MANUFACTURER));
            d.put("model", safe(Build.MODEL));
            d.put("sdk", Build.VERSION.SDK_INT);
            d.put("release", safe(Build.VERSION.RELEASE));
            d.put("abi", Build.SUPPORTED_ABIS.length > 0 ? safe(Build.SUPPORTED_ABIS[0]) : "");
            d.put("locale", Locale.getDefault().toLanguageTag());
            d.put("tz", java.util.TimeZone.getDefault().getID());
            Configuration cfg = c.getResources().getConfiguration();
            d.put("widthDp", cfg.screenWidthDp);
            d.put("heightDp", cfg.screenHeightDp);
            d.put("densityDpi", cfg.densityDpi);
            o.put("device", d);

            JSONObject s = new JSONObject();
            s.put("locked", st.apiLocked());
            JSONObject active = st.activeSummary();
            s.put("signedIn", active != null);
            s.put("accounts", st.accountSummary().length());
            s.put("storage", Diag.storageAllowed(c));
            s.put("termux", Diag.installed(c, Diag.TERMUX)
                    || Diag.installed(c, Diag.RETERMINAL));
            s.put("crash", Log.hasCrash());
            s.put("logLines", Log.size());
            o.put("state", s);

            JSONObject api = new JSONObject();
            api.put("running", ApiServer.running());
            api.put("public", st.apiPublic());
            api.put("keys", st.apiKeyList().length());
            o.put("api", api);

            o.put("auditEvents", st.auditLog(100).length());
            o.put("usage", cleanUsage(usage));

            // Останні рядки логу та текст збою: без них звіт майже марний.
            // Log сам вирізає токени, ключі й cookie перед записом.
            String logs = Log.history(150);
            if (logs.length() > 6000) logs = "…\n" + logs.substring(logs.length() - 6000);
            o.put("logs", logs);
            if (Log.hasCrash()) {
                String crash = Log.crashText();
                if (crash.length() > 3000) crash = crash.substring(0, 3000) + "\n…";
                o.put("crashText", crash);
            }
            // Тіло має влізати в ліміт Worker (16 КБ) навіть із кирилицею,
            // де один символ — два байти. Спочатку підрізаємо логи.
            for (int i = 0; i < 8 && bytes(o) > 15000 && logs.length() > 300; i++) {
                logs = "…\n" + logs.substring(logs.length() / 2);
                o.put("logs", logs);
            }
            if (bytes(o) > 15000 && o.has("crashText")) {
                String crash = o.optString("crashText", "");
                if (crash.length() > 800) o.put("crashText", crash.substring(0, 800) + "\n…");
            }
            // Не показуємо «logs 3» з памʼяті: рахуємо реальні рядки звіту.
            int rows = 0;
            if (!logs.isEmpty()) {
                for (String row : logs.split("\n")) if (!row.isEmpty()) rows++;
            }
            s.put("logLines", rows);
        } catch (Exception ignored) {}
        return o;
    }

    private static int bytes(JSONObject o) {
        try {
            return o.toString().getBytes(StandardCharsets.UTF_8).length;
        } catch (Exception e) {
            return 0;
        }
    }

    private static String safe(String v) {
        if (v == null) return "";
        return v.length() > 80 ? v.substring(0, 80) : v;
    }

    /** Тільки відомі числові лічильники; усе інше відкидаємо. */
    private static JSONObject cleanUsage(JSONObject usage) {
        JSONObject out = new JSONObject();
        if (usage == null) return out;
        for (String k : USAGE) {
            long v = usage.optLong(k, 0);
            if (v < 0) v = 0;
            if (v > 1000000000L) v = 1000000000L;
            try { out.put(k, v); } catch (Exception ignored) {}
        }
        return out;
    }

    private static File[] spooled() {
        File dir = new File(DIR);
        File[] fs = dir.listFiles((d, n) -> n.endsWith(".json"));
        return fs == null ? new File[0] : fs;
    }

    public static int pending() { return spooled().length; }

    /** Записати знімок у чергу. Повертає ім'я файла. */
    public static String write(Context c, Store st, JSONObject usage, String source) throws Exception {
        JSONObject o = snapshot(c, st, usage);
        o.put("source", source == null ? "" : source);
        return writeSnapshot(st, o);
    }

    public static String writeSnapshot(Store st, JSONObject o) throws Exception {
        File dir = new File(DIR);
        if (!dir.exists() && !dir.mkdirs()) throw new Exception("telemetry dir");
        prune(dir);
        String name = "t-" + System.currentTimeMillis() + "-"
                + st.telemetryId().substring(0, 6) + ".json";
        File f = new File(dir, name);
        FileOutputStream out = new FileOutputStream(f);
        try {
            out.write(o.toString(2).getBytes(StandardCharsets.UTF_8));
        } finally {
            out.close();
        }
        return name;
    }

    /**
     * Пряма відправка на HTTPS-приймач (Cloudflare Worker). Токен бота
     * лишається на приймачі, тому його немає ні в APK, ні в застосунку.
     */
    public static boolean post(String url, JSONObject o) {
        if (url == null || url.isEmpty()) return false;
        java.net.HttpURLConnection c = null;
        try {
            java.net.URL u = new java.net.URL(url);
            String proto = u.getProtocol();
            if (!"https".equals(proto) && !"http".equals(proto)) return false;
            byte[] data = o.toString().getBytes(StandardCharsets.UTF_8);
            if (data.length > 20000) return false;
            c = (java.net.HttpURLConnection) u.openConnection();
            c.setConnectTimeout(10000);
            c.setReadTimeout(15000);
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            c.setRequestProperty("User-Agent", "FunaChat/" + MainActivity.VERSION);
            c.setRequestProperty("X-Funa-Telemetry", "1");
            c.getOutputStream().write(data);
            int code = c.getResponseCode();
            return code >= 200 && code < 300;
        } catch (Exception e) {
            return false;
        } finally {
            if (c != null) try { c.disconnect(); } catch (Exception ignored) {}
        }
    }

    /** Не роздумуємо сховище, якщо Termux-релей довго не запускали. */
    private static void prune(File dir) {
        File[] fs = dir.listFiles((d, n) -> n.endsWith(".json"));
        if (fs == null || fs.length < KEEP) return;
        Arrays.sort(fs, Comparator.comparingLong(File::lastModified));
        for (int i = 0; i <= fs.length - KEEP; i++) {
            try { fs[i].delete(); } catch (Exception ignored) {}
        }
    }

    public static JSONObject state(Store st) {
        JSONObject o = new JSONObject();
        try {
            o.put("on", st.telemetryOn());
            o.put("last", st.telemetryLast());
            o.put("pending", pending());
            o.put("url", st.telemetryUrl());
        } catch (Exception ignored) {}
        return o;
    }
}
