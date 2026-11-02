package com.funa.chat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Locale;

/**
 * Простий файловий лог застосунку.
 *
 * Тримає останні рядки в памʼяті (щоб показати звіт навіть без доступу до диска)
 * і пише їх у files/logs/funa.log з ротацією. Після збою повний лог копіюється
 * в crash.txt — його пропонує надіслати екран «Допомога й логи».
 */
public final class Log {
    private static final int MAX_LINES = 600;
    private static final long MAX_BYTES = 256 * 1024;
    private static final int CRASH_COPY_LINES = 600;

    /**
     * Секреты не должны попадать даже в локальный лог: его копируют в отчёт
     * и пересылают. Шаблоны намеренно узкие — id сессий и телефоны не трогаем.
     */
    private static final java.util.regex.Pattern[] SECRETS = {
            java.util.regex.Pattern.compile("(?i)(bearer\\s+)[A-Za-z0-9._~+/=-]{12,}"),
            java.util.regex.Pattern.compile("(?i)((?:authorization|set-cookie|cookie|"
                    + "usertoken|user_token|access_token|refresh_token|x-auth-token|"
                    + "api[_-]?key|apikey|_k)[\"']?\\s*[:=]\\s*[\"']?)[^\\s\"',;]{8,}"),
            java.util.regex.Pattern.compile("eyJ[A-Za-z0-9_-]{6,}\\.[A-Za-z0-9_-]{6,}"
                    + "\\.[A-Za-z0-9_-]{4,}"),
            java.util.regex.Pattern.compile("\\bfk-[A-Za-z0-9]{6,}")
    };

    private static String scrub(String s) {
        if (s == null || s.isEmpty()) return "";
        for (int i = 0; i < SECRETS.length; i++) {
            java.util.regex.Matcher m = SECRETS[i].matcher(s);
            String repl = i == 0 || i == 1 ? "$1***" : "***";
            try { s = m.replaceAll(repl); } catch (Exception ignored) {}
        }
        return s;
    }

