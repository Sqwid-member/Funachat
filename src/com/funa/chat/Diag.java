package com.funa.chat;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.os.StatFs;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Діагностика застосунку для екрана «Допомога й логи».
 *
 * Кожен пункт: id, state (ok/warn/bad/info), title, hint, action.
 * action — що зробити за кнопкою: settings, grant, termux, api, battery, report.
 */
public final class Diag {
    public static final String TERMUX = "com.termux";
    public static final String TERMUX_API = "com.termux.api";
    public static final String RETERMINAL = "com.rk.terminal";
    private static final long MIN_FREE = 200L * 1024 * 1024;

    private Diag() {}

    static String tr(Store st, String uk, String en, String ru) {
        String l = st == null ? "uk" : st.lang();
        return "en".equals(l) ? en : ("ru".equals(l) ? ru : uk);
    }

    public static JSONArray collect(Context c, Store st) {
        JSONArray out = new JSONArray();
        try {
            if (Log.hasCrash()) {
                add(out, "crash", "bad",
                        tr(st, "Зафіксовано збій", "A crash was recorded", "Зафиксирован сбой"),
                        tr(st, "Надішли звіт — це допоможе швидко виправити помилку.",
                                "Send the report — it helps fix the bug quickly.",
                                "Отправь отчёт — это поможет быстро исправить ошибку."),
                        "report",
                        tr(st, "Надіслати", "Send", "Отправить"));
            }

            boolean storage = storageAllowed(c);
            boolean writable = Tools.storageOk();
            if (!storage) {
                add(out, "storage", "bad",
                        tr(st, "Немає доступу до файлів", "No storage access", "Нет доступа к файлам"),
                        tr(st, "Дозволь «Керування всіма файлами» — інакше файли, PDF і Termux-міст не працюють.",
                                "Allow “All files access” — otherwise files, PDF and the Termux bridge fail.",
                                "Разреши «Управление всеми файлами» — иначе файлы, PDF и Termux-мост не работают."),
                        "grant",
                        tr(st, "Дозволити", "Allow", "Разрешить"));
            } else if (!writable) {
                add(out, "storage", "warn",
                        tr(st, "Папка FunaFiles недоступна", "FunaFiles folder is not writable", "Папка FunaFiles недоступна"),
                        Tools.ROOT + tr(st, " — не вдалося створити або записати.",
                                " — could not create or write.", " — не удалось создать или записать."),
                        "grant",
                        tr(st, "Перевірити", "Check", "Проверить"));
            } else {
                add(out, "storage", "ok",
                        tr(st, "Доступ до файлів є", "Storage access is granted", "Доступ к файлам есть"),
                        Tools.ROOT, "", "");
            }

            boolean termux = installed(c, TERMUX);
            boolean termuxApi = installed(c, TERMUX_API);
            boolean reterm = installed(c, RETERMINAL);
            if (!termux && !reterm) {
                add(out, "termux", "warn",
                        tr(st, "Термінал не встановлено", "No terminal installed", "Терминал не установлен"),
                        tr(st, "Команди sh, файли поза Download і міст недоступні. Постав Termux або ReTerminal з F-Droid — версія Termux із Play застаріла.",
                                "sh commands, files outside Download and the bridge are unavailable. Install Termux or ReTerminal from F-Droid — the Play Termux build is outdated.",
                                "Команды sh, файлы вне Download и мост недоступны. Установи Termux или ReTerminal из F-Droid — версия Termux из Play устарела."),
                        "termux",
                        tr(st, "F-Droid", "F-Droid", "F-Droid"));
            } else if (termux) {
                String hint = termuxApi
                        ? tr(st, "Termux і Termux:API знайдено.", "Termux and Termux:API found.",
                             "Termux и Termux:API найдены.")
                        : tr(st, "Termux є. Termux:API не потрібен для більшості інструментів, але корисний для буфера обміну й тостів.",
                             "Termux found. Termux:API is optional, useful for clipboard and toasts.",
                             "Termux есть. Termux:API не обязателен, но полезен для буфера обмена и тостов.");
                if (reterm) hint += tr(st, " ReTerminal теж є — він теж підходить.",
                        " ReTerminal is installed too and works as well.",
                        " ReTerminal тоже есть — он тоже подходит.");
                add(out, "termux", "ok",
                        tr(st, "Termux встановлено", "Termux is installed", "Termux установлен"),
                        hint,
                        "termux_open",
                        tr(st, "Відкрити", "Open", "Открыть"));
            } else {
                add(out, "termux", "ok",
                        tr(st, "ReTerminal встановлено", "ReTerminal is installed", "ReTerminal установлен"),
                        tr(st, "Запусти міст: sh /sdcard/Download/FunaFiles/funa-bridge.sh — і залиш сесію відкритою. ReTerminal не має termux-setup-storage, доступ до /sdcard дає сама програма.",
                                "Run the bridge: sh /sdcard/Download/FunaFiles/funa-bridge.sh and keep the session open. ReTerminal has no termux-setup-storage; the app itself grants /sdcard access.",
                                "Запусти мост: sh /sdcard/Download/FunaFiles/funa-bridge.sh — и оставь сессию открытой. В ReTerminal нет termux-setup-storage; доступ к /sdcard даёт само приложение."),
                        "reterm",
                        tr(st, "Відкрити", "Open", "Открыть"));
            }

            if (termux || reterm) {
                String termName = termux ? "Termux" : "ReTerminal";
                File q = new File(Tools.QUEUE);
                boolean qok = q.isDirectory() && q.canWrite();
                // Версія скрипта: стара v10 після [[DONE]] могла отримувати
                // дописи від фонових дітей і «вішати» канал. Підказуємо
                // перезберегти, але не перезаписуємо файл без згоди.
                File script = new File(Tools.ROOT, "funa-bridge.sh");
                boolean missingScript = !script.isFile();
                boolean oldScript = false;
                if (!missingScript) {
                    // Версія у шапці скрипта, а не в shebang: читаємо кілька
                    // перших рядків, інакше v11 завжди виглядав застарілим.
                    String head = "";
                    try (java.io.BufferedReader r = new java.io.BufferedReader(
                            new java.io.InputStreamReader(new java.io.FileInputStream(script),
                                    java.nio.charset.StandardCharsets.UTF_8))) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < 12; i++) {
                            String ln = r.readLine();
                            if (ln == null) break;
                            sb.append(ln).append('\n');
                        }
                        head = sb.toString();
                    } catch (Exception ignored) {}
                    oldScript = head.indexOf("v11") < 0;
                }
                String hint = tr(st, "Якщо команда не відповідає — запусти funa-bridge у " + termName + " і залиш його відкритим.",
                        "If a command hangs — run funa-bridge in " + termName + " and keep it open.",
                        "Если команда не отвечает — запусти funa-bridge в " + termName + " и оставь его открытым.");
                if (missingScript) hint += " " + tr(st,
                        "Скрипт ще не збережено — натисни «Зберегти скрипт».",
                        "The script has not been saved yet — tap “Save script”.",
                        "Скрипт ещё не сохранён — нажми «Сохранить скрипт».");
                else if (oldScript) hint += " " + tr(st,
                        "Скрипт у Download застарілий — натисни «Зберегти скрипт» і перезапусти міст.",
                        "The script in Download is outdated — tap “Save script” and restart the bridge.",
                        "Скрипт в Download устарел — нажми «Сохранить скрипт» и перезапусти мост.");
                add(out, "bridge", qok ? "info" : "warn",
                        qok ? tr(st, "Черга " + termName + " доступна", termName + " queue is writable", "Очередь " + termName + " доступна")
                            : tr(st, "Черга " + termName + " недоступна", termName + " queue is not writable", "Очередь " + termName + " недоступна"),
                        hint,
                        "bridge_save",
                        tr(st, "Зберегти скрипт", "Save script", "Сохранить скрипт"));
            }

