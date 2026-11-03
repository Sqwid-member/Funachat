package com.funa.chat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {
    static final String VERSION = "2.42.37";
    static final int CODE = 112;

    private static final int REQ_PICK = 11;
    private static final int REQ_ATTACH = 12;
    private static final int REQ_SITE = 13;
    private static final long MAX_ATTACH = 50L * 1024 * 1024;
    /** Стеля для session.json: більше — це вже не токен, а сміття. */
    private static final long MAX_SESSION = 4L * 1024 * 1024;
    private WebView web;
    private Bridge bridge;
    private Store store;
    private Auth auth;
    private Memory mem;
    private Folders folderStore;
    private volatile Agent agent;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private volatile String sessionId;
    /** Покоління відкриття чату: пізня історія не перебиває новіший вибір. */
    private volatile int openSeq;
    /** Чат, історія якого вантажиться: видалення скасовує це відкриття. */
    private volatile String openingId;
    /**
     * Перезапуск через смерть рендерера: локальний API-сервер не має
     * падати разом із WebView, інакше зовнішній клієнт втрачає з'єднання
     * на рівному місці (і не підніметься, якщо сервер запускали вручну).
     */
    private volatile boolean keepApiServer;

    /**
     * Скасовує незавершене відкриття чату. Мало підняти openSeq: без
     * скидання openingId видалення цього чату пізніше виглядало б як
     * «видалили поточний» і закривало б зовсім іншу сесію.
     */
    private synchronized void cancelOpen() {
        ++openSeq;
        openingId = null;
    }
    private volatile long parentId;
    private volatile boolean first = true;
    private volatile String summary = "";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Log.init(new File(getFilesDir(), "logs"));
        Log.setVersion(VERSION);
        Log.installCrashHandler();
        Log.add("app", "start " + VERSION + " (" + CODE + ") sdk " + Build.VERSION.SDK_INT
                + " " + Build.MANUFACTURER + " " + Build.MODEL);
        store = new Store(this);
        Skills.init(this);
        auth = new Auth(this, store);
        // «Запитувати пароль при запуску» = кожен холодний старт без сесії
        if (auth.anyLock()) auth.logoutAll();
        mem = new Memory(this, store);
        folderStore = new Folders(this, store);
        SubTask.setSink(this::emit);
        agent = new Agent(store, this::emit);
        agent.setStateListener((sid, parent, fst) -> { sessionId = sid; parentId = parent; first = fst; });

        keepApiServer = false;
        cleanUploads();
        if (store.apiAuto()) ApiServer.start(store);

        web = new WebView(this);
        web.setBackgroundColor(0xFF0F1216);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        // Сторінка застосунку — file:///android_asset (assets доступні й при
        // вимкненому file access). Довільні file:// і content:// цьому вікну
        // не потрібні: менше шляхів витягнути локальний файл з HTML артефакту.
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setAllowFileAccessFromFileURLs(false);
        s.setAllowUniversalAccessFromFileURLs(false);
        if (android.os.Build.VERSION.SDK_INT >= 21)
            s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setTextZoom(100);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setLoadWithOverviewMode(false);
        s.setUseWideViewPort(false);
        web.setHorizontalScrollBarEnabled(false);
        web.setVerticalScrollBarEnabled(false);
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView v, String url, android.graphics.Bitmap favicon) {
                // Перезавантаження сторінки (наприклад, після падіння
                // рендерера) дає новий BRIDGE_KEY в app.js: старий ключ
                // треба скинути, інакше місток назавжди відхиляв би команди.
                // Скидаємо лише для головної сторінки, не для iframe артефактів.
                if (url != null && url.startsWith("file:///android_asset/index.html")) {
                    bridge.resetKey();
                }
            }

            @Override
            public boolean onRenderProcessGone(WebView v,
                    android.webkit.RenderProcessGoneDetail detail) {
                // Рендерер помер (найчастіше — завелика відповідь або
                // артефакт з'їв пам'ять). WebView після цього непридатний:
                // без обробника застосунок просто показує чорний екран.
                Log.e("web", "renderer gone, crash=" + (detail != null && detail.didCrash()));
                keepApiServer = true;
                if (web == v) web = null;
                try {
                    android.view.ViewGroup parent = (android.view.ViewGroup) v.getParent();
                    if (parent != null) parent.removeView(v);
                    v.destroy();
                } catch (Exception ignored) {}
                ui.post(() -> {
                    try { recreate(); } catch (Exception e) { finish(); }
                });
                return true;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView v, String url) {
                if (url == null) return true;
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    // openUrl сам перевіряє схему й довжину — одна поведінка
                    // для посилань із чату та з HTML-артефактів
                    openUrl(url);
                    return true;
                }
                // Дозволяємо лише власні сторінки; javascript:, file:, intent:
                // та інші схеми у головному WebView не потрібні.
                return !url.startsWith("file:///android_asset/");
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsAlert(WebView v, String url, String message, final JsResult r) {
                new AlertDialog.Builder(MainActivity.this).setMessage(message)
                        .setPositiveButton("OK", (d, w) -> r.confirm()).setOnCancelListener(d -> r.cancel()).show();
                return true;
            }

            @Override
            public boolean onJsConfirm(WebView v, String url, String message, final JsResult r) {
                new AlertDialog.Builder(MainActivity.this).setMessage(message)
                        .setPositiveButton("OK", (d, w) -> r.confirm())
                        .setNegativeButton(tr("Скасувати", "Cancel", "Отмена"), (d, w) -> r.cancel())
                        .setOnCancelListener(d -> r.cancel()).show();
                return true;
            }

            @Override
            public boolean onJsPrompt(WebView v, String url, String message, String def, final JsPromptResult r) {
                final EditText e = new EditText(MainActivity.this);
                e.setInputType(InputType.TYPE_CLASS_TEXT);
                e.setText(def == null ? "" : def);
                e.setSelection(e.getText().length());
                new AlertDialog.Builder(MainActivity.this).setTitle(message).setView(e)
                        .setPositiveButton("OK", (d, w) -> r.confirm(e.getText().toString()))
                        .setNegativeButton(tr("Скасувати", "Cancel", "Отмена"), (d, w) -> r.cancel())
                        .setOnCancelListener(d -> r.cancel()).show();
                return true;
            }
        });
        bridge = new Bridge();
        web.addJavascriptInterface(bridge, "FunaBridge");
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
        setColors();
    }

    private void setColors() {
        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.setStatusBarColor(0xFF0F1216);
        w.setNavigationBarColor(0xFF0F1216);
        w.getDecorView().setSystemUiVisibility(0);
    }

    /**
     * События с данными аккаунта. Под локальным замком они не едут в
     * WebView: ответ, начатый за миг до блокировки, иначе дорисовал бы
     * чаты, память или страницы под экраном пароля. После разблокировки
     * UI сам запрашивает нужное заново.
     */
    private static final Set<String> LOCKED_EVENTS = new HashSet<>(Arrays.asList(
            "accounts", "acc_result", "acc_check", "whoami", "contacts",
            "sessions", "history", "session", "clean", "u_id",
            "user", "a_start", "a_id", "think", "answer", "a_end", "req_end",
            "sub_start", "sub_list", "sub_live", "sub_log", "sub_state", "sub_end", "sub_view",
            "sub_retry_ok",
            "tool", "folders", "file", "file_thumb", "file_info", "view", "pdf_pages",
            "view_error", "file_saved", "compacted", "compact_error", "saved_file",
            "chat_exported", "pages", "page", "title", "api", "deleted", "renamed",
            "saved", "mem", "mem_saved", "mem_deleted", "pinned", "mem_exported",
            "mem_imported", "mem_toggled", "diag", "report", "imported"));

    private void emit(JSONObject o) {
        final String json = o.toString();
        final String ev = o.optString("e", "");
        ui.post(() -> {
            // account_switched проходит: UI должен узнать о смене аккаунта
            // даже если новый тоже под замком.
            if (!auth.unlocked() && LOCKED_EVENTS.contains(ev)) return;
            // Поздняя дельта/карточка субагента старого аккаунта не должна
            // появиться в чате нового: stopAll() обрывает задачу, но её
            // последнее событие могло уже стоять в очереди UI-потока.
            if (ev.startsWith("sub_")) {
                SubTask t = SubTask.get(o.optString("id", ""));
                if (t != null && !t.account().equals(store.activeId())) return;
            }
            eval(json);
        });
    }

    private void eval(String json) {
        if (web == null) return;
        String js = "window.__funa(" + JSONObject.quote(json) + ")";
        web.evaluateJavascript(js, null);
    }

    private String tr(String uk, String en, String ru) {
        String l = store == null ? "uk" : store.lang();
        return "en".equals(l) ? en : ("ru".equals(l) ? ru : uk);
    }

    private void toast(final String m) {
        ui.post(() -> Toast.makeText(this, m, Toast.LENGTH_SHORT).show());
    }

    private void openUrl(String url) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            ui.post(() -> openUrl(url));
            return;
        }
        // лише справжні веб-посилання: intent:, file:, content: з тексту
        // відповіді не мають запускати нічого стороннього
        String u = url == null ? "" : url.trim();
        if (u.length() < 8 || u.length() > 8192) return;
        String low = u.toLowerCase(java.util.Locale.US);
        if (!low.startsWith("http://") && !low.startsWith("https://")) return;
        for (int i = 0; i < u.length(); i++) {
            char ch = u.charAt(i);
            if (ch < 0x20 || ch == 0x7f) return;
        }
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(u));
            i.addCategory(Intent.CATEGORY_BROWSABLE);
            startActivity(i);
        } catch (Exception e) {
            toast(tr("Немає браузера", "No browser", "Нет браузера"));
        }
    }

    private void openBattery() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            ui.post(() -> openBattery());
            return;
        }
        try {
            startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
        } catch (Exception e) {
            try {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            } catch (Exception ignored) {
                toast(tr("Налаштування недоступні", "Settings unavailable", "Настройки недоступны"));
            }
        }
    }

    public boolean storageAllowed() {
        if (Build.VERSION.SDK_INT >= 30) return Environment.isExternalStorageManager();
        return checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void askStorage() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            ui.post(() -> askStorage());
            return;
        }
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                Intent i = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                i.setData(Uri.parse("package:" + getPackageName()));
                startActivity(i);
            } else {
                requestPermissions(new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE}, 7);
            }
        } catch (Exception e) {
            try { startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)); }
            catch (Exception ignored) {
                toast(tr("Немає налаштувань дозволів", "No permission settings",
                        "Нет настроек разрешений"));
            }
        }
    }

    /** Номер натискання «назад» і підтвердження від JS — див. onBackPressed. */
    private final java.util.concurrent.atomic.AtomicLong backSeq =
            new java.util.concurrent.atomic.AtomicLong();
    private volatile long backAck;

    @Override
    public void onBackPressed() {
        // Рендерер мертвий (або сторінка ще не завантажилась): «back»
        // у нікуди не дійде, і застосунок неможливо закрити кнопкою.
        if (web == null) {
            finish();
            return;
        }
        final JSONObject o = new JSONObject();
        try { o.put("e", "back"); } catch (Exception ignored) {}
        final long id = backSeq.incrementAndGet();
        emit(o);
        // Якщо JS не підтвердив обробку — вважаємо, що міст не відповідає,
        // і закриваємо застосунок самі. Запас 2.5 с: важкий Markdown-
        // рендер може зайняти головний потік на секунду-дві, і закривати
        // застосунок через це не можна.
        ui.postDelayed(() -> {
            if (id == backSeq.get() && backAck < id && !isFinishing()) finish();
        }, 2500);
    }

    @Override
    protected void onResume() {
        super.onResume();
        JSONObject o = new JSONObject();
        try {
            o.put("e", "storage");
            o.put("ok", storageAllowed());
        } catch (Exception ignored) {}
        emit(o);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_ATTACH) {
            if (res == RESULT_OK && data != null) handleAttach(data);
            return;
        }
        if (req == REQ_SITE) {
            if (res == RESULT_OK && data != null) {
                String tk = SiteLogin.cleanTok(data.getStringExtra(SiteLogin.EXTRA_TOKEN));
                String ck = data.getStringExtra(SiteLogin.EXTRA_COOKIE);
                Log.add("auth", "site import token=" + tk.length() + " cookie="
                        + (ck == null ? 0 : ck.length()));
                bridge.importSession(tk, ck == null ? "" : ck);
            } else {
                Log.add("auth", "site login cancelled");
                auth.clearReset();
            }
            return;
        }
        if (req != REQ_PICK || res != RESULT_OK || data == null || data.getData() == null) return;
        final Uri uri = data.getData();
        new Thread(() -> {
            try (java.io.InputStream in = getContentResolver().openInputStream(uri)) {
                if (in == null) {
                    toast(tr("Не вдалося відкрити файл", "Can't open file", "Не удалось открыть файл"));
                    return;
                }
                java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) {
                    bo.write(buf, 0, n);
                    if (bo.size() > MAX_SESSION) throw new Exception("too big");
                }
                bridge.applySession(new String(bo.toByteArray(), StandardCharsets.UTF_8));
            } catch (Exception e) {
                if ("too big".equals(e.getMessage()))
                    toast(tr("Файл сесії завеликий (макс. 4 МБ)", "Session file is too large (max 4 MB)",
                            "Файл сессии слишком большой (макс. 4 МБ)"));
                else toast(tr("Помилка читання: ", "Read error: ", "Ошибка чтения: ") + e.getMessage());
            }
        }).start();
    }


    private void cleanUploads() { trimUploads(null); }

    /**
     * Тримає кеш вкладень у межах бюджету. Викликається і на старті, і
     * після кожного копіювання: інакше 20 файлів по 50 МБ (дозволено за
     * раз) переповнюють кеш ще до наступного запуску застосунку.
     * keep — щойно скопійований файл: його не видаляємо ніколи.
     */
    private void trimUploads(File keep) {
        try {
            File dir = new File(getCacheDir(), "uploads");
            File[] fs = dir.listFiles();
            if (fs == null) return;
            long cut = System.currentTimeMillis() - 24L * 3600 * 1000;
            long total = 0;
            java.util.ArrayList<File> live = new java.util.ArrayList<>();
            for (File f : fs) {
                if (!f.isFile()) continue;
                if (!f.equals(keep) && f.lastModified() < cut) { f.delete(); continue; }
                total += f.length();
                live.add(f);
            }
            long budget = 200L * 1024 * 1024;
            if (total <= budget) return;
            live.sort((a, b) -> Long.compare(a.lastModified(), b.lastModified()));
            long fresh = System.currentTimeMillis() - 10L * 60 * 1000;
            for (File f : live) {
                if (total <= budget) break;
                if (f.equals(keep)) continue;
                // Свіжий файл може ще летіти на сервер: видалення посеред
                // читання зламало б завантаження. Хай краще кеш на 200 МБ
                // перевищить бюджет, ніж вкладення «загубиться».
                if (f.lastModified() > fresh) continue;
                long n = f.length();
                if (f.delete()) total -= n;
            }
        } catch (Exception ignored) {}
    }

    private String displayName(Uri uri) {
        String name = null;
        try (Cursor c = getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (i >= 0) name = c.getString(i);
            }
        } catch (Exception ignored) {}
        if (name == null || name.trim().isEmpty()) {
            String p = uri.getLastPathSegment();
            name = p == null ? "file" : p.substring(p.lastIndexOf('/') + 1);
        }
        return name;
    }

    /**
     * Вільне імʼя у кеші. Файл створюється атомарно: два вкладення з
     * однаковою назвою копіюються паралельно й інакше перезаписали б одне
     * одного (друге пішло б на сервер замість першого).
     */
    private File uniqueFile(File dir, String name) {
        File f = new File(dir, name);
        try {
            if (f.createNewFile()) return f;
        } catch (Exception ignored) {}
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        String ext = dot > 0 ? name.substring(dot) : "";
        for (int i = 1; i < 1000; i++) {
            f = new File(dir, base + "-" + i + ext);
            try {
                if (f.createNewFile()) return f;
            } catch (Exception ignored) {}
        }
        f = new File(dir, System.currentTimeMillis() + "-" + name);
        try { f.createNewFile(); } catch (Exception ignored) {}
        return f;
    }

    /** Видаляє теку з усім вмістом (сторінки акаунта). */
    private static void deleteTree(File f) {
        if (f == null) return;
        try {
            if (f.isDirectory()) {
                File[] fs = f.listFiles();
                if (fs != null) for (File x : fs) deleteTree(x);
            }
            f.delete();
        } catch (Exception ignored) {}
    }

    private void handleAttach(Intent data) {
        ArrayList<Uri> uris = new ArrayList<>();
        ClipData cd = data.getClipData();
        if (cd != null) {
            for (int i = 0; i < cd.getItemCount(); i++) {
                Uri u = cd.getItemAt(i).getUri();
                if (u != null) uris.add(u);
            }
        } else if (data.getData() != null) {
            uris.add(data.getData());
        }
        // Кожен файл копіюється в кеш і їде на сервер: 20 за один вибір
        // достатньо, а сотня дрібних лише заб'є канал і памʼять.
        if (uris.size() > 20) {
            uris = new ArrayList<>(uris.subList(0, 20));
            toast(tr("Додано перші 20 файлів", "Added the first 20 files",
                    "Добавлены первые 20 файлов"));
        }
        for (final Uri u : uris) {
            new Thread(() -> {
                String name = displayName(u);
                String mime = getContentResolver().getType(u);
                if (mime == null || mime.isEmpty()) mime = Attach.mime(name);
                File dir = new File(getCacheDir(), "uploads");
                if (!dir.exists()) dir.mkdirs();
                File out = uniqueFile(dir, Attach.sanitize(name));
                long total = 0;
                try (InputStream in = getContentResolver().openInputStream(u);
                     OutputStream os = new FileOutputStream(out)) {
                    if (in == null) throw new Exception("can't open");
                    byte[] buf = new byte[16384];
                    int n;
                    while ((n = in.read(buf)) > 0) {
                        total += n;
                        if (total > MAX_ATTACH) throw new Exception("too big");
                        os.write(buf, 0, n);
                    }
                } catch (Exception e) {
                    out.delete();
                    if ("too big".equals(e.getMessage())) {
                        toast(tr("Файл завеликий (макс. 50 МБ)", "File is too large (max 50 MB)", "Файл слишком большой (макс. 50 МБ)"));
                    } else {
                        toast(tr("Не вдалося прочитати файл", "Can't read the file", "Не удалось прочитать файл"));
                    }
                    return;
                }
                if (total <= 0) {
                    out.delete();
                    toast(tr("Порожній файл", "Empty file", "Пустой файл"));
                    return;
                }
                trimUploads(out);
                bridge.uploadOne(out, name, mime);
            }, "funa-copy").start();
        }
    }

    private class Bridge {
        /**
         * Ключ сторінки. JavascriptInterface видно й з пісочниці артефакту
         * (sandbox без allow-same-origin не забороняє викликати інжектований
         * обʼєкт), тому команда без ключа головної сторінки не виконується:
         * інакше HTML від моделі міг би читати файли чи слати повідомлення.
         */
        private final Object KEY_LOCK = new Object();
        private String pageKey = "";
        /** Серіалізує створення теки сторінок і перенесення легасі-файлів. */
        private final Object PAGES_LOCK = new Object();

        /**
         * Команды, которые отдают данные аккаунта. Пока локальная блокировка
         * не снята, их нельзя даже запрашивать: иначе один случайный вызов из
         * UI (или старый обработчик) вытащил бы чужие чаты без пароля.
         * Настройки, тема, логи, auth_* и файлы устройства остаются доступны.
         */
        private final Set<String> NEEDS_UNLOCK = new HashSet<>(Arrays.asList(
                "whoami", "sessions", "open", "send", "edit", "regen", "cont", "branch",
                "save", "delete", "rename", "pin", "export_chat", "compact",
                "mem_list", "mem_save", "mem_del", "mem_toggle", "mem_export", "mem_import",
                "folders", "folder_add", "folder_rename", "folder_del", "folder_move",
                "accounts", "acc_add", "acc_check", "acc_use", "acc_del",
                "contacts_save", "api_state", "api_apply", "api_start", "api_stop",
                "api_key", "sub_view", "sub_list", "sub_retry",
                "pages_list", "page_read", "page_del", "save_html"));

        void resetKey() {
            synchronized (KEY_LOCK) { pageKey = ""; }
        }

        private boolean allowed(JSONObject c) {
            String k = c.optString("_k", "");
            if (k.isEmpty() || k.length() > 80) return false;
            synchronized (KEY_LOCK) {
                if (pageKey.isEmpty()) { pageKey = k; return true; }
                return pageKey.equals(k);
            }
        }

        @JavascriptInterface
        public void post(String raw) {
            JSONObject c;
            try { c = new JSONObject(raw); } catch (Exception e) { return; }
            if (!allowed(c)) {
                Log.add("bridge", "rejected call without the page key");
                return;
            }
            String cmd = c.optString("cmd", "");
            if (!NEEDS_UNLOCK.contains(cmd) || auth.unlocked()) {
                run(cmd, c);
            } else {
                Log.add("bridge", "blocked while locked: " + cmd);
            }
        }

        private void run(String cmd, JSONObject c) {
            switch (cmd) {
                case "back_ack": backAck = backSeq.get(); break;
                case "boot": boot(); break;
                case "import": importSession(); break;
                case "whoami": whoami(); break;
                case "sessions": sessions(); break;
                case "open": open(c.optString("id", "")); break;
                case "new": newChat(c.optBoolean("keep", false)); break;
                case "send": send(c); break;
                case "pick_file": ui.post(this::pickAttachment); break;
                case "edit": edit(c); break;
                case "regen": regen(c); break;
                case "cont": cont(c); break;
                case "branch": branch(c); break;
                case "export_chat": exportChat(c); break;
                case "pages_list": pagesList(); break;
                case "page_read": pageRead(c.optString("name", "")); break;
                case "page_del": pageDel(c.optString("name", "")); break;
                case "view_file": viewFile(c.optString("path", ""), c.optInt("seq", 0)); break;
                case "file_info": fileInfo(c.optString("path", "")); break;
                case "file_thumb": fileThumb(c.optString("path", "")); break;
                case "pdf_more": pdfMore(c.optString("path", ""), c.optInt("offset", 0), c.optInt("count", 4), c.optInt("seq", 0)); break;
                case "open_with": openWith(c.optString("path", "")); break;
                case "share_file": shareFile(c.optString("path", "")); break;
                case "save_file": saveFile(c.optString("path", "")); break;
                case "compact": compact(c.optString("transcript", ""), c.optBoolean("auto", false),
                        c.optString("sid", "")); break;
                case "stop": agent.stop(); break;
                case "sub_view": subView(c.optString("id", "")); break;
                case "sub_list": subList(); break;
                case "sub_stop": SubTask.stop(c.optString("id", "")); break;
                case "sub_retry": retrySub(c.optString("id", "")); break;
                case "sub_stop_all": SubTask.stopAll(); break;
                case "save": save(c); break;
                case "delete": delete(c.optString("id", "")); break;
                case "rename": rename(c.optString("id", ""), c.optString("title", "")); break;
                case "pin": pin(c.optString("id", ""), c.optBoolean("on", true)); break;
                case "perm": askStorage(); break;
                case "copy": copy(c.optString("text", "")); break;
                case "open_url": openUrl(c.optString("url", "")); break;
                case "title": {
                    final String tt = c.optString("text", "");
                    ui.post(() -> MainActivity.this.setTitle(tt));
                    break;
                }
                case "exit": ui.post(MainActivity.this::finish); break;
                case "toast": toast(c.optString("text", "")); break;
                case "mem_list": memList(); break;
                case "mem_save": memSave(c); break;
                case "mem_del": memDel(c.optLong("id", 0)); break;
                case "mem_toggle": memToggle(c.optLong("id", 0), c.optBoolean("on", true)); break;
                case "mem_export": memExport(); break;
                case "mem_import": memImport(); break;
                case "accounts": accountsList(); break;
                case "acc_add": accountAdd(c.optString("token", ""), c.optString("src", "token")); break;
                case "acc_check": accountCheck(c.optString("id", "")); break;
                case "acc_use": accountUse(c.optString("id", ""), c.optInt("index", -1)); break;
                case "acc_del": accountDelete(c.optString("id", ""), c.optInt("index", -1)); break;
                case "folders": foldersList(); break;
                case "folder_add": folderAdd(c.optString("name", "")); break;
                case "folder_rename": folderRename(c.optString("id", ""), c.optString("name", "")); break;
                case "folder_del": folderDelete(c.optString("id", "")); break;
                case "folder_move": folderMove(c.optString("sid", ""), c.optString("fid", "")); break;
                case "save_html": saveHtml(c.optString("name", ""), c.optString("content", "")); break;
                case "api_state": apiState(); break;
                case "api_apply": apiApply(c); break;
                case "pub_apply": store.setPubUrl(c.optString("url", ""));
                    store.setUpdUrl(c.optString("upd", "")); apiState(); break;
                case "pub_file": pubFromFile(c.optBoolean("quiet", false)); break;
                case "api_start": store.setApiAuto(true); apiStart(); break;
                case "api_stop": store.setApiAuto(false);
                    ApiServer.stop(); apiState(); break;
                case "api_key": store.newApiKey(); apiState(); keysState(); break;
                case "keys_list": keysState(); break;
                case "key_add": keyAdd(c.optString("name", ""), c.optLong("ttl", 0)); break;
                case "key_del": {
                    store.apiKeyDelete(c.optString("id", ""));
                    keysState();
                    apiState();
                    break;
                }
                case "key_copy": {
                    if (store.apiLocked()) break;
                    String kk = store.apiKeyById(c.optString("id", ""));
                    if (!kk.isEmpty()) copy(kk);
                    break;
                }
                case "api_public": store.setApiPublic(c.optBoolean("on", true)); break;
                case "audit_copy": if (!store.apiLocked()) copy(store.ensureAuditSecret()); break;
                case "audit_rotate":
                    if (!store.apiLocked()) { store.rotateAuditSecret(); apiState(); }
                    break;
                case "audit_apply":
                    if (!store.apiLocked()) {
                        store.setAuditContacts(c.optString("contact", ""), c.optString("hook", ""));
                        apiState();
                    }
                    break;
                case "audit_log": {
                    if (store.apiLocked()) break;
                    JSONObject ae = new JSONObject();
                    try {
                        ae.put("e", "audit_log");
                        ae.put("items", store.auditLog(20));
                    } catch (Exception ignored) {}
                    emit(ae);
                    break;
                }
                case "guard_state": guardState(); break;
                case "guard_apply": guardApply(c); break;
                case "guard_reset": Guard.reset(store); guardState(); break;
                case "fold_apply": store.setFoldAll(c.optBoolean("on", true)); break;
                case "diag": diag(); break;
                case "contacts_save": contactsSave(c); break;
                case "report_email": sendReport(true); break;
                case "report_share": sendReport(false); break;
                case "log_copy": copyReport(); break;
                case "log_clear": Log.clear(); Log.add("app", "log cleared"); diag(); break;
                case "diag_action": diagAction(c.optString("id", "")); break;
                case "auth_open": authEmit(new JSONObject(), "state"); break;
                case "log": jsLog(c); break;
                case "report_preview": previewReport(); break;
                case "telemetry_state": telemetryState(); break;
                case "telemetry_apply":
                    store.setTelemetry(c.optBoolean("on", true));
                    telemetryState();
                    break;
                case "telemetry_preview":
                    telemetryPreview(c.optJSONObject("stats"));
                    break;
                case "telemetry_send":
                    telemetrySend(c.optJSONObject("stats"), true);
                    break;
                case "telemetry_auto":
                    telemetryAuto(c.optJSONObject("stats"));
                    break;
                case "crash_seen": store.setCrashSeen(true); break;
                case "intro_seen": store.setIntroSeen(true); break;
                case "bridge_save": saveBridge(); break;
                case "auth_state": authEmit(new JSONObject(), "state"); break;
                case "auth_logout":
                    auth.logout();
                    authEmit(new JSONObject(), "logout");
                    break;
                case "auth_lock": authSetLock(c); break;
                case "auth_skip": auth.setSkipped(true); authEmit(new JSONObject(), "skip"); break;
                case "auth_unlock": authUnlock(c); break;
                case "auth_site": openSiteLogin(false); break;
                case "auth_verify": openSiteLogin(true); break;
                case "auth_forgot": auth.requestReset(); openSiteLogin(false); break;
                case "auth_key": importSession(c.optString("key", "").trim(),
                        c.optString("key", "").trim()); break;
                default: break;
            }
        }

        private void boot() {
            JSONObject o = new JSONObject();
            try {
                String lang = store.lang();
                // Під локальним замком у WebView не має їхати нічого, крім
                // того, що потрібно для екрана розблокування. Інакше email,
                // телефон, список акаунтів і ключ API лежали б у DOM під
                // оверлеєм, а після збою рендерера ще й відновилися б.
                final boolean lockedNow = !auth.unlocked();
                o.put("e", "init");
                o.put("signedIn", !store.token().isEmpty());
                o.put("activeId", store.activeId());
                o.put("base", store.base(lang));
                o.put("baseDefault", Store.defaultBase(lang));
                o.put("baseAuto", store.baseRaw().isEmpty());
                o.put("thinking", store.thinking());
                o.put("search", store.search());
                o.put("model", store.model());
                o.put("ctxLimit", store.ctxLimit());
                o.put("foldAll", store.foldAll());
                o.put("lang", lang);
                o.put("storage", storageAllowed());
                o.put("version", VERSION);
                o.put("crash", Log.hasCrash());
                o.put("crashNew", Log.hasCrash() && !store.crashSeen());
                o.put("intro", !store.introSeen());
                o.put("contacts", lockedNow ? new JSONObject() : contactsJson());
                JSONObject au = auth.state();
                if (lockedNow) {
                    au.remove("name"); au.remove("email");
                    au.remove("phone"); au.remove("uid");
                }
                o.put("auth", au);
                o.put("device", Build.MODEL);
                o.put("memCount", lockedNow ? 0 : mem.count());
                o.put("accounts", lockedNow ? new JSONArray() : store.accountSummary());
                o.put("active", lockedNow ? -1 : store.activeIndex());
                o.put("apiRunning", ApiServer.running());
                o.put("apiPort", ApiServer.running() ? ApiServer.runningPort() : store.apiPort());
                o.put("apiKey", lockedNow ? "" : store.ensureApiKey());
                o.put("apiAuto", store.apiAuto());
                o.put("apiLan", store.apiLan());
                o.put("apiDel", store.apiDelete());
                o.put("apiPublic", store.apiPublic());
                o.put("auditMask", store.auditMasked());
                o.put("auditContact", lockedNow ? "" : store.auditContact());
                o.put("auditHook", lockedNow ? "" : store.auditHook());
                o.put("telemetryOn", store.telemetryOn());
                o.put("telemetryLast", store.telemetryLast());
                o.put("telemetryPending", Telemetry.pending());
                o.put("code", CODE);
                o.put("pubUrl", store.pubUrl());
                o.put("updUrl", store.updUrl());
                guardFields(o);
            } catch (Exception ignored) {}
            emit(o);
            if (!store.token().isEmpty() && auth.unlocked()) whoami();
        }

        private void keysState() {
            JSONObject o = new JSONObject();
            try {
                o.put("e", "keys");
                o.put("items", store.apiKeyList());
            } catch (Exception ignored) {}
            emit(o);
        }

        /** Створює ключ і віддає його один раз — далі в UI лишається маска. */
        private void keyAdd(String name, long ttlMs) {
            JSONObject k = store.apiKeyAdd(name, ttlMs);
            JSONObject o = new JSONObject();
            try {
                o.put("e", "key_new");
                o.put("key", k.optString("key", ""));
                o.put("id", k.optString("id", ""));
                o.put("name", k.optString("name", ""));
                o.put("exp", k.optLong("exp", 0));
            } catch (Exception ignored) {}
            emit(o);
            keysState();
            apiState();
        }

        private void apiState() {
            JSONObject o = new JSONObject();
            try {
                boolean lockedApi = store.apiLocked();
                o.put("e", "api");
                o.put("running", ApiServer.running());
                o.put("port", ApiServer.running() ? ApiServer.runningPort() : store.apiPort());
                o.put("key", lockedApi ? "" : store.ensureApiKey());
                o.put("auto", store.apiAuto());
                o.put("lan", store.apiLan());
                o.put("del", store.apiDelete());
                o.put("pub", store.apiPublic());
                o.put("keys", store.apiKeyList().length());
                o.put("auditMask", store.auditMasked());
                o.put("auditContact", lockedApi ? "" : store.auditContact());
                o.put("auditHook", lockedApi ? "" : store.auditHook());
                o.put("host", store.apiLan() ? "0.0.0.0" : "127.0.0.1");
                o.put("code", CODE);
                o.put("pubUrl", store.pubUrl());
                o.put("updUrl", store.updUrl());
            } catch (Exception ignored) {}
            emit(o);
        }

        /**
         * Адресу тунелю пише скрипт у Download/FunaFiles/tunnel.txt, щоб не
         * перебирати її вручну після кожного перезапуску cloudflared.
         */
        private void pubFromFile(final boolean quiet) {
            new Thread(() -> {
                String url = "";
                String[] paths = {"/sdcard/Download/FunaFiles/tunnel.txt",
                        "/storage/emulated/0/Download/FunaFiles/tunnel.txt"};
                for (String p : paths) {
                    java.io.File f = new java.io.File(p);
                    if (!f.canRead()) continue;
                    try (java.io.BufferedReader r = new java.io.BufferedReader(
                            new java.io.InputStreamReader(new java.io.FileInputStream(f),
                                    StandardCharsets.UTF_8))) {
                        String line;
                        int n = 0;
                        while ((line = r.readLine()) != null && ++n < 32) {
                            line = line.trim();
                            int i = line.indexOf("https://");
                            if (i < 0) i = line.indexOf("http://");
                            if (i >= 0) { url = line.substring(i).trim(); break; }
                        }
                    } catch (Exception ignored) {}
                    if (!url.isEmpty()) break;
                }
                final String u = url;
                ui.post(() -> {
                    if (u.isEmpty()) {
                        if (!quiet) toast(tr("Не знайдено Download/FunaFiles/tunnel.txt",
                                "Download/FunaFiles/tunnel.txt not found",
                                "Download/FunaFiles/tunnel.txt не найден"));
                        if (quiet) apiState();
                        return;
                    } else {
                        store.setPubUrl(u);
                        if (!quiet) toast(tr("Публічна адреса: " + u, "Public URL: " + u, "Публичный адрес: " + u));
                    }
                    apiState();
                });
            }, "funa-pub").start();
        }

        private JSONObject contactsJson() {
            JSONObject o = new JSONObject();
            try {
                o.put("email", store.contactEmail());
                o.put("phone", store.contactPhone());
                o.put("google", store.contactGoogle());
            } catch (Exception ignored) {}
            return o;
        }

        private void diag() {
            new Thread(() -> {
                JSONObject o = new JSONObject();
                try {
                    o.put("e", "diag");
                    o.put("items", Diag.collect(MainActivity.this, store));
                    o.put("crash", Log.hasCrash());
                    o.put("version", VERSION);
                    o.put("logLines", Log.size());
                } catch (Exception e) {
                    Log.e("diag", "failed", e);
                    try { o.put("e", "diag"); o.put("items", new JSONArray()); } catch (Exception ignored) {}
                }
                emit(o);
            }, "funa-diag").start();
        }

        private void previewReport() {
            new Thread(() -> {
                JSONObject o = new JSONObject();
                try {
                    o.put("e", "report");
                    o.put("text", Diag.report(MainActivity.this, store));
                } catch (Exception e) {
                    try { o.put("e", "report"); o.put("text", "report error: " + e.getMessage()); } catch (Exception ignored) {}
                }
                emit(o);
            }, "funa-report").start();
        }

        private void telemetryState() {
            new Thread(() -> {
                JSONObject o = new JSONObject();
                try {
                    o.put("e", "telemetry");
                    o.put("on", store.telemetryOn());
                    o.put("last", store.telemetryLast());
                    o.put("pending", Telemetry.pending());
                } catch (Exception ignored) {}
                emit(o);
            }, "funa-telemetry").start();
        }

        private void telemetryPreview(final JSONObject usage) {
            new Thread(() -> {
                JSONObject o = new JSONObject();
                try {
                    o.put("e", "telemetry");
                    o.put("preview", true);
                    o.put("text", Telemetry.snapshot(MainActivity.this, store, usage).toString(2));
                } catch (Exception e) {
                    try { o.put("e", "telemetry"); o.put("text", "telemetry error: " + e.getMessage()); }
                    catch (Exception ignored) {}
                }
                emit(o);
            }, "funa-telemetry").start();
        }

        private void telemetrySend(final JSONObject usage, final boolean manual) {
            new Thread(() -> {
                JSONObject o = new JSONObject();
                boolean queued = false;
                boolean sent = false;
                String error = "";
                try {
                    if (!store.telemetryOn()) throw new Exception("telemetry disabled");
                    JSONObject snap = Telemetry.snapshot(MainActivity.this, store, usage);
                    snap.put("source", manual ? "manual" : "auto");
                    String url = store.telemetryUrl();
                    if (!url.isEmpty() && Telemetry.post(url, snap)) {
                        sent = true;
                        // Раніше позначка «надіслано» ставилася навіть при
                        // збої, і звіт більше не пробували. Тепер — лише
                        // після успіху, а чергу досилаємо одразу.
                        store.telemetryMarkSent(System.currentTimeMillis());
                        Telemetry.flush(url, 3);
                    } else {
                        // Немає приймача або мережа впала — не втрачаємо звіт:
                        // кладемо у файлову чергу, наступний запуск повторить.
                        Telemetry.writeSnapshot(store, snap);
                        queued = true;
                    }
                } catch (Exception e) {
                    error = e.getMessage() == null ? "error" : e.getMessage();
                    Log.e("telemetry", "queue failed", e);
                }
                try {
                    o.put("e", "telemetry");
                    o.put("on", store.telemetryOn());
                    o.put("last", store.telemetryLast());
                    o.put("pending", Telemetry.pending());
                    o.put("queued", queued);
                    o.put("sent", sent);
                    if (!error.isEmpty()) o.put("error", error);
                } catch (Exception ignored) {}
                emit(o);
                if (manual) toast(sent
                        ? tr("Діагностику надіслано", "Diagnostics sent", "Диагностика отправлена")
                        : (queued ? tr("Діагностику додано в чергу", "Diagnostics queued", "Диагностика добавлена в очередь")
                        : tr("Не вдалося зберегти діагностику", "Could not queue diagnostics", "Не удалось сохранить диагностику")));
            }, "funa-telemetry").start();
        }

        /** Не частіше ніж раз на добу; лише якщо увімкнено й є доступ до файлів. */
        private void telemetryAuto(final JSONObject usage) {
            if (!store.telemetryOn()) return;
            long now = System.currentTimeMillis();
            if (now - store.telemetryLast() < 24L * 60 * 60 * 1000) return;
            if (!Diag.storageAllowed(MainActivity.this)) return;
            telemetrySend(usage, false);
        }

        private void contactsSave(JSONObject c) {
            store.setContacts(c.optString("email", ""), c.optString("phone", ""),
                    c.optString("google", ""));
            Log.add("app", "contacts updated");
            JSONObject o = new JSONObject();
            try {
                o.put("e", "contacts");
                o.put("contacts", contactsJson());
            } catch (Exception ignored) {}
            emit(o);
            toast(tr("Контакти збережено", "Contacts saved", "Контакты сохранены"));
        }

        private void jsLog(JSONObject c) {
            String msg = c.optString("msg", "");
            if (msg.isEmpty()) return;
            if (msg.length() > 2000) msg = msg.substring(0, 2000);
            if ("error".equals(c.optString("level", ""))) Log.e("js", msg);
            else Log.add("js", msg);
        }

        private void sendReport(final boolean email) {
            new Thread(() -> {
                final String text = Diag.report(MainActivity.this, store);
                final String subject = Diag.subject(MainActivity.this, store);
                ui.post(() -> {
                    try {
                        Intent i;
                        if (email) {
                            i = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"));
                            i.putExtra(Intent.EXTRA_SUBJECT, subject);
                            i.putExtra(Intent.EXTRA_TEXT, text);
                            if (i.resolveActivity(getPackageManager()) == null) {
                                // немає поштового клієнта — пропонуємо будь-який спосіб
                                i = new Intent(Intent.ACTION_SEND);
                                i.setType("text/plain");
                                i.putExtra(Intent.EXTRA_SUBJECT, subject);
                                i.putExtra(Intent.EXTRA_TEXT, text);
                            }
                        } else {
                            i = new Intent(Intent.ACTION_SEND);
                            i.setType("text/plain");
                            i.putExtra(Intent.EXTRA_SUBJECT, subject);
                            i.putExtra(Intent.EXTRA_TEXT, text);
                        }
                        startActivity(Intent.createChooser(i,
                                tr("Надіслати звіт", "Send report", "Отправить отчёт")));
                    } catch (Exception e) {
                        Log.e("diag", "send report failed", e);
                        copy(text);
                        toast(tr("Немає застосунку для надсилання — звіт скопійовано",
                                "No app to send — report copied", "Нет приложения для отправки — отчёт скопирован"));
                    }
                });
            }, "funa-report-send").start();
        }

        private void copyReport() {
            new Thread(() -> {
                final String text = Diag.report(MainActivity.this, store);
                ui.post(() -> copy(text));
            }, "funa-report-copy").start();
        }

        private void diagAction(String id) {
            switch (id) {
                case "grant": askStorage(); break;
                case "termux": openUrl("https://f-droid.org/packages/com.termux/"); break;
                case "termux_open": launchPkg(Diag.TERMUX, "https://f-droid.org/packages/com.termux/"); break;
                case "reterm": launchPkg(Diag.RETERMINAL, "https://f-droid.org/packages/com.rk.terminal/"); break;
                case "bridge_save": saveBridge(); break;
                case "api": apiStart(); break;
                case "battery": openBattery(); break;
                case "account": authEmit(new JSONObject(), "state"); break;
                default: break;
            }
        }

        /** Відкриває застосунок термінала, а якщо він раптом зник — сторінку F-Droid. */
        private void launchPkg(final String pkg, final String fallback) {
            if (Looper.myLooper() != Looper.getMainLooper()) {
                ui.post(() -> launchPkg(pkg, fallback));
                return;
            }
            try {
                Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
                if (i != null) {
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    return;
                }
            } catch (Exception ignored) {}
            openUrl(fallback);
        }

        /**
         * Людський текст для кодів, які реально повертають локальне блокування
         * та імпорт сесії. Коди нативного JSON-логіна DeepSeek більше не
         * використовуються: вхід іде через сайт, тому мертві гілки прибрано.
         */
        private String authMsg(String code, int minutes) {
            switch (code) {
                case "need_pass":
                    return tr("Придумай пароль для FunaChat — мінімум 6 символів",
                            "Choose a FunaChat password — at least 6 characters",
                            "Придумай пароль для FunaChat — минимум 6 символов");
                case "bad_pass":
                    return tr("Невірний пароль", "Wrong password", "Неверный пароль");
                case "tries":
                    return tr("Забагато спроб. Спробуй через " + Math.max(1, minutes) + " хв.",
                            "Too many attempts. Try again in " + Math.max(1, minutes) + " min.",
                            "Слишком много попыток. Попробуй через " + Math.max(1, minutes) + " мин.");
                case "locked":
                    return tr("Спочатку розблокуй FunaChat", "Unlock FunaChat first",
                            "Сначала разблокируй FunaChat");
                case "no_account":
                case "no_pass":
                    return tr("Спочатку увійди в акаунт DeepSeek", "Sign in to a DeepSeek account first",
                            "Сначала войди в аккаунт DeepSeek");
                case "bad_token":
                    return tr("DeepSeek не прийняв токен або cookie — увійди через сайт ще раз",
                            "DeepSeek rejected the token or cookie — sign in through the site again",
                            "DeepSeek не принял токен или cookie — войди через сайт ещё раз");
                case "net":
                    return tr("Немає зв'язку з DeepSeek — перевір мережу",
                            "No connection to DeepSeek — check the network",
                            "Нет связи с DeepSeek — проверь сеть");
                case "logout":
                    return tr("Сесію завершено", "Signed out", "Сессия завершена");
                case "401":
                case "403":
                    return tr("Сесія прострочена — увійди через сайт ще раз",
                            "The session expired — sign in through the site again",
                            "Сессия истекла — войди через сайт ещё раз");
                case "429":
                    return tr("Забагато запитів — зачекай кілька хвилин",
                            "Too many requests — wait a few minutes",
                            "Слишком много запросов — подожди несколько минут");
                case "error":
                case "":
                    return "";
                default:
                    try {
                        int c = Integer.parseInt(code);
                        if (c >= 500) return tr("Сервер DeepSeek тимчасово недоступний — спробуй пізніше",
                                "DeepSeek server is temporarily unavailable — try later",
                                "Сервер DeepSeek временно недоступен — попробуй позже");
                    } catch (Exception ignored) {}
                    return "";
            }
        }

        /** Cookie сайту: пари name=value, без керівних символів і без межі в кілобайти. */
        boolean cookieLike(String v) {
            if (v == null) return false;
            v = v.trim();
            if (v.length() < 8 || v.length() > 16384 || v.indexOf('=') < 0) return false;
            for (int i = 0; i < v.length(); i++) {
                char c = v.charAt(i);
                // Cookie у заголовку: керівні символи ламають запит або
                // дозволяють ін'єкцію, тому лишаємо пробіл і звичайний текст.
                if ((c < 0x20 && c != ' ') || c == 0x7f) return false;
            }
            return true;
        }

        /** Схоже на справжній токен, а не на шматок JSON чи порожнє значення. */
        boolean tokenLike(String v) {
            if (v == null) return false;
            v = v.trim();
            if (v.length() < 20 || v.length() > 4096) return false;
            for (int i = 0; i < v.length(); i++) {
                char c = v.charAt(i);
                if (c <= ' ' || c == '"' || c == '{' || c == '}' || c == '[' || c == ']') return false;
            }
            return true;
        }

        /** Увімкнення/вимкнення блокування; для ввімкнення потрібен пароль. */
        private void authSetLock(final JSONObject c) {
            // PBKDF2 на 120k ітерацій не має виконуватися в UI-потоці WebView
            new Thread(() -> authSetLockNow(c), "funa-auth").start();
        }

        private void authSetLockNow(JSONObject c) {
            final boolean on = c.optBoolean("on", true);
            if (!on) {
                // Вимкнути замок можна лише з розблокованого застосунку.
                // Інакше застарілий виклик зі сторінки знімав би захист без
                // пароля, ще до екрана розблокування.
                if (!auth.unlocked()) {
                    authEmit(fail("locked"), "lock");
                    return;
                }
                auth.setLock(false);
                authEmit(new JSONObject(), "lock");
                return;
            }
            if (auth.hasPassword()) {
                auth.setLock(true);
                auth.logout();
                // Ответ мог идти прямо сейчас: обрываем агента и субагентов,
                // иначе они продолжат жечь запросы старого аккаунта под
                // экраном пароля (их события всё равно отфильтрованы).
                resetAgent();
                cancelOpen();
                authEmit(new JSONObject(), "lock");
                return;
            }
            final String pass = c.optString("password", "");
            if (pass.length() < 6) {
                JSONObject r = fail("need_pass");
                try {
                    r.put("msg", tr("Придумай пароль для FunaChat — мінімум 6 символів",
                            "Choose a FunaChat password — at least 6 characters",
                            "Придумай пароль для FunaChat — минимум 6 символов"));
                } catch (Exception ignored) {}
                authEmit(r, "lock");
                return;
            }
            // Пароль блокування — локальний: хеш PBKDF2 у сховищі застосунку.
            // Пароль DeepSeek не запитуємо й нікуди не надсилаємо.
            if (!auth.setPassword(pass) || !auth.hasPassword()) {
                JSONObject r = fail("no_account");
                try {
                    r.put("msg", tr("Спочатку увійди в акаунт DeepSeek",
                            "Sign in to a DeepSeek account first",
                            "Сначала войди в аккаунт DeepSeek"));
                } catch (Exception ignored) {}
                authEmit(r, "lock");
                return;
            }
            auth.setLock(true);
            auth.logout();
            // Ответ мог идти прямо сейчас: обрываем агента и субагентов,
            // иначе их дельты дорисуются под экраном пароля.
            resetAgent();
            cancelOpen();
            Log.add("auth", "local lock enabled");
            JSONObject ok = new JSONObject();
            try { ok.put("ok", true); } catch (Exception ignored) {}
            authEmit(ok, "lock");
        }

        private JSONObject fail(String code) {
            JSONObject o = new JSONObject();
            try { o.put("ok", false); o.put("code", code); } catch (Exception ignored) {}
            return o;
        }

        /** Кладе актуальний скрипт моста у Download/FunaFiles, щоб його було легко запустити. */
        private void saveBridge() {
            new Thread(() -> {
                File f = new File(Tools.ROOT, "funa-bridge.sh");
                try {
                    File dir = f.getParentFile();
                    if (dir != null && !dir.exists() && !dir.mkdirs())
                        throw new Exception("mkdir");
                    // Атомарно: оборваний запис не має лишити порожній
                    // funa-bridge.sh, який потім не запускається.
                    java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
                    try (InputStream in = getAssets().open("funa-bridge.sh")) {
                        byte[] b = new byte[8192];
                        int n;
                        while ((n = in.read(b)) > 0) bo.write(b, 0, n);
                    }
                    Tools.writeAtomic(f, bo.toByteArray());
                    Log.add("bridge", "saved to " + f.getAbsolutePath());
                    toast(tr("Скрипт збережено: " + f.getAbsolutePath()
                                    + "\nTermux: bash ~/storage/downloads/FunaFiles/funa-bridge.sh"
                                    + "\nReTerminal: sh /sdcard/Download/FunaFiles/funa-bridge.sh",
                            "Script saved: " + f.getAbsolutePath()
                                    + "\nTermux: bash ~/storage/downloads/FunaFiles/funa-bridge.sh"
                                    + "\nReTerminal: sh /sdcard/Download/FunaFiles/funa-bridge.sh",
                            "Скрипт сохранён: " + f.getAbsolutePath()
                                    + "\nTermux: bash ~/storage/downloads/FunaFiles/funa-bridge.sh"
                                    + "\nReTerminal: sh /sdcard/Download/FunaFiles/funa-bridge.sh"));
                } catch (Exception e) {
                    Log.e("bridge", "save failed", e);
                    toast(tr("Не вдалося зберегти скрипт моста",
                            "Could not save the bridge script",
                            "Не удалось сохранить скрипт моста"));
                }
            }, "funa-bridge-save").start();
        }

        /** Вхід на справжньому сайті DeepSeek у WebView (обходить risk/429). */
        private void openSiteLogin() {
            openSiteLogin(false);
        }

        /**
         * verify=true — не вхід, а перевірка hCaptcha (коди 40303/40304):
         * сайт відкривається на чаті, користувач надсилає повідомлення й
         * проходить капчу, потім імпортує оновлену сесію.
         */
        private void openSiteLogin(final boolean verify) {
            if (Looper.myLooper() != Looper.getMainLooper()) {
                ui.post(() -> openSiteLogin(verify));
                return;
            }
            try {
                Intent i = new Intent(MainActivity.this, SiteLogin.class);
                if (verify) i.putExtra(SiteLogin.EXTRA_VERIFY, true);
                startActivityForResult(i, REQ_SITE);
            } catch (Exception e) {
                Log.e("auth", "site login failed to start", e);
                toast(tr("Не вдалося відкрити сайт", "Could not open the site",
                        "Не удалось открыть сайт"));
            }
        }

        private void authUnlock(JSONObject c) {
            new Thread(() -> authEmit(auth.unlock(c.optString("password", "")), "unlock"),
                    "funa-auth").start();
        }

        private void authEmit(JSONObject res, String action) {
            JSONObject o = new JSONObject();
            try {
                o.put("e", "auth");
                o.put("action", action);
                o.put("ok", res.optBoolean("ok", true));
                String code = res.optString("code", "");
                String msg = authMsg(code, res.optInt("minutes", 0));
                if (msg.isEmpty()) msg = res.optString("msg", "").trim();
                if (msg.isEmpty()) msg = tr("Помилка. Спробуй ще раз", "Something went wrong. Try again",
                        "Ошибка. Попробуй ещё раз");
                o.put("code", code);
                o.put("msg", msg);
                if (!res.optBoolean("ok", true)) {
                    Log.add("auth", "failed [" + action + "] " + code + ": " + msg);
                }
                o.put("signedIn", !store.token().isEmpty());
                JSONObject st = res.optJSONObject("state") == null ? auth.state() : res.optJSONObject("state");
                // Под локальным замком email/телефон/uid не должны попадать
                // в DOM: экрану разблокировки хватает has/lock/hasPass.
                if (!auth.unlocked()) {
                    st.remove("name"); st.remove("email");
                    st.remove("phone"); st.remove("uid");
                }
                o.put("state", st);
                if ("logout".equals(action)) o.put("code", "logout");
            } catch (Exception ignored) {}
            emit(o);
        }

        private void guardFields(JSONObject o) {
            try {
                o.put("guardOn", store.guardOn());
                o.put("guardGap", store.guardGapMs() / 1000);
                o.put("guardPerMin", store.guardPerMin());
                o.put("guardPerDay", store.guardPerDay());
                o.put("guardCount", store.guardCount());
                o.put("guardCool", Math.max(0, store.guardCooldown() - System.currentTimeMillis()));
                o.put("guardNote", Guard.noteText(store.guardNote(), store.lang()));
            } catch (Exception ignored) {}
        }

        private void guardState() {
            JSONObject o = new JSONObject();
            try {
                o.put("e", "guard");
                guardFields(o);
            } catch (Exception ignored) {}
            emit(o);
        }

        private void guardApply(JSONObject c) {
            store.setGuardOn(c.optBoolean("on", store.guardOn()));
            store.setGuardGap(c.optInt("gap", store.guardGapMs() / 1000) * 1000);
            store.setGuardPerMin(c.optInt("perMin", store.guardPerMin()));
            store.setGuardPerDay(c.optInt("perDay", store.guardPerDay()));
            guardState();
        }

        private void apiStart() {
            String err = ApiServer.start(store);
            JSONObject o = new JSONObject();
            try {
                o.put("e", "api");
                o.put("running", ApiServer.running());
                o.put("port", ApiServer.running() ? ApiServer.runningPort() : store.apiPort());
                o.put("key", store.ensureApiKey());
                o.put("auto", store.apiAuto());
                o.put("lan", store.apiLan());
                o.put("del", store.apiDelete());
                o.put("host", store.apiLan() ? "0.0.0.0" : "127.0.0.1");
                if (err != null) o.put("err", err);
            } catch (Exception ignored) {}
            emit(o);
            if (err != null) toast(tr("API: ", "API: ", "API: ") + err);
        }

        private void apiApply(JSONObject c) {
            store.setApiPort(c.optInt("port", store.apiPort()));
            store.setApiAuto(c.optBoolean("auto", store.apiAuto()));
            store.setApiLan(c.optBoolean("lan", store.apiLan()));
            store.setApiDelete(c.optBoolean("del", store.apiDelete()));
            store.setApiPublic(c.optBoolean("pub", store.apiPublic()));
            boolean restart = ApiServer.running();
            if (restart) ApiServer.stop();
            if (store.apiAuto()) apiStart();
            else apiState();
        }

        private void importSession() {
            new Thread(() -> {
                File f = new File("/sdcard/Download/FunaFiles/session.json");
                if (!f.exists()) f = new File("/storage/emulated/0/Download/FunaFiles/session.json");
                if (f.exists() && f.canRead()) {
                    try {
                        if (f.length() > MAX_SESSION) throw new Exception("too big");
                        byte[] data = new byte[(int) f.length()];
                        try (java.io.FileInputStream in = new java.io.FileInputStream(f)) {
                            int off = 0;
                            while (off < data.length) {
                                int n = in.read(data, off, data.length - off);
                                if (n <= 0) break;
                                off += n;
                            }
                            if (off != data.length) data = java.util.Arrays.copyOf(data, off);
                        }
                        boolean imported = applySession(new String(data, StandardCharsets.UTF_8));
                        if (imported && store.importClean()) {
                            // токен у спільній памʼяті — прибираємо після імпорту
                            if (f.delete()) toast(tr("session.json видалено з Download",
                                    "session.json removed from Download",
                                    "session.json удалён из Download"));
                        }
                        return;
                    } catch (Exception e) {
                        if ("too big".equals(e.getMessage())) {
                            toast(tr("Файл сесії завеликий (макс. 4 МБ)",
                                    "Session file is too large (max 4 MB)",
                                    "Файл сессии слишком большой (макс. 4 МБ)"));
                            return;
                        }
                        toast(tr("Помилка читання: ", "Read error: ", "Ошибка чтения: ") + e.getMessage());
                    }
                }
                ui.post(() -> pickSessionFile());
            }).start();
        }

        private void pickAttachment() {
            try {
                Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                startActivityForResult(i, REQ_ATTACH);
            } catch (Exception e) {
                toast(tr("Немає файлового менеджера", "No file manager available", "Нет файлового менеджера"));
            }
        }

        private void uploadOne(final File f, final String name, final String mime) {
            final int ep = store.epoch();
            // Файл належить поточному акаунту: фіксуємо токен зараз, щоб
            // після перемикання він не поїхав у чужий акаунт.
            final String tok = store.token();
            JSONObject start = new JSONObject();
            try {
                start.put("e", "file");
                start.put("status", "uploading");
                start.put("name", name);
                start.put("size", f.length());
                start.put("mime", mime);
                start.put("local", f.getAbsolutePath());
                start.put("isImage", mime != null && mime.startsWith("image/"));
            } catch (Exception ignored) {}
            emit(start);
            new Thread(() -> {
                // Користувач перемкнув акаунт, поки файл копіювався: вантажити
                // його в новий акаунт не можна — файл належить іншому чату.
                if (store.epoch() != ep) { f.delete(); return; }
                JSONObject o = new JSONObject();
                try {
                    Net net = new Net(store, tok);
                    JSONObject up = net.uploadFile(f, name, mime, store.thinking(), pc -> {
                        if (store.epoch() != ep) return;
                        JSONObject p = new JSONObject();
                        try {
                            p.put("e", "file");
                            p.put("status", "uploading");
                            p.put("name", name);
                            p.put("local", f.getAbsolutePath());
                            p.put("progress", pc);
                        } catch (Exception ignored) {}
                        emit(p);
                    });
                    String id = up.optString("id", "");
                    if (id.isEmpty()) throw new Exception(tr("Сервер не повернув id файлу", "Server returned no file id", "Сервер не вернул id файла"));
                    JSONObject info = net.waitFile(id, 60000);
                    if (!"SUCCESS".equals(info.optString("status", ""))) {
                        throw new Exception(tr("Файл ще обробляється", "File is still processing", "Файл ещё обрабатывается"));
                    }
                    boolean img = info.optBoolean("is_image", false);
                    o.put("e", "file");
                    o.put("status", "ok");
                    o.put("id", id);
                    o.put("name", info.optString("file_name", name));
                    o.put("size", info.optLong("file_size", f.length()));
                    o.put("isImage", img);
                    o.put("mime", mime);
                    o.put("local", f.getAbsolutePath());
                    if (img) {
                        String th = Attach.thumbFile(f, 720);
                        if (!th.isEmpty()) o.put("thumb", th);
                    }
                } catch (Exception e) {
                    try {
                        o.put("e", "file");
                        o.put("status", "error");
                        o.put("name", name);
                        o.put("local", f.getAbsolutePath());
                        o.put("msg", e.getMessage() == null ? "upload error" : e.getMessage());
                    } catch (Exception ignored) {}
                }
                if (store.epoch() == ep) emit(o);
            }, "funa-upload").start();
        }

        private void pickSessionFile() {
            try {
                Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/json", "text/plain", "*/*"});
                startActivityForResult(i, REQ_PICK);
            } catch (Exception e) {
                toast(tr("Немає файлового менеджера", "No file manager available", "Нет файлового менеджера"));
            }
        }

        boolean applySession(String text) {
            try {
                JSONObject j = new JSONObject(text);
                JSONObject u = j.optJSONObject("user");
                String token = "";
                String uid = "";
                if (u != null) {
                    token = u.optString("token", "");
                    uid = u.optString("id", "");
                }
                if (token.isEmpty()) token = j.optString("token", "");
                JSONObject h = j.optJSONObject("headers");
                if (token.isEmpty() && h != null) token = h.optString("x-auth-token", "");
                if (token.isEmpty()) { toast(tr("У файлі немає токена", "No token in file", "В файле нет токена")); return false; }
                token = token.trim();
                if (!tokenLike(token)) {
                    toast(tr("У файлі не схоже на токен", "The file does not contain a token",
                            "В файле не похоже на токен"));
                    return false;
                }
                String nid = store.addAccount(token, uid, "", "", "", "import");
                if (nid == null) { toast(tr("Не вдалося зберегти токен", "Could not store the token", "Не удалось сохранить токен")); return false; }
                auth.afterLogin("");
                if (h != null) {
                    String cv = h.optString("x-client-version", "");
                    if (!cv.isEmpty()) store.setClientVersion(cv);
                    String dev = h.optString("x-device-model", "");
                    if (!dev.isEmpty()) store.setDevice(dev);
                    store.setClientLocale(h.optString("x-client-locale", ""));
                    store.setClientTz(h.optString("x-client-timezone-offset", ""));
                }
                JSONObject o = new JSONObject();
                o.put("e", "imported");
                o.put("signedIn", true);
                emit(o);
                toast(tr("Токен імпортовано", "Token imported", "Токен импортирован"));
                // Імпорт міг перемкнути активний акаунт: старий чат, вкладення
                // й конспект належать іншому акаунту, тому повний перехід,
                // як після звичайного перемикання, а не лише новий агент.
                afterAccountChange();
                return true;
            } catch (Exception e) {
                String m = e.getMessage();
                toast(tr("Це не session.json: ", "Not a session.json: ", "Это не session.json: ")
                        + (m == null ? e.getClass().getSimpleName() : m));
                return false;
            }
        }

        /**
         * Імпорт сесії сайту: спершу пробуємо Bearer-токен, якщо DeepSeek його
         * не приймає — ту саму сесію як cookie. Зберігаємо те, що працює.
         */
        private void importSession(final String token, final String cookie) {
            if ((token == null || token.isEmpty()) && (cookie == null || cookie.isEmpty())) {
                toast(tr("Сайт не повернув сесію", "The site returned no session",
                        "Сайт не вернул сессию"));
                return;
            }
            new Thread(() -> {
                JSONObject r = new JSONObject();
                String mode = null, value = null;
                JSONObject u = null;
                try {
                    if (token != null && !token.isEmpty() && tokenLike(token)) {
                        try {
                            u = pickUser(new Net(store, token).profile());
                            value = token;
                            mode = "bearer";
                        } catch (Net.ApiException e) {
                            Log.add("account", "site token rejected HTTP " + e.code);
                        }
                    }
                    if (value == null && cookieLike(cookie)) {
                        u = pickUser(new Net(store, cookie).asCookie().profile());
                        value = cookie;
                        mode = "cookie";
                    }
                    if (value == null) throw new Net.ApiException(401, "rejected");
                    String uid = u.optString("id", "");
                    String email = u.optString("email", "");
                    String phone = u.optString("mobile_number", "");
                    String name = u.optString("name", "");
                    String id = "cookie".equals(mode)
                            ? store.addAccountCookie(value, uid, email, phone, name, "site")
                            : store.addAccount(value, uid, email, phone, name, "site");
                    if (id == null) {
                        try {
                            r.put("ok", false);
                            r.put("msg", tr("Не вдалося зберегти акаунт (сховище або ліміт 30 акаунтів)",
                                    "Could not save the account (storage or the 30-account limit)",
                                    "Не удалось сохранить аккаунт (хранилище или лимит 30 аккаунтов)"));
                        } catch (Exception ignored) {}
                    } else {
                        Log.add("account", "site import ok mode=" + mode);
                        boolean reset = auth.consumeReset();
                        auth.afterLogin("");
                        try {
                            r.put("ok", true);
                            r.put("id", id);
                            if (reset) r.put("reset", true);
                        } catch (Exception ignored) {}
                    }
                } catch (Net.ApiException e) {
                    Log.add("account", "site import rejected HTTP " + e.code);
                    try {
                        r.put("ok", false);
                        r.put("code", "bad_token");
                        r.put("msg", tr("DeepSeek не прийняв ні токен, ні cookie сайту",
                                "DeepSeek rejected both the token and the site cookie",
                                "DeepSeek не принял ни токен, ни cookie сайта") + " (" + e.code + ")");
                    } catch (Exception ignored) {}
                } catch (Exception e) {
                    try {
                        r.put("ok", false);
                        r.put("code", "net");
                        r.put("msg", tr("Немає зв'язку з DeepSeek", "No connection to DeepSeek",
                                "Нет связи с DeepSeek"));
                    } catch (Exception ignored) {}
                }
                try { r.put("e", "acc_result"); } catch (Exception ignored) {}
                emit(r);
                if (r.optBoolean("ok", false)) afterAccountChange();
            }, "funa-site-import").start();
        }

        private JSONObject pickUser(JSONObject p) {
            JSONObject u = p.optJSONObject("user");
            return u == null ? p : u;
        }

        private void accountAddCookie(final String cookie, final String src) {
            if (cookie == null || cookie.trim().isEmpty() || cookie.indexOf('=') < 0) {
                toast(tr("Сайт не повернув сесію", "The site returned no session",
                        "Сайт не вернул сессию"));
                return;
            }
            final String ck = cookie.trim();
            new Thread(() -> {
                JSONObject r = new JSONObject();
                try {
                    JSONObject p = new Net(store, ck).asCookie().profile();
                    JSONObject u = p.optJSONObject("user");
                    if (u == null) u = p;
                    String id = store.addAccountCookie(ck, u.optString("id", ""),
                            u.optString("email", ""), u.optString("mobile_number", ""),
                            u.optString("name", ""), src);
                    if (id == null) {
                        r.put("ok", false);
                        r.put("msg", tr("Не вдалося зберегти акаунт (сховище або ліміт 30 акаунтів)",
                                "Could not save the account (storage or the 30-account limit)",
                                "Не удалось сохранить аккаунт (хранилище или лимит 30 аккаунтов)"));
                    } else {
                        auth.afterLogin("");
                        r.put("ok", true);
                        r.put("id", id);
                    }
                } catch (Net.ApiException e) {
                    Log.add("account", "cookie session rejected HTTP " + e.code);
                    try {
                        r.put("ok", false);
                        r.put("code", "bad_token");
                        r.put("msg", tr("DeepSeek не прийняв сесію сайту",
                                "DeepSeek rejected the site session",
                                "DeepSeek не принял сессию сайта") + " (" + e.code + ")");
                    } catch (Exception ignored) {}
                } catch (Exception e) {
                    try {
                        r.put("ok", false);
                        r.put("code", "net");
                        r.put("msg", tr("Немає зв'язку з DeepSeek", "No connection to DeepSeek",
                                "Нет связи с DeepSeek"));
                    } catch (Exception ignored) {}
                }
                try { r.put("e", "acc_result"); } catch (Exception ignored) {}
                emit(r);
                if (r.optBoolean("ok", false)) {
                    Log.add("account", "added (cookie)");
                    afterAccountChange();
                }
            }, "funa-acc-cookie").start();
        }

        private void whoami() {
            new Thread(() -> {
                // Акаунт фіксуємо до запиту: якщо його перемкнуть, відповідь
                // не повинна записатися в чужий профіль.
                final String aid = store.activeId();
                final int ep = store.epoch();
                JSONObject o = new JSONObject();
                try {
                    o.put("e", "whoami");
                    if (store.token(aid).isEmpty()) { o.put("ok", false); o.put("msg", "no token"); emit(o); return; }
                    JSONObject p = new Net(store, store.token(aid)).profile();
                    JSONObject u = p.optJSONObject("user");
                    if (u == null) u = p;
                    o.put("ok", true);
                    o.put("email", u.optString("email", ""));
                    o.put("id", u.optString("id", ""));
                    o.put("user", u.optString("name", ""));
                    store.updateAccount(aid, u.optString("id", ""), u.optString("email", ""),
                            u.optString("mobile_number", ""), u.optString("name", ""));
                    store.setStatus(aid, "ok");
                } catch (Net.ApiException e) {
                    store.setStatus(aid, "expired");
                    try {
                        o.put("ok", false);
                        o.put("code", e.code);
                        o.put("msg", tr("Акаунт більше не діє — увійди знову",
                                "This account is no longer valid — sign in again",
                                "Аккаунт больше не действует — войди снова") + " (" + e.code + ")");
                    } catch (Exception ignored) {}
                } catch (Exception e) {
                    try { o.put("ok", false); o.put("msg", e.getMessage()); } catch (Exception ignored) {}
                }
                if (store.epoch() == ep) {
                    emit(o);
                    accountsList();
                }
            }).start();
        }

        private void sessions() {
            final int ep = store.epoch();
            // Токен фіксуємо зараз: інакше потік, що стартував після
            // перемикання акаунта, смикнув би список чужого акаунта.
            final String tok = store.token();
            new Thread(() -> {
                JSONObject o = new JSONObject();
                try {
                    JSONArray a = new Net(store, tok).sessions();
                    // Список із сервера може містити зайві поля й тисячі чатів:
                    // у UI потрібні лише id/title/updated_at/pinned.
                    JSONArray slim = new JSONArray();
                    for (int i = 0; i < a.length() && slim.length() < 400; i++) {
                        JSONObject s = a.optJSONObject(i);
                        if (s == null) continue;
                        JSONObject q = new JSONObject();
                        String t = s.optString("title", "");
                        if (t.length() > 200) t = t.substring(0, 200);
                        q.put("id", s.optString("id", ""));
                        q.put("title", t);
                        q.put("updated_at", s.optLong("updated_at", 0));
                        q.put("pinned", s.optBoolean("pinned", false));
                        slim.put(q);
                    }
                    o.put("e", "sessions");
                    o.put("items", slim);
                } catch (Exception e) {
                    try { o.put("e", "error"); o.put("msg", e.getMessage()); } catch (Exception ignored) {}
                }
                if (store.epoch() == ep) emit(o);
            }).start();
        }

        private void open(String id) {
            if (agent.busy()) { toast(tr("Спочатку зупини відповідь", "Stop the current answer first", "Сначала останови ответ")); return; }
            if (id.isEmpty()) { newChat(false); return; }
            final int ep = store.epoch();
            final int seq = ++openSeq;
            openingId = id;
            final String tok = store.token();
            new Thread(() -> {
                try {
                    Net net = new Net(store, tok);
                    List<JSONObject> items = Agent.historyMessages(net, id, store.base(store.lang()));
                    long last = 0;
                    for (JSONObject m : items) last = Math.max(last, m.optLong("id", 0));
                    // Дуже довгий чат не влазить у WebView одним eval(): тримаємо
                    // останні повідомлення в межах бюджету, а скільки приховано —
                    // кажемо UI. Контекст на сервері DeepSeek від цього не змінюється.
                    int hidden = 0;
                    long budget = 4L * 1024 * 1024;
                    long used = 0;
                    for (JSONObject m : items) used += m.toString().length();
                    while (items.size() > 600 || (used > budget && items.size() > 40)) {
                        JSONObject drop = items.remove(0);
                        used -= drop.toString().length();
                        hidden++;
                    }
                    JSONArray arr = new JSONArray();
                    for (JSONObject m : items) arr.put(m);
                    // акаунт могли перемкнути або відкрити інший чат, поки
                    // вантажилась історія: пізня відповідь не має перебивати
                    if (store.epoch() != ep || seq != openSeq) return;
                    openingId = null;
                    sessionId = id;
                    parentId = last;
                    first = items.isEmpty();
                    agent.setSession(id, parentId, first);

                    JSONObject h = new JSONObject();
                    h.put("e", "history");
                    h.put("id", id);
                    h.put("items", arr);
                    h.put("first", first);
                    if (hidden > 0) h.put("older", hidden);
                    emit(h);

                    JSONObject s = new JSONObject();
                    s.put("e", "session");
                    s.put("id", id);
                    s.put("title", "");
                    emit(s);
                    agent.onSessionReady();
                } catch (Exception e) {
                    if (store.epoch() != ep || seq != openSeq) return;
                    openingId = null;
                    JSONObject o = new JSONObject();
                    try { o.put("e", "error"); o.put("msg", e.getMessage()); } catch (Exception ignored) {}
                    emit(o);
                }
            }).start();
        }

        private void subList() {
            JSONObject o = new JSONObject();
            try {
                o.put("e", "sub_list");
                o.put("items", SubTask.listFor(agent.sessionId()));
            } catch (Exception ignored) {}
            emit(o);
        }

        private void retrySub(String id) {
            SubTask t = SubTask.retry(store, id);
            JSONObject o = new JSONObject();
            try {
                if (t == null) {
                    o.put("e", "sub_retry_fail");
                    o.put("id", id);
                } else {
                    o.put("e", "sub_retry_ok");
                    o.put("id", t.id);
                    o.put("old", id);
                }
            } catch (Exception ignored) {}
            emit(o);
        }

        private void subView(String id) {
            JSONObject o;
            SubTask t = SubTask.get(id);
            if (t == null) {
                o = new JSONObject();
                try {
                    o.put("e", "sub_view");
                    o.put("id", id);
                    o.put("missing", true);
                    o.put("error", tr("Сесію субагента не знайдено", "Subagent session not found", "Сессия субагента не найдена"));
                } catch (Exception ignored) {}
            } else {
                o = t.view();
            }
            emit(o);
        }

        /**
         * Скидає поточний чат у нативній частині, не торкаючись UI.
         * Потрібно і тоді, коли екран заблокований: інакше sessionId
         * старого акаунта лишився б й новий агент пішов би в чужий чат.
         */
        private void resetChatState() {
            // незавершене відкриття старого чату не повинно повернути його назад
            cancelOpen();
            sessionId = null;
            parentId = 0;
            first = true;
            summary = "";
            agent.setSession(null, 0, true);
        }

        private void newChat(boolean keep) {
            if (agent.busy()) { toast(tr("Спочатку зупини відповідь", "Stop the current answer first", "Сначала останови ответ")); return; }
            resetChatState();
            JSONObject o = new JSONObject();
            try {
                o.put("e", "clean");
                o.put("keep", keep);
            } catch (Exception ignored) {}
            emit(o);
        }

        private void compact(final String transcript, final boolean auto, final String want) {
            final int ep = store.epoch();
            // Стиснення триває секунди: за цей час користувач може відкрити
            // інший чат. Конспект має сенс лише для того чату, який стискали.
            final String sid = sessionId;
            // Транскрипт належить поточному акаунту. Якщо зафіксувати токен
            // пізніше, після перемикання чужий текст пішов би в новий акаунт.
            final String tok = store.token();
            if (transcript == null || transcript.trim().length() < 120) {
                JSONObject o = new JSONObject();
                try {
                    o.put("e", "compact_error");
                    o.put("sid", want);
                    o.put("msg", tr("Закоротка історія для стиснення", "Conversation is too short to compact", "История слишком короткая для сжатия"));
                    o.put("auto", auto);
                } catch (Exception ignored) {}
                emit(o);
                return;
            }
            new Thread(() -> {
                JSONObject o = new JSONObject();
                try {
                    if (store.epoch() != ep) return;
                    String sum = Compact.summarize(new Net(store, tok), transcript, store.lang());
                    if (sum == null || sum.trim().isEmpty()) throw new Exception(tr("Порожній конспект", "Empty summary", "Пустая сводка"));
                    // пока шла суммаризация, могли переключить аккаунт или
                    // открыть другой чат: конспект не должен попасть не туда
                    if (store.epoch() != ep || !java.util.Objects.equals(sid, sessionId)) return;
                    summary = sum;
                    o.put("e", "compacted");
                    o.put("sid", want);
                    o.put("summary", sum);
                    o.put("auto", auto);
                } catch (Exception e) {
                    if (store.epoch() != ep || !java.util.Objects.equals(sid, sessionId)) return;
                    try {
                        o.put("e", "compact_error");
                        o.put("sid", want);
                        o.put("msg", e.getMessage() == null ? "compact error" : e.getMessage());
                        o.put("auto", auto);
                    } catch (Exception ignored) {}
                }
                if (store.epoch() == ep) emit(o);
            }, "funa-compact").start();
        }

        private String tr(String uk, String en, String ru) {
            String l = store.lang();
            return "en".equals(l) ? en : ("ru".equals(l) ? ru : uk);
        }

        private void edit(JSONObject c) {
            String text = capInput(c.optString("text", ""));
            if (text == null || text.trim().isEmpty()) return;
            if (agent.busy()) { toast(tr("Вже відповідаю…", "Already answering…", "Уже отвечаю…")); idle(); return; }
            long id = c.optLong("id", 0);
            if (id <= 0) return;
            // незавершене відкриття іншого чату не має перебити цю дію
            cancelOpen();
            store.setThinking(c.optBoolean("thinking", store.thinking()));
            store.setSearch(c.optBoolean("search", store.search()));
            agent.setSession(sessionId, 0, false);
            if (!agent.sendEdit(text, id, fileIds(c))) idle();
        }

        private void regen(JSONObject c) {
            if (agent.busy()) { toast(tr("Вже відповідаю…", "Already answering…", "Уже отвечаю…")); idle(); return; }
            long child = c.optLong("child", 0);
            if (child <= 0 || sessionId == null) return;
            cancelOpen();
            store.setThinking(c.optBoolean("thinking", store.thinking()));
            store.setSearch(c.optBoolean("search", store.search()));
            agent.setSession(sessionId, 0, false);
            if (!agent.sendRegen(child)) idle();
        }

        private void cont(JSONObject c) {
            if (agent.busy()) { toast(tr("Вже відповідаю…", "Already answering…", "Уже отвечаю…")); idle(); return; }
            long id = c.optLong("id", 0);
            if (id <= 0 || sessionId == null) return;
            cancelOpen();
            agent.setSession(sessionId, 0, false);
            if (!agent.sendContinue(id)) idle();
        }

        /**
         * Після перемикання версії повідомлення наступне повідомлення має
         * піти саме в цю гілку, а не в ту, що була активною до перемикання.
         */
        private void branch(JSONObject c) {
            if (sessionId == null) return;
            // Під час відповіді гілку не міняємо: інакше нативний parentId
            // зміниться посеред запиту.
            if (agent.busy()) return;
            parentId = Math.max(0, c.optLong("parent", 0));
            first = false;
            agent.setSession(sessionId, parentId, false);
            Log.add("chat", "branch parent=" + parentId);
        }

        private void exportChat(JSONObject c) {
            final String md = c.optString("md", "");
            final String name = c.optString("name", "chat");
            new Thread(() -> {
                JSONObject o = new JSONObject();
                try {
                    File dir = new File(Tools.ROOT, "exports");
                    String safe = (name == null ? "" : name).replaceAll("[^A-Za-z0-9._-]", "");
                    if (safe.isEmpty()) safe = "chat";
                    if (safe.length() > 40) safe = safe.substring(0, 40);
                    String stamp = new java.text.SimpleDateFormat("yyyyMMdd-HHmm", java.util.Locale.US).format(new java.util.Date());
                    File f = new File(dir, safe + "-" + stamp + ".md");
                    // Чат на тисячі повідомлень — це десятки мегабайт:
                    // обрізаємо (з поміткою), щоб не зловити OOM у WebView.
                    String text = md == null ? "" : md;
                    if (text.length() > 8 * 1024 * 1024) {
                        text = text.substring(0, 8 * 1024 * 1024) +
                                "\n\n_(експорт обрізано: чат завеликий)_\n";
                    }
                    Tools.writeAtomic(f, text.getBytes(StandardCharsets.UTF_8));
                    o.put("e", "chat_exported");
                    o.put("path", f.getAbsolutePath());
                    o.put("bytes", f.length());
                } catch (Exception e) {
                    try { o.put("e", "error"); o.put("msg", "Export: " + e.getMessage()); } catch (Exception ignored) {}
                }
                emit(o);
            }).start();
        }


        private void viewFile(final String path, final int seq) {
            final int ep = store.epoch();
            new Thread(() -> {
                JSONObject o = new JSONObject();
                try {
                    File f = Tools.resolve(path);
                    o = Viewer.view(f);
                    o.put("e", "view");
                    if (seq > 0) o.put("seq", seq);
                } catch (Exception e) {
                    try {
                        o = new JSONObject();
                        o.put("e", "view_error");
                        o.put("path", path);
                        if (seq > 0) o.put("seq", seq);
                        o.put("msg", e.getMessage() == null ? "error" : e.getMessage());
                    } catch (Exception ignored) {}
                }
                if (store.epoch() == ep) emit(o);
            }, "funa-view").start();
        }

        private void fileInfo(final String path) {
            final int ep = store.epoch();
            new Thread(() -> {
                JSONObject o;
                try {
                    File f = Tools.resolve(path);
                    o = Viewer.info(f);
                    o.put("e", "file_info");
                    if ("image".equals(o.optString("kind")) && f.exists() && f.length() < 24L * 1024 * 1024) {
                        String th = Attach.thumbFile(f, 900);
                        if (!th.isEmpty()) o.put("thumb", th);
                    }
                } catch (Exception e) {
                    try { o = new JSONObject(); o.put("e", "file_info"); o.put("path", path); o.put("exists", false); } catch (Exception ignored) { o = new JSONObject(); }
                }
                if (store.epoch() == ep) emit(o);
            }, "funa-info").start();
        }

        private void fileThumb(final String path) {
            final int ep = store.epoch();
            new Thread(() -> {
                JSONObject o = new JSONObject();
                try {
                    File f = Tools.resolve(path);
                    o.put("e", "file_thumb");
                    o.put("path", f.getAbsolutePath());
                    String th = Attach.thumbFile(f, 1000);
                    if (th.isEmpty()) o.put("error", true); else o.put("data", th);
                } catch (Exception e) {
                    try { o.put("e", "file_thumb"); o.put("path", path); o.put("error", true); } catch (Exception ignored) {}
                }
                if (store.epoch() == ep) emit(o);
            }, "funa-thumb").start();
        }

        private void pdfMore(final String path, final int offset, final int count, final int seq) {
            final int ep = store.epoch();
            new Thread(() -> {
                JSONObject o = new JSONObject();
                try {
                    File f = Tools.resolve(path);
                    int n = Math.max(1, Math.min(8, count));
                    org.json.JSONArray pages = Viewer.pdfPages(f, offset, n);
                    o.put("e", "pdf_pages");
                    o.put("path", f.getAbsolutePath());
                    if (seq > 0) o.put("seq", seq);
                    o.put("pages", pages);
                    o.put("next", offset + pages.length());
                    o.put("total", Viewer.pdfCount(f));
                } catch (Exception e) {
                    try {
                        o.put("e", "view_error");
                        if (seq > 0) o.put("seq", seq);
                        o.put("msg", e.getMessage());
                    } catch (Exception ignored) {}
                }
                if (store.epoch() == ep) emit(o);
            }, "funa-pdf").start();
        }

        private void openWith(final String path) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            ui.post(() -> openWith(path));
            return;
        }
            try {
                File f = Tools.resolve(path);
                if (!f.isFile()) throw new Exception("not found");
                Uri u = FileProviderX.uriFor(f);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setDataAndType(u, Attach.mime(f.getName()));
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(Intent.createChooser(i, tr("Відкрити за допомогою", "Open with", "Открыть с помощью")));
            } catch (Exception e) {
                toast(tr("Немає застосунку для цього файлу", "No app can open this file", "Нет приложения для этого файла"));
            }
        }

        private void shareFile(final String path) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            ui.post(() -> shareFile(path));
            return;
        }
            try {
                File f = Tools.resolve(path);
                if (!f.isFile()) throw new Exception("not found");
                Uri u = FileProviderX.uriFor(f);
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType(Attach.mime(f.getName()));
                i.putExtra(Intent.EXTRA_STREAM, u);
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(i, tr("Поділитися", "Share", "Поделиться")));
            } catch (Exception e) {
                toast(tr("Не вдалося поділитися", "Can't share this file", "Не удалось поделиться"));
            }
        }

        private void saveFile(final String path) {
            new Thread(() -> {
                JSONObject o = new JSONObject();
                try {
                    File f = Tools.resolve(path);
                    if (!f.isFile()) throw new Exception("not found");
                    File dir = new File(Tools.ROOT, "exports");
                    if (!dir.exists()) dir.mkdirs();
                    File out = new File(dir, f.getName());
                    int n = 1;
                    // якщо файл уже експортовано — не перезаписуємо й не
                    // вдаємо успіх для чужого вмісту з тим самим розміром
                    if (!out.getCanonicalPath().equals(f.getCanonicalPath())) {
                        int dot = f.getName().lastIndexOf('.');
                        String base = dot > 0 ? f.getName().substring(0, dot) : f.getName();
                        String ext = dot > 0 ? f.getName().substring(dot) : "";
                        while (out.exists()) {
                            out = new File(dir, base + "-" + n + ext);
                            n++;
                        }
                    }
                    if (!out.exists()) {
                        try (java.io.FileInputStream in = new java.io.FileInputStream(f);
                             java.io.FileOutputStream os = new java.io.FileOutputStream(out)) {
                            byte[] buf = new byte[32768];
                            int r;
                            while ((r = in.read(buf)) > 0) os.write(buf, 0, r);
                        }
                    }
                    o.put("e", "file_saved");
                    o.put("path", out.getAbsolutePath());
                    o.put("name", out.getName());
                } catch (Exception e) {
                    try { o.put("e", "error"); o.put("msg", tr("Не вдалося зберегти файл", "Can't save the file", "Не удалось сохранить файл") + ": " + e.getMessage()); } catch (Exception ignored) {}
                }
                emit(o);
            }, "funa-save").start();
        }

        /**
         * Тека збережених сторінок активного акаунта. Раніше вона була
         * спільною: під замком чужі артефакти читалися б без пароля, а
         * після перемикання акаунта — показувалися б іншому користувачу.
         * Старі файли з кореня pages/ забирає перший акаунт, який їх відкриє.
         */
        private File pagesDir() {
            return pagesDirFor(store.activeId());
        }

        private File pagesDirFor(String owner) {
            File root = new File(Tools.ROOT, "pages");
            if (owner == null || owner.isEmpty()) return root;
            // id акаунта завжди hex від Store; чужий рядок не стає шляхом
            if (!owner.matches("[A-Za-z0-9_-]{1,64}")) return root;
            File dir = new File(root, owner);
            synchronized (PAGES_LOCK) {
                // mkdirs()===true означає, що теку створили саме ми: лише
                // тоді забираємо легасі-файли, інакше два акаунти билися б
                // за них під час першого відкриття.
                if (dir.mkdirs()) {
                    File[] fs = root.listFiles();
                    if (fs != null) {
                        for (File f : fs) {
                            if (!f.isFile()) continue;
                            File dst = new File(dir, f.getName());
                            if (!dst.exists()) f.renameTo(dst);
                        }
                    }
                }
            }
            return dir;
        }

        private void pagesList() {
            new Thread(() -> {
                JSONObject o = new JSONObject();
                JSONArray arr = new JSONArray();
                try {
                    File dir = pagesDir();
                    File[] fs = dir.listFiles();
                    if (fs != null) {
                        java.util.Arrays.sort(fs, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
                        int shown = 0;
                        for (File f : fs) {
                            if (!f.isFile()) continue;
                            if (++shown > 500) break;
                            JSONObject it = new JSONObject();
                            it.put("name", f.getName());
                            it.put("size", f.length());
                            it.put("time", f.lastModified() / 1000L);
                            arr.put(it);
                        }
                    }
                    o.put("e", "pages");
                    o.put("items", arr);
                } catch (Exception e) {
                    try { o.put("e", "error"); o.put("msg", "Pages: " + e.getMessage()); } catch (Exception ignored) {}
                }
                emit(o);
            }).start();
        }

        private void pageRead(String name) {
            new Thread(() -> {
                JSONObject o = new JSONObject();
                try {
                    File base = pagesDir().getCanonicalFile();
                    File f = new File(base, name);
                    if (!f.exists() || !(f.getCanonicalPath().equals(base.getPath())
                            || f.getCanonicalPath().startsWith(base.getPath() + File.separator))) {
                        throw new Exception("not found");
                    }
                    byte[] buf = new byte[(int) Math.min(f.length(), 4 * 1024 * 1024)];
                    int off = 0;
                    try (java.io.FileInputStream in = new java.io.FileInputStream(f)) {
                        // один read() не зобовʼязаний віддати весь файл
                        while (off < buf.length) {
                            int n = in.read(buf, off, buf.length - off);
                            if (n <= 0) break;
                            off += n;
                        }
                    }
                    o.put("e", "page");
                    o.put("name", f.getName());
                    o.put("content", new String(buf, 0, off, StandardCharsets.UTF_8));
                } catch (Exception e) {
                    try { o.put("e", "error"); o.put("msg", "Page: " + e.getMessage()); } catch (Exception ignored) {}
                }
                emit(o);
            }).start();
        }

        private void pageDel(String name) {
            new Thread(() -> {
                try {
                    File base = pagesDir().getCanonicalFile();
                    File f = new File(base, name);
                    String cp = f.getCanonicalPath();
                    // видаляємо лише файл усередині pages — не теку й не «..»
                    if (f.isFile() && cp.startsWith(base.getPath() + File.separator)) f.delete();
                } catch (Exception ignored) {}
                pagesList();
            }).start();
        }

        private List<String> fileIds(JSONObject c) {
            List<String> ids = new ArrayList<>();
            JSONArray a = c.optJSONArray("files");
            if (a == null) return ids;
            for (int i = 0; i < a.length() && ids.size() < 40; i++) {
                String id = a.optString(i, "");
                if (!id.isEmpty() && !ids.contains(id)) ids.add(id);
            }
            return ids;
        }

        /**
         * Одно сообщение сверх этой длины уже не контекст, а свалка:
         * режем, чтобы JSON-тело и WebView не упирались в память.
         */
        private String capInput(String text) {
            if (text == null) return "";
            // 1M символів ≈ 300k токенів — запас під ліміт 1M, тіло ≤ 8 МБ.
            if (text.length() <= 1000000) return text;
            toast(tr("Повідомлення обрізано до 1 000 000 символів",
                    "Message trimmed to 1,000,000 characters",
                    "Сообщение обрезано до 1 000 000 символов"));
            return text.substring(0, 1000000) + "\n… (обрізано)";
        }

        private void send(JSONObject c) {
            String text = capInput(c.optString("text", ""));
            List<String> ids = fileIds(c);
            if ((text == null || text.trim().isEmpty()) && ids.isEmpty()) return;
            if (agent.busy()) { toast(tr("Вже відповідаю…", "Already answering…", "Уже отвечаю…")); idle(); return; }
            // История ещё грузится: sessionId/parentId относятся к прошлому
            // чату, и сообщение ушло бы не туда. Пусть пользователь подождёт.
            if (openingId != null) {
                toast(tr("Чат ще завантажується — секунду…", "The chat is still loading — one moment…",
                        "Чат ещё загружается — секунду…"));
                idle();
                return;
            }
            // якщо історія іншого чату ще вантажиться, вона не повинна
            // «стрибнути» під уже надіслане повідомлення
            cancelOpen();
            store.setThinking(c.optBoolean("thinking", store.thinking()));
            store.setSearch(c.optBoolean("search", store.search()));
            String lang = store.lang();
            String prefix = store.base(lang);
            String facts = mem.promptBlock(lang, 3000);
            if (!facts.isEmpty()) prefix = prefix + "\n\n" + facts;
            summary = c.optString("summary", "").trim();
            if (summary.length() > 40000) summary = summary.substring(0, 40000) + "\n… (обрізано)";
            if (!summary.isEmpty()) {
                prefix = prefix + "\n\n[FunaContext]\n" + summary + "\n[/FunaContext]";
            }
            agent.setSession(sessionId, parentId, first);
            // старт міг не відбутися (паралельна відповідь/закритий агент):
            // інакше UI назавжди лишився б у стані «відповідаю»
            if (!agent.send(text, prefix, ids)) {
                toast(tr("Вже відповідаю…", "Already answering…", "Уже отвечаю…"));
                idle();
            }
        }

        private void idle() {
            JSONObject o = new JSONObject();
            try { o.put("e", "busy"); o.put("on", false); } catch (Exception ignored) {}
            emit(o);
        }

        private void save(JSONObject c) {
            String lang = c.optString("lang", store.lang());
            store.setLang(lang);
            String base = c.optString("base", "");
            if (base.equals(Store.defaultBase(lang))) base = "";
            store.setBase(base);
            store.setThinking(c.optBoolean("thinking", true));
            store.setSearch(c.optBoolean("search", false));
            String model = c.optString("model", "default");
            store.setModel(model.isEmpty() ? "default" : model);
            store.setCtxLimit(c.optInt("ctxLimit", store.ctxLimit()));
            JSONObject o = new JSONObject();
            try {
                o.put("e", "saved");
                o.put("lang", lang);
                o.put("base", store.base(lang));
                o.put("baseAuto", store.baseRaw().isEmpty());
            } catch (Exception ignored) {}
            emit(o);
        }

        private void delete(String id) {
            final String tok = store.token();
            new Thread(() -> {
                try {
                    new Net(store, tok).deleteSession(id);
                    folderStore.forget(id);
                    // Видалили чат, який саме вантажився: інакше пізня
                    // історія воскресила б його id у UI. Якщо ж вантажиться
                    // інший чат — його відкриття не скасовуємо.
                    boolean wasOpening = id.equals(openingId);
                    if (wasOpening) cancelOpen();
                    if (wasOpening || id.equals(sessionId)) {
                        sessionId = null; parentId = 0; first = true;
                        agent.setSession(null, 0, true);
                    }
                    JSONObject o = new JSONObject();
                    o.put("e", "deleted");
                    o.put("id", id);
                    emit(o);
                    sessions();
                } catch (Exception e) {
                    toast(tr("Не вдалося видалити: ", "Delete failed: ", "Не удалось удалить: ") + e.getMessage());
                }
            }).start();
        }

        private void rename(String id, String title) {
            final String tok = store.token();
            new Thread(() -> {
                try {
                    new Net(store, tok).renameSession(id, title);
                    JSONObject o = new JSONObject();
                    o.put("e", "renamed");
                    o.put("id", id);
                    o.put("title", title);
                    emit(o);
                    sessions();
                } catch (Exception e) {
                    toast(tr("Не вдалося перейменувати: ", "Rename failed: ", "Не удалось переименовать: ") + e.getMessage());
                }
            }).start();
        }

        private void pin(String id, boolean on) {
            final String tok = store.token();
            new Thread(() -> {
                try {
                    new Net(store, tok).pinSession(id, on);
                    JSONObject o = new JSONObject();
                    o.put("e", "pinned");
                    o.put("id", id);
                    o.put("on", on);
                    emit(o);
                    sessions();
                } catch (Exception e) {
                    toast(tr("Не вдалося закріпити: ", "Pin failed: ", "Не удалось закрепить: ") + e.getMessage());
                }
            }).start();
        }

        private void memExport() {
            final int ep = store.epoch();
            new Thread(() -> {
                JSONObject o = new JSONObject();
                try {
                    if (store.epoch() != ep) return;
                    File dir = new File(Tools.ROOT);
                    if (!dir.exists()) dir.mkdirs();
                    File f = new File(dir, "memory.json");
                    JSONObject root = new JSONObject();
                    root.put("version", 1);
                    root.put("exported_at", System.currentTimeMillis());
                    root.put("cards", mem.list());
                    Tools.writeAtomic(f, root.toString(2).getBytes(StandardCharsets.UTF_8));
                    o.put("e", "mem_exported");
                    o.put("path", f.getAbsolutePath());
                    o.put("count", mem.count());
                } catch (Exception e) {
                    o = new JSONObject();
                    try { o.put("e", "error"); o.put("msg", "Export: " + e.getMessage()); } catch (Exception ignored) {}
                }
                if (store.epoch() == ep) emit(o);
            }).start();
        }

        private void memImport() {
            final int ep = store.epoch();
            new Thread(() -> {
                JSONObject o = new JSONObject();
                try {
                    if (store.epoch() != ep) return;
                    File f = new File(Tools.ROOT, "memory.json");
                    if (!f.exists()) throw new Exception("no memory.json in " + Tools.ROOT);
                    if (f.length() > 8L * 1024 * 1024) throw new Exception("memory.json too big (max 8 MB)");
                    byte[] data = new byte[(int) f.length()];
                    try (java.io.FileInputStream in = new java.io.FileInputStream(f)) {
                        int off = 0;
                        while (off < data.length) {
                            int n = in.read(data, off, data.length - off);
                            if (n <= 0) break;
                            off += n;
                        }
                        if (off != data.length) data = java.util.Arrays.copyOf(data, off);
                    }
                    JSONObject root = new JSONObject(new String(data, StandardCharsets.UTF_8));
                    JSONArray cards = root.optJSONArray("cards");
                    int n = 0;
                    long chars = 0;
                    if (cards != null) {
                        for (int i = 0; i < cards.length() && n < 2000; i++) {
                            // импорт не должен «переехать» в другой аккаунт
                            if (store.epoch() != ep) return;
                            JSONObject c2 = cards.optJSONObject(i);
                            if (c2 == null) continue;
                            String body = c2.optString("body", "");
                            // 2000 карток по 20 КБ — це 40 МБ у базі; тримаємо
                            // сумарний обсяг імпорту в межах 5 МБ тексту
                            chars += body.length() + c2.optString("title", "").length();
                            if (chars > 5L * 1024 * 1024) break;
                            mem.save(0, c2.optString("title", ""), body, c2.optString("tags", ""));
                            n++;
                        }
                    }
                    o.put("e", "mem_imported");
                    o.put("count", n);
                } catch (Exception e) {
                    o = new JSONObject();
                    try { o.put("e", "error"); o.put("msg", "Import: " + e.getMessage()); } catch (Exception ignored) {}
                }
                if (store.epoch() != ep) return;
                emit(o);
                memList();
            }).start();
        }

        private void accountsList() {
            JSONObject o = new JSONObject();
            try {
                o.put("e", "accounts");
                o.put("items", store.accountSummary());
                o.put("activeId", store.activeId());
                o.put("signedIn", !store.token().isEmpty());
            } catch (Exception ignored) {}
            emit(o);
        }

        /** Додає акаунт лише після перевірки токена на сервері. */
        private void accountAdd(final String token, final String src) {
            if (token == null || token.trim().isEmpty()) {
                toast(tr("Токен порожній", "Token is empty", "Токен пустой"));
                return;
            }
            final String tk = token.trim();
            if (!tokenLike(tk)) {
                toast(tr("Це не схоже на токен DeepSeek", "That does not look like a DeepSeek token",
                        "Это не похоже на токен DeepSeek"));
                return;
            }
            new Thread(() -> {
                JSONObject r = new JSONObject();
                try {
                  try {
                    JSONObject p = new Net(store, tk).profile();
                    JSONObject u = p.optJSONObject("user");
                    if (u == null) u = p;
                    String email = u.optString("email", "");
                    String phone = u.optString("mobile_number", "");
                    String name = u.optString("name", "");
                    String uid = u.optString("id", "");
                    String id = store.addAccount(tk, uid, email, phone, name, src);
                    if (id == null) {
                        r.put("ok", false);
                        r.put("msg", tr("Не вдалося зберегти акаунт (сховище або ліміт 30 акаунтів)",
                                "Could not save the account (storage or the 30-account limit)",
                                "Не удалось сохранить аккаунт (хранилище или лимит 30 аккаунтов)"));
                    } else {
                        auth.consumeReset();
                        auth.afterLogin("");
                        r.put("ok", true);
                        r.put("id", id);
                    }
                } catch (Net.ApiException e) {
                    r.put("ok", false);
                    r.put("code", "bad_token");
                    r.put("msg", tr("Токен не підійшов", "The token was rejected", "Токен не подошёл")
                            + " (" + e.code + ")");
                } catch (Exception e) {
                    r.put("ok", false);
                    r.put("code", "net");
                    r.put("msg", tr("Немає зв'язку з DeepSeek", "No connection to DeepSeek",
                            "Нет связи с DeepSeek"));
                  }
                } catch (Exception ignored) {
                }
                try { r.put("e", "acc_result"); } catch (Exception ignored) {}
                emit(r);
                if (r.optBoolean("ok", false)) {
                    Log.add("account", "added (" + src + ")");
                    afterAccountChange();
                }
            }, "funa-acc-add").start();
        }

        /** Перевіряє токен акаунта (за id) або активного. */
        private void accountCheck(String want) {
            final String id = want == null || want.isEmpty() ? store.activeId() : want;
            if (id.isEmpty() || store.token(id).isEmpty()) {
                toast(tr("Немає такого акаунта", "No such account", "Нет такого аккаунта"));
                return;
            }
            new Thread(() -> {
                JSONObject o = new JSONObject();
                try {
                    o.put("e", "acc_check");
                    JSONObject p = new Net(store, store.token(id)).profile();
                    JSONObject u = p.optJSONObject("user");
                    if (u == null) u = p;
                    store.updateAccount(id, u.optString("id", ""), u.optString("email", ""),
                            u.optString("mobile_number", ""), u.optString("name", ""));
                    store.setStatus(id, "ok");
                    o.put("ok", true);
                } catch (Net.ApiException e) {
                    store.setStatus(id, "expired");
                    try {
                        o.put("ok", false);
                        o.put("code", e.code);
                        o.put("msg", tr("Токен більше не діє — увійди знову",
                                "The token no longer works — sign in again",
                                "Токен больше не действует — войди снова") + " (" + e.code + ")");
                    } catch (Exception ignored) {}
                } catch (Exception e) {
                    try {
                        o.put("ok", false);
                        o.put("msg", tr("Немає зв'язку з DeepSeek", "No connection to DeepSeek",
                                "Нет связи с DeepSeek"));
                    } catch (Exception ignored) {}
                }
                emit(o);
                accountsList();
            }, "funa-acc-check").start();
        }

        private void accountUse(String id, int index) {
            if (id == null || id.isEmpty()) {
                JSONObject o = store.accountSummary().optJSONObject(index);
                id = o == null ? "" : o.optString("id", "");
            }
            if (id.isEmpty()) return;
            if (id.equals(store.activeId())) return;
            if (store.token(id).isEmpty()) {
                toast(tr("Токен цього акаунта недоступний — увійди знову",
                        "This account token is unavailable — sign in again",
                        "Токен этого аккаунта недоступен — войди снова"));
                store.setStatus(id, "expired");
                accountsList();
                return;
            }
            if (agent.busy()) {
                toast(tr("Спочатку зупини відповідь", "Stop the current answer first",
                        "Сначала останови ответ"));
                return;
            }
            if (!store.switchAccount(id)) return;
            agent.stop();
            SubTask.stopAll();
            // Сесію не закриваємо: користувач уже розблокував застосунок.
            auth.normalizeLock();
            Log.add("account", "switched");
            afterAccountChange();
            authEmit(auth.state(), "switch");
        }

        private void accountDelete(String id, int index) {
            if (id == null || id.isEmpty()) {
                JSONObject o = store.accountSummary().optJSONObject(index);
                id = o == null ? "" : o.optString("id", "");
            }
            if (id.isEmpty()) return;
            boolean wasActive = id.equals(store.activeId());
            if (wasActive && agent.busy()) agent.stop();
            if (wasActive) SubTask.stopAll();
            // Локальні дані акаунта зникають разом із ним: інакше памʼять,
            // папки й прапорці блокування лишалися б у сховищі назавжди.
            // id випадковий і не перевикористовується, тож явний purge
            // безпечний навіть якщо акаунт був активним.
            auth.purge(id);
            mem.purge(id);
            folderStore.purge(id);
            // id акаунта — hex від Store; усе інше не має шансу стати шляхом
            if (id.matches("[A-Za-z0-9_-]{1,64}")) deleteTree(new File(Tools.ROOT, "pages/" + id));
            store.deleteAccount(id);
            auth.normalizeLock();
            Log.add("account", "removed");
            JSONObject del = new JSONObject();
            try { del.put("e", "account_deleted"); del.put("id", id); } catch (Exception ignored) {}
            emit(del);
            if (wasActive) {
                afterAccountChange();
            } else {
                // Видалення чужого акаунта не має скидати поточний чат
                // і перезапускати агента: активний акаунт не змінився.
                accountsList();
                authEmit(auth.state(), "state");
            }
        }

        /** Новий агент із токеном поточного акаунта. */
        private final Object AGENT_LOCK = new Object();

        private void resetAgent() {
            // Викликається і з фонових потоків (імпорт/перемикання акаунта):
            // два одночасні reset не мають лишити «сирітський» агент.
            synchronized (AGENT_LOCK) {
                Agent old = agent;
                // Старий потік міг ще стрімити: після close() він більше не
                // пише в UI, а фонові субагенти тримають Net зі старим токеном.
                if (old != null) old.close();
                SubTask.stopAll();
                cancelOpen();
                Agent next = new Agent(store, MainActivity.this::emit);
                next.setStateListener((sid, parent, fst) -> {
                    sessionId = sid; parentId = parent; first = fst;
                });
                agent = next;
            }
        }

        /** Спільне оновлення UI після зміни активного акаунта. */
        private void afterAccountChange() {
            // Агент фіксує токен на момент створення — після зміни акаунта
            // його треба перезібрати, інакше відповіді підуть від старого.
            resetAgent();
            idle();
            JSONObject o = new JSONObject();
            try {
                o.put("e", "account_switched");
                o.put("signedIn", !store.token().isEmpty());
                o.put("locked", !auth.unlocked());
                // UI має одразу знати новий акаунт: інакше «clean» нижче
                // встигає завантажити чернетку старого акаунта
                o.put("activeId", store.activeId());
            } catch (Exception ignored) {}
            emit(o);
            // accountsList() отдаёт email/phone/uid всех аккаунтов — под
            // замком он должен ждать разблокировки; после неё UI сам
            // запросит «accounts».
            if (auth.unlocked()) accountsList();
            guardState();
            // Поки екран заблокований, чужі чати, памʼять і чернетки не
            // вантажимо: інакше вони лежали б у DOM під оверлеєм, а після
            // збою WebView ще й перезавантажилися б без пароля.
            if (auth.unlocked()) {
                newChat(false);
                foldersList();
                memList();
                if (!store.token().isEmpty()) {
                    sessions();
                    whoami();
                }
            } else {
                resetChatState();
            }
        }

        private void foldersList() {
            JSONObject o = new JSONObject();
            try {
                o.put("e", "folders");
                o.put("items", folderStore.list());
                o.put("map", folderStore.map());
            } catch (Exception ignored) {}
            emit(o);
        }

        private void folderAdd(String name) {
            if (name == null || name.trim().isEmpty()) return;
            name = name.trim();
            if (name.length() > 60) name = name.substring(0, 60);
            folderStore.add(name);
            foldersList();
        }

        private void folderRename(String id, String name) {
            if (id == null || id.isEmpty()) return;
            if (name == null) name = "";
            name = name.trim();
            if (name.length() > 60) name = name.substring(0, 60);
            folderStore.rename(id, name);
            foldersList();
        }

        private void folderDelete(String id) {
            if (id == null || id.isEmpty()) return;
            folderStore.remove(id);
            foldersList();
        }

        private void folderMove(String sid, String fid) {
            if (sid == null || sid.isEmpty()) return;
            folderStore.move(sid, fid);
            foldersList();
        }

        private void saveHtml(String name, String content) {
            // Власника фіксуємо до старту потоку: інакше сторінка,
            // збережена під час перемикання акаунта, лягла б у чужу теку.
            final String owner = store.activeId();
            final int ep = store.epoch();
            new Thread(() -> {
                JSONObject o = new JSONObject();
                try {
                    File dir = pagesDirFor(owner);
                    if (!dir.exists()) dir.mkdirs();
                    String safe = (name == null ? "" : name).replaceAll("[^A-Za-z0-9._-]", "");
                    // «.» / «..» — не імʼя сторінки; і довжина теж має межу
                    safe = safe.replaceAll("^[.]+", "");
                    if (safe.length() > 120) safe = safe.substring(0, 120);
                    if (safe.isEmpty()) safe = "page-" + System.currentTimeMillis() + ".html";
                    if (!safe.toLowerCase().endsWith(".html")) safe += ".html";
                    // атомарне вільне імʼя: два збереження поспіль не зіллються
                    File f = uniqueFile(dir, safe);
                    String body = content == null ? "" : content;
                    // HTML-артефакт більший за 4 МБ — це вже не сторінка
                    if (body.length() > 4 * 1024 * 1024) body = body.substring(0, 4 * 1024 * 1024);
                    Tools.writeAtomic(f, body.getBytes(StandardCharsets.UTF_8));
                    o.put("e", "saved_file");
                    o.put("path", f.getAbsolutePath());
                    o.put("bytes", f.length());
                } catch (Exception e) {
                    try { o.put("e", "error"); o.put("msg", "Save HTML: " + e.getMessage()); } catch (Exception ignored) {}
                }
                // Файл уже збережено у теку власника; після перемикання
                // акаунта не показуємо його шлях у чужому UI.
                if (store.epoch() == ep) emit(o);
            }).start();
        }

        private void memList() {
            final int ep = store.epoch();
            new Thread(() -> {
                JSONObject o = new JSONObject();
                try {
                    o.put("e", "mem");
                    o.put("items", mem.list());
                } catch (Exception ignored) {}
                // карточки принадлежат активному аккаунту: список старого
                // аккаунта не должен появиться после переключения
                if (store.epoch() == ep) emit(o);
            }).start();
        }

        private void memSave(JSONObject c) {
            long id = c.optLong("id", 0);
            JSONObject o = new JSONObject();
            try {
                long nid = mem.save(id,
                        c.optString("title", ""),
                        c.optString("body", ""),
                        c.optString("tags", ""));
                o.put("e", "mem_saved");
                o.put("ok", nid > 0);
                o.put("id", nid);
                if (nid <= 0) o.put("msg", tr("Не вдалося записати картку",
                        "Could not save the card", "Не удалось сохранить карточку"));
            } catch (Exception e) {
                Log.e("mem", "save failed", e);
                try {
                    o.put("e", "mem_saved");
                    o.put("ok", false);
                    o.put("msg", tr("Помилка пам'яті: ", "Memory error: ", "Ошибка памяти: ")
                            + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
                } catch (Exception ignored) {}
            }
            emit(o);
            memList();
        }

        private void memDel(long id) {
            mem.delete(id);
            JSONObject o = new JSONObject();
            try {
                o.put("e", "mem_deleted");
                o.put("id", id);
            } catch (Exception ignored) {}
            emit(o);
            memList();
        }

        private void memToggle(long id, boolean on) {
            mem.toggle(id, on);
            JSONObject o = new JSONObject();
            try {
                o.put("e", "mem_toggled");
                o.put("id", id);
                o.put("on", on);
            } catch (Exception ignored) {}
            emit(o);
        }
    }

    private void copy(final String src) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            ui.post(() -> copy(src));
            return;
        }
        String text = src == null ? "" : src;
        // Буфер обміну йде через Binder: кілька мегабайт тексту — це
        // TransactionTooLargeException прямо в UI-потоці.
        if (text.length() > 200000) {
            text = text.substring(0, 200000);
            toast(tr("Скопійовано перші 200 000 символів", "Copied the first 200,000 characters",
                    "Скопированы первые 200 000 символов"));
        } else {
            toast(tr("Скопійовано", "Copied", "Скопировано"));
        }
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm == null) return;
        try {
            cm.setPrimaryClip(ClipData.newPlainText("funa", text));
        } catch (Exception e) {
            Log.e("app", "clipboard: " + e.getMessage(), e);
        }
    }

    @Override
    protected void onDestroy() {
        Log.add("app", "stop");
        agent.close();
        SubTask.stopAll();
        // без UI сервер лишався б висіти у фоні й тримати сокет/токен.
        // Виняток — перезапуск після смерті рендерера: Activity створюється
        // заново, а зовнішній API-клієнт не повинен цього помітити.
        if (keepApiServer) Log.add("api", "kept through renderer restart");
        else ApiServer.stop();
        keepApiServer = false;
        if (web != null) {
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }
}
