package com.funa.chat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

/**
 * Базові детерміновані інструменти агента: час, калькулятор, одиниці,
 * випадкові значення, base64, хеші. Без Android і без мережі — тому
 * їх можна перевіряти на JVM.
 *
 * Контракт як у Tools: {"ok":true,"text":"..."} або {"ok":false,"error":"..."}.
 */
public final class Builtin {
    private Builtin() {}

    public static final String[] NAMES = {"time", "calc", "convert", "random", "base64", "hash", "skill"};

    private static final DateTimeFormatter HUMAN =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss (EEE)", Locale.ENGLISH);

    public static JSONObject exec(JSONObject call) {
        String tool = call.optString("tool", "").toLowerCase(Locale.US);
        try {
            switch (tool) {
                case "time": return time(call);
                case "calc": return calc(call);
                case "convert": return convert(call);
                case "random": return random(call);
                case "base64": return b64(call);
                case "hash": return hash(call);
                case "skill": return skill(call);
                default: return err("unknown builtin: " + tool);
            }
        } catch (Exception e) {
            String m = e.getMessage();
            return err(m == null || m.isEmpty() ? e.getClass().getSimpleName() : m);
        }
    }

    public static boolean isBuiltin(String name) {
        if (name == null) return false;
        String n = name.toLowerCase(Locale.US);
        for (String s : NAMES) if (s.equals(n)) return true;
        return false;
    }

    static JSONObject err(String m) {
        JSONObject o = new JSONObject();
        try { o.put("ok", false); o.put("error", m); } catch (Exception ignored) {}
        return o;
    }

    static JSONObject ok(String text) {
        JSONObject o = new JSONObject();
        try { o.put("ok", true); o.put("text", text); } catch (Exception ignored) {}
        return o;
    }

    /* ------------------------------- time ------------------------------ */

    static JSONObject time(JSONObject c) throws Exception {
        String tz = c.optString("tz", "").trim();
        ZoneId zone;
        if (tz.isEmpty()) zone = ZoneId.systemDefault();
        else {
            try { zone = ZoneId.of(tz); }
            catch (Exception e) { return err("невідомий часовий пояс: " + tz); }
        }
        long offsetDays = c.optLong("offset_days", 0);
        long delta = c.optLong("offset", 0); // секунди
        // без меж множення переповнюється й дата стає випадковою
        if (offsetDays > 365000L) offsetDays = 365000L;
        if (offsetDays < -365000L) offsetDays = -365000L;
        if (delta > 1000000000L) delta = 1000000000L;
        if (delta < -1000000000L) delta = -1000000000L;
        Instant at = Instant.now().plusSeconds(offsetDays * 86400L + delta);
        ZonedDateTime z = at.atZone(zone);

        StringBuilder sb = new StringBuilder();
        sb.append(z.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)).append('\n');
        sb.append(z.format(HUMAN)).append('\n');
        sb.append("zone: ").append(z.getZone()).append("\noffset: ").append(z.getOffset())
                .append("\nepoch: ").append(z.toEpochSecond());

        JSONObject o = ok(sb.toString());
        try {
            o.put("iso", z.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            o.put("epoch", z.toEpochSecond());
            o.put("tz", z.getZone().toString());
            o.put("offset", z.getOffset().toString());
        } catch (Exception ignored) {}
        return o;
    }

    /* ------------------------------- calc ------------------------------ */

    static JSONObject calc(JSONObject c) throws Exception {
        String expr = c.optString("expr", c.optString("expression", "")).trim();
        if (expr.isEmpty()) return err("порожній вираз");
        if (expr.length() > 500) return err("вираз задовгий (макс. 500)");
        boolean deg = c.optBoolean("deg", false);
        double v = new Parser(expr, deg).parse();
        if (Double.isNaN(v)) return err("результат NaN");
        if (Double.isInfinite(v)) return err("ділення на нуль або переповнення");
        String shown = fmt(v);
        JSONObject o = ok(expr + " = " + shown);
        try {
            o.put("expr", expr);
            o.put("value", v);
        } catch (Exception ignored) {}
        return o;
    }

