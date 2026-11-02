package com.funa.chat;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

/**
 * Локальне блокування застосунку.
 *
 * Акаунт — це запис у {@link Store}; тут зберігається лише:
 *  - глобальний перемикач блокування;
 *  - прапорець «сесія розблокована»;
 *  - для кожного акаунта — PBKDF2-хеш локального пароля з власною сіллю.
 *
 * Пароль DeepSeek тут не використовується: сам пароль ніде не зберігається
 * і нікуди не надсилається, розблокування перевіряється локально.
 * Якщо пароль забуто — повторний вхід у DeepSeek через сайт скидає його
 * (див. {@link #requestReset()} / {@link #consumeReset()}).
 */
public final class Auth {
    /** Старий спільний перемикач — лише для міграції на пер-акаунтний. */
    private static final String K_LOCK = "auth_lock";
    private static final String K_LOCK_A = "auth_lock_";
    private static final String K_LOCK_MIG = "auth_lock_migrated";
    private static final String K_SESSION_A = "auth_session_";
    private static final String K_SESSION_MIG = "auth_session_migrated";
    private static final String K_SESSION = "auth_session";
    private static final String K_SKIP = "auth_skip";
    private static final String K_TRIES = "auth_tries";
    private static final String K_TRY_TS = "auth_try_ts";
    private static final String K_OLD_PROFILE = "auth_profile";
    private static final String K_PW = "auth_pw_";
    private static final String K_RESET = "auth_reset_pending";
    private static final int MAX_TRIES = 8;
    private static final long TRY_WINDOW = 10 * 60 * 1000L;

    private final SharedPreferences p;
    private final Store store;

    public Auth(Context c, Store store) {
        this.p = c.getSharedPreferences("funa", Context.MODE_PRIVATE);
        this.store = store;
        migrateOldProfile();
        migrateLock();
        migrateSession();
    }

    /**
     * «Сесія розблоковано» теж була спільна. Через це розблокування першого
     * акаунта відкривало й другий із власним паролем.
     */
    private void migrateSession() {
        if (p.getBoolean(K_SESSION_MIG, false)) return;
        String id = store.activeId();
        android.content.SharedPreferences.Editor e = p.edit().putBoolean(K_SESSION_MIG, true);
        if (!id.isEmpty() && p.getBoolean(K_SESSION, false)) e.putBoolean(K_SESSION_A + id, true);
        e.remove(K_SESSION).apply();
    }

    /**
     * Блокування раніше було одне на застосунок. Тепер воно належить
     * акаунту: інакше додавання другого акаунта без пароля тихо вимикало
     * захист першого.
     */
    private void migrateLock() {
        if (p.getBoolean(K_LOCK_MIG, false)) return;
        String id = store.activeId();
        boolean legacy = p.getBoolean(K_LOCK, false);
        android.content.SharedPreferences.Editor e = p.edit().putBoolean(K_LOCK_MIG, true);
        if (!id.isEmpty() && legacy) e.putBoolean(K_LOCK_A + id, true);
        e.remove(K_LOCK).apply();
    }

    /** Переносить хеш пароля зі старої локальної моделі на активний акаунт. */
    private void migrateOldProfile() {
        String raw = p.getString(K_OLD_PROFILE, "");
        if (raw.isEmpty()) return;
        try {
            JSONObject pr = new JSONObject(raw);
            String salt = pr.optString("salt", "");
            String hash = pr.optString("hash", "");
            String id = store.activeId();
            if (!id.isEmpty() && !salt.isEmpty() && !hash.isEmpty() && !hasPassword(id)) {
                JSONObject w = new JSONObject();
                w.put("salt", salt);
                w.put("hash", hash);
                p.edit().putString(pwKey(id), w.toString()).apply();
                Log.add("auth", "lock password moved to the active account");
            }
        } catch (Exception ignored) {}
        p.edit().remove(K_OLD_PROFILE).apply();
    }

    private static String pwKey(String accountId) { return K_PW + accountId; }

    /**
     * Скидає лічильник невдалих спроб акаунта. Потрібен щоразу, коли пароль
     * змінено чи скинуто: інакше після 8 промахів новий пароль усе одно
     * впирається в 10-хвилинне вікно.
     */
    private void resetTries(String id) {
        android.content.SharedPreferences.Editor e = p.edit().remove(K_TRIES);
        if (id != null && !id.isEmpty()) {
            e.remove(K_TRIES + "_" + id).remove(K_TRY_TS + "_" + id);
        }
        e.apply();
    }

    public boolean has() {
        return !store.activeId().isEmpty();
    }

    public boolean lock() {
        String id = store.activeId();
        return !id.isEmpty() && p.getBoolean(K_LOCK_A + id, false);
    }

    public void setLock(boolean b) {
        String id = store.activeId();
        if (id.isEmpty()) return;
        p.edit().putBoolean(K_LOCK_A + id, b).apply();
    }

