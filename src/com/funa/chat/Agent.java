package com.funa.chat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class Agent {
    public interface Emit { void event(JSONObject o); }

    /** Синхронізація стану сесії з Activity. */
    public interface State { void onState(String sid, long parent, boolean first); }

    private static final int MAX_ROUNDS = 10;
    private final Store st;
    /**
     * Свіжий Net на кожен запуск: у нього вбудований прапорець cancel(),
     * тож після «Стоп» наступне повідомлення не має успадкувати скасування.
     */
    private volatile Net net;
    private final Emit emit;
    private volatile boolean stopped;
    private volatile boolean closed;
    private volatile HttpURLConnection conn;
    private Thread thread;

    private String sessionId;
    private long parentId;
    private long lastMessageId;
    private boolean first = true;
    /** Навички, прочитані в поточному запуску: doc без них не створюємо. */
    private final Set<String> skillsRead = new HashSet<>();
    private String mode = "send";
    private long targetId = 0;
    private final List<String> refIds = new ArrayList<>();
    private volatile State stateCb;
    /** Завершені фонові субагенти, які ще не віддані моделі (за сесією). */
    private final Map<String, List<SubTask>> pending = new HashMap<>();

    public Agent(Store s, Emit e) {
        st = s;
        net = new Net(s);
        emit = e;
        SubTask.setDone(t -> onTaskDone(t));
    }

    public synchronized void setSession(String id, long parent, boolean isFirst) {
        sessionId = id;
        parentId = parent;
        first = isFirst;
    }

    public synchronized String sessionId() { return sessionId; }
    public synchronized long parentId() { return parentId; }
    public synchronized boolean isFirst() { return first; }

    public void setStateListener(State s) { stateCb = s; }

    private void notifyState() {
        State s = stateCb;
        if (s == null) return;
        try { s.onState(sessionId, parentId, first); } catch (Exception ignored) {}
    }

    public boolean busy() { return thread != null && thread.isAlive(); }

    /** Зайнятий іншим потоком (для доставки повідомлень із власного finally). */
    private boolean busyOther() {
        Thread t = thread;
        return t != null && t.isAlive() && t != Thread.currentThread();
    }

    public void stop() {
        stopped = true;
        // conn з'являється лише ПІСЛЯ завершення streamRequest, тому під час
        // стріму він null. Реальне з'єднання тримає Net.active — рвемо його,
        // інакше «Стоп» чекав би наступної дельти або кінця відповіді.
        final Net n = net;
        try { n.cancel(); } catch (Exception ignored) {}
        HttpURLConnection c = conn;
        if (c != null) c.disconnect();
        // інструмент міг застрягти в очікуванні моста термінала — перериваємо
        // його сон, інакше «Стоп» не спрацює до таймауту команди
        Thread t = thread;
        if (t != null && t != Thread.currentThread()) t.interrupt();
        final String sid = sessionId;
        final long mid = lastMessageId;
        if (sid != null && mid > 0) {
            // Фіксуємо саме той Net, який обслуговував цю відповідь.
            new Thread(() -> {
                try { n.stopStream(sid, mid); } catch (Exception ignored) {}
            }, "funa-stop").start();
        }
    }

    /** Остаточне закриття: більше не запускаємо фонові повідомлення. */
    public void close() {
        closed = true;
        stop();
    }

    public boolean send(final String text, final String prefix) {
        if (busy()) return false;
        setRefs(null);
        return start(text, prefix, "send", 0);
    }

    public boolean send(final String text, final String prefix, final List<String> files) {
        if (busy()) return false;
        setRefs(files);
        return start(text, prefix, "send", 0);
    }

    /** Редагування повідомлення користувача на сервері (/chat/edit_message). */
    public boolean sendEdit(final String text, final long messageId) {
        if (busy()) return false;
        setRefs(null);
        return start(text, null, "edit", messageId);
    }

    public boolean sendEdit(final String text, final long messageId, final List<String> files) {
        if (busy()) return false;
        setRefs(files);
        return start(text, null, "edit", messageId);
    }

    private void setRefs(final List<String> files) {
        refIds.clear();
        if (files == null) return;
        for (String f : files) {
            if (f != null && !f.trim().isEmpty() && !refIds.contains(f.trim())) refIds.add(f.trim());
        }
    }

    private JSONArray refArray() {
        JSONArray a = new JSONArray();
        for (String f : refIds) a.put(f);
        return a;
    }

    /** Перегенерація відповіді (/chat/regenerate), child = id повідомлення асистента. */
    public boolean sendRegen(final long childId) {
        return start("", null, "regen", childId);
    }

    /** Продовження обірваної відповіді (/chat/continue). */
    public boolean sendContinue(final long messageId) {
        return start("", null, "cont", messageId);
    }

    private synchronized boolean start(final String text, final String prefix, final String m, final long target) {
        if (closed || busyOther()) return false;
        stopped = false;
        // Токен і прапорець cancel() фіксуються на старті відповіді.
        net = new Net(st);
        mode = m;
        targetId = target;
        thread = new Thread(() -> run(text, prefix), "funa-agent");
        thread.start();
        return true;
    }

    private JSONObject ev(String name) {
        JSONObject o = new JSONObject();
        try { o.put("e", name); } catch (Exception ignored) {}
        return o;
    }

    /**
     * Після {@link #close()} агент уже не має права писати в UI: інакше
     * дельти старого акаунта домальовуються в чат нового.
     */
    private void push(JSONObject o) {
        if (closed) return;
        emit.event(o);
    }

    private void error(String msg) {
        error(msg, 0, false);
    }

    private void error(String msg, int code) {
        error(msg, code, false);
    }

    private void error(String msg, int code, boolean verify) {
        JSONObject o = ev("error");
        try {
            o.put("msg", msg);
            if (code != 0) o.put("code", code);
            if (code == 401) o.put("expired", true);
            if (verify) o.put("verify", true);
        } catch (Exception ignored) {}
        push(o);
    }

    /** DeepSeek просить підтвердити людину (hCaptcha): коди 40303/40304. */
    private static boolean challengeCode(int code) {
        return code == 40303 || code == 40304;
    }

    private void run(String text, String prefix) {
        // Акаунт фіксуємо на старті: якщо користувач встигне перемкнути
        // його до помилки, статус «expired» має дістатися саме тому
        // акаунту, чий токен відхилив сервер.
        final String aid = st.activeId();
        // Навички читаються заново в кожному запуску: модель має бачити
        // інструкцію в актуальному контексті, а не покладатися на минулий чат.
        skillsRead.clear();
        final long runT0 = System.currentTimeMillis();
        final boolean[] failed = {false};
        try {
            JSONObject busy = ev("busy");
            busy.put("on", true);
            push(busy);
            if (st.token().isEmpty()) throw new Exception(tr("Немає токена. Відкрий налаштування.", "No token. Open settings.", "Нет токена. Открой настройки."));

            final boolean isSend = "send".equals(mode);
            final boolean isEdit = "edit".equals(mode);
            final boolean isRegen = "regen".equals(mode);
            final boolean isCont = "cont".equals(mode);
            if (!isSend) {
                if (sessionId == null) throw new Exception(tr("Немає сесії для цієї дії", "No session for this action", "Нет сессии для этого действия"));
                first = false;
            } else if (sessionId == null) {
                JSONObject s = net.createSession();
                sessionId = s.optString("id");
                parentId = 0;
                first = true;
                notifyState();
                JSONObject e = ev("session");
                e.put("id", sessionId);
                e.put("title", s.optString("title", ""));
                push(e);
            }
            if (isSend) {
                JSONObject u = ev("user");
                u.put("text", text);
                push(u);
            }

            String prompt = text;
            if (isSend && first && prefix != null && !prefix.trim().isEmpty()) prompt = prefix.trim() + "\n\n" + text;

            for (int round = 0; round < MAX_ROUNDS && !stopped; round++) {
                String path = "/api/v0/chat/completion";
                JSONObject body = new JSONObject();
                body.put("chat_session_id", sessionId);
                if (round == 0 && isEdit) {
                    path = "/api/v0/chat/edit_message";
                    body.put("message_id", targetId);
                    body.put("prompt", prompt);
                    body.put("ref_file_ids", refArray());
                    body.put("search_enabled", st.search());
                } else if (round == 0 && isRegen) {
                    path = "/api/v0/chat/regenerate";
                    body.put("child_message_id", targetId);
                    body.put("thinking_enabled", st.thinking());
                    body.put("search_enabled", st.search());
                } else if (round == 0 && isCont) {
                    path = "/api/v0/chat/continue";
                    body.put("message_id", targetId);
                    body.put("fallback_to_resume", true);
                } else {
                    if (parentId > 0) body.put("parent_message_id", parentId);
                    else body.put("parent_message_id", JSONObject.NULL);
                    body.put("prompt", prompt);
                    body.put("ref_file_ids", round == 0 ? refArray() : new JSONArray());
                    body.put("thinking_enabled", st.thinking());
                    body.put("search_enabled", st.search());
                    body.put("preempt", false);
                    body.put("model_type", st.model());
                    body.put("action", "default");
                }

                JSONObject pow = isCont && round == 0 ? null : net.powHeader(path);
                if (stopped) break;
                StreamResult r = stream(path, body, pow);
                if (r == null) break;
                if (r.messageId > 0) parentId = r.messageId;
                first = false;
                notifyState();

                String answer = r.answer.toString();
                JSONObject tool = findTool(answer);
                if (tool == null || stopped) break;

                String name = tool.optString("tool", "?");
                // Мова інструкції для skill; інші інструменти lang не бачать,
                // щоб службове поле не потрапляло в картку аргументів.
                if ("skill".equalsIgnoreCase(name) && !tool.has("lang")) tool.put("lang", st.lang());
                // Акаунт могли перемкнути, поки модель думала. Тоді
                // startSubagents узяли б токен нового акаунта, а Tools
                // виконали б файлові операції вже "від його імені" —
                // результат при цьому пішов би в старий чат. Стару
                // задачу просто обриваємо: вона вже неактуальна.
                if (!aid.equals(st.activeId())) {
                    Log.add("agent", "account changed mid-run; skip tool: " + name);
                    stopped = true;
                    break;
                }
                JSONObject running = ev("tool");
                running.put("name", name);
                running.put("args", tool.toString());
                running.put("status", "run");
                push(running);

                JSONObject res;
                if ("subagent".equalsIgnoreCase(name)) {
                    res = startSubagents(tool);
                } else if (isDocTool(name) && !skillsRead.contains(docSkill(tool))) {
                    // Модель має спершу прочитати інструкцію, як робити
                    // DOCX/PDF, і лише потім створювати файл. Інакше вона
                    // пише таблиці й **жирний**, які генератор вирізає.
                    String need = docSkill(tool);
                    Log.add("agent", "doc gated: skill " + need + " not read");
                    res = Builtin.err(tr(
                            "Спочатку прочитай навичку: {\"tool\":\"skill\",\"name\":\"" + need + "\"}. " +
                                    "Після її тексту повтори той самий виклик doc — не переписуй документ заново.",
                            "Read the skill first: {\"tool\":\"skill\",\"name\":\"" + need + "\"}. " +
                                    "After its text, repeat the same doc call — do not rewrite the document.",
                            "Сначала прочитай навык: {\"tool\":\"skill\",\"name\":\"" + need + "\"}. " +
                                    "После его текста повтори тот же вызов doc — не переписывай документ заново."));
                } else {
                    res = Tools.exec(tool);
                }
                if ("skill".equalsIgnoreCase(name) && res.optBoolean("ok", false)) {
                    String read = Skills.norm(tool.optString("name", ""));
                    if (Skills.exists(read)) skillsRead.add(read);
                }
                boolean ok = res.optBoolean("ok", false);
                JSONObject done = ev("tool");
                done.put("name", name);
                // У картці не показуємо мегабайтний текст write/doc: DOM
                // інакше роздувається, а користувачу потрібен лише початок.
                String argStr = tool.toString();
                if (argStr.length() > 6000) {
                    argStr = argStr.substring(0, 6000) + "\n" +
                            tr("… (аргументи обрізано)", "… (args truncated)", "… (аргументы обрезаны)");
                }
                done.put("args", argStr);
                done.put("status", ok ? "done" : "error");
                String out = res.optString("text", res.optString("error", ""));
                if (out.length() > 6000) out = out.substring(0, 6000) + "\n… (обрізано)";
                done.put("output", out);
                done.put("exit", res.optInt("exit", 0));
                push(done);

                String result = res.optString("text", res.optString("error", ""));
                if (result.length() > 18000) result = result.substring(0, 18000) + "\n… (обрізано)";
                prompt = "Tool result «" + name + "» (ok=" + ok + "):\n```\n"
                        + result + "\n```\nContinue. If another tool is needed — output a new funa block.";
            }
        } catch (Exception e) {
            failed[0] = true;
            String msg = e.getMessage();
            if (msg == null || msg.isEmpty()) msg = e.getClass().getSimpleName();
            Log.e("agent", "run: " + msg, e);
            if (stopped) msg = tr("зупинено", "stopped", "остановлено");
            int code = e instanceof Net.ApiException ? ((Net.ApiException) e).code : 0;
            boolean challenge = challengeCode(code);
            if (code == 401) {
                // токен більше не діє: позначаємо акаунт, щоб UI запропонував вхід через сайт
                st.setStatus(aid, "expired");
            } else if (challenge) {
                // hCaptcha: акаунт живий, але DeepSeek вимагає пройти перевірку
                // на сайті. Автоматично її не розв'язати, тому показуємо кнопку.
                st.setStatus(aid, "challenge");
                msg = tr("DeepSeek просить підтвердити, що ти людина. "
                                + "Відкрий сайт, надішли будь-яке повідомлення й пройди перевірку, "
                                + "потім повернись і повтори.",
                        "DeepSeek asks you to confirm you are human. Open the site, send any message "
                                + "and complete the check, then come back and try again.",
                        "DeepSeek просит подтвердить, что ты человек. Открой сайт, отправь любое "
                                + "сообщение и пройди проверку, потом вернись и повтори.")
                        + " (" + code + ")";
            }
            error(msg, code, challenge);
        } finally {
            notifyState();
            // Підсумок запиту для статистики: один рядок на всю роботу,
            // включно з раундами інструментів.
            try {
                JSONObject re = ev("req_end");
                re.put("ms", Math.max(0, System.currentTimeMillis() - runT0));
                re.put("ok", !failed[0] && !stopped);
                re.put("stopped", stopped);
                push(re);
            } catch (Exception ignored) {}
            try {
                JSONObject busy = ev("busy");
                busy.put("on", false);
                push(busy);
            } catch (Exception ignored) {}
            // фонові субагенти могли завершитися, поки тривала відповідь
            maybeNotify();
        }
    }

    /* ------------------------- фонові субагенти ------------------------ */

    /** Запускає підзадачу (або tasks[]) у фоні; повертає результат моделі одразу. */
    private JSONObject startSubagents(JSONObject call) {
        JSONArray tasks = call.optJSONArray("tasks");
        String system = call.optString("system", "");
        List<String> ids = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        if (tasks != null && tasks.length() > 0) {
            // одновременно всё равно стартует MAX_RUNNING; больше 8 задач
            // в одном вызове — это уже не план, а мусор в промте
            int n = Math.min(tasks.length(), 8);
            for (int i = 0; i < n; i++) {
                String task = tasks.optString(i, "");
                SubTask t = SubTask.start(st, sessionId, task, system, 1);
                if (t != null) ids.add(t.id);
                else skipped.add(firstLine(task));
            }
            if (tasks.length() > n) {
                skipped.add("… (+" + (tasks.length() - n) + ")");
            }
        } else {
            SubTask t = SubTask.start(st, sessionId, call.optString("task", ""), system, 1);
            if (t != null) ids.add(t.id);
        }
        if (ids.isEmpty()) {
            boolean empty = (tasks == null || tasks.length() == 0) && call.optString("task", "").trim().isEmpty();
            return Builtin.err(empty
                    ? tr("subagent: потрібна непорожня задача",
                         "subagent: a non-empty task is required",
                         "subagent: нужна непустая задача")
                    : tr("subagent: уже працюють " + SubTask.MAX_RUNNING + " субагенти — спробуй пізніше",
                         "subagent: " + SubTask.MAX_RUNNING + " subagents are already running — try later",
                         "subagent: уже работают " + SubTask.MAX_RUNNING + " субагента — попробуй позже"));
        }
        StringBuilder sb = new StringBuilder();
        sb.append(tr("Субагенти запущені у фоні: ", "Subagents started in background: ",
                "Субагенты запущены в фоне: "));
        for (String id : ids) sb.append(id).append(' ');
        sb.append("\n").append(tr(
                "Вони працюють паралельно з чатом та один з одним. Результат прийде окремим повідомленням, коли задача завершиться. Не чекай — продовжуй іншу роботу або відповідай користувачу.",
                "They run in parallel with the chat and with each other. The result will arrive as a separate message when each task finishes. Do not wait — continue with other work or answer the user.",
                "Они работают параллельно с чатом и друг с другом. Результат придёт отдельным сообщением, когда задача завершится. Не жди — продолжай другую работу или отвечай пользователю."));
        if (!skipped.isEmpty()) {
            sb.append("\n").append(tr(
                    "Не запущено (одночасно працює щонайбільше " + SubTask.MAX_RUNNING + "): ",
                    "Not started (at most " + SubTask.MAX_RUNNING + " run at once): ",
                    "Не запущено (одновременно работает не больше " + SubTask.MAX_RUNNING + "): "));
            for (int i = 0; i < skipped.size(); i++) {
                if (i > 0) sb.append("; ");
                sb.append(skipped.get(i));
            }
            sb.append(". ").append(tr("Запусти їх пізніше, коли звільниться слот.",
                    "Start them later when a slot frees up.",
                    "Запусти их позже, когда освободится слот."));
        }
        return Builtin.ok(sb.toString());
    }

    private static String firstLine(String s) {
        String t = s == null ? "" : s.trim();
        int nl = t.indexOf('\n');
        if (nl > 0) t = t.substring(0, nl);
        t = t.replaceAll("\\s+", " ").trim();
        return t.length() > 60 ? t.substring(0, 60) + "…" : t;
    }


    private void onTaskDone(SubTask t) {
        if (t == null || t.parentSid == null || t.parentSid.isEmpty()) return;
        // ручну зупинку не пересилаємо моделі: користувач і так знає
        if ("stopped".equals(t.status)) return;
        synchronized (pending) {
            List<SubTask> l = pending.get(t.parentSid);
            if (l == null) {
                if (pending.size() >= 32) {
                    java.util.Iterator<String> it = pending.keySet().iterator();
                    if (it.hasNext()) { it.next(); it.remove(); }
                }
                l = new ArrayList<>();
                pending.put(t.parentSid, l);
            }
            // сесію могли закрити, не дочитавши результати: не тримаємо
            // їх у памʼяті без меж
            if (l.size() >= 20) l.remove(0);
            l.add(t);
        }
        maybeNotify();
    }

    /** Викликати після завантаження сесії в UI. */
    public void onSessionReady() { maybeNotify(); }

    /** Віддає моделі результати завершених задач поточної сесії. */
    private synchronized void maybeNotify() {
        if (closed || stopped || busyOther()) return;
        final String sid = sessionId;
        if (sid == null || sid.isEmpty() || st.token().isEmpty()) return;

        List<SubTask> batch;
        synchronized (pending) {
            List<SubTask> l = pending.get(sid);
            if (l == null || l.isEmpty()) return;
            batch = new ArrayList<>(l);
            pending.remove(sid);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[FunaTask]\n");
        for (SubTask t : batch) {
            sb.append("Subagent ").append(t.id).append(" — ");
            if ("done".equals(t.status)) {
                sb.append("finished\n");
            } else {
                sb.append("failed (").append(t.status).append(")");
                if (t.error != null && !t.error.isEmpty()) sb.append(": ").append(t.error);
                sb.append('\n');
            }
            sb.append("Task: ").append(t.title).append('\n');
            if (t.result != null && !t.result.trim().isEmpty()) {
                String r = t.result.trim();
                if (r.length() > 7000) r = r.substring(0, 7000) + "\n… (обрізано)";
                sb.append("Result:\n").append(r).append('\n');
            }
            sb.append('\n');
        }
        sb.append(tr(
                "Це автоматичне повідомлення про завершення фонових субагентів. Стисло повідом користувача про результат (або про помилку). Не переказуй увесь текст, якщо він довгий. Якщо ще є запущені субагенти — не чекай на них.",
                "This is an automatic notification that background subagents finished. Briefly tell the user the result (or the error). Do not repeat the whole text if it is long. If other subagents are still running, do not wait for them.",
                "Это автоматическое уведомление о завершении фоновых субагентов. Кратко сообщи пользователю результат (или ошибку). Не пересказывай весь текст, если он длинный. Если ещё есть запущенные субагенты — не жди их."));
        sb.append("\n[/FunaTask]\n");

        if (!start(sb.toString(), null, "notify", 0)) {
            // не вдалося стартувати — повертаємо задачі в чергу, віддамо пізніше
            synchronized (pending) {
                List<SubTask> l = pending.get(sid);
                if (l == null) {
                    l = new ArrayList<>();
                    pending.put(sid, l);
                }
                l.addAll(0, batch);
            }
        }
    }

    private static class StreamResult {
        long messageId;
        final StringBuilder answer = new StringBuilder();
    }

    /** Стеля тексту, який агент тримає в памʼяті (стрім усе одно йде в UI). */
    private static final int MAX_REPLY = 2000000;

    private static void appendCapped(StringBuilder sb, String text) {
        if (text == null || text.isEmpty() || sb.length() >= MAX_REPLY) return;
        int room = MAX_REPLY - sb.length();
        sb.append(text.length() <= room ? text : text.substring(0, room));
    }

    private String tr(String uk, String en, String ru) {
        String l = st.lang();
        return "en".equals(l) ? en : ("ru".equals(l) ? ru : uk);
    }

    /** Яку саме навичку вимагає виклик doc: docx чи pdf. */
    private static String docSkill(JSONObject call) {
        String fmt = call.optString("format", "pdf").toLowerCase(Locale.US);
        if ("word".equals(fmt) || "doc".equals(fmt)) fmt = "docx";
        return "docx".equals(fmt) ? "docx" : "pdf";
    }

    /** Інструмент створення документа (у Tools є аліас document). */
    private static boolean isDocTool(String name) {
        return "doc".equalsIgnoreCase(name) || "document".equalsIgnoreCase(name);
    }

    private StreamResult stream(String path, JSONObject body, JSONObject pow) throws Exception {
        final StreamResult res = new StreamResult();
        final String[] kind = {"answer"};
        final boolean[] finished = {false};
        // Час роботи моделі: від першого кадру до a_end. Це саме те, що
        // показуємо біля відповіді («працювала 12.4 s»).
        final long t0 = System.currentTimeMillis();

        JSONObject as = ev("a_start");
        push(as);

        conn = net.streamRequest(path, body, pow, new Net.Sse() {
            @Override public void onEvent(String name, JSONObject data) {
                // «Стоп» міг натиснути до того, як Net активував з'єднання:
                // перший кадр — останній шанс обірвати стрім, не чекаючи
                // наступної дельти чи кінця відповіді.
                if (stopped) { try { net.cancel(); } catch (Exception ignored) {} return; }
                try {
                    if ("ready".equals(name)) {
                        long id = data.optLong("response_message_id", 0);
                        if (id > 0) {
                            res.messageId = id;
                            lastMessageId = id;
                            JSONObject o = ev("a_id");
                            o.put("id", id);
                            push(o);
                        }
                        long uid = data.optLong("request_message_id", 0);
                        if (uid > 0) {
                            JSONObject o = ev("u_id");
                            o.put("id", uid);
                            push(o);
                        }
                    } else if ("title".equals(name)) {
                        JSONObject o = ev("title");
                        o.put("text", data.optString("content", ""));
                        push(o);
                    }
                } catch (Exception ignored) {}
            }

            @Override public void onData(JSONObject data) {
                if (stopped) { try { net.cancel(); } catch (Exception ignored) {} return; }
                try {
                    int code = data.optInt("code", 0);
                    if (code != 0) {
                        error(data.optString("msg", "помилка " + code));
                        return;
                    }
                    String p = data.optString("p", "");
                    Object v = data.opt("v");
                    if (!p.isEmpty()) {
                        if (p.equals("response/fragments") && "APPEND".equals(data.optString("o"))
                                && v instanceof JSONArray) {
                            JSONArray arr = (JSONArray) v;
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject f = arr.optJSONObject(i);
                                if (f == null) continue;
                                String type = f.optString("type", "RESPONSE");
                                String content = f.optString("content", "");
                                kind[0] = "THINK".equals(type) ? "think" : "answer";
                                if ("THINK".equals(type)) delta("think", content);
                                else { appendCapped(res.answer, content); delta("answer", content); }
                            }
                        } else if (p.endsWith("/content") && v instanceof String) {
                            String s = (String) v;
                            if ("think".equals(kind[0])) delta("think", s);
                            else { appendCapped(res.answer, s); delta("answer", s); }
                        } else if ("response/status".equals(p)) {
                            String s = String.valueOf(v);
                            if ("FINISHED".equals(s) || "INCOMPLETE".equals(s)) finished[0] = true;
                        }
                    } else if (v instanceof String) {
                        // скорочений дельта-кадр: {"v":"..."} продовжує останній фрагмент
                        String frag = (String) v;
                        if ("think".equals(kind[0])) delta("think", frag);
                        else { appendCapped(res.answer, frag); delta("answer", frag); }
                    } else if (v instanceof JSONObject && ((JSONObject) v).has("response")) {
                        JSONObject r = ((JSONObject) v).optJSONObject("response");
                        if (r == null) return;
                        long mid = r.optLong("message_id", 0);
                        if (mid > 0) {
                            res.messageId = mid;
                            lastMessageId = mid;
                            JSONObject o = ev("a_id");
                            o.put("id", mid);
                            push(o);
                        }
                        long ruid = r.optLong("parent_id", 0);
                        if (ruid > 0) {
                            JSONObject o = ev("u_id");
                            o.put("id", ruid);
                            push(o);
                        }
                        JSONArray frags = r.optJSONArray("fragments");
                        if (frags != null) {
                            for (int i = 0; i < frags.length(); i++) {
                                JSONObject f = frags.optJSONObject(i);
                                if (f == null) continue;
                                String type = f.optString("type", "RESPONSE");
                                String content = f.optString("content", "");
                                kind[0] = "THINK".equals(type) ? "think" : "answer";
                                if ("THINK".equals(type)) delta("think", content);
                                else { appendCapped(res.answer, content); delta("answer", content); }
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        });
        JSONObject ae = ev("a_end");
        ae.put("id", res.messageId);
        ae.put("status", finished[0] ? "FINISHED" : "STOPPED");
        ae.put("ts", System.currentTimeMillis());
        ae.put("ms", Math.max(0, System.currentTimeMillis() - t0));
        push(ae);
        conn = null;
        return res;
    }

    private void delta(String kind, String text) {
        if (text == null || text.isEmpty()) return;
        JSONObject o = ev(kind);
        try { o.put("v", text); } catch (Exception ignored) {}
        push(o);
    }

    public static JSONObject findTool(String text) {
        if (text == null) return null;
        int i = 0;
        while ((i = text.indexOf("```funa", i)) >= 0) {
            int start = text.indexOf('\n', i);
            if (start < 0) return null;
            // Закривати блок може лише ``` на початку рядка: інакше ``` у
            //середині JSON-рядка (наприклад, content для write) обрізав би його.
            int end = text.indexOf("\n```", start + 1);
            if (end < 0) return null;
            String json = text.substring(start + 1, end).trim();
            try {
                JSONObject o = new JSONObject(json);
                if (!o.has("tool")) {
                    // моделі інколи віддають OpenAI-стиль: name + arguments
                    String n = o.optString("name", "");
                    if (!n.isEmpty()) {
                        JSONObject f = new JSONObject();
                        Object args = o.opt("arguments");
                        if (args instanceof JSONObject) {
                            java.util.Iterator<String> it = ((JSONObject) args).keys();
                            while (it.hasNext()) {
                                String k = it.next();
                                f.put(k, ((JSONObject) args).opt(k));
                            }
                        } else if (args != null && args != JSONObject.NULL) {
                            try {
                                JSONObject a = new JSONObject(String.valueOf(args));
                                java.util.Iterator<String> it = a.keys();
                                while (it.hasNext()) {
                                    String k = it.next();
                                    f.put(k, a.opt(k));
                                }
                            } catch (Exception ignored) {}
                        }
                        f.put("tool", n);
                        return f;
                    }
                }
                return o;
            } catch (Exception ignored) {}
            i = end + 4;
        }
        return null;
    }

    private static String stripSummary(String s) {
        if (s == null) return "";
        String t = s.trim();
        if (!t.startsWith("[FunaContext]")) return s;
        int end = t.indexOf("[/FunaContext]");
        if (end < 0) return s;
        return t.substring(end + 14).trim();
    }

    /** Прибирає службові повідомлення про завершення субагентів з історії. */
    private static String stripTask(String s) {
        if (s == null) return "";
        String t = s;
        while (true) {
            int a = t.indexOf("[FunaTask]");
            if (a < 0) break;
            int b = t.indexOf("[/FunaTask]", a);
            if (b < 0) break;
            t = (t.substring(0, a) + t.substring(b + 11)).trim();
        }
        return t;
    }

    private static String stripFacts(String s) {
        String[] heads = {"Збережені факти", "Saved facts", "Сохранённые факты"};
        for (String h : heads) {
            if (s.startsWith(h)) {
                int i = s.indexOf("\n\n");
                return i < 0 ? "" : s.substring(i + 2).trim();
            }
        }
        return s;
    }

    /** Час повідомлення у мілісекундах; DeepSeek віддає секунди з дробом. */
    private static final String[] TS_KEYS = {
        "insert_at", "inserted_at", "created_at", "create_time", "created_time",
        "updated_at", "update_time", "send_time", "sent_at", "timestamp", "time",
        "createdAt", "insertedAt", "updatedAt"
    };

    private static long tsOf(JSONObject m) {
        if (m == null) return 0;
        for (String k : TS_KEYS) {
            Object v = m.opt(k);
            if (v == null) continue;
            long t = toMs(v);
            if (t > 0) return t;
        }
        // Деякі версії API кладуть час у вкладений обʼєкт.
        JSONObject nested = m.optJSONObject("message");
        if (nested == null) nested = m.optJSONObject("meta");
        return nested == null ? 0 : tsOf(nested);
    }

    private static long toMs(Object v) {
        if (v == null || v == JSONObject.NULL) return 0;
        if (v instanceof Number) {
            double d = ((Number) v).doubleValue();
            if (!(d > 0)) return 0;
            return d < 100000000000.0 ? (long) (d * 1000.0) : (long) d;
        }
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) return 0;
        try {
            double d = Double.parseDouble(s);
            if (d > 0) return d < 100000000000.0 ? (long) (d * 1000.0) : (long) d;
        } catch (Exception ignored) {}
        try {
            return java.time.OffsetDateTime.parse(s).toInstant().toEpochMilli();
        } catch (Exception ignored) {}
        try {
            return java.time.Instant.parse(s).toEpochMilli();
        } catch (Exception ignored) {}
        return 0;
    }

    /** Скільки модель працювала над відповіддю: updated_at - insert_at. */
    private static long workMs(JSONObject m) {
        if (m == null) return 0;
        long a = 0;
        for (String k : new String[]{"insert_at", "inserted_at", "created_at", "create_time", "created_time"}) {
            a = toMs(m.opt(k));
            if (a > 0) break;
        }
        long b = 0;
        for (String k : new String[]{"updated_at", "update_time", "completed_at", "finish_time"}) {
            b = toMs(m.opt(k));
            if (b > 0) break;
        }
        if (a <= 0 || b <= a) return 0;
        long d = b - a;
        return d > 6L * 3600 * 1000 ? 0 : d;
    }

    /**
     * Одне повідомлення історії у форматі UI. thumbBudget != null означає
     * «можна підвантажити прев'ю картинок» (лише для активної гілки).
     */
    private static JSONObject basicMessage(Net net, JSONObject m, String base, int[] thumbBudget) throws Exception {
        String role = m.optString("role", "");
        JSONArray frags = m.optJSONArray("fragments");
        StringBuilder text = new StringBuilder();
        StringBuilder think = new StringBuilder();
        JSONArray files = new JSONArray();
        if (frags != null) {
            for (int k = 0; k < frags.length(); k++) {
                JSONObject f = frags.optJSONObject(k);
                if (f == null) continue;
                String type = f.optString("type", "");
                String c = f.optString("content", "");
                if ("THINK".equals(type)) appendCapped(think, c);
                else if ("RESPONSE".equals(type) || "REQUEST".equals(type)) appendCapped(text, c);
                else if ("FILE".equals(type)) {
                    JSONArray fs = f.optJSONArray("files");
                    if (fs == null) continue;
                    for (int x = 0; x < fs.length() && files.length() < 40; x++) {
                        JSONObject it = fs.optJSONObject(x);
                        if (it == null) continue;
                        boolean img = it.optBoolean("is_image", false);
                        JSONObject q = new JSONObject();
                        q.put("id", it.optString("id", ""));
                        q.put("name", it.optString("file_name", "file"));
                        q.put("size", it.optLong("file_size", 0));
                        q.put("isImage", img);
                        if (img && thumbBudget != null && files.length() < 3 && thumbBudget[0] < 12) {
                            thumbBudget[0]++;
                            try {
                                byte[] data = net.fileBytes(it.optString("signed_path", ""));
                                String th = Attach.thumb(data, 520);
                                if (!th.isEmpty()) q.put("thumb", th);
                            } catch (Exception ignored) {}
                        }
                        files.put(q);
                    }
                }
            }
        }
        String t = text.toString();
        if ("USER".equals(role)) {
            if (base != null && !base.isEmpty() && t.startsWith(base)) t = t.substring(base.length()).trim();
            t = stripFacts(t);
            t = stripSummary(t);
            t = stripTask(t);
        }
        if ("USER".equals(role) && t.trim().isEmpty() && files.length() == 0) return null;
        JSONObject o = new JSONObject();
        o.put("role", "USER".equals(role) ? "user" : "assistant");
        o.put("text", t);
        o.put("think", think.toString());
        o.put("id", m.optLong("message_id", 0));
        o.put("pending", !"FINISHED".equals(m.optString("status")));
        long ts = tsOf(m);
        if (ts > 0) o.put("ts", ts);
        if (!"USER".equals(role)) {
            long ms = workMs(m);
            if (ms > 0) o.put("ms", ms);
        }
        if (files.length() > 0) o.put("files", files);
        return o;
    }

    /** Хвіст, що йшов під конкретною версією: спускаємось по найновішій дитині. */
    private static JSONArray tailAfter(JSONObject from, Map<Long, List<JSONObject>> kids,
                                       String base, int max) {
        JSONArray tail = new JSONArray();
        long cur = from.optLong("message_id", 0);
        for (int i = 0; i < max; i++) {
            List<JSONObject> l = kids.get(cur);
            if (l == null || l.isEmpty()) break;
            JSONObject c = l.get(l.size() - 1);
            try {
                JSONObject b = basicMessage(null, c, base, null);
                if (b != null) tail.put(b);
            } catch (Exception ignored) {}
            cur = c.optLong("message_id", 0);
        }
        return tail;
    }

    public static List<JSONObject> historyMessages(Net net, String sid, String base) throws Exception {
        JSONObject h = net.history(sid);
        JSONArray arr = h.optJSONArray("chat_messages");
        List<JSONObject> out = new ArrayList<>();
        if (arr == null || arr.length() == 0) return out;

        // Дерево гілок: кожна редагована репліка або перегенерована
        // відповідь — це сусід (sibling) з тим самим parent_id.
        Map<Long, JSONObject> byId = new HashMap<>();
        Map<Long, List<JSONObject>> kids = new HashMap<>();
        long maxId = 0;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject m = arr.optJSONObject(i);
            if (m == null) continue;
            long id = m.optLong("message_id", 0);
            if (id <= 0) continue;
            byId.put(id, m);
            long pid = m.optLong("parent_id", 0);
            List<JSONObject> l = kids.get(pid);
            if (l == null) { l = new ArrayList<>(); kids.put(pid, l); }
            l.add(m);
            if (id > maxId) maxId = id;
        }
        for (List<JSONObject> l : kids.values()) {
            Collections.sort(l, (a, b) -> Long.compare(a.optLong("message_id", 0), b.optLong("message_id", 0)));
        }

        // Якщо сервер сам каже, яка гілка активна, довіряємо йому: після
        // перемикання версії найбільший message_id не обовʼязково лист.
        long active = h.optLong("current_message_id", 0);
        JSONObject sess = h.optJSONObject("chat_session");
        if (active <= 0 && sess != null) {
            active = sess.optLong("current_message_id", 0);
            if (active <= 0) active = sess.optLong("current_leaf_id", 0);
        }
        long last = active > 0 && byId.containsKey(active) ? active : maxId;

        // Активна гілка: від вибраного листа до кореня.
        List<JSONObject> chain = new ArrayList<>();
        long cur = last;
        while (cur > 0 && byId.containsKey(cur) && chain.size() < 20000) {
            JSONObject m = byId.get(cur);
            chain.add(m);
            cur = m.optLong("parent_id", 0);
        }
        Collections.reverse(chain);

        int thumbs[] = {0};
        int n = chain.size();
        long prevTs = 0;
        for (int i = 0; i < n; i++) {
            JSONObject m = chain.get(i);
            JSONObject o = basicMessage(net, m, base, thumbs);
            if (o == null) continue;
            // Старі відповіді не мають збереженої тривалості — оцінюємо її
            // як проміжок від попереднього повідомлення до цього.
            long ts = o.optLong("ts", 0);
            if ("assistant".equals(o.optString("role")) && ts > 0 && !o.has("ms") && prevTs > 0
                    && ts > prevTs && ts - prevTs < 30L * 60 * 1000) {
                o.put("ms", ts - prevTs);
            }
            if (ts > 0) prevTs = ts;
            // Версії потрібні лише для свіжої частини чату: інакше історія
            // на 1000 повідомлень роздувається копіями хвостів.
            if (i >= n - 16) {
                long pid = m.optLong("parent_id", 0);
                List<JSONObject> sib = kids.get(pid);
                if (sib != null && sib.size() > 1 && sib.size() <= 50) {
                    JSONArray vers = new JSONArray();
                    int vi = 0, kept = 0;
                    for (int k = 0; k < sib.size(); k++) {
                        JSONObject s = sib.get(k);
                        if (!s.optString("role", "").equals(m.optString("role", ""))) continue;
                        JSONObject v = basicMessage(null, s, base, null);
                        if (v == null) continue;
                        if (s.optLong("message_id", 0) == m.optLong("message_id", 0)) vi = kept;
                        v.put("tail", tailAfter(s, kids, base, 60));
                        vers.put(v);
                        kept++;
                    }
                    if (vers.length() > 1) {
                        o.put("vers", vers);
                        o.put("vi", vi);
                    }
                }
            }
            out.add(o);
        }
        return out;
    }

}