            Auth au = new Auth(c, st);
            if (!au.has()) {
                add(out, "account", "bad",
                        tr(st, "Немає акаунта DeepSeek", "No DeepSeek account", "Нет аккаунта DeepSeek"),
                        tr(st, "Налаштування → Акаунт → увійди в DeepSeek або додай акаунт. Без нього чат не працює.",
                                "Settings → Account → sign in to DeepSeek or add an account. Chat needs it.",
                                "Настройки → Аккаунт → войди в DeepSeek или добавь аккаунт. Без него чат не работает."),
                        "account",
                        tr(st, "Увійти", "Sign in", "Войти"));
            } else {
                boolean hasToken = st.token() != null && !st.token().trim().isEmpty();
                JSONObject act = st.activeSummary();
                // Контакти в діагностику не потрапляють: звіт іде в месенджер,
                // тому показуємо лише імʼя, яке користувач задав сам.
                String who = act == null ? "" : act.optString("name", "").trim();
                if (who.isEmpty()) who = tr(st, "активний акаунт", "active account", "активный аккаунт");
                boolean expired = !hasToken || (act != null && "expired".equals(act.optString("status", "")));
                boolean challenge = act != null && "challenge".equals(act.optString("status", ""));
                StringBuilder sub = new StringBuilder();
                sub.append(tr(st, "Активний: ", "Active: ", "Активный: ")).append(who);
                if (expired) sub.append(tr(st, " · сесія прострочена — увійди знову",
                        " · session expired — sign in again",
                        " · сессия истекла — войди снова"));
                else if (challenge) sub.append(tr(st, " · DeepSeek просить пройти перевірку",
                        " · DeepSeek asks to complete a check",
                        " · DeepSeek просит пройти проверку"));
                else if (au.lock()) sub.append(tr(st, " · блокування паролем увімкнено",
                        " · password lock is on", " · блокировка паролем включена"));
                add(out, "account", expired || challenge ? "warn" : "ok",
                        expired
                                ? tr(st, "Акаунт DeepSeek прострочено", "DeepSeek account expired",
                                        "Аккаунт DeepSeek просрочен")
                                : challenge
                                ? tr(st, "DeepSeek просить перевірку", "DeepSeek asks for a check",
                                        "DeepSeek просит проверку")
                                : tr(st, "Акаунт DeepSeek підключено", "DeepSeek account is connected",
                                        "Аккаунт DeepSeek подключён"),
                        sub.toString(),
                        expired ? "account" : (challenge ? "verify" : ""),
                        expired ? tr(st, "Увійти", "Sign in", "Войти")
                                : (challenge ? tr(st, "Пройти", "Complete", "Пройти") : ""));
            }

