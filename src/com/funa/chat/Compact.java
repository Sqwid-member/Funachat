package com.funa.chat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;

/** Стиснення довгого контексту в конспект для нового чату. */
public final class Compact {
    private Compact() {}

    public static String instruction(String lang) {
        if ("en".equals(lang)) {
            return "Compress the conversation below into a dense, faithful handover summary. "
                    + "Keep: the user's goals, confirmed facts, names, numbers, dates, decisions, file paths, "
                    + "code/commands that matter, current state and open tasks. Drop greetings and repetition. "
                    + "Write in English, as structured bullet points, max 2500 words. Output only the summary.";
        }
        if ("ru".equals(lang)) {
            return "Сожми разговор ниже в плотную и точную сводку для передачи контекста. "
                    + "Сохрани: цели пользователя, подтверждённые факты, имена, числа, даты, решения, пути к файлам, "
                    + "важный код/команды, текущее состояние и незавершённые задачи. Убери приветствия и повторы. "
                    + "Пиши по-русски, структурированными пунктами, максимум 2500 слов. Выведи только сводку.";
        }
        return "Стисни розмову нижче в щільний і точний конспект для передачі контексту. "
                + "Збережи: цілі користувача, підтверджені факти, імена, числа, дати, рішення, шляхи до файлів, "
                + "важливий код/команди, поточний стан і незавершені задачі. Прибери вітання й повтори. "
                + "Пиши українською, структурованими пунктами, максимум 2500 слів. Виведи лише конспект.";
    }

    /** Конспект — это 2–3 тысячи слов; поток сверх этого не копим. */
    private static void add(StringBuilder sb, String s) {
        if (s == null || s.isEmpty() || sb.length() >= 60000) return;
        int room = 60000 - sb.length();
        sb.append(s.length() <= room ? s : s.substring(0, room));
    }

    public static String summarize(Net net, String transcript, String lang) throws Exception {
        if (transcript == null) transcript = "";
        // Довгий чат цілком у промт не влізе: лишаємо початок (цілі) і кінець
        // (поточний стан), а середину позначаємо.
        if (transcript.length() > 400000) {
            transcript = transcript.substring(0, 25000)
                    + "\n\n… (середину розмови опущено) …\n\n"
                    + transcript.substring(transcript.length() - 375000);
        }
        JSONObject s = net.createSession();
        String sid = s.optString("id", "");
        if (sid.isEmpty()) throw new Exception("no session");
        try {
            JSONObject body = new JSONObject();
            body.put("chat_session_id", sid);
            body.put("parent_message_id", JSONObject.NULL);
            body.put("prompt", instruction(lang) + "\n\n---\n" + transcript);
            body.put("ref_file_ids", new JSONArray());
            body.put("thinking_enabled", false);
            body.put("search_enabled", false);
            body.put("preempt", false);
            body.put("model_type", "default");
            body.put("action", "default");
            JSONObject pow = net.powHeader("/api/v0/chat/completion");
            final StringBuilder out = new StringBuilder();
            final String[] upstream = {""};
            net.streamRequest("/api/v0/chat/completion", body, pow, new Net.Sse() {
                @Override public void onEvent(String name, JSONObject data) {}
                @Override public void onData(JSONObject d) {
                    try {
                        int code = d.optInt("code", 0);
                        if (code != 0) {
                            // інакше помилка апстріму виглядає як «порожній конспект»
                            upstream[0] = d.optString("msg", "error " + code);
                            return;
                        }
                        String p = d.optString("p", "");
                        Object v = d.opt("v");
                        String o = d.optString("o", "");
                        if (p.equals("response/fragments") && "APPEND".equals(o) && v instanceof JSONArray) {
                            JSONArray arr = (JSONArray) v;
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject f = arr.optJSONObject(i);
                                if (f == null) continue;
                                if ("RESPONSE".equals(f.optString("type"))) add(out, f.optString("content", ""));
                            }
                        } else if (p.endsWith("/content") && v instanceof String) {
                            add(out, (String) v);
                        } else if (v instanceof String && p.isEmpty()) {
                            add(out, (String) v);
                        } else if (v instanceof JSONObject && ((JSONObject) v).has("response")) {
                            JSONObject r = ((JSONObject) v).optJSONObject("response");
                            if (r == null) return;
                            JSONArray fs = r.optJSONArray("fragments");
                            if (fs == null) return;
                            for (int i = 0; i < fs.length(); i++) {
                                JSONObject f = fs.optJSONObject(i);
                                if (f == null) continue;
                                if ("RESPONSE".equals(f.optString("type"))) add(out, f.optString("content", ""));
                            }
                        }
                    } catch (Exception ignored) {}
                }
            });
            if (!upstream[0].isEmpty()) throw new Exception(upstream[0]);
            String res = out.toString().trim();
            if (res.length() > 30000) res = res.substring(0, 30000) + "\n… (обрізано)";
            return res;
        } finally {
            try { net.deleteSession(sid); } catch (Exception ignored) {}
        }
    }
}
