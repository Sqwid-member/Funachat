package com.funa.chat;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Вхід на справжньому сайті DeepSeek у WebView.
 *
 * Пароль, капча й 2FA обробляються самим сайтом, тому немає
 * RISK_DEVICE_DETECTED і серверного ліміту спроб, який ловить нативний JSON-логін.
 * Після входу токен читається з localStorage цього ж origin (cross-origin
 * зі сторінки застосунку це неможливо, тому окремий Activity).
 */
public class SiteLogin extends Activity {
    public static final String EXTRA_TOKEN = "token";
    public static final String EXTRA_COOKIE = "cookie";
    public static final String EXTRA_INFO = "info";

    private static final String START = "https://chat.deepseek.com/sign_in";
    private static final String BG = "#0F1216";

    private WebView web;
    private volatile boolean done;
    /**
     * Одноразовий ключ сторінки DeepSeek. JS-міст видно будь-якій сторінці
     * цього WebView, тож без ключа сторонній сайт міг би підсунути свій
     * токен — і всі наступні запити користувача пішли б у чужий акаунт.
     * Ключ інжектується лише на сторінку deepseek.com і скидається при
     * кожному переході.
     */
    private volatile String nonce = "";

    private String tr(String uk, String en, String ru) {
        String l = new Store(this).lang();
        return "en".equals(l) ? en : ("ru".equals(l) ? ru : uk);
    }