    public boolean skipped() {
        return p.getBoolean(K_SKIP, false);
    }

    public void setSkipped(boolean b) {
        p.edit().putBoolean(K_SKIP, b).apply();
    }

    public boolean session() {
        String id = store.activeId();
        return !id.isEmpty() && p.getBoolean(K_SESSION_A + id, false);
    }

    public void logout() {
        logout(store.activeId());
    }

    /** Закриває сесію конкретного акаунта (наприклад, видаленого). */
    public void logout(String id) {
        if (id == null || id.isEmpty()) return;
        p.edit().putBoolean(K_SESSION_A + id, false).apply();
    }

    /** Чи бодай один акаунт має ввімкнене блокування. */
    public boolean anyLock() {
        for (java.util.Map.Entry<String, ?> e : p.getAll().entrySet()) {
            String k = e.getKey();
            // «auth_lock_migrated» також починається з «auth_lock_», але це
            // не блокування акаунта, а прапорець міграції: інакше anyLock()
            // завжди true і кожен старт скидав би сесії.
            if (k.equals(K_LOCK_MIG)) continue;
            if (k.startsWith(K_LOCK_A) && Boolean.TRUE.equals(e.getValue())) return true;
        }
        return false;
    }

    /**
     * Холодний старт: якщо блокування ввімкнене хоч де, жоден акаунт не
     * має прокидатися вже розблокованим (інакше перемикання на інший
     * акаунт обходило б пароль, введений у минулому запуску).
     */
    public void logoutAll() {
        android.content.SharedPreferences.Editor e = p.edit();
        for (String k : p.getAll().keySet()) {
            // прапорець міграції не є сесією акаунта — не чіпаємо
            if (k.equals(K_SESSION_MIG)) continue;
            if (k.startsWith(K_SESSION_A)) e.putBoolean(k, false);
        }
        e.apply();
    }

    public boolean unlocked() {
        // Немає пароля для цього акаунта — блокувати просто нема чим;
        // пароль іншого акаунта для нього не діє.
        return !has() || !lock() || session() || !hasPassword();
    }

    /* ---------------- per-account password ---------------- */

    private JSONObject pw(String id) {
        if (id == null || id.isEmpty()) return null;
        String raw = p.getString(pwKey(id), "");
        if (raw.isEmpty()) return null;
        try { return new JSONObject(raw); } catch (Exception e) { return null; }
    }

    public boolean hasPassword() { return hasPassword(store.activeId()); }

    public boolean hasPassword(String id) { return pw(id) != null; }

    /** Зберігає хеш пароля для активного акаунта. false — немає акаунта. */
    public boolean setPassword(String pass) {
        String id = store.activeId();
        if (id.isEmpty()) return false;
        if (pass == null || pass.length() < 6) {
            p.edit().remove(pwKey(id)).apply();
            return false;
        }
        try {
            String salt = AuthCore.newSalt();
            JSONObject w = new JSONObject();
            w.put("salt", salt);
            w.put("hash", AuthCore.b64(AuthCore.hash(pass, AuthCore.unb64(salt))));
            p.edit().putString(pwKey(id), w.toString()).apply();
            resetTries(id);
            return true;
        } catch (Exception e) {
            Log.e("auth", "save password failed", e);
            return false;
        }
    }

    public void clearPassword(String id) {
        if (id == null || id.isEmpty()) return;
        p.edit().remove(pwKey(id)).remove(K_LOCK_A + id).apply();
        resetTries(id);
    }

    /**
     * Остаточно прибирає локальний стан видаленого акаунта: пароль,
     * блокування, сесію, лічильник спроб і незавершене скидання. Без цього
     * у SharedPreferences лишалися б «мертві» прапорці назавжди.
     */
    public void purge(String id) {
        if (id == null || id.isEmpty()) return;
        clearPassword(id);
        p.edit()
                .remove(K_SESSION_A + id)
                .remove(K_TRIES + "_" + id)
                .remove(K_TRY_TS + "_" + id)
                .apply();
        if (id.equals(p.getString(K_RESET, ""))) clearReset();
    }

    /* ---------------- скидання локального пароля ---------------- */

    /**
     * «Забув пароль»: скидання дозволене лише після повторного підтвердження
     * входу в DeepSeek. Тут лише запам'ятовуємо намір для активного акаунта.
     */
    public void requestReset() {
        String id = store.activeId();
        if (!id.isEmpty()) p.edit().putString(K_RESET, id).apply();
    }

    public void clearReset() {
        p.edit().remove(K_RESET).apply();
    }

    public boolean pendingReset() {
        String id = store.activeId();
        return !id.isEmpty() && id.equals(p.getString(K_RESET, ""));
    }

