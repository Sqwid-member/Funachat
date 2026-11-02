package com.funa.chat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Локальний OpenAI-сумісний API для FunaChat.
 *
 *   GET  /health
 *   GET  /v1/models            + /v1/models/{id}
 *   POST /v1/chat/completions  (stream / non-stream, tools, Bearer key)
 *   POST /v1/completions       (legacy prompt -> text)
 *
 * Кожен запит = нова сесія DeepSeek; після відповіді вона видаляється
 * (можна вимкнути в налаштуваннях). Запити виконуються по черзі:
 * PoW-обчислення важкі для телефона.
 */
public class ApiServer {
    /** Скільки раундів «модель -> інструмент -> модель» робить auto-tools. */
    static int AUTO_ROUNDS = 4;
    /** Скільки інструментів виконуємо за один раунд auto-tools. */
    private static final int MAX_TOOL_CALLS = 8;
    /** Стеля промту: стільки DeepSeek усе одно не прийме. */
    private static final int PROMPT_CAP = 1000000;
    /** Стеля тексту відповіді, який тримаємо в памʼяті для фінального JSON. */
    private static final int ANSWER_CAP = 2000000;

    private static final Object LOCK = new Object();
    private static ApiServer INSTANCE;

    /* ---- захист локального API ---- */
    /** Одночасних з'єднань (решта -> 503). */
    static int maxConns = 8;
    /** Запитів на хвилину з однієї IP (решта -> 429). */
    static int perIpLimit = 120;
    private static final AtomicInteger CONNS = new AtomicInteger();
    private static final Map<String, long[]> IP_STATS = new ConcurrentHashMap<>();

    /**
     * Публічний ключ розробника для аудиту злитих API-ключів.
     * Приватна половина зберігається лише поза репозиторієм
     * (~/.config/funachat/dev_audit_key.pem). Запит підписується нею,
     * тому без приватного ключа аудит недоступний.
     */
    private static final String DEV_AUDIT_PUB =
            "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEvUT5MEH9ILHRT+vZnM52KaTuC+X19gkNeWd8xm4QUIPscr1SKZ3wqDJpUTnxujYbp+04Hd1MzdStmR2xPLrrzg==";

    /** id, опис, model_type, thinking, search. */
    private static final String[][] MODELS = {
            {"deepseek-chat", "DeepSeek V4.1 Flash", "default", "0", "0"},
            {"deepseek-reasoner", "DeepSeek V4.1 Flash · Reasoner", "expert", "1", "0"},
            {"deepseek-expert", "DeepSeek V4.1 Flash · Expert", "expert", "1", "0"},
            {"deepseek-search", "DeepSeek V4.1 Flash · Search", "default", "0", "1"},
    };

    private final Store st;
    private final ServerSocket server;
    private final Thread accept;
    private final int port;
    private volatile boolean alive = true;

    private ApiServer(Store s, ServerSocket ss, int p) {
        st = s;
        server = ss;
        port = p;
        accept = new Thread(this::loop, "funa-api");
        accept.setDaemon(true);
    }

    public static synchronized boolean running() {
        return INSTANCE != null && INSTANCE.alive;
    }

    public static synchronized int runningPort() {
        return INSTANCE == null ? 0 : INSTANCE.port;
    }

    /** Повертає null у разі успіху, інакше текст помилки. */
    public static synchronized String start(Store st) {
        if (INSTANCE != null) {
            if (INSTANCE.alive) return null;
            // Потік accept уже завершився (наприклад, після помилки), але
            // сокет міг лишитися відкритим: без закриття новий bind падає
            // з «Address already in use», і API більше не піднімається.
            try { INSTANCE.server.close(); } catch (IOException ignored) {}
            INSTANCE = null;
        }
        try {
            InetAddress addr = st.apiLan()
                    ? InetAddress.getByName("0.0.0.0")
                    : InetAddress.getByName("127.0.0.1");
            ServerSocket ss = new ServerSocket();
            ss.setReuseAddress(true);
            ss.bind(new InetSocketAddress(addr, st.apiPort()), 16);
            ApiServer a = new ApiServer(st, ss, st.apiPort());
            INSTANCE = a;
            a.accept.start();
            Log.add("api", "started on " + addr.getHostAddress() + ":" + st.apiPort());
            return null;
        } catch (Exception e) {
            String m = e.getMessage();
            m = m == null || m.isEmpty() ? e.getClass().getSimpleName() : m;
            Log.e("api", "start failed: " + m);
            return m;
        }
    }

    public static synchronized void stop() {
        ApiServer a = INSTANCE;
        INSTANCE = null;
        if (a == null) return;
        a.alive = false;
        try { a.server.close(); } catch (IOException ignored) {}
        Log.add("api", "stopped");
    }

    private void loop() {
        while (alive) {
            Socket s = null;
            try {
                s = server.accept();
            } catch (IOException e) {
                break;
            }
            final Socket sock = s;
            // Понад ліміт — відмовляємо в потоці accept: інакше тисяча
            // з'єднань створила б тисячу потоків лише для відповіді 503.
            // Нічого не читаємо: клієнт уже знає, що сервер зайнятий.
            if (CONNS.get() >= maxConns) {
                try {
                    OutputStream o = new BufferedOutputStream(sock.getOutputStream());
                    send(o, 503, "application/json", err("Too many connections", "server_error"));
                    o.flush();
                } catch (IOException ignored) {
                } finally {
                    try { sock.close(); } catch (IOException ignored) {}
                }
                continue;
            }
            Thread t = new Thread(() -> handle(sock), "funa-api-conn");
            t.setDaemon(true);
            t.start();
        }
        alive = false;
    }

    /* ------------------------------------------------------------------ */

