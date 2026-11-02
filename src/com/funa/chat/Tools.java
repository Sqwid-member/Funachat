package com.funa.chat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

public class Tools {
    public static final String ROOT = "/sdcard/Download/FunaFiles";
    public static final String QUEUE = "/sdcard/Download/FunaQueue";
    private static final int MAX_READ = 200000;
    private static final int MAX_OUT = 24000;

    public static JSONObject exec(JSONObject call) {
        String tool = call.optString("tool", "").toLowerCase(Locale.US);
        try {
            if (Builtin.isBuiltin(tool)) return Builtin.exec(call);
            switch (tool) {
                case "read": return read(call);
                case "write": return write(call, false);
                case "append": return write(call, true);
                case "edit": return edit(call);
                case "list": case "ls": return list(call);
                case "sh": case "bash": case "termux": return sh(call);
                case "doc": case "document": return Docs.create(call);
                default: return err("невідомий інструмент: " + tool);
            }
        } catch (Exception e) {
            return err(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    static JSONObject err(String m) {
        JSONObject o = new JSONObject();
        try { o.put("ok", false); o.put("error", m); } catch (Exception ignored) {}
        return o;
    }

    static JSONObject ok() {
        JSONObject o = new JSONObject();
        try { o.put("ok", true); } catch (Exception ignored) {}
        return o;
    }

    /**
     * Файл у межах дозволених тек. Шлях нормалізується через getCanonicalFile:
     * інакше `..` або симлінк виводили за /sdcard — аж до приватних даних
     * застосунку (shared_prefs, база памʼяті).
     */
    static File resolve(String path) throws IOException {
        if (path == null) path = "";
        path = path.trim();
        if (path.isEmpty()) path = ".";
        if (path.startsWith("~")) path = path.substring(1);
        if (path.startsWith("/storage/emulated/0/")) path = "/sdcard/" + path.substring("/storage/emulated/0/".length());
        File f;
        boolean sd;
        if (path.equals("/sdcard") || path.startsWith("/sdcard/")) { f = new File(path); sd = true; }
        else if (path.startsWith("/")) throw new IOException("доступ лише до /sdcard (або відносний шлях)");
        else { f = new File(ROOT, path); sd = false; }
        File canon = f.getCanonicalFile();
        File base = new File(sd ? "/sdcard" : ROOT).getCanonicalFile();
        String cp = canon.getPath(), bp = base.getPath();
        if (!cp.equals(bp) && !cp.startsWith(bp + File.separator))
            throw new IOException("шлях поза " + (sd ? "/sdcard" : ROOT));
        // повертаємо початковий шлях: у UI лишається звичний /sdcard/…
        return f;
    }

    static JSONObject read(JSONObject c) throws Exception {
        File f = resolve(c.optString("path"));
        if (!f.exists()) return err("файл не знайдено: " + f.getAbsolutePath());
        if (f.isDirectory()) return list(c);
        long size = f.length();
        byte[] data = new byte[(int) Math.min(size, MAX_READ * 4L)];
        try (FileInputStream in = new FileInputStream(f)) {
            int off = 0;
            while (off < data.length) {
                int n = in.read(data, off, data.length - off);
                if (n <= 0) break;
                off += n;
            }
            if (off != data.length) data = Arrays.copyOf(data, off);
        }
        String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(data)).toString();
        } catch (CharacterCodingException e) {
            StringBuilder hex = new StringBuilder();
            int n = Math.min(data.length, 4096);
            for (int i = 0; i < n; i++) {
                hex.append(String.format("%02x", data[i]));
                if (i % 32 == 31) hex.append('\n'); else if (i % 2 == 1) hex.append(' ');
            }
            JSONObject o = ok();
            o.put("binary", true);
            o.put("size", size);
            o.put("text", "Бінарний файл (" + size + " Б). Hex-початок:\n" + hex);
            return o;
        }
        String[] lines = text.split("\n", -1);
        int offset = Math.max(1, c.optInt("offset", 1));
        if (offset > lines.length + 1) offset = lines.length + 1;
        int limit = c.optInt("limit", 2000);
        if (limit <= 0 || limit > 20000) limit = 20000;
        StringBuilder sb = new StringBuilder();
        int end = Math.min(lines.length, offset - 1 + limit);
        for (int i = offset - 1; i < end; i++) {
            sb.append(i + 1).append("│ ").append(lines[i]).append('\n');
        }
        String body = sb.toString();
        if (body.length() > MAX_READ) body = body.substring(0, MAX_READ) + "\n… (обрізано)";
        JSONObject o = ok();
        o.put("path", f.getAbsolutePath());
        o.put("size", size);
        o.put("lines", lines.length);
        o.put("from", offset);
        o.put("to", end);
        o.put("text", body);
        return o;
    }

    static JSONObject write(JSONObject c, boolean append) throws Exception {
        File f = resolve(c.optString("path"));
        String content = c.optString("content", "");
        if (content.length() > 64L * 1024 * 1024) return err("вміст завеликий (макс. 64 МБ)");
        File p = f.getParentFile();
        if (p != null && !p.exists() && !p.mkdirs()) return err("не вдалось створити папку " + p);
        byte[] data = content.getBytes(StandardCharsets.UTF_8);
        if (append) {
            try (FileOutputStream out = new FileOutputStream(f, true)) { out.write(data); }
        } else {
            // повний перезапис — атомарно; дописування лишаємо прямим,
            // бо перечитувати 64 МБ заради atomic append — це OOM.
            writeAtomic(f, data);
        }
        JSONObject o = ok();
        o.put("path", f.getAbsolutePath());
        o.put("bytes", f.length());
        o.put("text", (append ? "Дописано" : "Записано") + ": " + f.getAbsolutePath() + " (" + f.length() + " Б)");
        return o;
    }

    static JSONObject edit(JSONObject c) throws Exception {
        File f = resolve(c.optString("path"));
        if (!f.exists()) return err("файл не знайдено: " + f.getAbsolutePath());
        // точкова заміна має сенс лише для тексту; цілий фільм у памʼять не тягнемо
        if (f.length() > 8L * 1024 * 1024) return err("файл завеликий для edit (макс. 8 МБ)");
        String text = new String(java.nio.file.Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        String find = c.optString("find", "");
        if (find.isEmpty()) return err("порожній find");
        int idx = text.indexOf(find);
        if (idx < 0) return err("фрагмент не знайдено");
        String replace = c.optString("replace", "");
        String out = text.substring(0, idx) + replace + text.substring(idx + find.length());
        // Через .part + rename: збій посеред запису не має лишити
        // напіввідредагований файл, який потім виглядає цілим.
        writeAtomic(f, out.getBytes(StandardCharsets.UTF_8));
        JSONObject o = ok();
        o.put("path", f.getAbsolutePath());
        o.put("text", "Замінено 1 входження у " + f.getAbsolutePath());
        return o;
    }

    static JSONObject list(JSONObject c) throws Exception {
        File f = resolve(c.optString("path", "."));
        if (!f.exists()) return err("не знайдено: " + f.getAbsolutePath());
        if (!f.isDirectory()) return err("це не папка: " + f.getAbsolutePath());
        File[] items = f.listFiles();
        if (items == null) return err("немає доступу до " + f.getAbsolutePath());
        Arrays.sort(items, (a, b) -> {
            if (a.isDirectory() != b.isDirectory()) return a.isDirectory() ? -1 : 1;
            return a.getName().compareToIgnoreCase(b.getName());
        });
        StringBuilder sb = new StringBuilder();
        JSONArray arr = new JSONArray();
        sb.append(f.getAbsolutePath()).append('\n');
        int count = 0;
        for (File x : items) {
            if (count++ >= 500) { sb.append("… (більше 500)\n"); break; }
            String kind = x.isDirectory() ? "d" : "f";
            sb.append(kind).append(' ').append(x.getName());
            if (!x.isDirectory()) sb.append("  ").append(human(x.length()));
            sb.append('\n');
            JSONObject it = new JSONObject();
            it.put("name", x.getName());
            it.put("dir", x.isDirectory());
            it.put("size", x.length());
            arr.put(it);
        }
        JSONObject o = ok();
        o.put("path", f.getAbsolutePath());
        o.put("items", arr);
        o.put("text", sb.toString());
        return o;
    }

    /**
     * Атомарний запис: спершу `<name>.part`, потім rename. Так експорт,
     * що впав посередині (мало місця, OOM), не лишає обрізаний файл,
     * який потім виглядає як успішний результат.
     */
    static void writeAtomic(File f, byte[] data) throws java.io.IOException {
        File dir = f.getParentFile();
        if (dir != null && !dir.exists() && !dir.mkdirs())
            throw new java.io.IOException("не вдалося створити теку " + dir);
        // Имя временного файла уникально: два параллельных сохранения в один
        // файл (экспорт в ту же минуту, правка из двух инструментов) иначе
        // писали бы в один .part и могли смешать содержимое.
        String uniq = Long.toHexString(java.util.UUID.randomUUID().getMostSignificantBits()
                & 0xffffffffffffL);
        File tmp = new File(dir, f.getName() + "." + uniq + ".part");
        try {
            try (FileOutputStream out = new FileOutputStream(tmp)) {
                out.write(data);
                out.flush();
                try { out.getFD().sync(); } catch (Exception ignored) {}
            }
            if (f.exists() && !f.delete())
                throw new java.io.IOException("не вдалося замінити " + f.getName());
            if (!tmp.renameTo(f)) {
                try (java.io.FileInputStream in = new java.io.FileInputStream(tmp);
                     FileOutputStream out = new FileOutputStream(f)) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                }
                if (!tmp.delete()) Log.add("tools", "part file not removed: " + tmp.getName());
            }
        } catch (java.io.IOException e) {
            if (tmp.exists() && !tmp.delete()) Log.add("tools", "part file not removed: " + tmp.getName());
            throw e;
        } catch (RuntimeException e) {
            if (tmp.exists() && !tmp.delete()) Log.add("tools", "part file not removed: " + tmp.getName());
            throw e;
        }
    }

    static String human(long n) {
        if (n < 1024) return n + " Б";
        if (n < 1024 * 1024) return String.format(Locale.US, "%.1f КБ", n / 1024.0);
        return String.format(Locale.US, "%.1f МБ", n / 1024.0 / 1024.0);
    }

    /** Канал зайнятий цим процесом — щоб два паралельні sh не взяли один файл. */
    private static final Object QUEUE_LOCK = new Object();
    private static final Set<String> BUSY = new HashSet<>();

    static JSONObject sh(JSONObject c) throws Exception {
        String cmd = c.optString("cmd", "").trim();
        if (cmd.isEmpty()) return err("порожня команда");
        // команда летить у файл черги й у логи моста: 60 КБ більш ніж досить
        if (cmd.length() > 60000) return err("команда задовга (макс. 60000 символів)");
        int timeout = c.optInt("timeout", 120);
        if (timeout < 5) timeout = 5;
        if (timeout > 900) timeout = 900;
        File q = new File(QUEUE);
        if (!q.exists() && !q.mkdirs()) return err("немає доступу до " + QUEUE);
        String token = Long.toHexString(new Random().nextLong() & 0x7fffffffffffffffL);
        String[] chans = {"/funa_a.cmd.txt", "/funa_b.cmd.txt", "/funa_c.cmd.txt"};
        File target = null;
        synchronized (QUEUE_LOCK) {
            for (String ch : chans) {
                if (BUSY.contains(ch)) continue;
                File f = new File(QUEUE + ch);
                if (!f.exists() || f.length() == 0) { target = f; break; }
                String cur = readSmall(f);
                // «мрець»: міст упав або вбитий посеред команди й [[DONE]]
                // уже не зʼявиться. Такий канал інакше лишався б зайнятим
                // назавжди — після трьох таких викликів міст «мовчить».
                boolean dead = System.currentTimeMillis() - f.lastModified() > 30L * 60 * 1000;
                if (finished(cur) || dead) {
                    if (dead && !finished(cur)) Log.add("tools", "stale queue channel reclaimed: " + ch);
                    target = f; break;
                }
            }
            if (target != null) {
                BUSY.add(target.getName());
                try {
                    // #long — сумісність зі старим мостом (600 с); #limit — точний
                    // ліміт для v10+. Обидва рядки для sh є коментарями.
                    StringBuilder head = new StringBuilder("#funa:").append(token).append('\n');
                    // #limit пишем завжди: інакше для timeout<=85 міст жив
                    // бы по своєму дефолту (85 с) і команда продолжала
                    // работать уже после того, как модель получила таймаут.
                    if (timeout > 85) head.append("#long\n");
                    head.append("#limit:").append(timeout).append('\n');
                    byte[] data2 = (head + cmd + "\n").getBytes(StandardCharsets.UTF_8);
                    // Публікуємо команду атомарним rename: міст читає канал
                    // саме в цю мить, і напівзаписаний файл означав би
                    // виконання обрізаної команди. На /sdcard rename працює
                    // через FUSE; якщо ні — лишається прямий запис.
                    File tmp2 = new File(target.getParentFile(), target.getName() + ".tmp");
                    boolean published = false;
                    try {
                        try (FileOutputStream out = new FileOutputStream(tmp2, false)) {
                            out.write(data2);
                            out.flush();
                        }
                        published = tmp2.renameTo(target);
                    } catch (Exception ignored) {
                    }
                    if (!published) {
                        try (FileOutputStream out = new FileOutputStream(target, false)) {
                            out.write(data2);
                        }
                        if (tmp2.exists() && !tmp2.delete()) Log.add("tools", "tmp not removed: " + tmp2.getName());
                    }
                } catch (Exception e) {
                    BUSY.remove(target.getName());
                    throw e;
                }
            }
        }
        if (target == null) return err("усі канали термінала зайняті, спробуй пізніше");
        final String chName = target.getName();
        // +5 с: міст сам ріже команду на тому ж ліміті, і без запасу
        // ми встигали оголосити таймаут за мить до [[DONE]].
        long deadline = System.currentTimeMillis() + timeout * 1000L + 5000L;
        boolean started = false;
        // Маркер має простояти два опитування: вивід самої команди може
        // закінчуватися рядком [[DONE:0]], інакше його легко прийняти за
        // завершення ще до того, як міст дописав справжній маркер.
        boolean sawFin = false;
        boolean mineSeen = false;
        long stableLen = -1;
        // Таймаут після того, як міст уже взяв команду: канал не можна
        // віддавати наступній команді, поки міст не допише [[DONE]].
        // Інакше старий вивід прилетів би новому очікувачу як його власний.
        boolean drain = false;
        try {
            while (System.currentTimeMillis() < deadline) {
                Thread.sleep(120);
                // Читаємо лише краї файлу: повний вивід (до 500 КБ) на
                // кожному кроці — це мегабайти сміття для GC, поки модель
                // ще навіть не почала відповідати.
                String head = started ? "" : readEdge(target, 8192, true);
                String tail = readEdge(target, 8192, false);
                // чужий [[DONE]] у файлі не має завершити нашу команду
                if (!mineSeen) mineSeen = head.contains("[[ID:" + token + "]]");
                if (mineSeen && !started && head.contains("[[RUN]]")) started = true;
                // Маркер — це останній непорожній рядок файлу. Пошук підрядка
                // ловив «[[DONE:» усередині виводу команди (echo, cat логу,
                // grep) і завершував очікування достроково.
                if (mineSeen && finished(tail)) {
                    long len = target.length();
                    if (sawFin && len == stableLen) break;
                    sawFin = true;
                    stableLen = len;
                } else {
                    sawFin = false;
                }
            }
            String content = readSmall(target);
            // Маркер перевіряємо по хвосту файлу: readSmall читає лише
            // перші 600 КБ, а команда з великим виводом (cap_file ріже на
            // 500 КБ) може покласти [[DONE]] за цю межу.
            boolean done = mineSeen && sawFin && finished(readEdge(target, 8192, false));
            if (!done) {
                drain = started;
                JSONObject o = err(started ? "команда ще виконується (таймаут " + timeout + " с)"
                        : "міст термінала не відповідає. Перевір, що funa-bridge запущений у Termux або ReTerminal");
                o.put("text", strip(content));
                return o;
            }
            JSONObject o = ok();
            o.put("exit", exitCode(content));
            String body = strip(content);
            if (body.length() > MAX_OUT) body = body.substring(0, MAX_OUT) + "\n… (обрізано, " + body.length() + " символів)";
            o.put("text", body.isEmpty() ? "(без виводу)" : body);
            return o;
        } finally {
            synchronized (QUEUE_LOCK) {
                if (drain) {
                    // Канал лишається зайнятим, доки міст не завершить стару
                    // команду. Прибиральник звільнить його за [[DONE]] або
                    // через 30 хв, якщо міст помер.
                    startDrain(target, chName);
                } else {
                    BUSY.remove(chName);
                    // міст так і не взяв команду — звільняємо канал, інакше він
                    // висітиме вічно й «з'їсть» одну з трьох черг
                    if (!started) {
                        try (FileOutputStream out = new FileOutputStream(target, false)) { }
                    }
                }
            }
        }
    }

    /**
     * Чекає, поки міст допише [[DONE]] у канал, який ми вже покинули за
     * таймаутом, і лише тоді звільняє його. Без цього наступна команда
     * могла б стартувати поверх незавершеної й отримати чужий вивід.
     */
    private static void startDrain(final File target, final String chName) {
        Thread t = new Thread(() -> {
            long until = System.currentTimeMillis() + 30L * 60 * 1000;
            while (System.currentTimeMillis() < until) {
                if (!target.exists()) break;
                if (finished(readEdge(target, 8192, false))) break;
                try { Thread.sleep(700); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            synchronized (QUEUE_LOCK) {
                try (FileOutputStream out = new FileOutputStream(target, false)) { }
                catch (Exception ignored) {}
                BUSY.remove(chName);
            }
        }, "funa-queue-drain");
        t.setDaemon(true);
        t.start();
    }

    /** Початок або кінець файлу без читання всього виводу (для опитування). */
    static String readEdge(File f, int max, boolean head) {
        try (java.io.RandomAccessFile r = new java.io.RandomAccessFile(f, "r")) {
            long len = r.length();
            int n = (int) Math.min(len, max);
            if (n <= 0) return "";
            r.seek(head ? 0 : Math.max(0, len - n));
            byte[] b = new byte[n];
            int off = 0;
            while (off < n) {
                int k = r.read(b, off, n - off);
                if (k <= 0) break;
                off += k;
            }
            return new String(b, 0, off, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    static String readSmall(File f) {
        try (FileInputStream in = new FileInputStream(f)) {
            byte[] b = new byte[(int) Math.min(f.length(), 600000)];
            int off = 0;
            while (off < b.length) {
                int n = in.read(b, off, b.length - off);
                if (n <= 0) break;
                off += n;
            }
            return new String(b, 0, off, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    /** Останній непорожній рядок файлу черги. */
    static String lastLine(String s) {
        if (s == null) return "";
        int end = s.length();
        while (end > 0) {
            int start = s.lastIndexOf('\n', end - 1) + 1;
            String line = s.substring(start, end).trim();
            if (!line.isEmpty()) return line;
            end = start - 1;
        }
        return "";
    }

    /** Чи міст уже дописав маркер завершення саме в кінець файлу. */
    static boolean finished(String s) {
        return lastLine(s).matches("\\[\\[DONE:-?\\d+\\]\\]");
    }

    static int exitCode(String s) {
        String line = lastLine(s);
        if (!line.matches("\\[\\[DONE:-?\\d+\\]\\]")) return -1;
        try { return Integer.parseInt(line.substring(7, line.length() - 2).trim()); }
        catch (Exception e) { return -1; }
    }

    static String strip(String s) {
        if (s == null) return "";
        // Шапка [[ID]]/[[RUN]] стоїть на початку — прибираємо саме її, а не
        // такі самі рядки, якщо команда вивела їх у тексті.
        String tok = tokenOf(s);
        if (!tok.isEmpty()) s = s.replace("[[ID:" + tok + "]]\n", "");
        s = s.replaceFirst("(?m)^#funa:[0-9a-f]+\\r?\\n", "");
        s = s.replaceFirst("(?m)^\\s*\\[\\[RUN\\]\\]\\r?\\n?", "");
        s = s.replaceAll("(?s)\\s*\\[\\[DONE:-?\\d+\\]\\]\\s*$", "");
        return s.trim();
    }

    private static String tokenOf(String s) {
        int i = s.indexOf("[[ID:");
        if (i < 0) return "";
        int j = s.indexOf("]]", i);
        return j < 0 ? "" : s.substring(i + 5, j);
    }

    public static boolean storageOk() {
        File f = new File(ROOT);
        if (!f.exists()) f.mkdirs();
        return f.isDirectory() && f.canWrite();
    }

    public static List<String> tree(String dir, int depth) {
        List<String> out = new ArrayList<>();
        walk(new File(dir), depth, out);
        return out;
    }

    private static void walk(File f, int depth, List<String> out) {
        if (depth < 0 || out.size() > 400) return;
        File[] items = f.listFiles();
        if (items == null) return;
        Arrays.sort(items, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        for (File x : items) {
            out.add((x.isDirectory() ? "d " : "f ") + x.getAbsolutePath());
            if (x.isDirectory()) walk(x, depth - 1, out);
        }
    }
}
