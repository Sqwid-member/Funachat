package com.funa.chat;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

public class Store implements Guard.State {
    private final SharedPreferences p;
    private final Context ctx;

    public Store(Context c) {
        ctx = c.getApplicationContext();
        p = c.getSharedPreferences("funa", Context.MODE_PRIVATE);
    }

    public String userId() {
        JSONObject a = activeSummary();
        return a == null ? "" : a.optString("uid", "");
    }
    public String baseRaw() { return p.getString("base", ""); }
    public boolean thinking() { return p.getBoolean("thinking", true); }
    public boolean search() { return p.getBoolean("search", false); }
    public String model() { return p.getString("model", "default"); }
    /** Ліміт контексту в токенах. 1M — максимум DeepSeek V4.1 Flash. */
    public int ctxLimit() {
        int v = p.getInt("ctx_limit", 1000000);
        if (v < 60000) v = 60000;
        if (v > 1000000) v = 1000000;
        return v;
    }
    public void setCtxLimit(int v) {
        if (v < 60000) v = 60000;
        if (v > 1000000) v = 1000000;
        p.edit().putInt("ctx_limit", v).apply();
    }
    public String clientVersion() { return p.getString("cver", "2.7.1"); }
    public String device() { return p.getString("device", "2602EPTC0G"); }
    public String lang() { return p.getString("lang", "uk"); }
    /** Відбиток клієнта з session.json (щоб не відрізнятися від застосунку). */
    public String clientLocale() { return p.getString("clocale", "uk"); }
    public String clientTz() { return p.getString("ctz", "10800"); }

    /* ---------------- accounts (DeepSeek) ---------------- */

    private static final String K_ACC = "acc_v2";
    private static final String K_ACTIVE = "acc_active_id";
    private static final String K_EPOCH = "acc_epoch";

    private static String newId() {
        return "a" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    /** Список акаунтів. Токени всередині зашифровані (див. Vault). */
    private JSONArray readAcc() {
        String raw = p.getString(K_ACC, "");
        if (raw.isEmpty()) return migrateAcc();
        JSONArray out = new JSONArray();
        try {
            JSONArray a = new JSONArray(raw);
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.optJSONObject(i);
                if (o == null) continue;
                if (o.optString("id", "").isEmpty()) continue;
                out.put(o);
            }
        } catch (Exception e) {
            // Список акаунтів не читається: не затираємо його наступним
            // записом — зберігаємо копію, щоб було що відновити вручну.
            p.edit().putString(K_ACC + "_broken", raw).apply();
            Log.e("store", "accounts unreadable, kept a copy", e);
        }
        return out;
    }

    private void writeAcc(JSONArray a) {
        p.edit().putString(K_ACC, a.toString()).apply();
        modeTok = "";
        modeVal = "";
    }

    /**
     * Перенос старої схеми (окремий token + список з відкритими токенами)
     * у зашифрований список із стабільними id.
     */
    private JSONArray migrateAcc() {
        JSONArray out = new JSONArray();
        boolean lost = false;
        long now = System.currentTimeMillis();
        try {
            JSONArray old = null;
            String raw = p.getString("accounts", "");
            if (!raw.isEmpty()) {
                try { old = new JSONArray(raw); } catch (Exception ignored) {}
            }
            if (old != null) {
                for (int i = 0; i < old.length(); i++) {
                    JSONObject o = old.optJSONObject(i);
                    if (o == null) continue;
                    String tk = o.optString("token", "");
                    if (tk.isEmpty()) continue;
                    String sealed = Vault.seal(tk);
                    if (sealed == null) lost = true;
                    JSONObject n = new JSONObject();
                    n.put("id", newId());
                    n.put("tok", sealed == null ? "" : sealed);
                    n.put("uid", o.optString("uid", ""));
                    n.put("email", o.optString("email", ""));
                    n.put("phone", "");
                    n.put("name", "");
                    n.put("src", "legacy");
                    n.put("added", now);
                    n.put("used", now);
                    n.put("status", sealed == null ? "expired" : "ok");
                    out.put(n);
                }
            }
            if (out.length() == 0) {
                String tk = p.getString("token", "");
                if (!tk.isEmpty()) {
                    String sealed = Vault.seal(tk);
                    if (sealed == null) lost = true;
                    JSONObject n = new JSONObject();
                    n.put("id", newId());
                    n.put("tok", sealed == null ? "" : sealed);
                    n.put("uid", p.getString("uid", ""));
                    n.put("email", "");
                    n.put("phone", "");
                    n.put("name", "");
                    n.put("src", "legacy");
                    n.put("added", now);
                    n.put("used", now);
                    n.put("status", sealed == null ? "expired" : "ok");
                    out.put(n);
                }
            }
            String active = "";
            if (out.length() > 0) {
                int idx = p.getInt("acc_active", 0);
                idx = Math.max(0, Math.min(idx, out.length() - 1));
                JSONObject o = out.optJSONObject(idx);
                active = o == null ? "" : o.optString("id", "");
            }
            android.content.SharedPreferences.Editor ed = p.edit()
                    .putString(K_ACC, out.toString())
                    .putString(K_ACTIVE, active);
            // Якщо Keystore тимчасово недоступний, старі дані не видаляємо:
            // інакше токени зникли б назавжди.
            if (!lost) ed.remove("accounts").remove("acc_active").remove("token").remove("uid");
            ed.apply();
            if (out.length() > 0) Log.add("store", "accounts migrated: " + out.length());
        } catch (Exception e) {
            Log.e("store", "account migration failed", e);
        }
        return out;
    }