    private int dp(float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                getResources().getDisplayMetrics());
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Log.add("auth", "site login opened");

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF0F1216);

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(14), dp(12), dp(8), dp(10));

        TextView title = new TextView(this);
        title.setText(tr("Вхід у DeepSeek", "Sign in to DeepSeek", "Вход в DeepSeek"));
        title.setTextColor(0xFFE6EDF3);
        title.setTextSize(17);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        bar.addView(title, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button grab = new Button(this);
        grab.setText(tr("Імпортувати", "Import", "Импортировать"));
        grab.setTextColor(0xFF7FB2FF);
        grab.setBackgroundColor(0x00000000);
        grab.setAllCaps(false);
        grab.setOnClickListener(v -> captureSession());
        bar.addView(grab);

        Button close = new Button(this);
        close.setText(tr("Закрити", "Close", "Закрыть"));
        close.setTextColor(0xFF9FB0C3);
        close.setBackgroundColor(0x00000000);
        close.setAllCaps(false);
        close.setOnClickListener(v -> finish());
        bar.addView(close);
        root.addView(bar);

        TextView hint = new TextView(this);
        hint.setText(tr("Увійди як на сайті. Пароль і код нікуди не передаються — "
                        + "застосунок забирає лише токен сесії.",
                "Sign in as on the site. The password and code are not sent anywhere — "
                        + "the app only takes the session token.",
                "Войди как на сайте. Пароль и код никуда не передаются — "
                        + "приложение забирает только токен сессии."));
        hint.setTextColor(0xFF8A99A8);
        hint.setTextSize(12.5f);
        hint.setPadding(dp(14), 0, dp(14), dp(8));
        root.addView(hint);

        final ProgressBar bar2 = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar2.setIndeterminate(true);
        bar2.setVisibility(ProgressBar.VISIBLE);
        root.addView(bar2, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(2)));

        web = new WebView(this);
        web.setBackgroundColor(0xFF0F1216);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(false);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setUseWideViewPort(false);
        s.setLoadWithOverviewMode(false);
        s.setTextZoom(100);
        s.setMediaPlaybackRequiresUserGesture(true);
        // Прибираємо позначку «wv»: деякі анти-бот фільтри ріжуть WebView.
        String ua = WebSettings.getDefaultUserAgent(this);
        if (ua != null) s.setUserAgentString(ua.replace("; wv", "").replace(" Version/4.0", ""));

        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        cm.setAcceptThirdPartyCookies(web, true);

        web.addJavascriptInterface(new Bridge(), "FunaSite");

        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageStarted(WebView v, String url, android.graphics.Bitmap icon) {
                nonce = "";
            }

            @Override public void onPageFinished(WebView v, String url) {
                bar2.setVisibility(ProgressBar.GONE);
                inject(v, url);
            }

            @Override public boolean shouldOverrideUrlLoading(WebView v, String url) {
                if (url == null) return true;
                // http(s) лишаємо у WebView: OAuth-редирект має повернутися
                // на deepseek.com у тому самому контексті. Решта схем
                // (intent:, file:, content:) тут не потрібні.
                return !(url.startsWith("http://") || url.startsWith("https://"))
                        && !"about:blank".equals(url);
            }

            @Override public void onReceivedError(WebView v, int code, String desc, String url) {
                Log.add("auth", "site error " + code + " " + desc + " " + safeUrl(url));
            }

            @Override public void onReceivedHttpError(WebView v, android.webkit.WebResourceRequest r,
                                                      android.webkit.WebResourceResponse res) {
                if (r != null && r.isForMainFrame())
                    Log.add("auth", "site http " + res.getStatusCode() + " " + safeUrl(String.valueOf(r.getUrl())));
            }
        });

        root.addView(web, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);

        web.loadUrl(START);
        watchdog();
    }

    /**
     * URL для логу без query/fragment: у них цілком може лежати токен або
     * код підтвердження, а лог користувач надсилає нам.
     */
    private static String safeUrl(String url) {
        if (url == null) return "";
        String u = url;
        int q = u.indexOf('?');
        if (q >= 0) u = u.substring(0, q) + "?…";
        int h = u.indexOf('#');
        if (h >= 0) u = u.substring(0, h) + "#…";
        return u.length() > 200 ? u.substring(0, 200) + "…" : u;
    }

    /** Діагностика: раз на 4 с пише, що зараз на сторінці (без значень токена). */
    private void watchdog() {
        final android.os.Handler h = new android.os.Handler(getMainLooper());
        final int[] beat = {0};
        h.postDelayed(new Runnable() {
            @Override public void run() {
                if (done || web == null || beat[0]++ > 15) return;
                web.evaluateJavascript(
                        "(function(){try{var ks=[];for(var i=0;i<localStorage.length;i++)ks.push(localStorage.key(i));" +
                        "return JSON.stringify({u:location.pathname,ls:ks.slice(0,12),n:localStorage.length,ck:document.cookie.length});}" +
                        "catch(e){return 'err';}})()",
                        value -> Log.add("auth", "site state " + value));
                h.postDelayed(this, 4000);
            }
        }, 5000);
    }

    /**
     * Шукає токен сесії в localStorage цього ж origin. Спершу відомі ключі,
     * далі — будь-яке значення з JWT-подібним рядком (на випадок, якщо
     * DeepSeek перейменує ключ).
     */
    private static final String HOOK =
            "(function(){if(window.__funaTok)return;window.__funaTok=1;" +
            "var n=0;" +
            "function jwt(s){if(!s)return'';var m=String(s).match(/eyJ[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}/);return m?m[0]:'';}" +
            "function pick(){try{" +
            "var keys=['userToken','user_token','token','accessToken','deepseek_token','ds_token'];" +
            "function plain(v){if(!v||v.length<=20)return'';for(var q=0;q<v.length;q++){var n=v.charCodeAt(q);" +
            "if(n<=32||n===34||n===123||n===125||n===91||n===93||n===92)return'';}return v;}" +
            "for(var i=0;i<keys.length;i++){var raw=localStorage.getItem(keys[i]);if(!raw)continue;" +
            "var t='';try{var o=JSON.parse(raw);t=plain((o&&o.value)||(o&&o.token)||'');}catch(e){}" +
            "if(!t)t=jwt(raw);if(t)return[t,raw];}" +
            "for(var j=0;j<localStorage.length;j++){var k=localStorage.key(j);var v=localStorage.getItem(k);" +
            "if(!v)continue;var t2='';try{var o2=JSON.parse(v);t2=jwt((o2&&o2.value)||(o2&&o2.token)||'');}catch(e){}" +
            "if(!t2)t2=jwt(v);if(t2)return[t2,v];}}catch(e){}return null;}" +
            "var authed=0;" +
            "function fromHeader(v){if(!v)return;var t=jwt(String(v));if(t){authed=1;FunaSite.token(t,'header',window.__funaN||'');}}" +
            "try{var so=XMLHttpRequest.prototype.setRequestHeader;" +
            "XMLHttpRequest.prototype.setRequestHeader=function(k,v){try{" +
            "if(String(k).toLowerCase()==='authorization')fromHeader(v);}catch(e){}return so.apply(this,arguments);};}catch(e){}" +
            "try{var sf=window.fetch;if(sf){window.fetch=function(i,o){try{" +
            "var h=o&&o.headers;if(h){if(typeof h.get==='function')fromHeader(h.get('authorization'));" +
            "else if(Array.isArray(h)){for(var a=0;a<h.length;a++)if(String(h[a][0]).toLowerCase()==='authorization')fromHeader(h[a][1]);}" +
            "else{for(var k2 in h)if(String(k2).toLowerCase()==='authorization')fromHeader(h[k2]);}}}" +
            "catch(e){}return sf.apply(this,arguments);};}}catch(e){}" +
            "var iv=setInterval(function(){n++;if(n>2400){clearInterval(iv);return;}" +
            // на сторінці входу старий токен із localStorage не чіпаємо: інакше
            // вікно закриється з простроченою сесією ще до входу
            "var onAuth=/sign_?in|sign_?up|log_?in|register/i.test(location.pathname);" +
            "var p=(authed||!onAuth)?pick():null;if(p){clearInterval(iv);FunaSite.token(p[0],p[1],window.__funaN||'');}},600);" +
            "})();";

    /** Тільки справжній хост DeepSeek: "evil.com/?x=deepseek.com" не рахується. */
    private static boolean isDeepSeek(String url) {
        try {
            String h = Uri.parse(url).getHost();
            if (h == null) return false;
            h = h.toLowerCase(java.util.Locale.US);
            return h.equals("deepseek.com") || h.endsWith(".deepseek.com");
        } catch (Exception e) {
            return false;
        }
    }

    private void inject(WebView v, String url) {
        if (!isDeepSeek(url)) return;
        // Ключ живе лише в цій сторінці: зовнішній сайт у тому ж WebView
        // не може прочитати змінну чужого origin, тому його виклики
        // FunaSite.token(...) відхиляються.
        nonce = java.util.UUID.randomUUID().toString().replace("-", "");
        v.evaluateJavascript("window.__funaN='" + nonce + "';" + HOOK, null);
    }

    private String currentCookie() {
        try {
            String c = CookieManager.getInstance().getCookie("https://chat.deepseek.com");
            if (c == null) return "";
            if (c.length() > 16384) c = c.substring(0, 16384);
            return c;
        } catch (Exception e) {
            return "";
        }
    }

    private static String cookieNames(String cookie) {
        StringBuilder sb = new StringBuilder();
        if (cookie == null) return "";
        for (String p : cookie.split(";")) {
            int i = p.indexOf('=');
            if (i > 0) {
                String n = p.substring(0, i).trim();
                if (!n.isEmpty()) sb.append(n).append(' ');
            }
        }
        return sb.toString().trim();
    }

    /** Забирає поточну сесію: токен із сторінки (якщо є) + cookie. */
    private void captureSession() {
        if (web == null) return;
        if (!isDeepSeek(web.getUrl())) {
            Log.add("auth", "site import refused: not a deepseek page");
            android.widget.Toast.makeText(this,
                    tr("Спершу відкрий сторінку DeepSeek", "Open the DeepSeek page first",
                            "Сначала открой страницу DeepSeek"),
                    android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        final String ck = currentCookie();
        Log.add("auth", "site cookies: " + cookieNames(ck));
        web.evaluateJavascript(
                "(function(){try{" +
                "function val(raw){try{var o=JSON.parse(raw);return (o&&o.value)||(o&&o.token)||'';}catch(e){return raw;}}" +
                "var ks=['userToken','user_token','token','accessToken'];" +
                "for(var i=0;i<ks.length;i++){var v=localStorage.getItem(ks[i]);" +
                "if(v){var t=val(v);if(t&&t.length>20)return t;}}" +
                "for(var j=0;j<localStorage.length;j++){var k=localStorage.key(j);var v2=localStorage.getItem(k);" +
                "if(!v2||k.indexOf('__')===0)continue;var t2=val(v2);" +
                "if(t2&&t2.length>40&&!/\\s/.test(t2)&&/^[A-Za-z0-9_.-]+$/.test(t2))return t2;}" +
                "return '';}catch(e){return '';}})()",
                v -> {
                    String raw = v == null ? "" : v.trim();
                    if (raw.length() >= 2 && raw.charAt(0) == '"' && raw.endsWith("\""))
                        raw = raw.substring(1, raw.length() - 1);
                    finishWith(raw, "localStorage", ck);
                });
    }

    /** Прибирає JSON-обгортку й лапки; повертає "" якщо це не схоже на токен. */
    public static String cleanTok(String s) {
        if (s == null) return "";
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() > 2)
            s = s.substring(1, s.length() - 1).replace("\\\"", "\"").replace("\\\\", "\\");
        if (s.startsWith("{")) {
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("\"value\"\\s*:\\s*\"([^\"]+)\"").matcher(s);
            if (m.find()) s = m.group(1);
        }
        if (s.isEmpty() || s.length() > 4096) return "";
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c <= ' ' || c == '"' || c == '{' || c == '}' || c == '[' || c == ']') return "";
        }
        return s;
    }

    private void finishWith(String token, String info) {
        finishWith(token, info, currentCookie());
    }

    private void finishWith(String token, String info, String cookie) {
        if (done) return;
        token = cleanTok(token);
        if (token.length() < 20) token = "";
        done = true;
        Log.add("auth", "site session captured token=" + (token == null ? 0 : token.length())
                + " cookie=" + (cookie == null ? 0 : cookie.length())
                + " from=" + ("header".equals(info) ? "request header" : "localStorage"));
        Intent out = new Intent();
        out.putExtra(EXTRA_TOKEN, token == null ? "" : token);
        out.putExtra(EXTRA_COOKIE, cookie == null ? "" : cookie);
        if (info != null) out.putExtra(EXTRA_INFO, info);
        setResult(RESULT_OK, out);
        finish();
    }

    public class Bridge {
        @JavascriptInterface public void token(final String tok, final String info, final String key) {
            if (tok == null) return;
            final String n = nonce;
            // ключ має збігатися з інжектованим саме в поточну сторінку
            if (n.isEmpty() || key == null || !n.equals(key)) {
                Log.add("auth", "site token call rejected: foreign page");
                return;
            }
            String t = tok.trim();
            // Токен приходить із JS-контексту сторінки: обмежуємо довжину
            // й відсіюємо керівні символи, щоб у сховище не потрапило сміття.
            if (t.length() < 20 || t.length() > 8192) return;
            for (int i = 0; i < t.length(); i++) {
                char c = t.charAt(i);
                if (c <= ' ' || c == '"' || c == '{' || c == '}') return;
            }
            final String clean = t;
            runOnUiThread(() -> {
                // сторінка могла вже піти на інший домен, поки йшов виклик
                String u = web == null ? "" : web.getUrl();
                if (!isDeepSeek(u)) {
                    Log.add("auth", "site token call rejected: left deepseek");
                    return;
                }
                finishWith(clean, info, currentCookie());
            });
        }
    }

    @Override public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (web != null) {
            try {
                web.removeJavascriptInterface("FunaSite");
                web.loadUrl("about:blank");
                web.destroy();
            } catch (Exception ignored) {}
            web = null;
        }
        super.onDestroy();
    }
}