            if (!online(c)) {
                add(out, "net", "bad",
                        tr(st, "Немає інтернету", "No internet connection", "Нет интернета"),
                        tr(st, "Перевір Wi-Fi або мобільні дані.", "Check Wi-Fi or mobile data.",
                                "Проверь Wi-Fi или мобильные данные."),
                        "", "");
            } else {
                add(out, "net", "ok",
                        tr(st, "Інтернет є", "Internet is available", "Интернет есть"),
                        "", "", "");
            }

            if (ApiServer.running()) {
                add(out, "api", "ok",
                        tr(st, "Локальний API працює", "Local API is running", "Локальный API работает"),
                        (st.apiLan() ? "0.0.0.0:" : "127.0.0.1:") + ApiServer.runningPort(),
                        "", "");
            } else if (st.apiAuto()) {
                add(out, "api", "warn",
                        tr(st, "Локальний API не запущено", "Local API is not running", "Локальный API не запущен"),
                        tr(st, "Можливо, порт зайнятий. Зміни порт у налаштуваннях або запусти вручну.",
                                "The port may be busy. Change it in settings or start the server manually.",
                                "Возможно, порт занят. Смени порт в настройках или запусти вручную."),
                        "api",
                        tr(st, "Запустити", "Start", "Запустить"));
            } else {
                add(out, "api", "info",
                        tr(st, "Локальний API вимкнено", "Local API is off", "Локальный API выключен"),
                        tr(st, "Увімкни в Налаштуваннях → Локальний API, якщо потрібен доступ іншим агентам.",
                                "Enable it in Settings → Local API if other agents need access.",
                                "Включи в Настройках → Локальный API, если нужен доступ другим агентам."),
                        "settings",
                        tr(st, "Увімкнути", "Enable", "Включить"));
            }

            long cool = st.guardCooldown() - System.currentTimeMillis();
            if (cool > 0) {
                long mins = Math.max(1, cool / 60000);
                String note = Guard.noteText(st.guardNote(), st.lang());
                add(out, "guard", "warn",
                        tr(st, "Пауза захисту акаунта", "Account guard pause", "Пауза защиты аккаунта"),
                        tr(st, "Залишилось ~" + mins + " хв. ", "About " + mins + " min left. ",
                                "Осталось ~" + mins + " мин. ")
                                + (note == null ? "" : note),
                        "", "");
            } else if (st.guardOn()) {
                add(out, "guard", "ok",
                        tr(st, "Захист акаунта увімкнено", "Account guard is on", "Защита аккаунта включена"),
                        tr(st, st.guardPerMin() + "/хв, " + st.guardPerDay() + "/добу",
                                st.guardPerMin() + "/min, " + st.guardPerDay() + "/day",
                                st.guardPerMin() + "/мин, " + st.guardPerDay() + "/сутки"),
                        "", "");
            }

            if (!ignoringBattery(c)) {
                add(out, "battery", "warn",
                        tr(st, "Оптимізація батареї увімкнена", "Battery optimization is on", "Оптимизация батареи включена"),
                        tr(st, "Android може зупиняти фонові субагенти й локальний API. Дозволь роботу у фоні.",
                                "Android may stop background subagents and the local API. Allow background work.",
                                "Android может останавливать фоновые субагенты и локальный API. Разреши работу в фоне."),
                        "battery",
                        tr(st, "Налаштувати", "Configure", "Настроить"));
            }