    private static JSONObject findById(JSONArray a, String id) {
        if (id == null || id.isEmpty()) return null;
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o != null && id.equals(o.optString("id", ""))) return o;
        }
        return null;
    }

    /** Лічильник змін акаунта: фонові відповіді старого акаунта відкидаються. */
    public int epoch() { return p.getInt(K_EPOCH, 0); }

    private void bumpEpoch() { p.edit().putInt(K_EPOCH, epoch() + 1).apply(); }

    public String activeId() {
        JSONArray a = readAcc();
        String id = p.getString(K_ACTIVE, "");
        if (findById(a, id) != null) return id;
        JSONObject first = a.optJSONObject(0);
        return first == null ? "" : first.optString("id", "");
    }

    public int activeIndex() {
        JSONArray a = readAcc();
        String id = activeId();
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o != null && id.equals(o.optString("id", ""))) return i;
        }
        return 0;
    }

    public String token() { return token(activeId()); }

    // Розшифрування токена — це звернення до Android Keystore (кілька мс).
    // Net створюється на кожен запит, тож кешуємо останній розшифрований
    // результат; ключ кешу — сам шифротекст, тому зміна акаунта його скидає.
    private String tokId = "", tokRaw = "", tokVal = "";

    public synchronized String token(String id) {
        JSONObject o = findById(readAcc(), id);
        String raw = o == null ? "" : o.optString("tok", "");
        if (raw.isEmpty()) return "";
        if (id.equals(tokId) && raw.equals(tokRaw)) return tokVal;
        String v = Vault.open(raw);
        // порожній результат може бути тимчасовим збоєм Keystore — не кешуємо
        if (!v.isEmpty()) { tokId = id; tokRaw = raw; tokVal = v; }
        return v;
    }

    public JSONObject activeAccount() {
        return findById(readAcc(), activeId());
    }

    private JSONObject summary(JSONObject o, String activeId) {
        JSONObject s = new JSONObject();
        try {
            String id = o.optString("id", "");
            String email = o.optString("email", "");
            String phone = o.optString("phone", "");
            s.put("id", id);
            s.put("email", email);
            s.put("phone", phone);
            s.put("name", o.optString("name", ""));
            s.put("uid", o.optString("uid", ""));
            s.put("added", o.optLong("added", 0));
            s.put("used", o.optLong("used", 0));
            boolean ready = !Vault.open(o.optString("tok", "")).isEmpty();
            String st = o.optString("status", "ok");
            if (st.isEmpty()) st = "ok";
            if (!ready) st = "expired";
            s.put("status", st);
            s.put("src", o.optString("src", ""));
            s.put("mode", o.optString("mode", "bearer"));
            s.put("active", id.equals(activeId));
            s.put("ready", ready);
        } catch (Exception ignored) {}
        return s;
    }

    public JSONArray accountSummary() {
        JSONArray a = readAcc();
        JSONArray out = new JSONArray();
        String active = activeId();
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o != null) out.put(summary(o, active));
        }
        return out;
    }

    public JSONObject activeSummary() {
        JSONObject o = findById(readAcc(), activeId());
        return o == null ? null : summary(o, activeId());
    }

    /** Додає або оновлює акаунт і робить його активним. null — помилка сховища. */
    public synchronized String addAccount(String token, String uid, String email,
                                          String phone, String name, String src) {
        return addAccountInternal(token, "bearer", uid, email, phone, name, src);
    }

    /** Акаунт, авторизований cookie-сесією сайту (без Bearer-токена). */
    public synchronized String addAccountCookie(String cookie, String uid, String email,
                                                String phone, String name, String src) {
        return addAccountInternal(cookie, "cookie", uid, email, phone, name, src);
    }

    /** Режим авторизації для збереженого токена/cookie. */
    // Режим ("bearer"/"cookie") шукається за розшифрованим токеном. Без кешу
    // кожен Net-обʼєкт розшифровував би токени всіх акаунтів через Keystore.
    private volatile String modeTok = "", modeVal = "";

    public synchronized String accountMode(String token) {
        if (token == null || token.isEmpty()) return "";
        if (token.equals(modeTok)) return modeVal;
        JSONArray a = readAcc();
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o == null) continue;
            if (token.equals(Vault.open(o.optString("tok", "")))) {
                modeTok = token;
                modeVal = o.optString("mode", "bearer");
                return modeVal;
            }
        }
        return "";
    }

    private synchronized String addAccountInternal(String token, String mode,
                                                   String uid, String email,
                                                   String phone, String name, String src) {
        if (token == null || token.trim().isEmpty()) return null;
        token = token.trim();
        // Bearer ~1–2 КБ, cookie сайту — до кількох КБ. Все, що більше,
        // це вже не сесія, а випадковий файл: не роздуваємо сховище.
        if (token.length() > 16384) return null;
        String sealed = Vault.seal(token);
        if (sealed == null) return null;
        JSONArray a = readAcc();
        long now = System.currentTimeMillis();
        int found = -1;
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o == null) continue;
            String ouid = o.optString("uid", "");
            String oemail = o.optString("email", "");
            if (!uid.isEmpty() && !ouid.isEmpty() && uid.equals(ouid)) { found = i; break; }
            if (!email.isEmpty() && !oemail.isEmpty() && email.equalsIgnoreCase(oemail)) { found = i; break; }
            if (sealed.equals(o.optString("tok", ""))) { found = i; break; }
            if (token.equals(Vault.open(o.optString("tok", "")))) { found = i; break; }
        }
        if (found < 0 && a.length() >= 30) return null;
        JSONObject o;
        if (found >= 0) {
            o = a.optJSONObject(found);
        } else {
            o = new JSONObject();
            found = a.length();
            a.put(o);
        }
        try {
            o.put("id", o.optString("id", newId()));
            o.put("tok", sealed);
            // поля профілю показуються в списку акаунтів: без меж вони
            // роздувають prefs і ламають верстку
            if (uid != null && !uid.isEmpty()) o.put("uid", clip(uid, 64));
            if (email != null && !email.isEmpty()) o.put("email", clip(email, 200));
            if (phone != null && !phone.isEmpty()) o.put("phone", clip(phone, 40));
            if (name != null && !name.isEmpty()) o.put("name", clip(name, 100));
            o.put("src", src == null || src.isEmpty() ? "token" : src);
            o.put("mode", "cookie".equals(mode) ? "cookie" : "bearer");
            if (o.optLong("added", 0) == 0) o.put("added", now);
            o.put("used", now);
            o.put("status", "ok");
        } catch (Exception ignored) {}
        writeAcc(a);
        p.edit().putString(K_ACTIVE, o.optString("id", "")).apply();
        bumpEpoch();
        return o.optString("id", "");
    }

    public String addAccount(String token, String uid, String email) {
        return addAccount(token, uid, email, "", "", "token");
    }

    public synchronized boolean switchAccount(String id) {
        JSONArray a = readAcc();
        JSONObject o = findById(a, id);
        if (o == null) return false;
        if (id.equals(p.getString(K_ACTIVE, ""))) return true;
        try { o.put("used", System.currentTimeMillis()); writeAcc(a); } catch (Exception ignored) {}
        p.edit().putString(K_ACTIVE, id).apply();
        bumpEpoch();
        return true;
    }

    public boolean switchAccount(int index) {
        JSONArray a = readAcc();
        JSONObject o = a.optJSONObject(index);
        return o != null && switchAccount(o.optString("id", ""));
    }

    public synchronized void deleteAccount(String id) {
        JSONArray a = readAcc();
        JSONArray out = new JSONArray();
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o == null) continue;
            if (!id.equals(o.optString("id", ""))) out.put(o);
        }
        writeAcc(out);
        String active = p.getString(K_ACTIVE, "");
        if (active.equals(id) || findById(out, active) == null) {
            JSONObject first = out.optJSONObject(0);
            active = first == null ? "" : first.optString("id", "");
            p.edit().putString(K_ACTIVE, active).apply();
        }
        // Пауза й денний лічильник належать видаленому акаунту.
        guardPurge(id);
        bumpEpoch();
    }

    public void deleteAccount(int index) {
        JSONArray a = readAcc();
        JSONObject o = a.optJSONObject(index);
        if (o != null) deleteAccount(o.optString("id", ""));
    }

    public synchronized void updateAccount(String id, String uid, String email,
                                           String phone, String name) {
        JSONArray a = readAcc();
        JSONObject o = findById(a, id);
        if (o == null) return;
        try {
            // Профіль приходить від сервера: ті самі межі, що і в addAccount.
            if (uid != null && !uid.isEmpty()) o.put("uid", clip(uid, 64));
            if (email != null && !email.isEmpty()) o.put("email", clip(email, 200));
            if (phone != null && !phone.isEmpty()) o.put("phone", clip(phone, 40));
            if (name != null && !name.isEmpty()) o.put("name", clip(name, 100));
            writeAcc(a);
        } catch (Exception ignored) {}
    }

    public synchronized void setStatus(String id, String status) {
        JSONArray a = readAcc();
        JSONObject o = findById(a, id);
        if (o == null) return;
        try { o.put("status", status == null || status.isEmpty() ? "ok" : status); writeAcc(a); }
        catch (Exception ignored) {}
    }

    public void setEmail(String email) {
        updateAccount(activeId(), "", email, "", "");
    }

    public void setName(String name) {
        updateAccount(activeId(), "", "", "", name);
    }

    /* ---------------- local API ---------------- */

    /**
     * Локальний API має поважати блокування застосунку: інакше будь-хто в
     * тій самій мережі з ключем обходив би пароль FunaChat і писав від імені
     * акаунта DeepSeek. Ключі ті самі, що в Auth; префікси тут продубльовані,
     * бо ApiServer не має Context, щоб створити Auth.
     */
    public boolean apiLocked() {
        String id = activeId();
        if (id.isEmpty()) return false;
        if (!p.getBoolean("auth_lock_" + id, false)) return false;
        if (p.getString("auth_pw_" + id, "").isEmpty()) return false;
        return !p.getBoolean("auth_session_" + id, false);
    }

    /* ---------- діагностична телеметрія ---------- */

    /** Типово увімкнена: знеособлений знімок раз на добу. */
    public boolean telemetryOn() { return p.getBoolean("telemetry_on", true); }

    public void setTelemetry(boolean b) { p.edit().putBoolean("telemetry_on", b).apply(); }

    /** Випадковий ідентифікатор інсталяції; не пов'язаний з акаунтом чи ключем. */
    public String telemetryId() {
        String id = p.getString("telemetry_id", "");
        if (id.length() == 16) return id;
        id = randomHex(16);
        p.edit().putString("telemetry_id", id).apply();
        return id;
    }

    public long telemetryLast() { return p.getLong("telemetry_last", 0); }

    /** HTTPS-приймач (Cloudflare Worker). Порожній — локальна черга + Termux. */
    public String telemetryUrl() {
        String v = p.getString("telemetry_url", "").trim();
        return v.isEmpty() ? Telemetry.DEFAULT_URL : v;
    }

    public void setTelemetryUrl(String url) {
        String v = url == null ? "" : url.trim();
        if (v.length() > 300) v = v.substring(0, 300);
        p.edit().putString("telemetry_url", v).apply();
    }

    public void telemetryMarkSent(long ts) { p.edit().putLong("telemetry_last", ts).apply(); }

    public boolean apiAuto() { return p.getBoolean("api_auto", true); }
    public int apiPort() { return p.getInt("api_port", 8787); }
    public boolean apiLan() { return p.getBoolean("api_lan", false); }
    public boolean apiDelete() { return p.getBoolean("api_del", true); }

    /** Публічний доступ до API. Вимкнений — запити з тунелю отримують 403. */
    public boolean apiPublic() { return p.getBoolean("api_public", true); }
    public void setApiPublic(boolean b) { p.edit().putBoolean("api_public", b).apply(); }

    /* ---------- API-ключі: кілька, з терміном дії ---------- */

    // 48 hex = 192 біти. Старі 32-символьні ключі лишаються валідними.
    private static final int KEY_HEX = 48;
    private final Object keyLock = new Object();

    private static String randomHex(int n) {
        // ключ до локального API — це пароль до акаунта DeepSeek:
        // звичайний Random із часовим зерном тут непридатний
        java.security.SecureRandom r = new java.security.SecureRandom();
        String hex = "0123456789abcdef";
        StringBuilder b = new StringBuilder(n);
        for (int i = 0; i < n; i++) b.append(hex.charAt(r.nextInt(16)));
        return b.toString();
    }

    private static String randomKey() { return "fk-" + randomHex(KEY_HEX); }

    private static String randomId() { return randomHex(8); }

    private static JSONObject makeKey(String name, String key, long ttlMs) {
        JSONObject o = new JSONObject();
        try {
            long now = System.currentTimeMillis();
            o.put("id", randomId());
            o.put("name", name == null || name.trim().isEmpty() ? "API" : name.trim());
            o.put("key", key);
            o.put("created", now);
            o.put("exp", ttlMs > 0 ? now + ttlMs : 0L);
            o.put("used", 0L);
            o.put("hits", 0L);
            o.put("lastIp", "");
        } catch (Exception ignored) {}
        return o;
    }

    /** Масив ключів; старий одиночний ключ мігрує при першому звертанні. */
    private JSONArray keysLocked() {
        JSONArray a = null;
        if (p.contains("api_keys")) {
            try { a = new JSONArray(p.getString("api_keys", "[]")); } catch (Exception ignored) {}
            if (a != null) return a;
        }
        a = new JSONArray();
        String legacy = p.getString("api_key", "");
        if (legacy != null && legacy.startsWith("fk-") && legacy.length() >= 16)
            a.put(makeKey("Основний", legacy, 0));
        else
            a.put(makeKey("Основний", randomKey(), 0));
        p.edit().putString("api_keys", a.toString()).apply();
        return a;
    }

    private void saveKeys(JSONArray a) {
        p.edit().putString("api_keys", a.toString()).apply();
    }

    private static String normalizeKey(String presented) {
        if (presented == null) return "";
        presented = presented.trim();
        if (presented.toLowerCase(java.util.Locale.US).startsWith("bearer "))
            presented = presented.substring(7).trim();
        return presented;
    }

    private static String maskOf(String key) {
        if (key == null) return "";
        return key.length() > 12 ? key.substring(0, 7) + "…" + key.substring(key.length() - 4) : key;
    }

    /** Публічне (без самого ключа) подання запису. */
    private static JSONObject keyInfo(JSONObject k, long now) {
        JSONObject o = new JSONObject();
        try {
            String key = k.optString("key", "");
            long exp = k.optLong("exp", 0);
            o.put("id", k.optString("id", ""));
            o.put("name", k.optString("name", "API"));
            o.put("mask", maskOf(key));
            o.put("len", key.length());
            o.put("created", k.optLong("created", 0));
            o.put("exp", exp);
            o.put("used", k.optLong("used", 0));
            o.put("hits", k.optLong("hits", 0));
            o.put("lastIp", k.optString("lastIp", ""));
            o.put("expired", exp > 0 && now >= exp);
        } catch (Exception ignored) {}
        return o;
    }

    /** Публічний список без самих ключів. */
    public JSONArray apiKeyList() {
        JSONArray out = new JSONArray();
        synchronized (keyLock) {
            JSONArray a = keysLocked();
            long now = System.currentTimeMillis();
            for (int i = 0; i < a.length(); i++) {
                JSONObject k = a.optJSONObject(i);
                if (k != null) out.put(keyInfo(k, now));
            }
        }
        return out;
    }

    /** Створити ключ; ttlMs <= 0 — безстроковий. Повертає об'єкт із самим ключем. */
    public JSONObject apiKeyAdd(String name, long ttlMs) {
        synchronized (keyLock) {
            JSONArray a = keysLocked();
            JSONObject k = makeKey(name, randomKey(), ttlMs);
            a.put(k);
            saveKeys(a);
            return k;
        }
    }

    public boolean apiKeyDelete(String id) {
        synchronized (keyLock) {
            JSONArray a = keysLocked();
            JSONArray out = new JSONArray();
            boolean removed = false;
            for (int i = 0; i < a.length(); i++) {
                JSONObject k = a.optJSONObject(i);
                if (k == null) continue;
                if (id != null && id.equals(k.optString("id", ""))) { removed = true; continue; }
                out.put(k);
            }
            if (removed) saveKeys(out);
            return removed;
        }
    }

    /** Відкликати за самим значенням ключа (аудит злитого ключа). */
    public boolean apiKeyDeleteByValue(String presented) {
        String value = normalizeKey(presented);
        if (value.isEmpty()) return false;
        synchronized (keyLock) {
            JSONArray a = keysLocked();
            JSONArray out = new JSONArray();
            boolean removed = false;
            byte[] got = value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            for (int i = 0; i < a.length(); i++) {
                JSONObject k = a.optJSONObject(i);
                if (k == null) continue;
                String key = k.optString("key", "");
                if (!key.isEmpty() && java.security.MessageDigest.isEqual(
                        key.getBytes(java.nio.charset.StandardCharsets.UTF_8), got)) {
                    removed = true;
                    continue;
                }
                out.put(k);
            }
            if (removed) saveKeys(out);
            return removed;
        }
    }

    /** Довідка про ключ за самим значенням; null — не належить цьому пристрою. */
    public JSONObject apiKeyAudit(String presented) {
        String value = normalizeKey(presented);
        if (value.isEmpty()) return null;
        synchronized (keyLock) {
            JSONArray a = keysLocked();
            long now = System.currentTimeMillis();
            byte[] got = value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            for (int i = 0; i < a.length(); i++) {
                JSONObject k = a.optJSONObject(i);
                if (k == null) continue;
                String key = k.optString("key", "");
                if (key.isEmpty()) continue;
                if (!java.security.MessageDigest.isEqual(
                        key.getBytes(java.nio.charset.StandardCharsets.UTF_8), got)) continue;
                JSONObject info = keyInfo(k, now);
                try { info.put("remaining", a.length()); } catch (Exception ignored) {}
                return info;
            }
        }
        return null;
    }

    /** Повний ключ за id — лише для копіювання в буфер, у UI не показується. */
    public String apiKeyById(String id) {
        synchronized (keyLock) {
            JSONArray a = keysLocked();
            for (int i = 0; i < a.length(); i++) {
                JSONObject k = a.optJSONObject(i);
                if (k != null && id != null && id.equals(k.optString("id", "")))
                    return k.optString("key", "");
            }
        }
        return "";
    }

    /** Перший чинний ключ — для показу, копіювання та /health. */
    public String ensureApiKey() {
        synchronized (keyLock) {
            JSONArray a = keysLocked();
            long now = System.currentTimeMillis();
            for (int i = 0; i < a.length(); i++) {
                JSONObject k = a.optJSONObject(i);
                if (k == null) continue;
                long exp = k.optLong("exp", 0);
                if (exp == 0 || now < exp) return k.optString("key", "");
            }
            // усі прострочені або список порожній — новий безстроковий
            JSONObject k = makeKey("Основний", randomKey(), 0);
            a.put(k);
            saveKeys(a);
            return k.optString("key", "");
        }
    }

    public String newApiKey() { return apiKeyAdd("Основний", 0).optString("key", ""); }

    /**
     * Перевірка пред'явленого ключа.
     * 0 — чинний, 1 — прострочений, 2 — невідомий.
     */
    public int apiKeyCheck(String presented) { return apiKeyCheck(presented, ""); }

    public int apiKeyCheck(String presented, String ip) {
        String value = normalizeKey(presented);
        if (value.isEmpty()) return 2;
        byte[] got = value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        synchronized (keyLock) {
            JSONArray a = keysLocked();
            long now = System.currentTimeMillis();
            int res = 2;
            boolean matched = false;
            for (int i = 0; i < a.length(); i++) {
                JSONObject k = a.optJSONObject(i);
                if (k == null) continue;
                String key = k.optString("key", "");
                if (key.isEmpty()) continue;
                // сталі за часом порівняння; проходимо всі ключі, щоб не
                // зливати, який саме збігся
                if (!java.security.MessageDigest.isEqual(
                        key.getBytes(java.nio.charset.StandardCharsets.UTF_8), got)) continue;
                matched = true;
                long exp = k.optLong("exp", 0);
                res = (exp > 0 && now >= exp) ? 1 : 0;
                if (res == 0) {
                    long used = k.optLong("used", 0);
                    if (now - used > 60000L) {
                        try {
                            k.put("used", now);
                            k.put("hits", k.optLong("hits", 0) + 1);
                            if (ip != null && !ip.isEmpty()) k.put("lastIp", ip);
                        } catch (Exception ignored) {}
                        saveKeys(a);
                    }
                }
            }
            return matched ? res : 2;
        }
    }

    /* ---------- аудит злитих ключів ---------- */

    /** Секрет аудиту цього пристрою (fa- + 64 hex). */
    public String ensureAuditSecret() {
        synchronized (keyLock) {
            String s = p.getString("audit_secret", "");
            if (s.startsWith("fa-") && s.length() >= 32) return s;
            s = "fa-" + randomHex(64);
            p.edit().putString("audit_secret", s).apply();
            return s;
        }
    }

    public String rotateAuditSecret() {
        synchronized (keyLock) {
            String s = "fa-" + randomHex(64);
            p.edit().putString("audit_secret", s).apply();
            return s;
        }
    }

    public boolean auditCheck(String presented) {
        if (presented == null) return false;
        presented = presented.trim();
        if (presented.isEmpty()) return false;
        String s = ensureAuditSecret();
        return java.security.MessageDigest.isEqual(
                s.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                presented.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public String auditMasked() { return maskOf(ensureAuditSecret()); }

    public String auditContact() { return p.getString("audit_contact", ""); }
    public String auditHook() { return p.getString("audit_hook", ""); }

    public void setAuditContacts(String contact, String hook) {
        p.edit()
                .putString("audit_contact", contact == null ? "" : contact.trim())
                .putString("audit_hook", hook == null ? "" : hook.trim())
                .apply();
    }

    /** Власник цього пристрою — для відповіді аудиту. */
    public JSONObject ownerInfo() {
        JSONObject o = new JSONObject();
        try {
            JSONObject a = activeSummary();
            String email = a == null ? "" : a.optString("email", "");
            String name = a == null ? "" : a.optString("name", "");
            if (name.isEmpty()) name = email;
            o.put("name", name);
            o.put("email", email);
            o.put("contact", auditContact());
            o.put("device", android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL);
            o.put("publicUrl", pubUrl());
        } catch (Exception ignored) {}
        return o;
    }

    private JSONArray readAuditLog() {
        try { return new JSONArray(p.getString("audit_log", "[]")); }
        catch (Exception e) { return new JSONArray(); }
    }

    /** Останні події аудиту, новіші — зверху. */
    public JSONArray auditLog(int limit) {
        JSONArray a = readAuditLog();
        JSONArray out = new JSONArray();
        for (int i = a.length() - 1; i >= 0 && out.length() < limit; i--) {
            JSONObject o = a.optJSONObject(i);
            if (o != null) out.put(o);
        }
        return out;
    }

    private void auditLogAdd(String action, JSONObject key, String note, String ip) {
        synchronized (keyLock) {
            JSONArray a = readAuditLog();
            JSONObject e = new JSONObject();
            try {
                e.put("ts", System.currentTimeMillis());
                e.put("action", action);
                e.put("id", key == null ? "" : key.optString("id", ""));
                e.put("name", key == null ? "" : key.optString("name", ""));
                e.put("mask", key == null ? "" : key.optString("mask", ""));
                String n = note == null ? "" : note.trim();
                if (n.length() > 500) n = n.substring(0, 500);
                e.put("note", n);
                e.put("ip", ip == null ? "" : ip);
            } catch (Exception ignored) {}
            a.put(e);
            // не роздумуємо сховище: тримаємо останні 100 подій
            if (a.length() > 100) {
                JSONArray cut = new JSONArray();
                for (int i = a.length() - 100; i < a.length(); i++) {
                    JSONObject o = a.optJSONObject(i);
                    if (o != null) cut.put(o);
                }
                a = cut;
            }
            p.edit().putString("audit_log", a.toString()).apply();
        }
    }

    /** Записати подію без повідомлення (напр. звичайна перевірка info). */
    public void auditRecord(String action, JSONObject key, String note, String ip) {
        auditLogAdd(action, key, note, ip);
    }

    /** Записати подію, показати Toast і (якщо задано) відправити webhook. */
    public void auditNotify(String action, JSONObject key, String note, String ip) {
        auditLogAdd(action, key, note, ip);
        String mask = key == null ? "" : key.optString("mask", "");
        String verb = "revoke".equals(action) ? "відкликано"
                : ("notify".equals(action) ? "перевірено, власника повідомлено" : "перевірено");
        toast("FunaChat: ключ " + mask + " " + verb + " через аудит");
        final String hook = auditHook();
        if (!hook.isEmpty()) {
            final String act = action;
            final JSONObject k = key;
            final String n = note;
            final String src = ip;
            Thread t = new Thread(() -> postAuditHook(hook, act, k, n, src));
            t.setDaemon(true);
            t.start();
        }
    }

    private void postAuditHook(String hook, String action, JSONObject key, String note, String ip) {
        try {
            java.net.URL u = new java.net.URL(hook);
            String proto = u.getProtocol();
            if (!"http".equals(proto) && !"https".equals(proto)) return;
            String text = "FunaChat: аудит ключа " + (key == null ? "" : key.optString("mask", ""))
                    + " — " + action + (note == null || note.isEmpty() ? "" : " (" + note + ")");
            JSONObject body = new JSONObject();
            body.put("text", text);
            body.put("content", text);
            body.put("message", text);
            body.put("event", action);
            body.put("key", key == null ? new JSONObject() : key);
            body.put("owner", ownerInfo());
            body.put("note", note == null ? "" : note);
            body.put("ip", ip == null ? "" : ip);
            body.put("ts", System.currentTimeMillis());
            // Telegram Bot API принимает chat_id; для ntfy/Slack/Discord
            // лишнее поле безвредно.
            String contact = auditContact();
            if (!contact.isEmpty()) body.put("chat_id", contact);
            byte[] data = body.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            java.net.HttpURLConnection c = (java.net.HttpURLConnection) u.openConnection();
            c.setConnectTimeout(5000);
            c.setReadTimeout(5000);
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            c.getOutputStream().write(data);
            c.getResponseCode();
            c.disconnect();
        } catch (Exception ignored) {}
    }

    private void toast(String msg) {
        try {
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                    android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_LONG).show());
        } catch (Exception ignored) {}
    }

    /** Публічна адреса API (тунель cloudflared/zrok). Порожня — доступу ззовні немає. */
    public String pubUrl() { return p.getString("pub_url", ""); }
    public void setPubUrl(String u) {
        u = u == null ? "" : u.trim();
        while (u.endsWith("/")) u = u.substring(0, u.length() - 1);
        if (!u.isEmpty() && !(u.startsWith("https://") || u.startsWith("http://"))) u = "https://" + u;
        p.edit().putString("pub_url", u).apply();
    }
    /** Окремий канал оновлень; порожній — береться <pubUrl>/updates/latest.json. */
    public String updUrl() { return p.getString("upd_url", ""); }
    public void setUpdUrl(String u) {
        u = u == null ? "" : u.trim();
        while (u.endsWith("/")) u = u.substring(0, u.length() - 1);
        if (!u.isEmpty() && !(u.startsWith("https://") || u.startsWith("http://"))) u = "https://" + u;
        p.edit().putString("upd_url", u).apply();
    }

    public void setApiAuto(boolean b) { p.edit().putBoolean("api_auto", b).apply(); }
    public void setApiLan(boolean b) { p.edit().putBoolean("api_lan", b).apply(); }
    public void setApiDelete(boolean b) { p.edit().putBoolean("api_del", b).apply(); }

    public void setApiPort(int port) {
        if (port < 1024 || port > 65535) port = 8787;
        p.edit().putInt("api_port", port).apply();
    }

    /* ---------------- anti-ban guard ---------------- */

    /** Завжди згортати блоки мислення/інструментів у чаті. */
    public boolean foldAll() { return p.getBoolean("fold_all", false); }
    public void setFoldAll(boolean b) { p.edit().putBoolean("fold_all", b).apply(); }

    /** Стабільний ідентифікатор пристрою для запитів входу в DeepSeek. */
    public String deviceId() {
        String d = p.getString("ds_device", "");
        if (d == null || d.isEmpty()) {
            d = java.util.UUID.randomUUID().toString();
            p.edit().putString("ds_device", d).apply();
        }
        return d;
    }

    /* ---------------- crash notices ---------------- */

    public boolean crashSeen() { return p.getBoolean("crash_seen", false); }
    public void setCrashSeen(boolean b) { p.edit().putBoolean("crash_seen", b).apply(); }

    /* ---------------- first-run intro ---------------- */

    public boolean introSeen() { return p.getBoolean("intro_seen", false); }
    public void setIntroSeen(boolean b) { p.edit().putBoolean("intro_seen", b).apply(); }

    /* ---------------- contacts for reports ---------------- */

    public String contactEmail() { return p.getString("c_email", ""); }
    public String contactPhone() { return p.getString("c_phone", ""); }
    public String contactGoogle() { return p.getString("c_google", ""); }

    public void setContacts(String email, String phone, String google) {
        p.edit()
                .putString("c_email", email == null ? "" : email.trim())
                .putString("c_phone", phone == null ? "" : phone.trim())
                .putString("c_google", google == null ? "" : google.trim())
                .apply();
    }

    /**
     * Ключ лічильника Guard. Ліміти й пауза — властивість акаунта, а не
     * пристрою: після 429 на одному акаунті інший має працювати далі.
     * Налаштування (g_on/g_gap/g_permin/g_perday) лишаються спільними.
     */
    private String gkey(String base) {
        String id = activeId();
        return id.isEmpty() ? base : base + "_" + id;
    }

    public boolean guardOn() { return p.getBoolean("g_on", true); }
    public void setGuardOn(boolean b) { p.edit().putBoolean("g_on", b).apply(); }
    public int guardGapMs() { return p.getInt("g_gap", 3000); }
    public int guardPerMin() { return p.getInt("g_permin", 12); }
    public int guardPerDay() { return p.getInt("g_perday", 400); }
    public int guardCount() { return p.getInt(gkey("g_count"), 0); }
    public long guardDay() { return p.getLong(gkey("g_day"), 0); }
    public long guardLast() { return p.getLong(gkey("g_last"), 0); }
    public long guardCooldown() { return p.getLong(gkey("g_cool"), 0); }
    public String guardNote() { return p.getString(gkey("g_note"), ""); }
    public long guardMinStart() { return p.getLong(gkey("g_min"), 0); }
    public int guardMinCount() { return p.getInt(gkey("g_minc"), 0); }

    public void guardSave(int count, long day, long last, long cooldown, String note,
                          long minStart, int minCount) {
        p.edit()
                .putInt(gkey("g_count"), count)
                .putLong(gkey("g_day"), day)
                .putLong(gkey("g_last"), last)
                .putLong(gkey("g_cool"), cooldown)
                .putString(gkey("g_note"), note == null ? "" : note)
                .putLong(gkey("g_min"), minStart)
                .putInt(gkey("g_minc"), minCount)
                .apply();
    }

    /** Прибирає лічильники/паузу видаленого акаунта. */
    public void guardPurge(String id) {
        if (id == null || id.isEmpty()) return;
        p.edit()
                .remove("g_count_" + id)
                .remove("g_day_" + id)
                .remove("g_last_" + id)
                .remove("g_cool_" + id)
                .remove("g_note_" + id)
                .remove("g_min_" + id)
                .remove("g_minc_" + id)
                .apply();
    }

    public void setGuardGap(int ms) {
        if (ms < 0) ms = 0;
        if (ms > 60000) ms = 60000;
        p.edit().putInt("g_gap", ms).apply();
    }

    public void setGuardPerMin(int n) {
        if (n < 1) n = 1;
        if (n > 600) n = 600;
        p.edit().putInt("g_permin", n).apply();
    }

    public void setGuardPerDay(int n) {
        if (n < 1) n = 1;
        if (n > 100000) n = 100000;
        p.edit().putInt("g_perday", n).apply();
    }

    /** Видаляти session.json із Download після успішного імпорту. */
    public boolean importClean() { return p.getBoolean("imp_clean", true); }

    /* ---------------- settings ---------------- */

    /** Замінює токен активного акаунта (наприклад, після повторного входу). */
    public synchronized void setToken(String t) {
        String id = activeId();
        JSONArray a = readAcc();
        JSONObject o = findById(a, id);
        if (o == null) return;
        String sealed = Vault.seal(t == null ? "" : t.trim());
        try {
            o.put("tok", sealed == null ? "" : sealed);
            o.put("status", sealed == null ? "expired" : "ok");
            o.put("used", System.currentTimeMillis());
            writeAcc(a);
        } catch (Exception ignored) {}
    }

    /** Власний системний промт: 12 000 символів — уже занадто довго. */
    public void setBase(String t) {
        if (t == null) t = "";
        if (t.length() > 12000) t = t.substring(0, 12000);
        p.edit().putString("base", t).apply();
    }
    public void setThinking(boolean b) { p.edit().putBoolean("thinking", b).apply(); }
    public void setSearch(boolean b) { p.edit().putBoolean("search", b).apply(); }
    public void setModel(String m) {
        if (!"expert".equals(m)) m = "default";
        p.edit().putString("model", m).apply();
    }
    // Ці поля йдуть у заголовки запиту: обрізаємо, щоб session.json не міг
    // підсунути мегабайтний «відбиток клієнта».
    private static String clip(String v, int max) {
        if (v == null) return "";
        v = v.trim();
        return v.length() > max ? v.substring(0, max) : v;
    }
    public void setClientVersion(String v) { p.edit().putString("cver", clip(v, 40)).apply(); }
    public void setDevice(String d) { p.edit().putString("device", clip(d, 80)).apply(); }
    public void setLang(String l) {
        if (!"en".equals(l) && !"ru".equals(l)) l = "uk";
        p.edit().putString("lang", l).apply();
    }
    public void setClientLocale(String v) { if (v != null && !v.trim().isEmpty()) p.edit().putString("clocale", clip(v, 20)).apply(); }
    public void setClientTz(String v) { if (v != null && !v.trim().isEmpty()) p.edit().putString("ctz", clip(v, 10)).apply(); }

    /** Порожній base = автоматичний промт мовою інтерфейсу. */
    public String base(String lang) {
        String raw = baseRaw();
        return raw.isEmpty() ? defaultBase(lang) : raw;
    }

    public static String defaultBase(String lang) {
        if (lang == null) lang = "uk";
        switch (lang) {
            case "en": return EN;
            case "ru": return RU;
            default: return UK;
        }
    }

    private static final String ARTIFACT_UK =
        "\n\nКоли користувач просить інтерактивну сторінку, демо, гру, калькулятор або візуалізацію — " +
        "спершу виклич інструмент skill з name=\"design\", якщо ще не читав його в цьому чаті, і дотримуйся інструкції. " +
        "Потім поверни ПОВНИЙ самодостатній HTML-документ у блоці ```html ... ```. Усе CSS і JS має бути всередині, " +
        "без зовнішніх запитів. Клієнт покаже його як інтерактивну картку з кнопками розгортання й завантаження.\n" +
        "Математику пиши в LaTeX: $...$ для рядкових формул, $$...$$ для окремих блоків.";

    private static final String ARTIFACT_EN =
        "\n\nWhen the user asks for an interactive page, demo, game, calculator or visualization — " +
        "first call the skill tool with name=\"design\" if you have not read it in this chat, and follow it. " +
        "Then return a FULL self-contained HTML document inside a ```html ... ``` block. All CSS and JS must be inline, " +
        "with no external requests. The client renders it as an interactive card with expand and download buttons.\n" +
        "Write math in LaTeX: $...$ for inline formulas, $$...$$ for display blocks.";

    private static final String ARTIFACT_RU =
        "\n\nКогда пользователь просит интерактивную страницу, демо, игру, калькулятор или визуализацию — " +
        "сначала вызови инструмент skill с name=\"design\", если ещё не читал его в этом чате, и следуй инструкции. " +
        "Затем верни ПОЛНЫЙ самодостаточный HTML-документ в блоке ```html ... ```. Все CSS и JS должны быть внутри, " +
        "без внешних запросов. Клиент покажет его как интерактивную карточку с кнопками разворота и скачивания.\n" +
        "Математику пиши в LaTeX: $...$ для строчных формул, $$...$$ для отдельных блоков.";

    public static final String UK =
        "Ти — корисний агент у застосунку FunaChat. Відповідай тією ж мовою, якою пише користувач, " +
        "зазвичай українською. Будь точним і стислим, без зайвих вступів.\n\n" +
        "У тебе є інструменти. Коли потрібно працювати з файлами або терміналом, виводь РІВНО один блок:\n" +
        "```funa\n" +
        "{\"tool\":\"read\",\"path\":\"Download/FunaFiles/notes.txt\"}\n" +
        "```\n" +
        "Після блоку зупинись і чекай: клієнт виконає інструмент і надішле результат наступним повідомленням.\n\n" +
        "Інструменти:\n" +
        "- read {path, offset?, limit?} — прочитати текстовий файл (шлях від /sdcard або відносний до /sdcard/Download/FunaFiles; будь-яке розширення)\n" +
        "- write {path, content} — створити або повністю перезаписати файл\n" +
        "- append {path, content} — дописати в кінець\n" +
        "- edit {path, find, replace} — замінити фрагмент (перше входження)\n" +
        "- list {path} — список файлів і папок\n" +
        "- sh {cmd, timeout?} — виконати команду через міст термінала (Termux/ReTerminal), повертає stdout+stderr\n" +
        "- doc {format:\"pdf\"|\"docx\", title, text, path?} — створити PDF або Word у /sdcard/Download/FunaFiles/docs\n" +
        "- skill {name:\"docx\"|\"pdf\"|\"design\"|\"docs\"} — прочитати вбудовану навичку з правилами; виклич ПЕРЕД створенням файлу\n" +
        "- time {tz?, offset_days?, offset?} — поточні дата й час (ISO, зона, epoch)\n" +
        "- calc {expr, deg?} — калькулятор: + - * / % ^, дужки, sin/cos/tan, log, sqrt, pi\n" +
        "- convert {value, from, to} — одиниці: довжина, маса, дані, швидкість, об'єм, температура\n" +
        "- random {kind, min?, max?, items?, count?} — int/float/uuid/bytes/choice\n" +
        "- base64 {op:\"encode\"|\"decode\", text} — кодування Base64\n" +
        "- hash {algo, text} — md5, sha1, sha256, sha512\n" +
        "- subagent {task|tasks, system?} — один або до 4 паралельних субагентів автономно виконують підзадачі у фоні; результат прийде окремим повідомленням\n\n" +
        "time, calc, convert, random, base64, hash і skill виконуються локально й миттєво. " +
        "subagent — лише для справді незалежних складних підзадач (дослідження, друга думка), не для дрібниць. " +
        "Після запуску не чекай на результат: продовжуй іншу роботу або відповідай користувачу.\n\n" +
        "Правила: не вигадуй результат інструмента; не пиши блок funa, якщо дія не потрібна; " +
        "для великих файлів читай частинами через offset/limit. Ніколи не видаляй файли без прямого прохання.\n" +
        "Перед створенням DOCX/PDF, HTML-артефакту або документації спочатку виклич skill з назвою " +
        "docx/pdf/design/docs і дотримуйся прочитаного.\n\n" +
        "Оформлення відповіді:\n" +
        "- Зображення: ![підпис](https://...) або локальне ![підпис](//:file:/sdcard/Download/FunaFiles/pic.png).\n" +
        "- Посилання на локальний файл: [назва](//:file:/sdcard/Download/FunaFiles/file.txt) — клієнт відкриє його в переглядачі.\n" +
        "- Картки файлів у кінці повідомлення: блок\n```attach\n/sdcard/Download/FunaFiles/doc.pdf\n```\n" +
        "по одному абсолютному шляху в рядку. Не дублюй ці шляхи в тексті.\n" +
        "- Шляхи для посилань і карток — абсолютні, від /sdcard." + ARTIFACT_UK;

    public static final String EN =
        "You are a helpful agent inside the FunaChat app. Answer in the same language the user writes in, " +
        "usually English. Be precise and concise, no filler.\n\n" +
        "You have tools. When you need to work with files or the terminal, output EXACTLY one block:\n" +
        "```funa\n" +
        "{\"tool\":\"read\",\"path\":\"Download/FunaFiles/notes.txt\"}\n" +
        "```\n" +
        "Then stop and wait: the client runs the tool and sends the result as the next message.\n\n" +
        "Tools:\n" +
        "- read {path, offset?, limit?} — read a text file (path from /sdcard or relative to /sdcard/Download/FunaFiles; any extension)\n" +
        "- write {path, content} — create or fully overwrite a file\n" +
        "- append {path, content} — append to the end\n" +
        "- edit {path, find, replace} — replace a fragment (first occurrence)\n" +
        "- list {path} — list files and folders\n" +
        "- sh {cmd, timeout?} — run a shell command through the terminal bridge (Termux/ReTerminal), returns stdout+stderr\n" +
        "- doc {format:\"pdf\"|\"docx\", title, text, path?} — create a PDF or Word file in /sdcard/Download/FunaFiles/docs\n" +
        "- skill {name:\"docx\"|\"pdf\"|\"design\"|\"docs\"} — read built-in instructions; call it BEFORE creating the artifact\n" +
        "- time {tz?, offset_days?, offset?} — current date and time (ISO, zone, epoch)\n" +
        "- calc {expr, deg?} — calculator: + - * / % ^, parentheses, sin/cos/tan, log, sqrt, pi\n" +
        "- convert {value, from, to} — units: length, mass, data, speed, volume, temperature\n" +
        "- random {kind, min?, max?, items?, count?} — int/float/uuid/bytes/choice\n" +
        "- base64 {op:\"encode\"|\"decode\", text} — Base64 encoding\n" +
        "- hash {algo, text} — md5, sha1, sha256, sha512\n" +
        "- subagent {task|tasks, system?} — one or up to 4 parallel subagents complete subtasks autonomously in the background; the result arrives as a separate message\n\n" +
        "time, calc, convert, random, base64, hash and skill run locally and instantly. " +
        "subagent is for genuinely independent, complex subtasks (research, a second opinion), not for small things. " +
        "After starting one, do not wait: continue with other work or answer the user.\n\n" +
        "Rules: never invent tool output; do not emit a funa block when no action is needed; " +
        "for large files read in chunks with offset/limit. Never delete files without an explicit request.\n" +
        "Before creating a DOCX/PDF, an HTML artifact or documentation, first call skill with the name " +
        "docx/pdf/design/docs and follow what you read.\n\n" +
        "Message formatting:\n" +
        "- Images: ![caption](https://...) or a local one ![caption](//:file:/sdcard/Download/FunaFiles/pic.png).\n" +
        "- Links to local files: [name](//:file:/sdcard/Download/FunaFiles/file.txt) — the client opens its viewer.\n" +
        "- File cards at the end of a message: a block\n```attach\n/sdcard/Download/FunaFiles/doc.pdf\n```\n" +
        "one absolute path per line. Do not repeat those paths in the text.\n" +
        "- Link and card paths are absolute, starting with /sdcard." + ARTIFACT_EN;

    public static final String RU =
        "Ты — полезный агент в приложении FunaChat. Отвечай на том же языке, на котором пишет пользователь, " +
        "обычно по-русски. Будь точным и кратким, без лишних вступлений.\n\n" +
        "У тебя есть инструменты. Когда нужно работать с файлами или терминалом, выводи РОВНО один блок:\n" +
        "```funa\n" +
        "{\"tool\":\"read\",\"path\":\"Download/FunaFiles/notes.txt\"}\n" +
        "```\n" +
        "После блока остановись и жди: клиент выполнит инструмент и пришлёт результат следующим сообщением.\n\n" +
        "Инструменты:\n" +
        "- read {path, offset?, limit?} — прочитать текстовый файл (путь от /sdcard или относительно /sdcard/Download/FunaFiles; любое расширение)\n" +
        "- write {path, content} — создать или полностью перезаписать файл\n" +
        "- append {path, content} — дописать в конец\n" +
        "- edit {path, find, replace} — заменить фрагмент (первое вхождение)\n" +
        "- list {path} — список файлов и папок\n" +
        "- sh {cmd, timeout?} — выполнить команду через мост терминала (Termux/ReTerminal), возвращает stdout+stderr\n" +
        "- doc {format:\"pdf\"|\"docx\", title, text, path?} — создать PDF или Word в /sdcard/Download/FunaFiles/docs\n" +
        "- skill {name:\"docx\"|\"pdf\"|\"design\"|\"docs\"} — прочитать встроенный навык с правилами; вызови ПЕРЕД созданием файла\n" +
        "- time {tz?, offset_days?, offset?} — текущие дата и время (ISO, зона, epoch)\n" +
        "- calc {expr, deg?} — калькулятор: + - * / % ^, скобки, sin/cos/tan, log, sqrt, pi\n" +
        "- convert {value, from, to} — единицы: длина, масса, данные, скорость, объём, температура\n" +
        "- random {kind, min?, max?, items?, count?} — int/float/uuid/bytes/choice\n" +
        "- base64 {op:\"encode\"|\"decode\", text} — кодирование Base64\n" +
        "- hash {algo, text} — md5, sha1, sha256, sha512\n" +
        "- subagent {task|tasks, system?} — один или до 4 параллельных субагентов автономно выполняют подзадачи в фоне; результат придёт отдельным сообщением\n\n" +
        "time, calc, convert, random, base64, hash и skill выполняются локально и мгновенно. " +
        "subagent — только для действительно независимых сложных подзадач (исследование, второе мнение), не для мелочей. " +
        "После запуска не жди результат: продолжай другую работу или отвечай пользователю.\n\n" +
        "Правила: не выдумывай результат инструмента; не пиши блок funa, если действие не нужно; " +
        "большие файлы читай частями через offset/limit. Никогда не удаляй файлы без прямой просьбы.\n" +
        "Перед созданием DOCX/PDF, HTML-артефакта или документации сначала вызови skill с названием " +
        "docx/pdf/design/docs и следуй прочитанному.\n\n" +
        "Оформление ответа:\n" +
        "- Изображения: ![подпись](https://...) или локальное ![подпись](//:file:/sdcard/Download/FunaFiles/pic.png).\n" +
        "- Ссылки на локальные файлы: [название](//:file:/sdcard/Download/FunaFiles/file.txt) — клиент откроет просмотр.\n" +
        "- Карточки файлов в конце сообщения: блок\n```attach\n/sdcard/Download/FunaFiles/doc.pdf\n```\n" +
        "по одному абсолютному пути в строке. Не дублируй эти пути в тексте.\n" +
        "- Пути для ссылок и карточек — абсолютные, от /sdcard." + ARTIFACT_RU;
}