    private void handle(Socket sock) {
        int live = CONNS.incrementAndGet();
        try {
            if (live > maxConns) {
                OutputStream o = new BufferedOutputStream(sock.getOutputStream());
                send(o, 503, "application/json", err("Too many connections", "server_error"));
                return;
            }
            sock.setSoTimeout(30000);
            InputStream in = new BufferedInputStream(sock.getInputStream());
            OutputStream out = new BufferedOutputStream(sock.getOutputStream());
            String line = readLine(in);
            if (line == null) { sock.close(); return; }
            String[] parts = line.split(" ");
            if (parts.length < 2) { sock.close(); return; }
            String method = parts[0].toUpperCase(Locale.US);
            String path = parts[1];
            int q = path.indexOf('?');
            if (q >= 0) path = path.substring(0, q);

            Map<String, String> h = new HashMap<>();
            int hn = 0;
            while ((line = readLine(in)) != null && !line.isEmpty()) {
                if (++hn > 100) {
                    send(out, 431, "application/json",
                            err("Too many request headers", "invalid_request_error"));
                    sock.close();
                    return;
                }
                int c = line.indexOf(':');
                if (c > 0) h.put(line.substring(0, c).trim().toLowerCase(Locale.US),
                        line.substring(c + 1).trim());
            }
            int len = 0;
            try { len = Integer.parseInt(h.getOrDefault("content-length", "0")); }
            catch (Exception ignored) {}
            // Ліміт рахуємо після заголовків: за тунелем усі запити приходять
            // з 127.0.0.1, і без X-Forwarded-For / CF-Connecting-IP один
            // галасливий клієнт з'їдав би квоту всіх інших.
            String ip = clientIp(sock, h);
            if (!allowIp(ip)) {
                OutputStream o = new BufferedOutputStream(sock.getOutputStream());
                send(o, 429, "application/json", err("Too many requests from this address", "rate_limit_error"));
                return;
            }
            if (len < 0 || len > 8 * 1024 * 1024) {
                send(out, 413, "application/json", err("Request body too large", "invalid_request_error"));
                sock.close();
                return;
            }
            byte[] body = readN(in, len);

            if ("OPTIONS".equals(method)) {
                send(out, 204, null, new byte[0]);
                sock.close();
                return;
            }
            // Публічний доступ можна вимкнути: тунель може й далі працювати,
            // але запити, що прийшли ззовні (петля + заголовки проксі), ріжемо.
            if (!st.apiPublic() && viaProxy(sock, h)) {
                send(out, 403, "application/json",
                        err("Public access is disabled in FunaChat", "public_access_disabled"));
                sock.close();
                return;
            }
            // Аудит злитих ключів: окремий секрет пристрою або підпис
            // приватного ключа розробника. Звичайний API-ключ тут не діє.
            if ("POST".equals(method) && ("/v1/audit/key".equals(path) || "/audit/key".equals(path))) {
                audit(out, body, h, ip);
                out.flush();
                return;
            }

            int authState = auth(h, ip);
            if (authState != 0) {
                boolean expired = authState == 1;
                send(out, 401, "application/json",
                        err(expired ? "API key expired" : "Invalid API key",
                                expired ? "expired_api_key" : "invalid_api_key"));
                sock.close();
                return;
            }

            // Блокування FunaChat поширюється й на локальний API: ключ міг
            // осісти в конфігу іншого застосунку, а пароль знає лише власник.
            // /health лишаємо доступним, щоб клієнт бачив причину, а не
            // просто «сервер зник».
            final boolean locked = st.apiLocked();
            if ("GET".equals(method) && ("/health".equals(path) || "/".equals(path))) {
                JSONObject hi = health();
                try {
                    hi.put("locked", locked);
                    if (locked) hi.put("ok", false);
                } catch (Exception ignored) {}
                send(out, 200, "application/json", hi.toString().getBytes(StandardCharsets.UTF_8));
            } else if (locked) {
                send(out, 503, "application/json",
                        err("FunaChat is locked — unlock the app to use the API", "server_error"));
            } else if ("GET".equals(method) && "/v1/models".equals(path)) {
                send(out, 200, "application/json", models().toString().getBytes(StandardCharsets.UTF_8));
            } else if ("GET".equals(method) && ("/v1/tools".equals(path) || "/tools".equals(path))) {
                send(out, 200, "application/json", toolsList().toString().getBytes(StandardCharsets.UTF_8));
            } else if ("GET".equals(method) && path.startsWith("/v1/models/")) {
                String id = path.substring("/v1/models/".length());
                JSONObject m = modelById(id);
                if (m == null) send(out, 404, "application/json", err("Model not found: " + id, "invalid_request_error"));
                else send(out, 200, "application/json", m.toString().getBytes(StandardCharsets.UTF_8));
            } else if ("POST".equals(method) && ("/v1/chat/completions".equals(path) || "/chat/completions".equals(path))) {
                chat(out, body);
            } else if ("POST".equals(method) && ("/v1/completions".equals(path) || "/completions".equals(path))) {
                completions(out, body);
            } else {
                send(out, 404, "application/json", err("Not found: " + path, "invalid_request_error"));
            }
            out.flush();
        } catch (Exception ignored) {
        } finally {
            CONNS.decrementAndGet();
            try { sock.close(); } catch (IOException ignored) {}
        }
    }