            long free = freeBytes();
            if (free > 0 && free < MIN_FREE) {
                add(out, "space", "warn",
                        tr(st, "Мало вільного місця", "Low free space", "Мало свободного места"),
                        (free / (1024 * 1024)) + tr(st, " МБ вільно. Звільни місце для файлів і логів.",
                                " MB free. Free up space for files and logs.",
                                " МБ свободно. Освободи место для файлов и логов."),
                        "", "");
            }

            add(out, "device", "info",
                    tr(st, "Пристрій", "Device", "Устройство"),
                    Build.MANUFACTURER + " " + Build.MODEL + " · Android " + Build.VERSION.RELEASE
                            + " (SDK " + Build.VERSION.SDK_INT + ") · " + MainActivity.VERSION,
                    "", "");
        } catch (Exception e) {
            Log.e("diag", "collect failed", e);
        }
        return out;
    }

    private static void add(JSONArray a, String id, String state, String title, String hint,
                            String action, String act) {
        try {
            JSONObject o = new JSONObject();
            o.put("id", id);
            o.put("state", state);
            o.put("title", title);
            o.put("hint", hint == null ? "" : hint);
            o.put("action", action == null ? "" : action);
            o.put("act", act == null ? "" : act);
            a.put(o);
        } catch (Exception ignored) {}
    }

    static boolean installed(Context c, String pkg) {
        try {
            c.getPackageManager().getPackageInfo(pkg, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    static boolean storageAllowed(Context c) {
        try {
            if (Build.VERSION.SDK_INT >= 30) return Environment.isExternalStorageManager();
            return c.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        } catch (Exception e) {
            return false;
        }
    }

    static boolean online(Context c) {
        try {
            ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return true;
            NetworkInfo n = cm.getActiveNetworkInfo();
            return n != null && n.isConnected();
        } catch (Exception e) {
            return true;
        }
    }

    static boolean ignoringBattery(Context c) {
        try {
            PowerManager pm = (PowerManager) c.getSystemService(Context.POWER_SERVICE);
            return pm != null && pm.isIgnoringBatteryOptimizations(c.getPackageName());
        } catch (Exception e) {
            return true;
        }
    }

    static long freeBytes() {
        try {
            StatFs s = new StatFs(Tools.ROOT);
            return s.getAvailableBytes();
        } catch (Exception e) {
            return 0;
        }
    }

    /** Текст звіту: версія, пристрій, діагностика, лог. */
    public static String report(Context c, Store st) {
        StringBuilder sb = new StringBuilder();
        sb.append("FunaChat ").append(MainActivity.VERSION)
                .append(" (").append(MainActivity.CODE).append(")\n");
        sb.append("Time: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US)
                .format(new Date())).append('\n');
        sb.append("Device: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append('\n');
        sb.append("Android: ").append(Build.VERSION.RELEASE)
                .append(" (SDK ").append(Build.VERSION.SDK_INT).append(")\n");
        sb.append("ABI: ").append(Build.SUPPORTED_ABIS.length > 0 ? Build.SUPPORTED_ABIS[0] : "?").append('\n');
        sb.append("Locale: ").append(st.clientLocale()).append(" · TZ ").append(st.clientTz()).append('\n');
        sb.append("Lang: ").append(st.lang()).append('\n');

        sb.append('\n').append("Diagnostics:\n");
        JSONArray items = collect(c, st);
        for (int i = 0; i < items.length(); i++) {
            JSONObject o = items.optJSONObject(i);
            if (o == null) continue;
            sb.append("  [").append(o.optString("state")).append("] ")
                    .append(o.optString("title"));
            String hint = o.optString("hint");
            if (!hint.isEmpty()) sb.append(" — ").append(hint.replace("\n", " "));
            sb.append('\n');
        }

        if (Log.hasCrash()) {
            String crash = Log.crashText();
            if (crash.length() > 6000) crash = crash.substring(0, 6000) + "\n…";
            sb.append('\n').append("Last crash:\n").append(crash).append('\n');
        }

        String log = Log.tail(300);
        sb.append('\n').append("Log (").append(log.isEmpty() ? "empty" : "last 300").append("):\n");
        sb.append(log.isEmpty() ? "—" : log).append('\n');

        String text = sb.toString();
        if (text.length() > 48000) {
            text = text.substring(0, 6000) + "\n…\n" + text.substring(text.length() - 40000);
        }
        return text;
    }

    public static String subject(Context c, Store st) {
        return "FunaChat report " + MainActivity.VERSION + " · " + Build.MODEL;
    }
}
