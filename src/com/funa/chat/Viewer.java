package com.funa.chat;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Метадані та попередній перегляд файлів. */
public final class Viewer {
    private Viewer() {}

    public static String kind(String name) {
        String n = name == null ? "" : name.toLowerCase(Locale.US);
        if (n.matches(".*\\.(png|jpe?g|gif|webp|bmp|heic|heif)$")) return "image";
        if (n.endsWith(".pdf")) return "pdf";
        if (n.endsWith(".docx")) return "docx";
        if (n.endsWith(".doc")) return "doc";
        if (n.endsWith(".xlsx") || n.endsWith(".xls") || n.endsWith(".csv")) return "sheet";
        if (n.endsWith(".pptx") || n.endsWith(".ppt")) return "slides";
        if (n.matches(".*\\.(txt|md|log|json|xml|html?|css|js|ts|java|kt|py|c|cpp|h|sh|yml|yaml|ini|conf)$")) return "text";
        if (n.matches(".*\\.(mp3|wav|ogg|m4a|flac|aac)$")) return "audio";
        if (n.matches(".*\\.(mp4|mkv|webm|mov|avi|3gp)$")) return "video";
        if (n.matches(".*\\.(zip|rar|7z|tar|gz)$")) return "archive";
        return "other";
    }

    public static JSONObject info(File f) {
        JSONObject o = new JSONObject();
        try {
            o.put("path", f.getAbsolutePath());
            o.put("name", f.getName());
            o.put("exists", f.exists() && f.isFile());
            o.put("size", f.length());
            o.put("mime", Attach.mime(f.getName()));
            o.put("kind", kind(f.getName()));
        } catch (Exception ignored) {}
        return o;
    }

    public static JSONObject view(File f) throws Exception {
        JSONObject o = info(f);
        String k = o.optString("kind");
        if ("image".equals(k)) {
            o.put("data", Attach.imageData(f, 1800));
            return o;
        }
        if ("pdf".equals(k)) {
            o.put("pages", pdfPages(f, 0, 3));
            o.put("total", pdfCount(f));
            return o;
        }
        if ("docx".equals(k)) {
            o.put("text", docxText(f));
            return o;
        }
        if ("text".equals(k)) {
            o.put("text", text(f));
            return o;
        }
        return o;
    }

    public static JSONArray pdfPages(File f, int offset, int count) throws Exception {
        JSONArray out = new JSONArray();
        // Одна сторінка JPEG у base64 — це 1–2 МБ тексту. Пачка з восьми
        // таких сторінок не влазить у JSON-міст WebView і валить рендерер,
        // тому тримаємо і кількість, і сумарний розмір.
        final long budget = 6L * 1024 * 1024;
        long sent = 0;
        try (ParcelFileDescriptor pfd = ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
             PdfRenderer r = new PdfRenderer(pfd)) {
            int total = r.getPageCount();
            int from = Math.max(0, offset);
            int to = Math.min(total, from + Math.min(4, Math.max(1, count)));
            for (int i = from; i < to; i++) {
                try (PdfRenderer.Page p = r.openPage(i)) {
                    // Сторінка може бути екстремально витягнутою (креслення,
                    // сувій): обмежуємо обидві сторони, інакше bitmap на
                    // сотні мегабайт валить застосунок.
                    int pw = Math.max(1, p.getWidth());
                    int ph = Math.max(1, p.getHeight());
                    float k = Math.min(900f / pw, 4000f / ph);
                    int w = Math.max(1, Math.round(pw * k));
                    int h = Math.max(1, Math.round(ph * k));
                    Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                    Canvas cv = new Canvas(bm);
                    cv.drawColor(Color.WHITE);
                    p.render(bm, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                    ByteArrayOutputStream bo = new ByteArrayOutputStream();
                    bm.compress(Bitmap.CompressFormat.JPEG, 74, bo);
                    bm.recycle();
                    byte[] jpg = bo.toByteArray();
                    byte[] b64 = android.util.Base64.encode(jpg, android.util.Base64.NO_WRAP);
                    if (out.length() > 0 && sent + b64.length > budget) break;
                    sent += b64.length;
                    out.put("data:image/jpeg;base64," + new String(b64, StandardCharsets.US_ASCII));
                }
            }
        }
        return out;
    }

    public static int pdfCount(File f) {
        try (ParcelFileDescriptor pfd = ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
             PdfRenderer r = new PdfRenderer(pfd)) {
            return r.getPageCount();
        } catch (Exception e) {
            return 0;
        }
    }

    public static String docxText(File f) throws Exception {
        try (ZipFile z = new ZipFile(f)) {
            ZipEntry e = z.getEntry("word/document.xml");
            if (e == null) throw new Exception("word/document.xml not found");
            String xml;
            try (InputStream in = z.getInputStream(e)) {
                // docx на 50 МБ может распаковаться в сотни МБ XML:
                // для предпросмотра хватает 12 МБ текста, остальное
                // всё равно уйдёт в обрезку до 300k символов.
                xml = new String(readAll(in, 12 * 1024 * 1024), StandardCharsets.UTF_8);
            }
            xml = xml.replace("</w:p>", "\n")
                    .replace("<w:tab/>", "\t")
                    .replace("<w:br/>", "\n")
                    .replace("</w:tr>", "\n")
                    .replace("</w:tc>", "\t");
            xml = xml.replaceAll("<[^>]+>", "");
            xml = unescape(xml);
            xml = xml.replaceAll("[ \\t]+\\n", "\n").replaceAll("\\n{3,}", "\n\n");
            return cap(xml.trim(), 300000);
        }
    }

    public static String text(File f) throws Exception {
        try (InputStream in = new FileInputStream(f)) {
            byte[] data = readAll(in, 8 * 1024 * 1024);
            String s = new String(data, StandardCharsets.UTF_8);
            return cap(s, 300000);
        }
    }

    private static String cap(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "\n… (обрізано)";
    }

    private static byte[] readAll(InputStream in, int max) throws Exception {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[16384];
        int n, total = 0;
        while ((n = in.read(buf)) > 0) {
            bo.write(buf, 0, n);
            total += n;
            if (total > max) break;
        }
        return bo.toByteArray();
    }

    private static String unescape(String s) {
        return s.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
                .replace("&apos;", "'").replace("&amp;", "&");
    }
}
