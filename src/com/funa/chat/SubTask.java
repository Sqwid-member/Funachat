package com.funa.chat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Фоновий субагент для чату в застосунку.
 *
 * На відміну від {@link SubAgent#run} (синхронний, для локального API),
 * тут задача живе у власному потоці: чат і інші субагенти працюють далі,
 * а коли задача завершується — {@link Done}-колбек віддає результат
 * батьківському агенту, який надсилає його моделі новим повідомленням.
 *
 * Кожна задача має локальний транскрипт (задача, відповіді, інструменти),
 * який UI показує в картці та в read-only перегляді «сесії субагента».
 */
public final class SubTask {
    /** Події для UI. */
    public interface Sink { void event(JSONObject o); }

    /** Завершення задачі для батьківського агента. */
    public interface Done { void done(SubTask t); }

    /** Скільки завершених задач тримаємо в памʼяті для перегляду. */
    private static final int KEEP = 40;
    /** Скільки субагентів можуть працювати одночасно (решта — пізніше). */
    public static final int MAX_RUNNING = 4;

    private static final Map<String, SubTask> ALL =
            new LinkedHashMap<String, SubTask>(16, 0.75f, false) {
                @Override protected boolean removeEldestEntry(Map.Entry<String, SubTask> e) {
                    return size() > KEEP && !"run".equals(e.getValue().status);
                }
            };

    private static int seq;
    private static Sink sink;
    private static Done doneCb;

    public static synchronized void setSink(Sink s) { sink = s; }
    public static synchronized void setDone(Done d) { doneCb = d; }

    public static synchronized SubTask get(String id) {
        return id == null ? null : ALL.get(id);
    }

    public static SubTask start(Store st, String parentSid, String task, String system, int depth) {
        if (task == null || task.trim().isEmpty()) return null;
        final SubTask t;
        synchronized (SubTask.class) {
            int running = 0;
            for (SubTask x : ALL.values()) if ("run".equals(x.status)) running++;
            if (running >= MAX_RUNNING) return null;
            t = new SubTask("sub-" + (++seq), parentSid, task, system, depth, st.activeId());
            ALL.put(t.id, t);
        }
        t.logUser();
        t.emitStart();
        Thread th = new Thread(new Runnable() {
            @Override public void run() {
                t.worker = Thread.currentThread();
                try { t.execute(st); } finally { t.worker = null; }
            }
        }, "funa-" + t.id);
        th.setDaemon(true);
        th.start();
        return t;
    }

    public static void stop(String id) {
        SubTask t = get(id);
        if (t != null) t.stop();
    }

    public static synchronized void stopAll() {
        for (SubTask t : new ArrayList<>(ALL.values())) t.stop();
    }

    public static synchronized List<SubTask> running() {
        List<SubTask> out = new ArrayList<>();
        for (SubTask t : ALL.values()) if ("run".equals(t.status)) out.add(t);
        return out;
    }

    public final String id;
    public final String parentSid;
    public final String task;
    public final String system;
    public final String title;
    public final int depth;
    /** Аккаунт на момент запуска: после переключения задача уже чужая. */
    private final String accountId;
    public final long startedAt;

    public volatile long endedAt;
    public volatile String status = "run";   // run | done | error | stopped
    public volatile String result = "";
    public volatile String error = "";
    public volatile String live = "";

    private final JSONArray log = new JSONArray();
    private final StringBuilder think = new StringBuilder();
    private volatile boolean cancelled;
    private volatile Net net;
    private volatile Thread worker;
    private volatile long lastLive;

    private SubTask(String id, String parentSid, String task, String system, int depth,
                    String accountId) {
        this.id = id;
        this.accountId = accountId == null ? "" : accountId;
        this.parentSid = parentSid == null ? "" : parentSid;
        this.task = task == null ? "" : task.trim();
        this.system = system == null ? "" : system;
        this.depth = depth;
        this.startedAt = System.currentTimeMillis();
        this.title = makeTitle(this.task);
    }

    /** Аккаунт задачи: события чужого аккаунта не должны доходить до UI. */
    public String account() { return accountId; }

    public long ms() {
        long end = endedAt > 0 ? endedAt : System.currentTimeMillis();
        return Math.max(0, end - startedAt);
    }

    public void stop() {
        if (!"run".equals(status)) return;
        cancelled = true;
        Net n = net;
        if (n != null) n.cancel();
        // команда в мості термінала не реагує на cancel() — будимо потік
        Thread w = worker;
        if (w != null && w != Thread.currentThread()) w.interrupt();
    }

    private static String makeTitle(String task) {
        String s = task == null ? "" : task.trim();
        int nl = s.indexOf('\n');
        if (nl > 0) s = s.substring(0, nl);
        s = s.replaceAll("\\s+", " ").trim();
        if (s.length() > 90) s = s.substring(0, 90) + "…";
        return s.isEmpty() ? "Subagent" : s;
    }

    private void execute(final Store st) {
        try {
            // Пока задача ждала в очереди, аккаунт могли переключить:
            // запускать её с новым токеном нельзя — это чужие данные.
            // Токен фиксируем ДО проверки: иначе переключение между
            // activeId() и new Net(st) дало бы новый токен старой задаче.
            final String tok = st.token();
            if (cancelled || !accountId.equals(st.activeId())) {
                status = "stopped";
                return;
            }
            final Net n = new Net(st, tok);
            net = n;
            String out = SubAgent.runJob(n, st, task, system, depth, null,
                    new SubAgent.Listener() {
                        @Override public void onSession(String sid) { }

                        @Override public void onDelta(String kind, String text) {
                            if (text == null || text.isEmpty()) return;
                            if ("think".equals(kind)) {
                                synchronized (SubTask.this) {
                                    think.append(text);
                                    if (think.length() > 24000) think.delete(0, think.length() - 24000);
                                }
                                return;
                            }
                            synchronized (SubTask.this) {
                                live = tail(live + text, 900);
                            }
                            long now = System.currentTimeMillis();
                            if (now - lastLive >= 280) {
                                lastLive = now;
                                emitLive();
                            }
                        }

                        @Override public void onRound(String answer) {
                            String th;
                            synchronized (SubTask.this) {
                                th = think.toString().trim();
                                think.setLength(0);
                                live = "";
                            }
                            logAssistant(answer, th);
                            emitLog();
                        }

                        @Override public void onTool(String name, JSONObject call) {
                            logTool(name, call);
                            emitLog();
                        }

                        @Override public void onToolResult(String name, boolean ok, String text) {
                            toolResult(name, ok, text);
                            emitLog();
                        }

                        @Override public boolean cancelled() { return cancelled; }
                    });
            // 1 МБ — це стеля стріму, а не розмір картки: такий результат
            // їхав би одним eval() у WebView і тримався в памʼяті задачі.
            result = clip(out, 100000);
            status = cancelled ? "stopped" : "done";
        } catch (Exception e) {
            status = cancelled ? "stopped" : "error";
            String m = e.getMessage();
            error = m == null || m.isEmpty() ? e.getClass().getSimpleName() : m;
        } finally {
            endedAt = System.currentTimeMillis();
            net = null;
            emitEnd();
            Done d = doneCb;
            if (d != null) {
                try { d.done(this); } catch (Exception ignored) {}
            }
        }
    }

    /* --------------------------- транскрипт --------------------------- */

    private void logUser() {
        JSONObject m = new JSONObject();
        try {
            m.put("role", "user");
            m.put("text", task);
        } catch (Exception ignored) {}
        put(m);
    }

    private void logAssistant(String text, String thinkText) {
        if (text != null) text = text.replaceAll("(?s)```funa.*?```", "").trim();
        if ((text == null || text.trim().isEmpty()) && (thinkText == null || thinkText.isEmpty())) return;
        JSONObject m = new JSONObject();
        try {
            m.put("role", "assistant");
            // транскрипт показується в картці: довга відповідь не має
            // роздувати памʼять і кожен eval() у WebView
            m.put("text", clip(text, 20000));
            m.put("think", clip(thinkText, 20000));
        } catch (Exception ignored) {}
        put(m);
    }

    private void logTool(String name, JSONObject call) {
        JSONObject m = new JSONObject();
        try {
            m.put("role", "tool");
            m.put("name", name == null ? "tool" : name);
            m.put("args", call == null ? "" : clip(call.toString(), 4000));
            m.put("status", "run");
            m.put("output", "");
        } catch (Exception ignored) {}
        put(m);
    }

    private void toolResult(String name, boolean ok, String text) {
        String out = text == null ? "" : text;
        if (out.length() > 16000) out = out.substring(0, 16000) + "\n… (обрізано)";
        synchronized (log) {
            for (int i = log.length() - 1; i >= 0; i--) {
                JSONObject m = log.optJSONObject(i);
                if (m == null || !"tool".equals(m.optString("role"))) continue;
                if (!m.optString("name", "").equals(name == null ? "" : name)) continue;
                if (!"run".equals(m.optString("status", ""))) continue;
                try {
                    m.put("status", ok ? "done" : "error");
                    m.put("output", out);
                } catch (Exception ignored) {}
                return;
            }
        }
    }

    private static String clip(String v, int max) {
        if (v == null) return "";
        if (v.length() <= max) return v;
        return v.substring(0, max) + "\n… (обрізано)";
    }

    private void put(JSONObject m) {
        synchronized (log) {
            log.put(m);
            if (log.length() > 60) log.remove(0);
        }
    }

    private JSONArray copyLog() {
        synchronized (log) {
            JSONArray a = new JSONArray();
            for (int i = 0; i < log.length(); i++) {
                JSONObject m = log.optJSONObject(i);
                if (m != null) a.put(m);
            }
            return a;
        }
    }

    public JSONObject view() {
        JSONObject o = new JSONObject();
        try {
            o.put("e", "sub_view");
            o.put("id", id);
            o.put("title", title);
            o.put("task", task);
            o.put("status", status);
            o.put("result", result);
            o.put("error", error);
            o.put("ms", ms());
            o.put("t0", startedAt);
            o.put("live", live);
            o.put("messages", copyLog());
        } catch (Exception ignored) {}
        return o;
    }

    /** Короткий опис для картки (без транскрипту). */
    public JSONObject brief() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", id);
            o.put("title", title);
            o.put("task", task);
            o.put("status", status);
            // у списку карток достатньо початку; повний текст віддає view()
            String r = result == null ? "" : result;
            if (r.length() > 3000) r = r.substring(0, 3000) + "…";
            o.put("result", r);
            o.put("error", error);
            o.put("ms", ms());
            o.put("t0", startedAt);
            o.put("live", live);
        } catch (Exception ignored) {}
        return o;
    }

    public static synchronized JSONArray listFor(String sid) {
        JSONArray a = new JSONArray();
        if (sid == null || sid.isEmpty()) return a;
        for (SubTask t : ALL.values()) {
            if (sid.equals(t.parentSid)) a.put(t.brief());
        }
        return a;
    }

    /* ----------------------------- події ------------------------------ */

    private void emitStart() {
        JSONObject o = new JSONObject();
        try {
            o.put("e", "sub_start");
            o.put("id", id);
            o.put("sid", parentSid);
            o.put("title", title);
            o.put("task", task);
            o.put("status", status);
            o.put("t0", startedAt);
        } catch (Exception ignored) {}
        emit(o);
    }

    private void emitLive() {
        JSONObject o = new JSONObject();
        try {
            o.put("e", "sub_live");
            o.put("id", id);
            o.put("text", live);
        } catch (Exception ignored) {}
        emit(o);
    }

    private void emitLog() {
        JSONObject o = new JSONObject();
        try {
            o.put("e", "sub_log");
            o.put("id", id);
            o.put("n", log.length());
        } catch (Exception ignored) {}
        emit(o);
    }

    private void emitEnd() {
        JSONObject o = new JSONObject();
        try {
            o.put("e", "sub_end");
            o.put("id", id);
            o.put("sid", parentSid);
            o.put("status", status);
            o.put("result", result);
            o.put("error", error);
            o.put("ms", ms());
        } catch (Exception ignored) {}
        emit(o);
    }

    private static void emit(JSONObject o) {
        Sink s = sink;
        if (s == null) return;
        try { s.event(o); } catch (Exception ignored) {}
    }

    private static String tail(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : "…" + s.substring(s.length() - max);
    }
}