    private static final ArrayDeque<String> BUF = new ArrayDeque<>();
    private static final Object LOCK = new Object();
    private static final SimpleDateFormat TIME =
            new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US);

    private static File dir;
    private static File file;
    private static File crash;
    private static volatile boolean inited;
    private static volatile String appVersion = "";

    private Log() {}

    public static synchronized void init(File d) {
        if (d == null) return;
        dir = d;
        try { if (!dir.exists()) dir.mkdirs(); } catch (Exception ignored) {}
        file = new File(dir, "funa.log");
        crash = new File(dir, "crash.txt");
        inited = true;
    }

    public static File file() { return file; }

    /** Версія, якою позначено звіт про збій: старі збої не показуємо. */
    public static void setVersion(String v) {
        appVersion = v == null ? "" : v.trim();
    }

    public static boolean ready() { return inited; }

    public static void add(String tag, String msg) {
        write("I", tag, msg, null);
    }

    public static void e(String tag, String msg, Throwable t) {
        write("E", tag, msg, t);
    }

    public static void e(String tag, String msg) {
        write("E", tag, msg, null);
    }

    private static void write(String level, String tag, String msg, Throwable t) {
        String body = msg == null ? "" : msg;
        if (t != null) {
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            String st = sw.toString();
            if (st.length() > 4000) st = st.substring(0, 4000) + "\n…";
            body = body.isEmpty() ? st : body + "\n" + st;
        }
        // один рядок логу не має роздувати памʼять і файл: у текст помилки
        // може потрапити ціла HTML-сторінка від апстріму
        String flat = scrub(body.replace("\r", ""));
        if (flat.length() > 8000) flat = flat.substring(0, 8000) + " … (обрізано)";
        String line = TIME.format(new Date()) + " " + level + "/" + tag + ": " + flat;
        synchronized (LOCK) {
            BUF.addLast(line);
            while (BUF.size() > MAX_LINES) BUF.removeFirst();
            File f = file;
            if (f == null) return;
            try {
                if (f.length() > MAX_BYTES) {
                    File old = new File(dir, "funa.log.1");
                    if (old.exists()) old.delete();
                    if (!f.renameTo(old)) {
                        // не вдалося перейменувати — просто чистимо
                        new FileOutputStream(f, false).close();
                    }
                }
                try (FileOutputStream out = new FileOutputStream(f, true)) {
                    out.write((line + "\n").getBytes(StandardCharsets.UTF_8));
                }
            } catch (Exception ignored) {}
        }
    }

    /** Останні n рядків (з памʼяті, інакше з файлу). */
    public static String tail(int n) {
        if (n <= 0) return "";
        synchronized (LOCK) {
            if (!BUF.isEmpty()) {
                int skip = Math.max(0, BUF.size() - n);
                StringBuilder sb = new StringBuilder();
                int i = 0;
                for (String s : BUF) {
                    if (i++ < skip) continue;
                    if (sb.length() > 0) sb.append('\n');
                    sb.append(s);
                }
                return sb.toString();
            }
        }
        File f = file;
        if (f == null || !f.exists()) return "";
        String all = readAll(f, 512 * 1024);
        String[] rows = all.split("\n", -1);
        int from = Math.max(0, rows.length - n);
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < rows.length; i++) {
            if (sb.length() > 0) sb.append('\n');
            sb.append(rows[i]);
        }
        return sb.toString();
    }

    public static int size() {
        synchronized (LOCK) { return BUF.size(); }
    }

    public static void clear() {
        synchronized (LOCK) {
            BUF.clear();
            if (file != null) {
                try { new FileOutputStream(file, false).close(); } catch (Exception ignored) {}
            }
            if (crash != null) {
                try { crash.delete(); } catch (Exception ignored) {}
            }
        }
    }

    public static boolean hasCrash() {
        File c = crash;
        if (c == null || !c.exists() || c.length() == 0) return false;
        return currentCrash(c);
    }

    /** true, якщо збій стався в цій самій версії застосунку. */
    private static boolean currentCrash(File c) {
        String v = appVersion;
        if (v.isEmpty()) return true;
        String head = firstLine(c);
        // Формат: "crash <version> <time>" (старі файли без версії — застарілі).
        return head.startsWith("crash " + v + " ");
    }

    private static String firstLine(File f) {
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
            String l = r.readLine();
            return l == null ? "" : l;
        } catch (Exception e) {
            return "";
        }
    }

    public static String crashText() {
        File c = crash;
        if (c == null || !c.exists() || !currentCrash(c)) return "";
        return readAll(c, 256 * 1024);
    }

    /** Зберігає поточний лог як crash.txt (для наступного запуску). */
    public static synchronized void markCrash(Throwable t) {
        StringWriter sw = new StringWriter();
        if (t != null) t.printStackTrace(new PrintWriter(sw));
        String v = appVersion;
        String head = "crash " + (v.isEmpty() ? "" : v + " ")
                + TIME.format(new Date()) + "\n" + sw + "\n\n";
        String body = scrub(head + tail(CRASH_COPY_LINES));
        File c = crash;
        if (c == null) return;
        try (FileOutputStream out = new FileOutputStream(c, false)) {
            out.write(body.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {}
    }

    public static void installCrashHandler() {
        final Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, e) -> {
            try {
                e("crash", "thread " + thread.getName(), e);
                markCrash(e);
            } catch (Throwable ignored) {}
            if (prev != null) prev.uncaughtException(thread, e);
        });
    }

    private static String readAll(File f, int max) {
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[8192];
            int n;
            while ((n = r.read(buf)) > 0) {
                sb.append(buf, 0, n);
                if (sb.length() >= max) break;
            }
            if (sb.length() > max) return sb.substring(sb.length() - max);
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
