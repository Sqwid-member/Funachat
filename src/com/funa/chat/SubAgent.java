package com.funa.chat;

import org.json.JSONObject;

/**
 * Субагент: окрема сесія DeepSeek, яка виконує підзадачу і повертає
 * лише фінальний текст. Використовується інструментом
 *   {"tool":"subagent","task":"..."}
 * з чату в застосунку та з локального API (auto tools).
 *
 * Фонові субагенти (картки в чаті) викликають {@link #runJob} зі своїм
 * {@link Listener}: він віддає дельти стріму й події інструментів, а
 * {@link SubTask} складає з них транскрипт для перегляду.
 *
 * Обмеження: глибина вкладеності 2, 4 раунди інструментів, дедлайн 4 хв.
 * Темп запитів і паузи акаунта — як завжди через {@link Guard} у Net.
 */
public final class SubAgent {
    private SubAgent() {}

    public static final int MAX_DEPTH = 2;
    private static final int MAX_ROUNDS = 4;
    private static final long DEADLINE_MS = 240000L;
    private static final int MAX_TASK = 8000;
    private static final int MAX_CONTEXT = 6000;
    /** Стеля однієї відповіді, яку тримаємо в памʼяті (стрім іде далі). */
    private static final int MAX_REPLY = 1000000;

    /** Прогрес для картки інструмента в UI (може бути null). */
    public interface Progress {
        void on(String text);
    }

    /** Події фонового субагента для транскрипту (може бути null). */
    public interface Listener {
        void onSession(String sid);
        void onDelta(String kind, String text);
        void onRound(String answer);
        void onTool(String name, JSONObject call);
        void onToolResult(String name, boolean ok, String text);
        boolean cancelled();
    }

    private static final String SYSTEM_UK =
            "Ти — субагент. Виконай підзадачу автономно й поверни ТІЛЬКИ фінальний результат "
                    + "(без запитань і без пояснень про процес). Відповідай тією ж мовою, що й задача.\n"
                    + "Якщо потрібні файли, термінал, час, калькулятор чи інші дані — виводь рівно один блок:\n"
                    + "```funa\n{\"tool\":\"read\",\"path\":\"...\"}\n```\n"
                    + "і зупинись: клієнт виконає інструмент і надішле результат наступним кроком.\n"
                    + "Перед створенням DOCX/PDF спершу виклич {\"tool\":\"skill\",\"name\":\"docx\"} або \"pdf\".\n"
                    + "Фінальний результат має бути самодостатнім: коротко що зроблено, ключові факти й абсолютні шляхи до створених файлів. "
                    + "Не вигадуй даних, яких не бачив, і не переказуй задачу.";

    private static final String SYSTEM_EN =
            "You are a subagent. Complete the subtask autonomously and return ONLY the final result "
                    + "(no questions, no narration). Answer in the language of the task.\n"
                    + "If you need files, terminal, time, calculator or other data, output exactly one block:\n"
                    + "```funa\n{\"tool\":\"read\",\"path\":\"...\"}\n```\n"
                    + "then stop: the client runs the tool and sends the result as the next step.\n"
                    + "Before creating a DOCX/PDF, first call {\"tool\":\"skill\",\"name\":\"docx\"} or \"pdf\".\n"
                    + "Make the final result self-contained: briefly what was done, the key facts and absolute paths to any files you created. "
                    + "Do not invent data you did not see and do not restate the task.";

    private static final String SYSTEM_RU =
            "Ты — субагент. Выполни подзадачу автономно и верни ТОЛЬКО финальный результат "
                    + "(без вопросов и без рассказа о процессе). Отвечай на языке задачи.\n"
                    + "Если нужны файлы, терминал, время, калькулятор или другие данные — выводи ровно один блок:\n"
                    + "```funa\n{\"tool\":\"read\",\"path\":\"...\"}\n```\n"
                    + "и остановись: клиент выполнит инструмент и пришлёт результат следующим шагом.\n"
                    + "Перед созданием DOCX/PDF сначала вызови {\"tool\":\"skill\",\"name\":\"docx\"} или \"pdf\".\n"
                    + "Финальный результат должен быть самодостаточным: коротко что сделано, ключевые факты и абсолютные пути к созданным файлам. "
                    + "Не выдумывай данных, которых не видел, и не пересказывай задачу.";

    public static String run(Store st, String task, String system, String context, int depth,
                             Progress p) throws Exception {
        return runJob(new Net(st), st, task, system, context, depth, p, null);
    }

