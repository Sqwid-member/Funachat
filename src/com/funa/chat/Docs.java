package com.funa.chat;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Генерація PDF та DOCX без зовнішніх бібліотек. */
public final class Docs {
    private Docs() {}

    public static JSONObject create(JSONObject c) throws Exception {
        String fmt = c.optString("format", "pdf").toLowerCase();
        if (fmt.equals("word") || fmt.equals("doc")) fmt = "docx";
        if (!fmt.equals("pdf") && !fmt.equals("docx")) fmt = "pdf";
        String title = c.optString("title", "").trim();
        String text = c.optString("text", "");
        if (text.isEmpty()) text = c.optString("content", "");
        // Захист від OOM: PDF/DOCX збираються в памʼяті, а текст приходить
        // від моделі. 1.5 млн символів — це вже сотні сторінок.
        if (text.length() > 1500000) {
            text = text.substring(0, 1500000) + "\n\n… (текст обрізано до 1.5 млн символів)";
        }
        if (title.isEmpty()) title = "Документ";
        String path = c.optString("path", "").trim();
        String ext = "." + fmt;
        if (path.isEmpty()) path = "docs/" + safeName(title) + ext;
        File f = Tools.resolve(path);
        String lower = f.getName().toLowerCase();
        if (!lower.endsWith(ext)) {
            String name = f.getName();
            // Модель може дати шлях із «чужим» розширенням (report.docx при
            // format:"pdf"): інакше PDF-байти лягли б у файл .docx і він
            // відкривався б помилкою. Розширення завжди відповідає формату.
            if (lower.endsWith(".pdf") || lower.endsWith(".docx")) {
                name = name.substring(0, name.length() - 5) + ext;
            } else {
                name = name + ext;
            }
            f = new File(f.getParentFile(), name);
        }
        File dir = f.getParentFile();
        if (dir != null && !dir.exists() && !dir.mkdirs())
            throw new java.io.IOException("не вдалося створити теку " + dir);
        // Пишемо в .part і лише потім перейменовуємо: збій посередині
        // (OOM на великій сторінці, помилка диска) не має лишити
        // напівзбудований PDF/DOCX, який потім «успішно» відкриється.
        // Уникальный .part: два документа с одинаковым именем не должны
        // писать в один временный файл и смешивать PDF/DOCX.
        String uniq = Long.toHexString(java.util.UUID.randomUUID().getMostSignificantBits()
                & 0xffffffffffffL);
        File tmp = new File(dir, f.getName() + "." + uniq + ".part");
        try {
            if ("docx".equals(fmt)) docx(tmp, title, text); else pdf(tmp, title, text);
            if (!tmp.renameTo(f)) {
                // rename не працює на деяких FUSE-точках: копіюємо й чистимо
                try (java.io.FileInputStream in = new java.io.FileInputStream(tmp);
                     FileOutputStream out = new FileOutputStream(f)) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                }
                if (!tmp.delete()) Log.add("docs", "part file not removed: " + tmp.getName());
            }
        } catch (Exception e) {
            if (tmp.exists() && !tmp.delete()) Log.add("docs", "part file not removed: " + tmp.getName());
            throw e;
        }
        JSONObject o = Tools.ok();
        o.put("path", f.getAbsolutePath());
        o.put("size", f.length());
        o.put("format", fmt);
        o.put("text", "Створено " + fmt.toUpperCase() + ": " + f.getAbsolutePath() + " (" + f.length() + " Б)");
        return o;
    }

    private static String safeName(String s) {
        String r = s.replaceAll("[\\\\/:*?\"<>|\\r\\n]", "_").trim();
        if (r.length() > 50) r = r.substring(0, 50);
        return r.isEmpty() ? "document" : r;
    }

    private static void pdf(File f, String title, String text) throws Exception {
        PdfDocument doc = new PdfDocument();
        PdfDocument.Page page = null;
        try {
        Paint body = new Paint(Paint.ANTI_ALIAS_FLAG);
        body.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        body.setTextSize(11.5f);
        body.setColor(Color.rgb(24, 24, 24));
        Paint head = new Paint(Paint.ANTI_ALIAS_FLAG);
        head.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        head.setTextSize(18f);
        head.setColor(Color.rgb(10, 10, 10));
        Paint sub = new Paint(Paint.ANTI_ALIAS_FLAG);
        sub.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        sub.setTextSize(13.5f);
        sub.setColor(Color.rgb(20, 20, 20));

        final float W = 595f, H = 842f, M = 46f;
        float y = M + 18f;
        int pageNo = 1;
        page = doc.startPage(new PdfDocument.PageInfo.Builder((int) W, (int) H, pageNo).create());
        Canvas cv = page.getCanvas();
        cv.drawColor(Color.WHITE);

        List<String> lines = wrap(title, head, W - M * 2);
        for (String ln : lines) { cv.drawText(ln, M, y, head); y += head.getTextSize() * 1.35f; }
        y += 8f;

        String[] paras = text.replace("\r", "").split("\n", -1);
        int pageCap = 250;
        outer:
        for (String para : paras) {
            String raw = para.trim();
            Paint font = body;
            float gap = 5f;
            String out = raw;
            if (raw.startsWith("#")) {
                int n = 0;
                while (n < raw.length() && raw.charAt(n) == '#') n++;
                out = raw.substring(Math.min(n, raw.length())).trim();
                font = sub;
                gap = 7f;
                y += 4f;
            } else if (raw.startsWith("- ") || raw.startsWith("* ")) {
                out = "•  " + raw.substring(2).trim();
            }
            out = out.replace("**", "").replace("__", "").replace("`", "");
            if (out.isEmpty()) { y += 8f; continue; }
            List<String> ls = wrap(out, font, W - M * 2);
            for (String ln : ls) {
                if (y > H - M) {
                    if (pageNo >= pageCap) {
                        // 1.5 млн символів — це тисячі сторінок; телефон не
                        // повинен будувати PDF на 50 МБ і грітися хвилинами.
                        // Пишемо примітку на чистій сторінці, а не за краєм.
                        doc.finishPage(page);
                        pageNo++;
                        page = doc.startPage(new PdfDocument.PageInfo.Builder((int) W, (int) H, pageNo).create());
                        cv = page.getCanvas();
                        cv.drawColor(Color.WHITE);
                        cv.drawText("… (документ обрізано: " + pageCap + " стор.)", M, M + 6f, body);
                        break outer;
                    }
                    doc.finishPage(page);
                    pageNo++;
                    page = doc.startPage(new PdfDocument.PageInfo.Builder((int) W, (int) H, pageNo).create());
                    cv = page.getCanvas();
                    cv.drawColor(Color.WHITE);
                    y = M + 6f;
                }
                cv.drawText(ln, M, y, font);
                y += font.getTextSize() * 1.42f;
            }
            y += gap;
        }
        doc.finishPage(page);
        page = null;
        try (FileOutputStream out = new FileOutputStream(f)) { doc.writeTo(out); }
        } finally {
            // Навіть якщо сторінка обірвалась на півдорозі (OOM, збій
            // Canvas), закриваємо документ: інакше PdfDocument тримає
            // нативні ресурси до збирання сміття.
            if (page != null) {
                try { doc.finishPage(page); } catch (Exception ignored) {}
            }
            doc.close();
        }
    }

    private static List<String> wrap(String text, Paint p, float max) {
        List<String> out = new ArrayList<>();
        if (text == null) return out;
        for (String part : text.split("\n", -1)) {
            String[] words = splitWords(part);
            StringBuilder line = new StringBuilder();
            for (String w : words) {
                String cand = line.length() == 0 ? w : line + " " + w;
                if (p.measureText(cand) <= max) {
                    line.setLength(0);
                    line.append(cand);
                } else {
                    if (line.length() > 0) { out.add(line.toString()); line.setLength(0); }
                    if (p.measureText(w) <= max) {
                        line.append(w);
                    } else {
                        StringBuilder chunk = new StringBuilder();
                        for (int i = 0; i < w.length(); i++) {
                            chunk.append(w.charAt(i));
                            if (p.measureText(chunk.toString()) > max) {
                                chunk.setLength(chunk.length() - 1);
                                if (chunk.length() > 0) out.add(chunk.toString());
                                chunk.setLength(0);
                                chunk.append(w.charAt(i));
                            }
                        }
                        line.append(chunk);
                    }
                }
            }
            out.add(line.toString());
        }
        return out;
    }

    private static void docx(File f, String title, String text) throws Exception {
        StringBuilder body = new StringBuilder();
        body.append("<w:p><w:pPr><w:jc w:val=\"center\"/><w:spacing w:after=\"240\"/></w:pPr>")
                .append("<w:r><w:rPr><w:b/><w:sz w:val=\"40\"/></w:rPr><w:t xml:space=\"preserve\">")
                .append(xml(title)).append("</w:t></w:r></w:p>");
        for (String para : text.replace("\r", "").split("\n", -1)) {
            String raw = para.trim();
            boolean head = raw.startsWith("#");
            if (head) {
                int n = 0;
                while (n < raw.length() && raw.charAt(n) == '#') n++;
                raw = raw.substring(Math.min(n, raw.length())).trim();
            }
            if (raw.startsWith("- ") || raw.startsWith("* ")) raw = "•  " + raw.substring(2).trim();
            raw = raw.replace("**", "").replace("__", "").replace("`", "");
            body.append("<w:p>");
            if (head) body.append("<w:pPr><w:spacing w:before=\"200\" w:after=\"80\"/></w:pPr>");
            body.append("<w:r><w:rPr>");
            if (head) body.append("<w:b/>");
            body.append("<w:sz w:val=\"").append(head ? "30" : "24").append("\"/>")
                    .append("<w:szCs w:val=\"").append(head ? "30" : "24").append("\"/></w:rPr>");
            body.append("<w:t xml:space=\"preserve\">").append(xml(raw)).append("</w:t></w:r></w:p>");
        }
        String doc = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">"
                + "<w:body>" + body
                + "<w:sectPr><w:pgSz w:w=\"11906\" w:h=\"16838\"/>"
                + "<w:pgMar w:top=\"1134\" w:right=\"1134\" w:bottom=\"1134\" w:left=\"1134\"/></w:sectPr>"
                + "</w:body></w:document>";
        String ct = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                + "<Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>"
                + "</Types>";
        String rels = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/>"
                + "</Relationships>";
        try (ZipOutputStream z = new ZipOutputStream(new FileOutputStream(f))) {
            put(z, "[Content_Types].xml", ct);
            put(z, "_rels/.rels", rels);
            put(z, "word/document.xml", doc);
        }
    }

    private static void put(ZipOutputStream z, String name, String content) throws Exception {
        z.putNextEntry(new ZipEntry(name));
        z.write(content.getBytes(StandardCharsets.UTF_8));
        z.closeEntry();
    }

    /**
     * Слова, але задовге «слово» (base64, URL, суцільний текст без пробілів)
     * ріжемо на шматки. Інакше посимвольний підбір ширини в wrap() стає
     * квадратичним: один рядок на 1.5 млн символів «вішає» телефон.
     */
    private static String[] splitWords(String part) {
        String[] raw = part.split(" ");
        List<String> out = new ArrayList<>(raw.length);
        for (String w : raw) {
            if (w.length() <= 4096) { out.add(w); continue; }
            for (int i = 0; i < w.length(); i += 512) {
                out.add(w.substring(i, Math.min(w.length(), i + 512)));
            }
        }
        return out.toArray(new String[0]);
    }

    private static String xml(String s) {
        // XML 1.0 забороняє керівні символи — інакше Word не відкриє файл.
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isHighSurrogate(c)) {
                // пара сурогатів — один символ; одинак XML не приймає
                if (i + 1 < s.length() && Character.isLowSurrogate(s.charAt(i + 1))) {
                    b.append(c).append(s.charAt(i + 1));
                    i++;
                }
                continue;
            }
            if (Character.isLowSurrogate(c)) continue;
            if (c == '\t' || c == '\n' || c == '\r'
                    || (c >= 0x20 && c != 0xFFFE && c != 0xFFFF)) b.append(c);
        }
        return b.toString().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("\t", " ");
    }
}
