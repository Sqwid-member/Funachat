package com.funa.chat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class Net {
    public static final String BASE = "https://chat.deepseek.com";
    /** Локальний reverse-proxy у Termux: працює, навіть коли DNS у застосунку зламаний. */
    public static final String PROXY = "http://127.0.0.1:8898";
    private static volatile long proxyCheckedAt;
    private static volatile boolean proxyOk;
    private final Store st;
    /** Перекриває модель із налаштувань (використовує локальний API). */
    public String modelOverride;
    /** Активний SSE-запит — щоб субагент можна було зупинити з картки. */
    private volatile HttpURLConnection active;
    /**
     * Після cancel() цей Net більше не має права починати запити. Інакше
     * «Стоп» під час паузи Guard (sleep до 85+ с) пропускався б, і агент
     * усе одно надіслав би запит уже після зупинки.
     */
    private volatile boolean cancelled;

    /** Токен фіксується на момент створення: перемикання акаунта не змінює запит. */
    private final String token;

    public Net(Store s) { st = s; token = s.token(); }

    /** Мережа з явним токеном — для перевірки акаунта до його збереження. */
    public Net(Store s, String token) { st = s; this.token = token == null ? "" : token; }

    /** Авторизація cookie-сесією сайту замість Bearer-токена. */
    public Net asCookie() { cookieMode = true; return this; }

    /** Розриває поточний стрім (якщо є). */
    public void cancel() {
        cancelled = true;
        HttpURLConnection c = active;
        if (c == null) return;
        try { c.disconnect(); } catch (Exception ignored) {}
    }

    private static boolean proxyUp() {
        long now = System.currentTimeMillis();
        if (now - proxyCheckedAt < 30000) return proxyOk;
        boolean ok = false;
        try (Socket sock = new Socket()) {
            sock.connect(new InetSocketAddress("127.0.0.1", 8898), 250);
            ok = true;
        } catch (IOException ignored) {}
        proxyOk = ok;
        proxyCheckedAt = now;
        return ok;
    }

    static String base() {
        return proxyUp() ? PROXY : BASE;
    }

    /** Модель для заголовків x-model-type. */
    public String modelType() {
        return modelOverride == null || modelOverride.isEmpty() ? st.model() : modelOverride;
    }

    public static class ApiException extends IOException {
        public final int code;
        public ApiException(int code, String msg) { super(msg); this.code = code; }
    }

    private boolean cookieMode = false;
    /** Результат пошуку режиму в Store: один Net — один пошук, не на кожен запит. */
    private volatile Boolean modeCookie;

    private HttpURLConnection open(String path, String method, boolean stream) throws IOException {
        if (cancelled) throw new ApiException(0, "Скасовано");
        String b = base();
        URL u = new URL(b + path);
        // Захист від підміни хоста шляхом: "//evil", "@evil.com" тощо
        // склеїлися б із базою в чужий URL — і Bearer-токен пішов би туди.
        String want = new URL(b).getHost();
        if (!want.equalsIgnoreCase(u.getHost()))
            throw new IOException("host mismatch: " + u.getHost());
        HttpURLConnection c = (HttpURLConnection) u.openConnection();
        c.setRequestMethod(method);
        c.setConnectTimeout(20000);
        c.setReadTimeout(stream ? 300000 : 45000);
        // Токен іде в заголовок: CR/LF там заборонені (і це єдиний спосіб
        // зламати заголовок), тому чистимо на місці.
        String tok = token == null ? "" : token.replace("\r", "").replace("\n", "");
        boolean asCookie = cookieMode;
        if (!asCookie) {
            Boolean cached = modeCookie;
            if (cached == null) {
                cached = "cookie".equals(st.accountMode(tok));
                modeCookie = cached;
            }
            asCookie = cached;
        }
        if (asCookie) c.setRequestProperty("Cookie", tok);
        else c.setRequestProperty("Authorization", "Bearer " + tok);
        c.setRequestProperty("Accept", stream ? "text/event-stream" : "application/json");
        if (!"GET".equals(method)) c.setRequestProperty("Content-Type", "application/json");
        c.setRequestProperty("x-client-platform", "android");
        c.setRequestProperty("x-client-version", st.clientVersion());
        c.setRequestProperty("x-client-bundle-id", "com.deepseek.chat");
        c.setRequestProperty("x-client-locale", st.clientLocale());
        c.setRequestProperty("x-client-timezone-offset", st.clientTz());
        c.setRequestProperty("x-device-model", st.device());
        c.setRequestProperty("User-Agent", "okhttp/4.12.0");
        c.setUseCaches(false);
        return c;
    }

    public JSONObject request(String path, String method, JSONObject body, boolean stream) throws IOException {
        HttpURLConnection c = open(path, method, stream);
        try {
            if (body != null) {
                c.setDoOutput(true);
                byte[] data = body.toString().getBytes(StandardCharsets.UTF_8);
                c.setFixedLengthStreamingMode(data.length);
                try (OutputStream os = c.getOutputStream()) { os.write(data); }
            }
            int status = c.getResponseCode();
            InputStream in = status >= 400 ? c.getErrorStream() : c.getInputStream();
            String text = readAll(in);
            if (text == null || text.isEmpty()) {
                if (status >= 400) throw new ApiException(status, "HTTP " + status);
                return new JSONObject();
            }
            JSONObject j;
            try { j = new JSONObject(text); }
            catch (JSONException e) { throw new ApiException(status, text.length() > 300 ? text.substring(0, 300) : text); }
            JSONObject data = j.optJSONObject("data");
            int code = j.optInt("code", 0);
            int bcode = data == null ? 0 : data.optInt("biz_code", 0);
            if (status >= 400 || code != 0 || bcode != 0) {
                // Для HTTP-помилок беремо саме статус: у тілі може бути свій
                // biz-код (40003 тощо), і тоді 401/429 губились — локальний API
                // віддавав 502 замість 401, а Guard не ставив паузу.
                // Глобальний код DeepSeek (40303/40304 = hCaptcha) важливіший
                // за статус 200: без нього перевірка виглядала як успіх.
                int ec = code != 0 ? code : bcode;
                if (ec == 0) ec = status;
                String msg = data != null && !data.optString("biz_msg", "").isEmpty()
                        ? data.optString("biz_msg") : j.optString("msg", "HTTP " + status);
                throw new ApiException(ec != 0 ? ec : status, msg);
            }
            return j;
        } finally {
            c.disconnect();
        }
    }

    public JSONObject biz(JSONObject j) throws IOException {
        JSONObject data = j.optJSONObject("data");
        if (data == null) return new JSONObject();
        int bc = data.optInt("biz_code", 0);
        if (bc != 0) throw new ApiException(bc, data.optString("biz_msg", "biz error " + bc));
        JSONObject bd = data.optJSONObject("biz_data");
        return bd == null ? new JSONObject() : bd;
    }

    public JSONObject get(String path) throws IOException { return request(path, "GET", null, false); }
    public JSONObject post(String path, JSONObject body) throws IOException { return request(path, "POST", body, false); }

    public JSONObject profile() throws IOException { return biz(get("/api/v0/users/current")); }

    public JSONObject createSession() throws IOException {
        try {
            return biz(post("/api/v0/chat_session/create", new JSONObject())).getJSONObject("chat_session");
        } catch (JSONException e) {
            throw new IOException("create: " + e.getMessage());
        }
    }

    public JSONArray sessions() throws IOException {
        JSONObject r = biz(get("/api/v0/chat_session/fetch_page"));
        JSONArray a = r.optJSONArray("chat_sessions");
        return a == null ? new JSONArray() : a;
    }

    public JSONObject history(String id) throws IOException {
        String q;
        try { q = "?chat_session_id=" + URLEncoder.encode(id, "UTF-8") + "&scenario=chat"; }
        catch (Exception e) { throw new IOException(e); }
        return biz(get("/api/v0/chat/history_messages" + q));
    }

    public void pinSession(String id, boolean on) throws IOException {
        try {
            JSONObject b = new JSONObject();
            b.put("chat_session_id", id);
            b.put("pinned", on);
            post("/api/v0/chat_session/update_pinned", b);
        } catch (JSONException e) {
            throw new IOException("pin: " + e.getMessage());
        }
    }

    public void deleteSession(String id) throws IOException {
        JSONObject b = new JSONObject();
        try { b.put("chat_session_id", id); } catch (JSONException ignored) {}
        post("/api/v0/chat_session/delete", b);
    }

    public void renameSession(String id, String title) throws IOException {
        JSONObject b = new JSONObject();
        try { b.put("chat_session_id", id); b.put("title", title); } catch (JSONException ignored) {}
        post("/api/v0/chat_session/update_title", b);
    }

    public void stopStream(String sid, long mid) throws IOException {
        try {
            JSONObject b = new JSONObject();
            b.put("chat_session_id", sid);
            b.put("message_id", mid);
            post("/api/v0/chat/stop_stream", b);
        } catch (JSONException e) {
            throw new IOException("stop: " + e.getMessage());
        }
    }

    public JSONObject challenge(String targetPath) throws IOException {
        try {
            JSONObject b = new JSONObject();
            b.put("target_path", targetPath);
            return biz(post("/api/v0/chat/create_pow_challenge", b)).getJSONObject("challenge");
        } catch (JSONException e) {
            throw new IOException("challenge: " + e.getMessage());
        }
    }

    public JSONObject powHeader(String targetPath) throws IOException {
        JSONObject ch = challenge(targetPath);
        String salt = ch.optString("salt");
        long expire = ch.optLong("expire_at");
        long diff = ch.optLong("difficulty", 144000);
        // На телефоне миллионы хешей — это минуты без отклика; штатная
        // сложность DeepSeek ~144k, поэтому потолок защищает от зависания.
        if (diff < 1) diff = 1;
        if (diff > 3000000L) diff = 3000000L;
        long answer = Pow.solve(salt, expire, ch.optString("challenge"), diff);
        if (answer < 0) throw new IOException("PoW: не знайдено розвʼязок");
        try {
            ch.put("answer", answer);
            JSONObject out = new JSONObject();
            out.put("h", android.util.Base64.encodeToString(ch.toString().getBytes(StandardCharsets.UTF_8), android.util.Base64.NO_WRAP));
            return out;
        } catch (JSONException e) {
            throw new IOException("pow: " + e.getMessage());
        }
    }


    public interface Progress { void on(int percent); }

    /** Завантаження файлу на сервер (multipart). Повертає biz_data з id/status. */
    public JSONObject uploadFile(File f, String name, String mime, boolean thinking, Progress p) throws IOException {
        final String path = "/api/v0/file/upload_file";
        JSONObject pow = powHeader(path);
        String bnd = "----Funa" + Long.toHexString(System.nanoTime());
        String fn = (name == null || name.isEmpty()) ? "file" : name
                .replace("\"", "_").replace("\r", " ").replace("\n", " ");
        String ct = (mime == null || mime.isEmpty()) ? "application/octet-stream" : mime
                .replace("\r", "").replace("\n", "").replace("\"", "");
        byte[] head = ("--" + bnd + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"" + fn + "\"\r\n"
                + "Content-Type: " + ct + "\r\n\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] tail = ("\r\n--" + bnd + "--\r\n").getBytes(StandardCharsets.UTF_8);
        long total = head.length + f.length() + tail.length;
        HttpURLConnection c = open(path, "POST", false);
        c.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + bnd);
        c.setRequestProperty("X-DS-PoW-Response", pow.optString("h"));
        c.setRequestProperty("X-Thinking-Enabled", thinking ? "1" : "0");
        c.setRequestProperty("x-file-size", String.valueOf(f.length()));
        c.setRequestProperty("x-model-type", modelType());
        c.setDoOutput(true);
        c.setFixedLengthStreamingMode(total);
        try (OutputStream os = c.getOutputStream()) {
            os.write(head);
            long sent = 0;
            int last = -1;
            try (InputStream in = new FileInputStream(f)) {
                byte[] buf = new byte[16384];
                int n;
                while ((n = in.read(buf)) > 0) {
                    os.write(buf, 0, n);
                    sent += n;
                    if (p != null) {
                        int pc = (int) (sent * 100 / Math.max(1, f.length()));
                        if (pc / 5 != last / 5) { last = pc; p.on(pc); }
                    }
                }
            }
            os.write(tail);
        }
        int status = c.getResponseCode();
        InputStream in = status >= 400 ? c.getErrorStream() : c.getInputStream();
        String text = readAll(in);
        c.disconnect();
        if (status >= 400) throw new ApiException(status, text == null ? ("HTTP " + status) : text);
        try {
            JSONObject j = new JSONObject(text == null ? "{}" : text);
            JSONObject data = j.optJSONObject("data");
            int bc = data == null ? 0 : data.optInt("biz_code", 0);
            if (j.optInt("code", 0) != 0 || bc != 0) {
                throw new ApiException(bc != 0 ? bc : 1, data == null ? j.optString("msg", "upload error") : data.optString("biz_msg", "upload error"));
            }
            JSONObject bd = data == null ? null : data.optJSONObject("biz_data");
            return bd == null ? new JSONObject() : bd;
        } catch (JSONException e) {
            throw new IOException("upload: " + e.getMessage());
        }
    }

    /** Чекає, поки сервер обробить файл (PENDING -> SUCCESS). */
    public JSONObject waitFile(String id, long timeoutMs) throws IOException {
        long end = System.currentTimeMillis() + timeoutMs;
        JSONObject last = null;
        while (System.currentTimeMillis() < end) {
            try {
                String q;
                try { q = URLEncoder.encode(id, "UTF-8"); } catch (Exception e) { throw new IOException(e); }
                JSONObject b = biz(get("/api/v0/file/fetch_files?file_ids=" + q));
                JSONArray fs = b.optJSONArray("files");
                if (fs != null) {
                    for (int i = 0; i < fs.length(); i++) {
                        JSONObject it = fs.optJSONObject(i);
                        if (it == null || !id.equals(it.optString("id"))) continue;
                        last = it;
                        String status = it.optString("status");
                        if ("SUCCESS".equals(status)) return it;
                        if ("FAILED".equals(status) || "ERROR".equals(status)) {
                            throw new IOException("File rejected: " + it.optString("error_code", status));
                        }
                    }
                }
            } catch (IOException e) {
                if (last == null) throw e;
            }
            try { Thread.sleep(800); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("interrupted");
            }
        }
        if (last != null) return last;
        throw new IOException("File processing timeout");
    }

    /** Байти файлу за підписаним шляхом (для мініатюр). */
    public byte[] fileBytes(String signedPath) throws IOException {
        if (signedPath == null || signedPath.isEmpty()) return null;
        HttpURLConnection c = open(signedPath, "GET", false);
        c.setRequestProperty("Accept", "*/*");
        try {
            int status = c.getResponseCode();
            if (status >= 400) return null;
            try (InputStream in = c.getInputStream(); ByteArrayOutputStream bo = new ByteArrayOutputStream()) {
                byte[] buf = new byte[16384];
                int n, total = 0;
                while ((n = in.read(buf)) > 0) {
                    bo.write(buf, 0, n);
                    total += n;
                    if (total > 12 * 1024 * 1024) break;
                }
                return bo.toByteArray();
            }
        } finally {
            c.disconnect();
        }
    }

    public interface Sse {
        void onData(JSONObject data);
        void onEvent(String name, JSONObject data);
    }

    /** Opens the completion stream. Returns the connection so it can be aborted. */
    public HttpURLConnection completion(JSONObject body, JSONObject pow, Sse cb) throws IOException {
        return streamRequest("/api/v0/chat/completion", body, pow, cb);
    }

    /** Будь-який SSE-ендпоінт чату (completion / edit_message / regenerate / continue). */
    public HttpURLConnection streamRequest(String path, JSONObject body, JSONObject pow, Sse cb) throws IOException {
        try {
            Guard.gate(st);
        } catch (Guard.Limited e) {
            throw new ApiException(429, Guard.message(e, st.lang()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(0, "Перервано");
        }
        try {
            HttpURLConnection c = stream(path, body, pow, cb);
            Guard.ok(st);
            return c;
        } catch (ApiException e) {
            Guard.fail(st, e.code, e.getMessage());
            throw e;
        } catch (IOException e) {
            Guard.fail(st, 0, e.getMessage());
            throw e;
        } finally {
            active = null;
        }
    }

    private HttpURLConnection stream(String path, JSONObject body, JSONObject pow, Sse cb) throws IOException {
        HttpURLConnection c = open(path, "POST", true);
        active = c;
        if (pow != null) c.setRequestProperty("x-ds-pow-response", pow.optString("h"));
        c.setDoOutput(true);
        byte[] data = body.toString().getBytes(StandardCharsets.UTF_8);
        c.setFixedLengthStreamingMode(data.length);
        try (OutputStream os = c.getOutputStream()) { os.write(data); }
        int status = c.getResponseCode();
        InputStream in = status >= 400 ? c.getErrorStream() : c.getInputStream();
        if (status >= 400) {
            String t = readAll(in);
            c.disconnect();
            throw new ApiException(status, t == null ? ("HTTP " + status) : t);
        }
        String ctype = c.getContentType();
        if (ctype != null && ctype.toLowerCase().contains("application/json")) {
            String t = readAll(in);
            c.disconnect();
            String msg = "HTTP " + status;
            int ec = status;
            try {
                JSONObject j = new JSONObject(t);
                JSONObject d = j.optJSONObject("data");
                int gcode = j.optInt("code", 0);
                int bcode = d == null ? 0 : d.optInt("biz_code", 0);
                // 40303/40304 приходять із HTTP 200: сервер просить людину
                // пройти hCaptcha. Раніше код губився, і застосунок вважав
                // відповідь успішною або показував незрозуміле «HTTP 200».
                if (gcode != 0) ec = gcode;
                else if (bcode != 0) ec = bcode;
                if (d != null && !d.optString("biz_msg", "").isEmpty()) msg = d.optString("biz_msg");
                else if (!j.optString("msg", "").isEmpty()) msg = j.optString("msg");
            } catch (JSONException ignored) {}
            throw new ApiException(ec, msg);
        }
        InputStream r = new BufferedInputStream(in, 8192);
        String event = "";
        StringBuilder payload = new StringBuilder();
        boolean skip = false;
        String line;
        try {
            while ((line = readSseLine(r, MAX_SSE_LINE)) != null) {
                // Один рядок більший за стелю — це не дельта тексту: рвемо
                // кадр, інакше BufferedReader.readLine() спробував би
                // виділити під нього всю доступну памʼять.
                if (OVER_LINE.equals(line)) {
                    payload.setLength(0);
                    event = "";
                    skip = true;
                    continue;
                }
                if (line.isEmpty()) {
                    if (!skip && payload.length() > 0) dispatch(event, payload.toString(), cb);
                    event = "";
                    payload.setLength(0);
                    skip = false;
                    continue;
                }
                if (line.startsWith("event:")) event = line.substring(6).trim();
                else if (line.startsWith("data:")) {
                    // один SSE-кадр більший за 8 МБ — це вже не дельта тексту
                    if (!skip && payload.length() + line.length() > 8 * 1024 * 1024) {
                        payload.setLength(0);
                        skip = true;
                    }
                    if (!skip) {
                        if (payload.length() > 0) payload.append('\n');
                        payload.append(line.substring(5).trim());
                    }
                }
            }
            if (payload.length() > 0) dispatch(event, payload.toString(), cb);
        } finally {
            try { r.close(); } catch (IOException ignored) {}
        }
        return c;
    }

    private static void dispatch(String event, String raw, Sse cb) {
        JSONObject data;
        try { data = new JSONObject(raw); } catch (JSONException e) { return; }
        try {
            if (event != null && !event.isEmpty()) cb.onEvent(event, data);
            cb.onData(data);
        } catch (Exception ignored) {}
    }

    /** Відповідь без стріму: 32 МБ — уже підозріло багато для JSON. */
    private static final int MAX_BODY = 32 * 1024 * 1024;
    /** Один рядок SSE. Понад це — не текст відповіді, а сміття. */
    private static final int MAX_SSE_LINE = 8 * 1024 * 1024;
    private static final String OVER_LINE = "\u0000over\u0000";

    /**
     * Рядок SSE з жорсткою стелею: читаємо байти до '\n', а хвіст
     * задовгого рядка відкидаємо. Стандартний readLine() спершу накопичив
     * би рядок цілком — на пошкодженому потоці це OOM.
     */
    static String readSseLine(InputStream in, int max) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream(512);
        int c;
        boolean over = false;
        while ((c = in.read()) >= 0) {
            if (c == '\n') break;
            if (c == '\r') continue;
            if (b.size() < max) b.write(c);
            else over = true;
        }
        if (c < 0 && b.size() == 0 && !over) return null;
        if (over) return OVER_LINE;
        return new String(b.toByteArray(), StandardCharsets.UTF_8);
    }

    static String readAll(InputStream in) throws IOException {
        if (in == null) return "";
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            bo.write(buf, 0, n);
            if (bo.size() > MAX_BODY) throw new IOException("відповідь завелика (> 32 МБ)");
        }
        return new String(bo.toByteArray(), StandardCharsets.UTF_8);
    }
}