    public static String runJob(Net net, Store st, String task, String system, String context,
                                int depth, Progress p, Listener l) throws Exception {
        if (task == null || task.trim().isEmpty()) throw new Exception("subagent: порожня задача");
        if (depth > MAX_DEPTH) throw new Exception("subagent: перевищено глибину вкладення (" + MAX_DEPTH + ")");
        String job = task.trim();
        if (job.length() > MAX_TASK) job = job.substring(0, MAX_TASK);

        String sys = system == null ? "" : system.trim();
        // системний промт задає клієнт API: без межі він витісняє задачу
        if (sys.length() > 4000) sys = sys.substring(0, 4000);
        if (sys.isEmpty()) sys = defaultSystem(st.lang());

        // Контекст від батьківської моделі: факти, обмеження, шляхи до
        // файлів. Він не замінює задачу, тому має окрему межу.
        String ctx = context == null ? "" : context.trim();
        if (ctx.length() > MAX_CONTEXT) ctx = ctx.substring(0, MAX_CONTEXT);

        String sid = null;
        // Аккаунт фиксируем на старте: смена активного аккаунта посреди
        // задачи не должна отправить её инструменты (особенно вложенных
        // субагентов) уже от имени нового аккаунта.
        final String aid = st.activeId();
        long deadline = System.currentTimeMillis() + DEADLINE_MS;
        String prompt = sys + (ctx.isEmpty() ? "" : "\n\nContext:\n" + ctx)
                + "\n\nTask:\n" + job;
        String answer = "";
        try {
            JSONObject sess = net.createSession();
            sid = sess.optString("id", "");
            if (sid.isEmpty()) throw new Exception("subagent: DeepSeek не створив сесію");
            if (l != null) l.onSession(sid);

            for (int round = 0; round < MAX_ROUNDS; round++) {
                if (l != null && l.cancelled()) throw new Exception("subagent: зупинено");
                if (System.currentTimeMillis() > deadline) {
                    throw new Exception("subagent: перевищено час (" + (DEADLINE_MS / 1000) + " с)");
                }
                JSONObject body = new JSONObject();
                body.put("chat_session_id", sid);
                body.put("parent_message_id", JSONObject.NULL);
                body.put("prompt", prompt);
                body.put("ref_file_ids", new org.json.JSONArray());
                body.put("thinking_enabled", st.thinking());
                body.put("search_enabled", st.search());
                body.put("preempt", false);
                body.put("model_type", st.model());
                body.put("action", "default");

                JSONObject pow = net.powHeader("/api/v0/chat/completion");
                final StringBuilder sb = new StringBuilder();
                final StringBuilder think = new StringBuilder();
                net.streamRequest("/api/v0/chat/completion", body, pow, new Net.Sse() {
                    private String kind = "answer";

                    @Override public void onEvent(String name, JSONObject data) { }

                    @Override public void onData(JSONObject data) {
                        try {
                            if (data.optInt("code", 0) != 0) return;
                            String path = data.optString("p", "");
                            Object v = data.opt("v");
                            if ("response/fragments".equals(path) && "APPEND".equals(data.optString("o"))
                                    && v instanceof org.json.JSONArray) {
                                org.json.JSONArray arr = (org.json.JSONArray) v;
                                for (int i = 0; i < arr.length(); i++) {
                                    JSONObject f = arr.optJSONObject(i);
                                    if (f == null) continue;
                                    kind = "THINK".equals(f.optString("type", "RESPONSE")) ? "think" : "answer";
                                    String part = f.optString("content", "");
                                    if ("think".equals(kind)) {
                                        if (think.length() < MAX_REPLY) think.append(part);
                                        if (l != null) l.onDelta("think", part);
                                    } else {
                                        if (sb.length() < MAX_REPLY) sb.append(part);
                                        if (l != null) l.onDelta("answer", part);
                                    }
                                }
                            } else if (path.endsWith("/content") && v instanceof String) {
                                String part = (String) v;
                                if ("think".equals(kind)) {
                                    if (think.length() < MAX_REPLY) think.append(part);
                                    if (l != null) l.onDelta("think", part);
                                } else {
                                    if (sb.length() < MAX_REPLY) sb.append(part);
                                    if (l != null) l.onDelta("answer", part);
                                }
                            } else if (path.isEmpty() && v instanceof String) {
                                String part = (String) v;
                                if ("think".equals(kind)) {
                                    if (think.length() < MAX_REPLY) think.append(part);
                                    if (l != null) l.onDelta("think", part);
                                } else {
                                    if (sb.length() < MAX_REPLY) sb.append(part);
                                    if (l != null) l.onDelta("answer", part);
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                });
                answer = sb.toString().trim();
                if (l != null) l.onRound(answer);
                if (p != null) p.on(round == 0
                        ? brief(answer)
                        : brief("Крок " + (round + 1) + ":\n" + answer));

                JSONObject tool = Agent.findTool(answer);
                if (tool == null) break;

                String name = tool.optString("tool", "");
                if (name.isEmpty()) break;
                if (p != null) p.on("Інструмент: " + name);
                if (l != null) l.onTool(name, tool);

                // усі інструменти, включно з вкладеними субагентами, — через Runner
                Runner.Progress rp = p == null ? null : new Runner.Progress() {
                    @Override public void on(String text) { p.on(text); }
                };
                if (!aid.equals(st.activeId())) throw new Exception("subagent: аккаунт переключён");
                JSONObject res = Runner.exec(tool, st, rp, depth);
                boolean ok = res.optBoolean("ok", false);
                String text = res.optString("text", res.optString("error", ""));
                if (text.length() > 12000) text = text.substring(0, 12000) + "\n… (обрізано)";
                if (p != null) p.on(brief("Інструмент " + name + " (ok=" + ok + "):\n" + text));
                if (l != null) l.onToolResult(name, ok, text);
                // тримаємо повний транскрипт у prompt: сесія DeepSeek не гарантує
                // звʼязок із попереднім кроком (parent_message_id = null)
                prompt = prompt + "\n\nAssistant:\n" + answer
                        + "\n\nTool result «" + name + "» (ok=" + ok + "):\n```\n" + text
                        + "\n```\nПродовжуй. Якщо потрібен ще інструмент — виведи новий блок funa.";
                if (prompt.length() > 200000) {
                    prompt = prompt.substring(0, 20000) + "\n…\n"
                            + prompt.substring(prompt.length() - 180000);
                }
            }
            if (answer.isEmpty()) throw new Exception("subagent: порожня відповідь");
            return answer;
        } finally {
            if (sid != null && !sid.isEmpty() && st.apiDelete()) {
                try { net.deleteSession(sid); } catch (Exception ignored) {}
            }
        }
    }

    private static String defaultSystem(String lang) {
        if ("en".equals(lang)) return SYSTEM_EN;
        if ("ru".equals(lang)) return SYSTEM_RU;
        return SYSTEM_UK;
    }

    private static String brief(String s) {
        if (s == null) return "";
        return s.length() > 700 ? s.substring(0, 700) + "\n…" : s;
    }
}