    /**
     * Після успішного входу в DeepSeek: якщо для цього акаунта був запит
     * «забув пароль» — прибираємо хеш і вимикаємо блокування. Новий пароль
     * можна поставити в Налаштуваннях. Повертає true, якщо скидання відбулося.
     */
    public boolean consumeReset() {
        if (!pendingReset()) return false;
        String id = store.activeId();
        clearPassword(id);
        setLock(false);
        clearReset();
        Log.add("auth", "lock reset after DeepSeek sign-in");
        return true;
    }

    /**
     * Після успішного входу в DeepSeek: сесія відкрита, пароль (якщо відомий)
     * зберігається для локального розблокування.
     */
    public void afterLogin(String pass) {
        if (store.activeId().isEmpty()) return;
        if (pass != null && !pass.isEmpty()) setPassword(pass);
        String id = store.activeId();
        p.edit().putBoolean(K_SESSION_A + id, true).putBoolean(K_SKIP, false).apply();
        resetTries(id);
        // 401 ставив 30-хвилинну паузу Guard. Після нового входу токен уже
        // інший, і стара пауза лише блокувала б робочий акаунт: знімаємо її.
        Guard.reset(store);
        // Блокування іншого акаунта не чіпаємо: у цього просто немає пароля,
        // і unlocked() вважає його відкритим, доки пароль не задано.
        if (lock() && !hasPassword()) Log.add("auth", "lock: no local password for this account");
    }

    /** Сумісність: блокування тепер пер-акаунтне, вимикати нічого не треба. */
    public void normalizeLock() {
        if (lock() && !hasPassword()) Log.add("auth", "lock: no local password for this account");
    }

    public JSONObject state() {
        JSONObject o = new JSONObject();
        JSONObject a = store.activeSummary();
        try {
            o.put("has", a != null);
            o.put("hasPass", a != null && hasPassword(a.optString("id", "")));
            o.put("lock", lock());
            o.put("skipped", skipped());
            o.put("unlocked", unlocked());
            o.put("accountId", a == null ? "" : a.optString("id", ""));
            if (a != null) {
                o.put("name", a.optString("name", ""));
                o.put("email", a.optString("email", ""));
                o.put("phone", a.optString("phone", ""));
                o.put("uid", a.optString("uid", ""));
                o.put("since", a.optLong("added", 0));
                o.put("method", a.optString("src", ""));
                o.put("status", a.optString("status", "ok"));
            }
        } catch (Exception ignored) {}
        return o;
    }

    /** Розблокування без мережі: PBKDF2-перевірка пароля активного акаунта. */
    public JSONObject unlock(String pass) {
        JSONObject r = new JSONObject();
        try {
            String id = store.activeId();
            if (id.isEmpty()) return fail(r, "no_account");
            long wait = lockLeftMs();
            if (wait > 0) {
                r.put("ok", false);
                r.put("code", "tries");
                r.put("minutes", (wait + 59999) / 60000);
                return r;
            }
            JSONObject w = pw(id);
            if (w == null) return fail(r, "no_pass");
            byte[] calc = AuthCore.hash(pass == null ? "" : pass,
                    AuthCore.unb64(w.optString("salt", "")));
            if (!AuthCore.same(calc, AuthCore.unb64(w.optString("hash", "")))) {
                bump();
                return fail(r, "bad_pass");
            }
            p.edit().putBoolean(K_SESSION_A + id, true).apply();
            resetTries(id);
            r.put("ok", true);
            r.put("state", state());
            return r;
        } catch (Exception e) {
            Log.e("auth", "unlock failed", e);
            return fail(r, "error");
        }
    }

    /** Лічильник спроб — теж на акаунт: серія невдач на одному не блокує інший. */
    private void bump() {
        String id = store.activeId();
        long now = System.currentTimeMillis();
        String tsKey = K_TRY_TS + "_" + id;
        String nKey = K_TRIES + "_" + id;
        long ts = p.getLong(tsKey, 0);
        int n = p.getInt(nKey, 0);
        if (now - ts > TRY_WINDOW) n = 0;
        p.edit().putInt(nKey, n + 1).putLong(tsKey, now).apply();
    }

    private long lockLeftMs() {
        String id = store.activeId();
        String tsKey = K_TRY_TS + "_" + id;
        int n = p.getInt(K_TRIES + "_" + id, 0);
        if (n < MAX_TRIES) return 0;
        long ts = p.getLong(tsKey, 0);
        long left = ts + TRY_WINDOW - System.currentTimeMillis();
        if (left <= 0) {
            // вікно минуло — чистимо ключі цілком, а не лише обнуляємо:
            // інакше «auth_try_ts_<id>» лишається і плутає наступний bump
            resetTries(id);
            return 0;
        }
        return left;
    }

    private static JSONObject fail(JSONObject r, String code) {
        try {
            r.put("ok", false);
            r.put("code", code);
        } catch (Exception ignored) {}
        return r;
    }
}