    /**
     * Справжній IP клієнта. Заголовкам віримо лише тоді, коли з'єднання
     * прийшло з петлі: так їх ставить тунель (cloudflared/zrok), а не
     * сторонній клієнт у LAN.
     */
    private static String clientIp(Socket sock, Map<String, String> h) {
        String ip;
        try {
            ip = sock.getInetAddress().getHostAddress();
        } catch (Exception e) {
            return null;
        }
        if (ip == null) return null;
        boolean loop = "127.0.0.1".equals(ip) || "::1".equals(ip)
                || "0:0:0:0:0:0:0:1".equals(ip) || ip.startsWith("127.");
        if (!loop) return ip;
        String v = h.get("cf-connecting-ip");
        if (v == null) v = h.get("x-real-ip");
        if (v == null) {
            v = h.get("x-forwarded-for");
            if (v != null) {
                int c = v.indexOf(',');
                if (c > 0) v = v.substring(0, c);
            }
        }
        if (v == null) return ip;
        v = v.trim();
        if (v.isEmpty() || v.length() > 45) return ip;
        for (int i = 0; i < v.length(); i++) {
            char ch = v.charAt(i);
            boolean ok = (ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f')
                    || (ch >= 'A' && ch <= 'F') || ch == ':' || ch == '.';
            if (!ok) return ip;
        }
        return v;
    }

    /** Запит прийшов із тунелю: з'єднання з петлі плюс заголовки проксі. */
    private static boolean viaProxy(Socket sock, Map<String, String> h) {
        if (h.get("cf-connecting-ip") == null && h.get("x-forwarded-for") == null
                && h.get("x-real-ip") == null) return false;
        String ip;
        try { ip = sock.getInetAddress().getHostAddress(); } catch (Exception e) { return false; }
        if (ip == null) return false;
        return "127.0.0.1".equals(ip) || "::1".equals(ip)
                || "0:0:0:0:0:0:0:1".equals(ip) || ip.startsWith("127.");
    }

    /** Простий ковзний ліміт на IP: perIpLimit запитів за 60 с. */
    private static boolean allowIp(String ip) {
        if (ip == null) return true;

        long now = System.currentTimeMillis();
        // LAN-режим бачить будь-які адреси: не тримаємо їх вічно (сканер
        // мережі інакше роздуває мапу на тисячі записів).
        if (IP_STATS.size() > 512) {
            for (Iterator<Map.Entry<String, long[]>> it = IP_STATS.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<String, long[]> e = it.next();
                long[] v = e.getValue();
                if (v == null || now - v[0] >= 120000L) it.remove();
            }
            // Якщо всі записи свіжі (скан мережі), прибираємо зайві примусово:
            // краще скинути лічильник кільком IP, ніж тримати мапу без межі.
            if (IP_STATS.size() > 1024) {
                Iterator<Map.Entry<String, long[]>> it = IP_STATS.entrySet().iterator();
                for (int i = 0; i < 512 && it.hasNext(); i++) { it.next(); it.remove(); }
            }
        }
        long[] st = IP_STATS.get(ip);
        if (st == null) {
            st = new long[]{now, 1};
            IP_STATS.put(ip, st);
            return true;
        }
        synchronized (st) {
            if (now - st[0] >= 60000L) {
                st[0] = now;
                st[1] = 1;
                return true;
            }
            st[1]++;
            return st[1] <= perIpLimit;
        }
    }

    /** 0 — ключ чинний, 1 — прострочений, 2 — невідомий. */
    private int auth(Map<String, String> h, String ip) {
        String got = h.get("authorization");
        if (got == null) got = h.get("x-api-key");
        if (got == null) return 2;
        return st.apiKeyCheck(got, ip);
    }

    /** Перевірка ECDSA-підпису тіла запиту публічним ключем розробника. */
    private static boolean devSignature(byte[] body, String sigB64) {
        try {
            byte[] sig = java.util.Base64.getDecoder().decode(sigB64);
            byte[] der = java.util.Base64.getDecoder().decode(DEV_AUDIT_PUB);
            java.security.PublicKey pk = java.security.KeyFactory.getInstance("EC")
                    .generatePublic(new java.security.spec.X509EncodedKeySpec(der));
            java.security.Signature s = java.security.Signature.getInstance("SHA256withECDSA");
            s.initVerify(pk);
            s.update(body);
            return s.verify(sig);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * POST /v1/audit/key
     * {key, action: info|revoke|notify, note, ts, nonce}
     *
     * Доступ: X-Funa-Audit (секрет пристрою) або X-Funa-Dev-Signature
     * (ECDSA над тілом запиту; ts у межах 3 хвилин, nonce ≥ 16 символів).
     */
    private void audit(OutputStream out, byte[] body, Map<String, String> h, String ip) throws IOException {
        JSONObject req = null;
        try { req = new JSONObject(new String(body, StandardCharsets.UTF_8)); }
        catch (Exception ignored) {}
        if (req == null) {
            send(out, 400, "application/json", err("Invalid JSON", "invalid_request_error"));
            return;
        }
        boolean ok = st.auditCheck(h.get("x-funa-audit"));
        if (!ok) {
            String sig = h.get("x-funa-dev-signature");
            long ts = req.optLong("ts", 0);
            String nonce = req.optString("nonce", "");
            long skew = Math.abs(System.currentTimeMillis() - ts);
            if (sig != null && nonce.length() >= 16 && skew <= 180000L)
                ok = devSignature(body, sig);
        }
        if (!ok) {
            send(out, 403, "application/json", err("Audit access denied", "audit_forbidden"));
            return;
        }
        String key = req.optString("key", "").trim();
        String action = req.optString("action", "info").trim().toLowerCase(Locale.US);
        if (!"info".equals(action) && !"revoke".equals(action) && !"notify".equals(action)) {
            send(out, 400, "application/json", err("Unknown audit action", "invalid_request_error"));
            return;
        }
        JSONObject info = st.apiKeyAudit(key);
        if (info == null) {
            send(out, 404, "application/json", err("API key not found on this device", "key_not_found"));
            return;
        }
        boolean revoked = false;
        boolean notified = false;
        if ("revoke".equals(action)) revoked = st.apiKeyDeleteByValue(key);
        if ("revoke".equals(action) || "notify".equals(action)) {
            st.auditNotify(action, info, req.optString("note", ""), ip);
            notified = true;
        } else {
            st.auditRecord("info", info, req.optString("note", ""), ip);
        }
        JSONObject r = new JSONObject();
        try {
            r.put("ok", true);
            r.put("action", action);
            r.put("key", info);
            r.put("owner", st.ownerInfo());
            r.put("version", MainActivity.VERSION);
            r.put("code", MainActivity.CODE);
            r.put("revoked", revoked);
            r.put("notified", notified);
            r.put("remaining", st.apiKeyList().length());
            r.put("audit", st.auditLog(8));
        } catch (Exception ignored) {}
        send(out, 200, "application/json", r.toString().getBytes(StandardCharsets.UTF_8));
    }

    private JSONObject health() {
        JSONObject o = new JSONObject();
        try {
            JSONArray eps = new JSONArray();
            eps.put("/health").put("/v1/models").put("/v1/models/{id}").put("/v1/tools")
                    .put("/v1/chat/completions").put("/v1/completions")
                    .put("/v1/audit/key");
            JSONArray ids = new JSONArray();
            for (String[] m : MODELS) ids.put(m[0]);
            o.put("ok", true);
            o.put("app", "FunaChat");
            o.put("version", MainActivity.VERSION);
            o.put("model", st.model());
            o.put("tools", true);
            o.put("auto_tools", true);
            o.put("subagents", true);
            o.put("stream", true);
            o.put("models", ids);
            o.put("endpoints", eps);
        } catch (Exception ignored) {}
        return o;
    }

    private JSONObject models() {
        JSONObject out = new JSONObject();
        try {
            JSONArray data = new JSONArray();
            for (String[] m : MODELS) data.put(modelObject(m[0]));
            out.put("object", "list");
            out.put("data", data);
        } catch (Exception ignored) {}
        return out;
    }

    private static JSONObject modelById(String id) {
        JSONObject m = modelObject(id);
        if (m == null) return null;
        return m;
    }

    private static JSONObject modelObject(String id) {
        for (String[] m : MODELS) {
            if (!m[0].equals(id)) continue;
            JSONObject o = new JSONObject();
            try {
                o.put("id", m[0]);
                o.put("object", "model");
                o.put("created", 0);
                o.put("owned_by", "funa");
                o.put("description", m[1]);
            } catch (Exception ignored) {}
            return o;
        }
        return null;
    }

    /** model_type для апстріму; невідомі id мапляться на налаштування застосунку. */
    private String modelTypeFor(String id) {
        for (String[] m : MODELS) if (m[0].equals(id)) return m[2];
        return st.model();
    }

    private boolean thinkingFor(String id, JSONObject req) {
        boolean t = req.optBoolean("thinking", false);
        for (String[] m : MODELS) if (m[0].equals(id)) return t || "1".equals(m[3]);
        return t;
    }

    private boolean searchFor(String id, JSONObject req) {
        boolean s = req.optBoolean("search", st.search());
        for (String[] m : MODELS) if (m[0].equals(id)) return s || "1".equals(m[4]);
        return s;
    }

    /* ---------------------------- chat -------------------------------- */

    private void chat(OutputStream out, byte[] raw) throws Exception {
        JSONObject req = body(raw, out);
        if (req == null) return;

        boolean auto = req.optBoolean("auto_tools", false);
        Object rawTools = req.opt("tools");
        JSONArray tools = null;
        if (rawTools instanceof JSONArray) tools = (JSONArray) rawTools;
        else if (rawTools instanceof String) {
            String ts = ((String) rawTools).trim();
            if ("auto".equalsIgnoreCase(ts) || "all".equalsIgnoreCase(ts)) auto = true;
        }
        if (auto) tools = mergeTools(tools);
        String choice = toolChoice(req.opt("tool_choice"));
        boolean toolsOn = tools != null && tools.length() > 0 && !"none".equals(choice);
        // auto_tools без фактичних інструментів (tool_choice="none") — це не
        // 4 раунди «модель -> інструмент -> модель», а звичайна відповідь.
        // Інакше апстрім викликався б чотири рази, а стрім дублював текст.
        if (!toolsOn) auto = false;

        String prompt;
        try {
            prompt = flatten(req.optJSONArray("messages"));
        } catch (Exception e) {
            prompt = "";
        }
        if (prompt.trim().isEmpty()) {
            send(out, 400, "application/json", err("messages[] is empty", "invalid_request_error"));
            return;
        }
        // Тіло ≤8 МБ, але в контекст DeepSeek усе одно стільки не влізе:
        // ріжемо середину, лишаючи початок (системні інструкції) і кінець.
        if (toolsOn) prompt = capPrompt(toolPreamble(tools, choice) + prompt);
        else prompt = capPrompt(prompt);
        if (!ready(out)) return;
        // Токен фіксуємо на вхід запиту: якщо акаунт перемкнуть посеред
        // відповіді, вона має лишитися в тому акаунті, куди її надіслали,
        // а не піти токеном нового.
        final String reqTok = st.token();
        final String reqAid = st.activeId();

        final boolean stream = req.optBoolean("stream", false);
        String mid = req.optString("model", "deepseek-chat");
        if (mid.isEmpty()) mid = "deepseek-chat";
        final String modelId = mid;
        final String modelType = modelTypeFor(mid);
        final boolean thinking = thinkingFor(mid, req);
        final boolean search = searchFor(mid, req);

        final String id = "chatcmpl-" + Long.toString(System.currentTimeMillis(), 36);
        final long created = System.currentTimeMillis() / 1000L;
        final StringBuilder answer = new StringBuilder();
        final StringBuilder think = new StringBuilder();
        final OutputStream so = out;

        if (stream) {
            sseHeaders(out);
            sse(out, firstChunk(id, created, modelId));
        }

        String work = prompt;
        String lastResults = "";
        int rounds = auto ? AUTO_ROUNDS : 1;

        for (int round = 0; round < rounds; round++) {
            answer.setLength(0);
            // reasoning кожного раунду окремий: інакше фінальний JSON
            // склеює «думки» всіх раундів автоматичних інструментів
            think.setLength(0);
            try {
                synchronized (LOCK) {
                    upstream(work, modelType, thinking, search, reqTok, (kind, text) -> {
                        // Стрім усе одно віддаємо клієнту, але копію для
                        // фінального JSON тримаємо в межах: інакше одна
                        // «нескінченна» відповідь з'їдає пам'ять процесу.
                        if ("think".equals(kind)) {
                            appendCap(think, text);
                            if (stream && !toolsOn) sse(so, chunk(id, created, modelId, "reasoning_content", text, null));
                        } else {
                            appendCap(answer, text);
                            if (stream && !toolsOn) sse(so, chunk(id, created, modelId, "content", text, null));
                        }
                    });
                }
            } catch (ClientGone gone) {
                // Клиент ушёл: не пытаемся писать ему ответ об ошибке.
                return;
            } catch (Exception e) {
                String m = message(e);
                boolean isLimited = limited(e);
                boolean isExpired = expired(e);
                Log.e("api", (isLimited ? "limited: " : isExpired ? "expired: " : "upstream: ") + m);
                if (stream) {
                    // Если контент ещё не ушёл клиенту (в auto-режиме он
                    // копится до финального раунда), текст ошибки нужно
                    // отдать явно — иначе клиент видит пустой поток.
                    if (answer.length() == 0) {
                        JSONObject eo = new JSONObject();
                        try { eo.put("error", m); } catch (Exception ignored) {}
                        sse(out, eo);
                    }
                    sse(out, chunk(id, created, modelId, null, null, "error"));
                    sseRaw(out, "[DONE]");
                } else if (answer.length() == 0) {
                    send(out, isLimited ? 429 : isExpired ? 401 : 502, "application/json",
                            err(m, isLimited ? "rate_limit_error"
                                    : isExpired ? "authentication_error" : "bad_gateway"));
                } else {
                    send(out, 200, "application/json", completion(id, created, modelId,
                            answer.toString(), null, "error", m, prompt.length())
                            .toString().getBytes(StandardCharsets.UTF_8));
                }
                return;
            }

            if (!auto) break;
            JSONArray calls = parseToolCalls(answer.toString());
            if (calls.length() == 0) break;

            String text = stripToolBlocks(answer.toString()).trim();
            StringBuilder results = new StringBuilder();
            int cn = Math.min(calls.length(), MAX_TOOL_CALLS);
            for (int i = 0; i < cn; i++) {
                JSONObject call = calls.optJSONObject(i);
                if (call == null) continue;
                JSONObject rc = toRunnerCall(call);
                // Инструмент-субагент взял бы токен уже нового аккаунта:
                // лучше пропустить вызов, чем выполнить его от чужого имени.
                JSONObject res = reqAid.equals(st.activeId())
                        ? Runner.exec(rc, st, null, 0)
                        : Builtin.err("account switched — tool skipped");
                String name = rc.optString("tool", "tool");
                boolean ok = res.optBoolean("ok", false);
                String bodyText = res.optString("text", res.optString("error", ""));
                if (bodyText.length() > 12000) bodyText = bodyText.substring(0, 12000) + "\n… (truncated)";
                results.append("- ").append(name).append(" (ok=").append(ok).append("):\n")
                        .append(bodyText).append("\n");
            }
            if (calls.length() > cn) {
                results.append("- ... ще ").append(calls.length() - cn)
                        .append(" викликів пропущено (ліміт ").append(MAX_TOOL_CALLS).append(" за раунд)\n");
            }
            lastResults = results.toString();
            work = capPrompt(work + "\n\nAssistant:\n" + text + "\n\nTool results:\n" + lastResults
                    + "\nGive the final answer now. Use another tool only if it is really required.");
        }

        String finalText = answer.toString();
        if (toolsOn) {
            JSONArray calls = parseToolCalls(finalText);
            String text = stripToolBlocks(finalText).trim();
            if (!auto && calls.length() > 0) {
                if (stream) {
                    JSONObject tc = toolDelta(id, created, modelId, calls);
                    sse(out, tc);
                    sse(out, chunk(id, created, modelId, null, null, "tool_calls"));
                    sseRaw(out, "[DONE]");
                } else {
                    send(out, 200, "application/json", completion(id, created, modelId,
                            text, calls, "tool_calls", null, prompt.length())
                            .toString().getBytes(StandardCharsets.UTF_8));
                }
                return;
            }
            finalText = text;
        }
        if (auto && finalText.trim().isEmpty()) finalText = lastResults;

        if (stream) {
            if (!auto && toolsOn && !finalText.isEmpty()) {
                sse(out, chunk(id, created, modelId, "content", finalText, null));
            } else if (auto && !finalText.isEmpty()) {
                sse(out, chunk(id, created, modelId, "content", finalText, null));
            }
            sse(out, chunk(id, created, modelId, null, null, "stop"));
            sseRaw(out, "[DONE]");
        } else {
            JSONObject o = completion(id, created, modelId, finalText, null, "stop", null, prompt.length());
            if (think.length() > 0) {
                try {
                    o.getJSONArray("choices").getJSONObject(0).getJSONObject("message")
                            .put("reasoning_content", think.toString());
                } catch (Exception ignored) {}
            }
            send(out, 200, "application/json", o.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    /** Копия ответа для JSON: не длиннее ANSWER_CAP, даже если дельта огромная. */
    private static void appendCap(StringBuilder sb, String text) {
        if (text == null || text.isEmpty() || sb.length() >= ANSWER_CAP) return;
        int room = ANSWER_CAP - sb.length();
        sb.append(text.length() <= room ? text : text.substring(0, room));
    }

    /**
     * Промт для апстріму: залишає початок (системні інструкції) і кінець
     * (поточний стан), а середину позначає. Body-ліміт 8 МБ цього не
     * замінює: 1M символів ≈ 300k токенів, запас під контекст 1M.
     */
    private static String capPrompt(String prompt) {
        if (prompt == null) return "";
        if (prompt.length() <= PROMPT_CAP) return prompt;
        String head = prompt.substring(0, 20000);
        String tail = prompt.substring(prompt.length() - (PROMPT_CAP - 20000));
        return head + "\n\n… (частину історії опущено) …\n\n" + tail;
    }

    private static JSONObject toRunnerCall(JSONObject call) {
        JSONObject fn = call.optJSONObject("function");
        String name = fn == null ? call.optString("name", "") : fn.optString("name", "");
        Object args = fn == null ? call.opt("arguments") : fn.opt("arguments");
        JSONObject o = new JSONObject();
        if (args instanceof JSONObject) {
            // копія, щоб не мутувати обʼєкт клієнта
            JSONObject src = (JSONObject) args;
            java.util.Iterator<String> it = src.keys();
            while (it.hasNext()) {
                String k = it.next();
                try { o.put(k, src.opt(k)); } catch (Exception ignored) {}
            }
        } else {
            String text = args == null || args == JSONObject.NULL ? "{}" : String.valueOf(args);
            try { o = new JSONObject(text.isEmpty() ? "{}" : text); }
            catch (Exception e) { o = new JSONObject(); }
        }
        try { o.put("tool", name); } catch (Exception ignored) {}
        return o;
    }

    /** Список інструментів, які сервер виконує сам. */
    private JSONObject toolsList() {
        JSONObject o = new JSONObject();
        try {
            JSONArray data = builtinTools();
            o.put("object", "list");
            o.put("data", data);
            o.put("auto_tools", true);
            o.put("subagents", true);
        } catch (Exception ignored) {}
        return o;
    }

    private static JSONArray builtinTools() {
        JSONArray a = new JSONArray();
        a.put(tdef("time", "Current date and time in a time zone, optionally shifted by days/seconds.",
                params(prop("string", "IANA time zone, e.g. Europe/Kyiv. Default: device zone.")
                        .put2("offset_days", prop("integer", "Shift by N days, may be negative."))
                        .put2("offset", prop("integer", "Shift by N seconds.")))));
        a.put(tdef("calc", "Evaluate a math expression: + - * / % ^, parentheses, sin/cos/tan, log, sqrt, pi, e.",
                params(prop("string", "Expression, e.g. 2*(3+4)^2 or sin(30)."))
                        .put2("deg", prop("boolean", "Use degrees for trigonometry. Default false.")),
                "expr"));
        a.put(tdef("convert", "Convert units: length, mass, data, speed, volume, temperature.",
                params(prop("number", "Value to convert."))
                        .put2("from", prop("string", "Source unit: m, km, mi, kg, lb, mb, gb, kmh, mph, l, c, f, k…"))
                        .put2("to", prop("string", "Target unit.")),
                "value", "from", "to"));
        a.put(tdef("random", "Random integer, float, UUID, hex bytes, or pick an item.",
                params(prop("string", "Kind: int (default), float, uuid, bytes, choice."))
                        .put2("min", prop("number", "Minimum (int/float)."))
                        .put2("max", prop("number", "Maximum (int/float)."))
                        .put2("count", prop("integer", "Number of random bytes, 1..256."))
                        .put2("items", prop("array", "Items for kind=choice."))));
        a.put(tdef("base64", "Encode or decode Base64 text.",
                params(prop("string", "Operation: encode (default) or decode."))
                        .put2("text", prop("string", "Text to process.")),
                "text"));
        a.put(tdef("hash", "Hash text with md5, sha1, sha256 or sha512.",
                params(prop("string", "Algorithm: md5, sha1, sha256 (default), sha512."))
                        .put2("text", prop("string", "Text to hash.")),
                "text"));
        a.put(tdef("skill", "Read built-in instructions for producing a DOCX, a PDF, an HTML artifact or documentation. "
                        + "Call it BEFORE creating the artifact and follow the returned text.",
                params(prop("string", "Skill name: docx, pdf, design or docs.")), "name"));
        a.put(tdef("subagent", "Run a subtask (or up to 4 independent subtasks in parallel) in separate "
                        + "autonomous sessions and get only the final result. Use for independent research, "
                        + "multi-step work or a second opinion.",
                params(prop("string", "Clear, self-contained task for one subagent."))
                        .put2("tasks", prop("array", "Up to 4 independent tasks to run in parallel."))
                        .put2("system", prop("string", "Optional custom system prompt for the subagent(s)."))));
        return a;
    }

    /** Додає вбудовані інструменти до клієнтських без дублікатів. */
    private static JSONArray mergeTools(JSONArray client) {
        JSONArray out = new JSONArray();
        java.util.Set<String> seen = new java.util.HashSet<>();
        JSONArray builtins = builtinTools();
        for (int i = 0; i < builtins.length(); i++) {
            JSONObject t = builtins.optJSONObject(i);
            String n = toolName(t);
            if (n != null && seen.add(n)) out.put(t);
        }
        if (client != null) {
            for (int i = 0; i < client.length() && out.length() < 64; i++) {
                JSONObject t = client.optJSONObject(i);
                String n = toolName(t);
                if (t != null && n != null && seen.add(n)) out.put(t);
            }
        }
        return out;
    }

    private static String toolName(JSONObject t) {
        if (t == null) return null;
        JSONObject f = t.optJSONObject("function");
        String n = f == null ? t.optString("name", "") : f.optString("name", "");
        return n == null || n.isEmpty() ? null : n;
    }

    private static JSONObject tdef(String name, String desc, JSONObject props, String... required) {
        JSONObject fn = new JSONObject();
        JSONObject params = new JSONObject();
        try {
            java.util.Iterator<String> it = props.keys();
            while (it.hasNext()) {
                String k = it.next();
                params.put(k, props.opt(k));
            }
            fn.put("name", name);
            fn.put("description", desc);
            params.put("type", "object");
            if (required.length > 0) {
                JSONArray req = new JSONArray();
                for (String r : required) req.put(r);
                params.put("required", req);
            }
            fn.put("parameters", params);
            JSONObject tool = new JSONObject();
            tool.put("type", "function");
            tool.put("function", fn);
            return tool;
        } catch (Exception e) {
            return fn;
        }
    }

    private static Props prop(String type, String desc) {
        Props o = new Props();
        try {
            o.put("type", type);
            o.put("description", desc);
        } catch (Exception ignored) {}
        return o;
    }

    /** Маленький конструктор: properties без окремого класу. */
    private static final class Props extends JSONObject {
        Props put2(String k, JSONObject v) {
            try { put(k, v); } catch (Exception ignored) {}
            return this;
        }
    }

    private static Props params(JSONObject first) {
        Props p = new Props();
        try {
            java.util.Iterator<String> it = first.keys();
            while (it.hasNext()) {
                String k = it.next();
                p.put(k, first.opt(k));
            }
        } catch (Exception ignored) {}
        return p;
    }

    /* ------------------------- legacy completions ---------------------- */

    @SuppressWarnings("unused")
    private void completions(OutputStream out, byte[] raw) throws Exception {
        JSONObject req = body(raw, out);
        if (req == null) return;

        String prompt = capPrompt(legacyPrompt(req.opt("prompt")));
        if (prompt.trim().isEmpty()) {
            send(out, 400, "application/json", err("prompt is empty", "invalid_request_error"));
            return;
        }
        if (!ready(out)) return;
        final String reqTok = st.token();

        final boolean stream = req.optBoolean("stream", false);
        String mid = req.optString("model", "deepseek-chat");
        if (mid.isEmpty()) mid = "deepseek-chat";
        final String modelId = mid;
        final String modelType = modelTypeFor(mid);
        final boolean thinking = thinkingFor(mid, req);
        final boolean search = searchFor(mid, req);

        final String id = "cmpl-" + Long.toString(System.currentTimeMillis(), 36);
        final long created = System.currentTimeMillis() / 1000L;
        final StringBuilder answer = new StringBuilder();
        final OutputStream so = out;

        if (stream) {
            sseHeaders(out);
            sse(out, textChunk(id, created, modelId, ""));
        }
        try {
            synchronized (LOCK) {
                upstream(prompt, modelType, thinking, search, reqTok, (kind, text) -> {
                    if (!"think".equals(kind)) {
                        // стрим отдаём целиком, а копию для JSON держим в пределах
                        if (answer.length() < ANSWER_CAP) {
                            int room = ANSWER_CAP - answer.length();
                            answer.append(text.length() <= room ? text : text.substring(0, room));
                        }
                        if (stream) sse(so, textChunk(id, created, modelId, text));
                    }
                });
            }
        } catch (ClientGone gone) {
            // клиент закрыл поток — отвечать некуда
            return;
        } catch (Exception e) {
            if (stream) {
                JSONObject eo = new JSONObject();
                try { eo.put("error", message(e)); } catch (Exception ignored) {}
                sse(out, eo);
                sseRaw(out, "[DONE]");
            } else {
                boolean isLimited = limited(e);
                boolean isExpired = expired(e);
                send(out, isLimited ? 429 : isExpired ? 401 : 502, "application/json",
                        err(message(e), isLimited ? "rate_limit_error"
                                : isExpired ? "authentication_error" : "bad_gateway"));
            }
            return;
        }

        if (stream) {
            sse(out, finishTextChunk(id, created, modelId));
            sseRaw(out, "[DONE]");
        } else {
            JSONObject o = new JSONObject();
            try {
                JSONObject ch = new JSONObject();
                ch.put("index", 0);
                ch.put("text", answer.toString());
                ch.put("finish_reason", "stop");
                ch.put("logprobs", JSONObject.NULL);
                o.put("id", id);
                o.put("object", "text_completion");
                o.put("created", created);
                o.put("model", modelId);
                o.put("choices", new JSONArray().put(ch));
                o.put("usage", usage(prompt.length(), answer.length()));
            } catch (Exception ignored) {}
            send(out, 200, "application/json", o.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    /* ------------------------- upstream request ------------------------ */

    private interface Emit {
        void text(String kind, String text) throws IOException;
    }

    /**
     * Клиент закрыл SSE-соединение. Это не ошибка DeepSeek: просто дальше
     * писать некому, а генерировать ответ (и тратить квоту) незачем.
     */
    private static final class ClientGone extends IOException {
        ClientGone() { super("client disconnected"); }
    }

    /**
     * Один запит до DeepSeek. Кидає IOException з людським текстом у разі помилки.
     * Сесія створюється і (за налаштуванням) видаляється тут.
     */
    private void upstream(String prompt, String modelType, boolean thinking, boolean search,
                          String tok, Emit emit) throws Exception {
        // Токен пришёл из начала запроса: смена активного аккаунта не
        // должна подменить автора ответа.
        Net net = new Net(st, tok);
        net.modelOverride = modelType;
        String sid = null;
        // Причина обрыва потока: видна и в catch, если cancel() прервал чтение.
        final IOException[] failure = {null};
        try {
            JSONObject sess = net.createSession();
            sid = sess.optString("id", "");
            if (sid.isEmpty()) throw new IOException("DeepSeek: empty session id");

            JSONObject body = new JSONObject();
            body.put("chat_session_id", sid);
            body.put("parent_message_id", JSONObject.NULL);
            body.put("prompt", prompt);
            body.put("ref_file_ids", new JSONArray());
            body.put("thinking_enabled", thinking || "expert".equals(modelType));
            body.put("search_enabled", search);
            body.put("preempt", false);
            body.put("model_type", modelType);
            body.put("action", "default");

            JSONObject pow = net.powHeader("/api/v0/chat/completion");

            net.streamRequest("/api/v0/chat/completion", body, pow, new Net.Sse() {
                private String kind = "answer";

                @Override public void onEvent(String name, JSONObject data) { }

                @Override public void onData(JSONObject data) {
                    if (failure[0] != null) return;
                    try {
                        int code = data.optInt("code", 0);
                        if (code != 0) {
                            failure[0] = new IOException(data.optString("msg", "DeepSeek error " + code));
                            return;
                        }
                        String p = data.optString("p", "");
                        Object v = data.opt("v");
                        if (!p.isEmpty()) {
                            if ("response/fragments".equals(p) && "APPEND".equals(data.optString("o"))
                                    && v instanceof JSONArray) {
                                JSONArray arr = (JSONArray) v;
                                for (int i = 0; i < arr.length(); i++) {
                                    JSONObject f = arr.optJSONObject(i);
                                    if (f == null) continue;
                                    kind = "THINK".equals(f.optString("type", "RESPONSE")) ? "think" : "answer";
                                    emit.text(kind, f.optString("content", ""));
                                }
                            } else if (p.endsWith("/content") && v instanceof String) {
                                emit.text(kind, (String) v);
                            }
                        } else if (v instanceof String) {
                            emit.text(kind, (String) v);
                        } else if (v instanceof JSONObject && ((JSONObject) v).has("response")) {
                            JSONObject r = ((JSONObject) v).optJSONObject("response");
                            JSONArray fr = r == null ? null : r.optJSONArray("fragments");
                            if (fr == null) return;
                            for (int i = 0; i < fr.length(); i++) {
                                JSONObject f = fr.optJSONObject(i);
                                if (f == null) continue;
                                kind = "THINK".equals(f.optString("type", "RESPONSE")) ? "think" : "answer";
                                emit.text(kind, f.optString("content", ""));
                            }
                        }
                    } catch (java.io.IOException e) {
                        // Ошибка записи клиенту: соединение разорвано (или
                        // таймаут сокета). Прекращаем читать DeepSeek, иначе
                        // приложение продолжит генерировать ответ в пустоту.
                        failure[0] = new ClientGone();
                        net.cancel();
                    } catch (Exception ignored) {}
                }
            });
            if (failure[0] != null) throw failure[0];
        } catch (java.io.IOException e) {
            // cancel() мог оборвать чтение раньше, чем streamRequest
            // вернулся: важнее сохранить причину (клиент ушёл).
            if (failure[0] != null) throw failure[0];
            throw e;
        } finally {
            if (sid != null && !sid.isEmpty() && st.apiDelete()) {
                // net мог быть уже отменён (клиент закрыл поток) — cancel()
                // запрещает новые запросы, поэтому для уборки берём свежий
                // Net с тем же токеном.
                try { new Net(st, tok).deleteSession(sid); } catch (Exception ignored) {}
            }
        }
    }

    /* ---------------------------- tools -------------------------------- */

    /** auto | none | required | any | @<name>. */
    private static String toolChoice(Object v) {
        if (v == null || v == JSONObject.NULL) return "auto";
        if (v instanceof String) return ((String) v).toLowerCase(Locale.US);
        if (v instanceof JSONObject) {
            JSONObject o = (JSONObject) v;
            JSONObject f = o.optJSONObject("function");
            String name = f == null ? "" : f.optString("name", "");
            if (!name.isEmpty()) return "@" + name;
        }
        return "auto";
    }

    /** Компактний список оголошень інструментів для промту. */
    private static JSONArray toolDefs(JSONArray tools) {
        JSONArray out = new JSONArray();
        // 64 инструментов хватает любому клиенту; остальное — раздувание
        // промта, которое DeepSeek всё равно обрежет (или отвергнет).
        int n = Math.min(tools.length(), 64);
        for (int i = 0; i < n; i++) {
            JSONObject t = tools.optJSONObject(i);
            if (t == null) continue;
            JSONObject f = t.optJSONObject("function");
            if (f == null) f = t;
            String name = f.optString("name", "");
            if (name.isEmpty()) continue;
            JSONObject d = new JSONObject();
            try {
                d.put("name", name);
                String desc = f.optString("description", "");
                if (desc.length() > 500) desc = desc.substring(0, 500) + "…";
                if (!desc.isEmpty()) d.put("description", desc);
                Object params = f.opt("parameters");
                if (params == null || params == JSONObject.NULL) {
                    JSONObject empty = new JSONObject();
                    empty.put("type", "object");
                    empty.put("properties", new JSONObject());
                    params = empty;
                }
                // схема на десятки килобайт — это уже не описание аргументов
                if (params.toString().length() > 8000) {
                    JSONObject empty = new JSONObject();
                    empty.put("type", "object");
                    empty.put("properties", new JSONObject());
                    params = empty;
                }
                d.put("parameters", params);
            } catch (Exception ignored) {}
            out.put(d);
        }
        return out;
    }

    private static String toolPreamble(JSONArray tools, String choice) {
        StringBuilder sb = new StringBuilder(512);
        sb.append("You have access to tools (function calling). Available tools, JSON:\n");
        String defs = toolDefs(tools).toString();
        // даже 64 описания могут дать мегабайты: держим блок в разумных рамках
        if (defs.length() > 60000) defs = defs.substring(0, 60000);
        sb.append(defs).append("\n\n");
        sb.append("Rules:\n");
        sb.append("1. To call a tool reply with only fenced blocks like:\n");
        sb.append("```funa-tool\n{\"name\":\"tool_name\",\"arguments\":{\"arg\":\"value\"}}\n```\n");
        sb.append("2. \"arguments\" must be valid JSON matching the tool parameters.\n");
        sb.append("3. Use exact tool names; never invent tools.\n");
        sb.append("4. If no tool is needed, answer normally without any funa-tool block.\n");
        if (choice != null && choice.startsWith("@")) {
            sb.append("5. You MUST call the tool \"").append(choice.substring(1)).append("\".\n");
        } else if ("required".equals(choice) || "any".equals(choice)) {
            sb.append("5. You MUST call at least one tool.\n");
        }
        sb.append("\n");
        return sb.toString();
    }

    /** Витягує tool_calls із тексту моделі (funa-tool або json-блоки). */
    private static JSONArray parseToolCalls(String text) {
        JSONArray out = new JSONArray();
        if (text == null || text.isEmpty()) return out;
        int i = 0;
        while (i < text.length()) {
            int a = text.indexOf("```funa-tool", i);
            if (a < 0) break;
            int nl = text.indexOf('\n', a);
            int b = text.indexOf("```", nl < 0 ? a + 12 : nl + 1);
            if (b < 0) break;
            String body = text.substring(nl < 0 ? a + 12 : nl + 1, b).trim();
            JSONObject call = toCall(body);
            if (call != null) out.put(call);
            i = b + 3;
        }
        if (out.length() == 0) {
            i = 0;
            while (i < text.length()) {
                int a = text.indexOf("```json", i);
                if (a < 0) break;
                int nl = text.indexOf('\n', a);
                int b = text.indexOf("```", nl + 1);
                if (nl < 0 || b < 0) break;
                JSONObject call = toCall(text.substring(nl + 1, b).trim());
                if (call != null) out.put(call);
                i = b + 3;
            }
        }
        return out;
    }

    private static JSONObject toCall(String body) {
        if (body == null || body.isEmpty()) return null;
        JSONObject j;
        try { j = new JSONObject(body); } catch (Exception e) { return null; }
        String name = j.optString("name", "");
        if (name.isEmpty()) {
            JSONObject f = j.optJSONObject("function");
            if (f != null) {
                name = f.optString("name", "");
                j = f;
            }
        }
        if (name.isEmpty()) return null;
        Object args = j.opt("arguments");
        if (args == null || args == JSONObject.NULL) args = new JSONObject();
        String argText = args instanceof String ? (String) args : args.toString();
        JSONObject o = new JSONObject();
        try {
            JSONObject fn = new JSONObject();
            fn.put("name", name);
            fn.put("arguments", argText);
            o.put("id", "call_" + Long.toString(System.nanoTime(), 36));
            o.put("type", "function");
            o.put("function", fn);
        } catch (Exception ignored) {}
        return o;
    }

    private static String stripToolBlocks(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder(text.length());
        int i = 0;
        while (i < text.length()) {
            int a = text.indexOf("```", i);
            if (a < 0) { sb.append(text, i, text.length()); break; }
            int nl = text.indexOf('\n', a);
            if (nl < 0) { sb.append(text, i, text.length()); break; }
            String tag = text.substring(a + 3, nl).trim();
            if (!tag.startsWith("funa-tool") && !tag.startsWith("json")) {
                sb.append(text, i, nl + 1);
                i = nl + 1;
                continue;
            }
            int b = text.indexOf("```", nl + 1);
            if (b < 0) { sb.append(text, i, text.length()); break; }
            sb.append(text, i, a);
            i = b + 3;
            while (i < text.length() && (text.charAt(i) == '\n' || text.charAt(i) == '\r')) i++;
        }
        return sb.toString();
    }

    /* -------------------------- json builders -------------------------- */

    private static JSONObject completion(String id, long created, String model, String content,
                                         JSONArray calls, String finish, String error, int promptChars) {
        JSONObject o = new JSONObject();
        try {
            JSONObject msg = new JSONObject();
            msg.put("role", "assistant");
            msg.put("content", content == null ? "" : content);
            if (calls != null && calls.length() > 0) msg.put("tool_calls", calls);
            JSONObject ch = new JSONObject();
            ch.put("index", 0);
            ch.put("message", msg);
            ch.put("finish_reason", finish);
            ch.put("logprobs", JSONObject.NULL);
            o.put("id", id);
            o.put("object", "chat.completion");
            o.put("created", created);
            o.put("model", model);
            o.put("choices", new JSONArray().put(ch));
            o.put("usage", usage(promptChars, content == null ? 0 : content.length()));
            o.put("system_fingerprint", "funa");
            if (error != null) o.put("error", error);
        } catch (Exception ignored) {}
        return o;
    }

    private static JSONObject usage(int promptChars, int chars) {
        JSONObject u = new JSONObject();
        try {
            int p = Math.max(0, promptChars / 3);
            int t = Math.max(1, chars / 3);
            u.put("prompt_tokens", p);
            u.put("completion_tokens", t);
            u.put("total_tokens", p + t);
        } catch (Exception ignored) {}
        return u;
    }

    /** Перший SSE-кадр: delta.role = assistant. */
    private static JSONObject firstChunk(String id, long created, String model) {
        JSONObject o = new JSONObject();
        try {
            JSONObject delta = new JSONObject();
            delta.put("role", "assistant");
            delta.put("content", "");
            JSONObject ch = new JSONObject();
            ch.put("index", 0);
            ch.put("delta", delta);
            ch.put("finish_reason", JSONObject.NULL);
            o.put("id", id);
            o.put("object", "chat.completion.chunk");
            o.put("created", created);
            o.put("model", model);
            o.put("choices", new JSONArray().put(ch));
        } catch (Exception ignored) {}
        return o;
    }

    /** OpenAI-сумісний фрагмент SSE. */
    private static JSONObject chunk(String id, long created, String model, String field,
                                    String text, String finish) {
        JSONObject o = new JSONObject();
        try {
            JSONObject delta = new JSONObject();
            if (field != null && text != null) delta.put(field, text);
            JSONObject ch = new JSONObject();
            ch.put("index", 0);
            ch.put("delta", delta);
            ch.put("finish_reason", finish == null ? JSONObject.NULL : finish);
            o.put("id", id);
            o.put("object", "chat.completion.chunk");
            o.put("created", created);
            o.put("model", model);
            o.put("choices", new JSONArray().put(ch));
        } catch (Exception ignored) {}
        return o;
    }

    /** Кадр з tool_calls (усі виклики одним пакетом). */
    private static JSONObject toolDelta(String id, long created, String model, JSONArray calls) {
        JSONObject o = new JSONObject();
        try {
            JSONArray tcs = new JSONArray();
            for (int i = 0; i < calls.length(); i++) {
                JSONObject c = calls.getJSONObject(i);
                JSONObject tc = new JSONObject();
                tc.put("index", i);
                tc.put("id", c.optString("id"));
                tc.put("type", "function");
                tc.put("function", c.optJSONObject("function"));
                tcs.put(tc);
            }
            JSONObject delta = new JSONObject();
            delta.put("tool_calls", tcs);
            JSONObject ch = new JSONObject();
            ch.put("index", 0);
            ch.put("delta", delta);
            ch.put("finish_reason", JSONObject.NULL);
            o.put("id", id);
            o.put("object", "chat.completion.chunk");
            o.put("created", created);
            o.put("model", model);
            o.put("choices", new JSONArray().put(ch));
        } catch (Exception ignored) {}
        return o;
    }

    private static JSONObject textChunk(String id, long created, String model, String text) {
        JSONObject o = new JSONObject();
        try {
            JSONObject ch = new JSONObject();
            ch.put("index", 0);
            ch.put("text", text);
            ch.put("finish_reason", JSONObject.NULL);
            ch.put("logprobs", JSONObject.NULL);
            o.put("id", id);
            o.put("object", "text_completion");
            o.put("created", created);
            o.put("model", model);
            o.put("choices", new JSONArray().put(ch));
        } catch (Exception ignored) {}
        return o;
    }

    private static JSONObject finishTextChunk(String id, long created, String model) {
        JSONObject o = textChunk(id, created, model, "");
        try {
            o.getJSONArray("choices").getJSONObject(0).put("finish_reason", "stop");
        } catch (Exception ignored) {}
        return o;
    }

    /* --------------------------- prompt utils -------------------------- */

    /**
     * DeepSeek API працює зі станом сесії, тому історію повідомлень
     * склеюємо в один промт. Одне повідомлення йде без обгортки.
     */
    private static String flatten(JSONArray msgs) throws Exception {
        if (msgs == null || msgs.length() == 0) return "";
        StringBuilder sb = new StringBuilder();
        int n = msgs.length();
        boolean single = n == 1;
        for (int i = 0; i < n; i++) {
            JSONObject m = msgs.optJSONObject(i);
            if (m == null) continue;
            String role = m.optString("role", "user");
            String text = content(m.opt("content"));
            JSONArray calls = m.optJSONArray("tool_calls");
            if ("assistant".equals(role) && calls != null && calls.length() > 0) {
                if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '\n') sb.append('\n');
                sb.append("Assistant called tools: ").append(callList(calls)).append("\n\n");
                if (!text.isEmpty()) sb.append("Assistant: ").append(text).append("\n\n");
                continue;
            }
            if (text.isEmpty()) continue;
            if (single) return text;
            if ("system".equals(role) || "developer".equals(role)) sb.append("System: ");
            else if ("assistant".equals(role)) sb.append("Assistant: ");
            else if ("tool".equals(role) || "function".equals(role)) {
                sb.append("Tool result");
                String name = m.optString("name", "");
                if (!name.isEmpty()) sb.append(" (").append(name).append(")");
                sb.append(": ");
            } else sb.append("User: ");
            sb.append(text).append("\n\n");
        }
        // Не ріжемо тут: capPrompt() лишає початок (системний промт) і
        // кінець, а «хвіст останніх 200 КБ» викидав саме інструкції.
        return sb.toString().trim();
    }

    private static String callList(JSONArray calls) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < calls.length(); i++) {
            JSONObject c = calls.optJSONObject(i);
            if (c == null) continue;
            JSONObject f = c.optJSONObject("function");
            if (f == null) continue;
            if (b.length() > 0) b.append(", ");
            b.append(f.optString("name", "?")).append('(').append(f.optString("arguments", "{}")).append(')');
        }
        return b.toString();
    }

    private static String legacyPrompt(Object p) {
        if (p == null || p == JSONObject.NULL) return "";
        if (p instanceof String) return (String) p;
        if (p instanceof JSONArray) {
            StringBuilder b = new StringBuilder();
            JSONArray a = (JSONArray) p;
            for (int i = 0; i < a.length(); i++) {
                String s = a.optString(i, "");
                if (b.length() > 0) b.append('\n');
                b.append(s);
            }
            return b.toString();
        }
        return String.valueOf(p);
    }

    private static String content(Object c) throws Exception {
        if (c == null || c == JSONObject.NULL) return "";
        if (c instanceof String) return (String) c;
        if (c instanceof JSONArray) {
            StringBuilder b = new StringBuilder();
            JSONArray a = (JSONArray) c;
            for (int i = 0; i < a.length(); i++) {
                JSONObject p = a.optJSONObject(i);
                if (p == null) continue;
                String t = p.optString("text", "");
                if (!t.isEmpty()) {
                    if (b.length() > 0) b.append('\n');
                    b.append(t);
                }
            }
            return b.toString();
        }
        return String.valueOf(c);
    }

    /* -------------------------- http utils ----------------------------- */

    private JSONObject body(byte[] raw, OutputStream out) throws IOException {
        try {
            return new JSONObject(new String(raw, StandardCharsets.UTF_8));
        } catch (Exception e) {
            send(out, 400, "application/json", err("Invalid JSON body", "invalid_request_error"));
            return null;
        }
    }

    private boolean ready(OutputStream out) throws IOException {
        if (!st.token().isEmpty()) return true;
        send(out, 503, "application/json", err("No DeepSeek token in FunaChat settings", "server_error"));
        return false;
    }

    /** Ліміт/пауза Guard -> HTTP 429. */
    private static boolean limited(Throwable e) {
        return e instanceof Net.ApiException && ((Net.ApiException) e).code == 429;
    }

    /** Токен DeepSeek більше не діє -> HTTP 401, щоб клієнт оновив сесію. */
    private static boolean expired(Throwable e) {
        return e instanceof Net.ApiException && ((Net.ApiException) e).code == 401;
    }

    private static String message(Throwable e) {
        String m = e.getMessage();
        return m == null || m.isEmpty() ? e.getClass().getSimpleName() : m;
    }

    private static void sseHeaders(OutputStream out) throws IOException {
        String h = "HTTP/1.1 200 OK\r\n"
                + "Content-Type: text/event-stream; charset=utf-8\r\n"
                + "Cache-Control: no-cache\r\n"
                + "Access-Control-Allow-Origin: *\r\n"
                + "Access-Control-Allow-Headers: *\r\n"
                + "Access-Control-Allow-Methods: POST, GET, OPTIONS\r\n"
                + "Connection: close\r\n\r\n";
        out.write(h.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    private static void sse(OutputStream out, JSONObject o) throws IOException {
        sseRaw(out, o.toString());
    }

    private static void sseRaw(OutputStream out, String data) throws IOException {
        out.write(("data: " + data + "\n\n").getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    private static void send(OutputStream out, int code, String type, byte[] body) throws IOException {
        StringBuilder h = new StringBuilder();
        h.append("HTTP/1.1 ").append(code).append(reason(code)).append("\r\n");
        if (type != null) h.append("Content-Type: ").append(type).append("\r\n");
        h.append("Content-Length: ").append(body.length).append("\r\n");
        h.append("Access-Control-Allow-Origin: *\r\n");
        h.append("Access-Control-Allow-Headers: *\r\n");
        h.append("Access-Control-Allow-Methods: POST, GET, OPTIONS\r\n");
        h.append("Connection: close\r\n\r\n");
        out.write(h.toString().getBytes(StandardCharsets.UTF_8));
        if (body.length > 0) out.write(body);
        out.flush();
    }

    /** Текст причини: клієнти показують його в помилках, тому не «Error» на все. */
    private static String reason(int code) {
        switch (code) {
            case 200: return " OK";
            case 204: return " No Content";
            case 400: return " Bad Request";
            case 401: return " Unauthorized";
            case 403: return " Forbidden";
            case 404: return " Not Found";
            case 408: return " Request Timeout";
            case 413: return " Payload Too Large";
            case 429: return " Too Many Requests";
            case 431: return " Request Header Fields Too Large";
            case 500: return " Internal Server Error";
            case 502: return " Bad Gateway";
            case 503: return " Service Unavailable";
            default: return " Error";
        }
    }

    private static byte[] err(String msg, String code) {
        JSONObject e = new JSONObject();
        JSONObject o = new JSONObject();
        try {
            e.put("message", msg == null ? "error" : msg);
            boolean server = "server_error".equals(code) || "bad_gateway".equals(code);
            e.put("type", server ? "server_error"
                    : "rate_limit_error".equals(code) ? "rate_limit_error"
                    : "authentication_error".equals(code) ? "authentication_error"
                    : "invalid_request_error");
            e.put("code", code);
            o.put("error", e);
        } catch (Exception ignored) {}
        return o.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        int c;
        boolean over = false;
        while ((c = in.read()) >= 0) {
            if (c == '\n') break;
            if (c == '\r') continue;
            if (b.size() >= 16 * 1024) { over = true; continue; }
            b.write(c);
        }
        if (c < 0 && b.size() == 0 && !over) return null;
        // Слишком длинную строку дочитываем до конца, но отдаём обрезанной:
        // иначе остаток строки разбирался бы как следующий заголовок и
        // сбивал разбор запроса.
        return new String(b.toByteArray(), StandardCharsets.UTF_8);
    }

    private static byte[] readN(InputStream in, int len) throws IOException {
        byte[] d = new byte[len];
        int off = 0;
        while (off < len) {
            int n = in.read(d, off, len - off);
            if (n <= 0) break;
            off += n;
        }
        if (off == len) return d;
        byte[] t = new byte[off];
        System.arraycopy(d, 0, t, 0, off);
        return t;
    }
}
