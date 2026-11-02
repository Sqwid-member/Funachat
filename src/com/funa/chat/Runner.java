package com.funa.chat;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Єдина точка виконання інструментів агента: базові (Builtin), файлові
 * (Tools) і субагенти. Використовується чатом, субагентом і локальним API.
 *
 * Субагент: {"tool":"subagent","task":"..."} — один, або
 * {"tool":"subagent","tasks":["...","..."]} — до 4 паралельно.
 */
public final class Runner {
    private Runner() {}

    /** Скільки субагентів можна запустити паралельно. */
    public static final int MAX_PARALLEL = 4;
    private static final long JOIN_MS = 250000L;

    /** Прогрес виконання (картка інструмента в UI). */
    public interface Progress {
        void on(String text);
    }

    public static JSONObject exec(JSONObject call, Store st, final Progress p, final int depth) {
        String name = call.optString("tool", "");
        // Мова інтерфейсу для вбудованої навички (skill). Інші інструменти
        // не отримують службове поле lang.
        if (st != null && "skill".equalsIgnoreCase(name) && !call.has("lang")) {
            try { call.put("lang", st.lang()); } catch (Exception ignored) {}
        }
        if ("subagent".equalsIgnoreCase(name)) {
            if (depth + 1 > SubAgent.MAX_DEPTH) {
                return Builtin.err("subagent: перевищено глибину вкладення (" + SubAgent.MAX_DEPTH + ")");
            }
            JSONArray tasks = call.optJSONArray("tasks");
            if (tasks != null && tasks.length() > 0) return parallel(call, st, p, depth, tasks);
            return one(call.optString("task", ""), call.optString("system", ""), st, p, depth);
        }
        if (Builtin.isBuiltin(name)) return Builtin.exec(call);
        return Tools.exec(call);
    }

    private static JSONObject one(String task, String system, Store st, Progress p, int depth) {
        if (task == null || task.trim().isEmpty()) return Builtin.err("subagent: потрібна задача");
        try {
            SubAgent.Progress sp = p == null ? null : new SubAgent.Progress() {
                @Override public void on(String text) { p.on(text); }
            };
            String out = SubAgent.run(st, task, system, depth + 1, sp);
            return Builtin.ok(out);
        } catch (Exception e) {
            String m = e.getMessage();
            m = m == null || m.isEmpty() ? e.getClass().getSimpleName() : m;
            Log.e("subagent", "run: " + m, e);
            return Builtin.err("subagent: " + m);
        }
    }

    /** Кілька незалежних підзадач одночасно; порядок відповідей збережено. */
    private static JSONObject parallel(JSONObject call, final Store st, final Progress p,
                                       final int depth, JSONArray tasks) {
        int n = Math.min(MAX_PARALLEL, tasks.length());
        final String system = call.optString("system", "");
        final String[] out = new String[n];
        final String[] err = new String[n];
        Thread[] threads = new Thread[n];
        for (int i = 0; i < n; i++) {
            final int idx = i;
            final String task = tasks.optString(i, "");
            threads[i] = new Thread(new Runnable() {
                @Override public void run() {
                    try {
                        SubAgent.Progress sp = p == null ? null : new SubAgent.Progress() {
                            @Override public void on(String text) {
                                p.on("[" + (idx + 1) + "] " + text);
                            }
                        };
                        out[idx] = SubAgent.run(st, task, system, depth + 1, sp);
                    } catch (Exception e) {
                        String m = e.getMessage();
                        err[idx] = m == null || m.isEmpty() ? e.getClass().getSimpleName() : m;
                    }
                }
            }, "funa-sub-" + (i + 1));
            threads[i].setDaemon(true);
            threads[i].start();
        }
        long until = System.currentTimeMillis() + JOIN_MS;
        for (Thread t : threads) {
            long left = until - System.currentTimeMillis();
            try {
                if (left > 0) t.join(left);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tasks.length(); i++) {
            if (i > 0) sb.append("\n\n");
            sb.append("[").append(i + 1).append("] ");
            if (i >= n) {
                // задачі понад ліміт не мають зникати без сліду
                sb.append("error: пропущено (максимум ").append(MAX_PARALLEL)
                        .append(" паралельних субагентів)");
            } else if (out[i] != null) {
                sb.append(out[i].trim());
            } else {
                sb.append("error: ").append(err[i] == null ? "timeout" : err[i]);
            }
        }
        return Builtin.ok(sb.toString());
    }
}