    static String fmt(double v) {
        if (v == Math.rint(v) && Math.abs(v) < 1e15) return String.valueOf((long) v);
        String s = String.format(Locale.US, "%.12g", v);
        if (s.contains(".") && !s.contains("e") && !s.contains("E")) {
            while (s.endsWith("0")) s = s.substring(0, s.length() - 1);
            if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    /** Рекурсивний спуск: + - * / % ^, дужки, функції, константи. */
    static final class Parser {
        private final String s;
        private final boolean deg;
        private int i;

        Parser(String s, boolean deg) {
            this.s = s;
            this.deg = deg;
        }

        double parse() throws Exception {
            double v = expr();
            skip();
            if (i < s.length()) throw new Exception("зайвий символ: " + s.substring(i));
            return v;
        }

        private void skip() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        }

        private double expr() throws Exception {
            double v = term();
            while (true) {
                skip();
                if (i >= s.length()) return v;
                char ch = s.charAt(i);
                if (ch == '+') { i++; v += term(); }
                else if (ch == '-') { i++; v -= term(); }
                else return v;
            }
        }

        private double term() throws Exception {
            double v = unary();
            while (true) {
                skip();
                if (i >= s.length()) return v;
                char ch = s.charAt(i);
                if (ch == '*') { i++; v *= unary(); }
                else if (ch == '/') { i++; v /= unary(); }
                else if (ch == '%') { i++; v %= unary(); }
                else return v;
            }
        }

        private double unary() throws Exception {
            skip();
            if (i < s.length() && s.charAt(i) == '-') { i++; return -unary(); }
            if (i < s.length() && s.charAt(i) == '+') { i++; return unary(); }
            double base = primary();
            skip();
            if (i < s.length() && s.charAt(i) == '^') { i++; return Math.pow(base, unary()); }
            return base;
        }

        private double primary() throws Exception {
            skip();
            if (i >= s.length()) throw new Exception("неочікуваний кінець виразу");
            char ch = s.charAt(i);
            if (ch == '(') {
                i++;
                double v = expr();
                skip();
                if (i >= s.length() || s.charAt(i) != ')') throw new Exception("немає ')'");
                i++;
                return v;
            }
            if (Character.isDigit(ch) || ch == '.') return number();
            if (Character.isLetter(ch) || ch == '_') return ident();
            throw new Exception("неочікуваний символ: " + ch);
        }

        private double number() {
            int start = i;
            while (i < s.length() && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.')) i++;
            if (i < s.length() && (s.charAt(i) == 'e' || s.charAt(i) == 'E')) {
                int save = i;
                i++;
                if (i < s.length() && (s.charAt(i) == '+' || s.charAt(i) == '-')) i++;
                if (i < s.length() && Character.isDigit(s.charAt(i))) {
                    while (i < s.length() && Character.isDigit(s.charAt(i))) i++;
                } else i = save;
            }
            return Double.parseDouble(s.substring(start, i));
        }

        private double ident() throws Exception {
            int start = i;
            while (i < s.length() && (Character.isLetterOrDigit(s.charAt(i)) || s.charAt(i) == '_')) i++;
            String name = s.substring(start, i).toLowerCase(Locale.US);
            skip();
            boolean call = i < s.length() && s.charAt(i) == '(';
            if (!call) {
                if ("pi".equals(name)) return Math.PI;
                if ("e".equals(name)) return Math.E;
                if ("tau".equals(name)) return Math.PI * 2;
                throw new Exception("невідома константа: " + name);
            }
            i++;
            java.util.List<Double> args = new java.util.ArrayList<>();
            skip();
            if (i < s.length() && s.charAt(i) == ')') {
                i++;
            } else {
                while (true) {
                    args.add(expr());
                    skip();
                    if (i >= s.length()) throw new Exception("немає ')' у " + name);
                    if (s.charAt(i) == ',') { i++; continue; }
                    if (s.charAt(i) == ')') { i++; break; }
                    throw new Exception("очікувався ',' або ')': " + s.charAt(i));
                }
            }
            double a = args.isEmpty() ? 0 : args.get(0);
            double b = args.size() > 1 ? args.get(1) : 0;
            double angle = deg ? Math.toRadians(a) : a;
            switch (name) {
                case "abs": return Math.abs(a);
                case "sqrt": return Math.sqrt(a);
                case "cbrt": return Math.cbrt(a);
                case "round": return Math.signum(a) * Math.floor(Math.abs(a) + 0.5);
                case "floor": return Math.floor(a);
                case "ceil": return Math.ceil(a);
                case "trunc": return (double) (long) a;
                case "sign": return Math.signum(a);
                case "exp": return Math.exp(a);
                case "ln": return Math.log(a);
                case "log": return Math.log10(a);
                case "log2": return Math.log(a) / Math.log(2);
                case "sin": return Math.sin(angle);
                case "cos": return Math.cos(angle);
                case "tan": return Math.tan(angle);
                case "asin": return Math.asin(a);
                case "acos": return Math.acos(a);
                case "atan": return Math.atan(a);
                case "atan2": return Math.atan2(a, b);
                case "min": return Math.min(a, b);
                case "max": return Math.max(a, b);
                case "pow": return Math.pow(a, b);
                case "hypot": return Math.hypot(a, b);
                default: throw new Exception("невідома функція: " + name);
            }
        }
    }

    /* ------------------------------ convert ---------------------------- */

    private static final String[][] LENGTH = {
            {"mm", "0.001"}, {"cm", "0.01"}, {"m", "1"}, {"km", "1000"},
            {"in", "0.0254"}, {"ft", "0.3048"}, {"yd", "0.9144"}, {"mi", "1609.344"},
            {"nmi", "1852"},
    };
    private static final String[][] MASS = {
            {"mg", "0.001"}, {"g", "1"}, {"kg", "1000"}, {"t", "1000000"},
            {"oz", "28.349523125"}, {"lb", "453.59237"},
    };
    private static final String[][] DATA = {
            {"b", "1"}, {"kb", "1024"}, {"mb", "1048576"}, {"gb", "1073741824"},
            {"tb", "1099511627776"}, {"bit", "0.125"},
    };
    private static final String[][] SPEED = {
            {"mps", "1"}, {"kmh", "0.2777777777777778"}, {"mph", "0.44704"},
            {"knot", "0.5144444444444445"},
    };
    private static final String[][] VOLUME = {
            {"ml", "0.001"}, {"l", "1"}, {"m3", "1000"},
            {"gal", "3.785411784"}, {"pt", "0.473176473"},
    };

    static JSONObject convert(JSONObject c) throws Exception {
        double value = c.optDouble("value", Double.NaN);
        String from = norm(c.optString("from", ""));
        String to = norm(c.optString("to", ""));
        if (Double.isNaN(value)) return err("потрібне числове value");
        if (from.isEmpty() || to.isEmpty()) return err("потрібні from і to");

        Double out = null;
        if (isTemp(from) || isTemp(to)) {
            Double k = toKelvin(value, from);
            if (k != null) out = fromKelvin(k, to);
        } else {
            String[][] table = tableFor(from, to);
            if (table != null) {
                Double a = factor(table, from);
                Double b = factor(table, to);
                if (a != null && b != null) out = value * a / b;
            }
        }
        if (out == null) return err("несумісні або невідомі одиниці: " + from + " -> " + to);
        String text = fmt(out) + " " + to;
        if (isTemp(from) || isTemp(to)) text = fmt(out) + " " + to;
        JSONObject o = ok(fmt(value) + " " + from + " = " + text);
        try {
            o.put("value", out);
            o.put("unit", to);
        } catch (Exception ignored) {}
        return o;
    }

    private static String norm(String u) {
        if (u == null) return "";
        u = u.trim().toLowerCase(Locale.US).replace("°", "").replace(" ", "");
        if (u.equals("с") || u.equals("ц")) u = "c";
        if (u.equals("км")) u = "km";
        if (u.equals("м")) u = "m";
        if (u.equals("см")) u = "cm";
        if (u.equals("мм")) u = "mm";
        if (u.equals("кг")) u = "kg";
        if (u.equals("г")) u = "g";
        if (u.equals("мг")) u = "mg";
        if (u.equals("ф") || u.equals("fahrenheit")) u = "f";
        return u;
    }

    private static boolean isTemp(String u) {
        return "c".equals(u) || "f".equals(u) || "k".equals(u);
    }

    private static Double toKelvin(double v, String u) {
        if ("c".equals(u)) return v + 273.15;
        if ("f".equals(u)) return (v - 32) * 5 / 9 + 273.15;
        if ("k".equals(u)) return v;
        return null;
    }

    private static Double fromKelvin(double k, String u) {
        if ("c".equals(u)) return k - 273.15;
        if ("f".equals(u)) return (k - 273.15) * 9 / 5 + 32;
        if ("k".equals(u)) return k;
        return null;
    }

    private static String[][] tableFor(String a, String b) {
        String[][][] all = {LENGTH, MASS, DATA, SPEED, VOLUME};
        for (String[][] t : all) if (factor(t, a) != null && factor(t, b) != null) return t;
        return null;
    }

    private static Double factor(String[][] table, String unit) {
        for (String[] row : table) if (row[0].equals(unit)) return Double.parseDouble(row[1]);
        return null;
    }

    /* ------------------------------ random ----------------------------- */

    static JSONObject random(JSONObject c) throws Exception {
        String kind = c.optString("kind", c.optString("type", "int")).toLowerCase(Locale.US);
        Random r = new Random();
        if ("uuid".equals(kind) || "guid".equals(kind)) {
            String u = UUID.randomUUID().toString();
            JSONObject o = ok(u);
            try { o.put("uuid", u); } catch (Exception ignored) {}
            return o;
        }
        if ("choice".equals(kind) || "pick".equals(kind)) {
            JSONArray items = c.optJSONArray("items");
            if (items == null || items.length() == 0) return err("потрібен непорожній items");
            int i = r.nextInt(items.length());
            return ok(String.valueOf(items.opt(i)));
        }
        if ("float".equals(kind)) {
            double min = c.optDouble("min", 0);
            double max = c.optDouble("max", 1);
            if (max <= min) return err("max має бути більше min");
            double v = min + r.nextDouble() * (max - min);
            return ok(fmt(v));
        }
        if ("bytes".equals(kind) || "hex".equals(kind)) {
            int n = Math.max(1, Math.min(256, c.optInt("count", 16)));
            byte[] b = new byte[n];
            r.nextBytes(b);
            StringBuilder sb = new StringBuilder();
            for (byte x : b) sb.append(String.format(Locale.US, "%02x", x));
            return ok(sb.toString());
        }
        long min = c.optLong("min", 0);
        long max = c.optLong("max", 100);
        if (max <= min) return err("max має бути більше min");
        // max - min + 1 переповнюється на межах long
        if (max - min < 0) return err("діапазон завеликий");
        long v = min + (long) (r.nextDouble() * (max - min + 1));
        if (v > max) v = max;
        JSONObject o = ok(String.valueOf(v));
        try { o.put("value", v); } catch (Exception ignored) {}
        return o;
    }

    /* ---------------------------- base64/hash -------------------------- */

    static JSONObject b64(JSONObject c) throws Exception {
        String op = c.optString("op", "encode").toLowerCase(Locale.US);
        String text = c.optString("text", "");
        // base64 роздуває дані на третину: 200 КБ вхідних — це вже 270 КБ
        // у відповіді, яку все одно обріже агент.
        if (text.length() > 200000) return err("текст задовгий (макс. 200000 символів)");
        if ("decode".equals(op)) {
            try {
                String out = new String(Base64.getDecoder().decode(text.trim()), StandardCharsets.UTF_8);
                return ok(out);
            } catch (Exception e) {
                return err("некоректний base64");
            }
        }
        String out = Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
        return ok(out);
    }

    static JSONObject skill(JSONObject c) throws Exception {
        String raw = c.optString("name", "").trim();
        String n = Skills.norm(raw);
        String text = Skills.get(n, c.optString("lang", "uk"));
        if (text == null) {
            return err("unknown skill: " + (raw.isEmpty() ? "(empty)" : raw)
                    + "; available: " + Skills.names());
        }
        return ok(text);
    }

    static JSONObject hash(JSONObject c) throws Exception {
        String algo = c.optString("algo", "sha256").toLowerCase(Locale.US).replace("-", "");
        String jdk;
        if ("md5".equals(algo)) jdk = "MD5";
        else if ("sha1".equals(algo)) jdk = "SHA-1";
        else if ("sha256".equals(algo)) jdk = "SHA-256";
        else if ("sha512".equals(algo)) jdk = "SHA-512";
        else return err("алгоритм: md5, sha1, sha256, sha512");
        String text = c.optString("text", "");
        if (text.length() > 1000000) return err("текст задовгий (макс. 1 МБ)");
        byte[] d = MessageDigest.getInstance(jdk)
                .digest(text.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(d.length * 2);
        for (byte x : d) sb.append(String.format(Locale.US, "%02x", x));
        return ok(sb.toString());
    }
}
