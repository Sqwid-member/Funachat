package com.funa.chat;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.Locale;

/** Допоміжні операції з вкладеннями: імена, MIME, мініатюри. */
public final class Attach {
    private Attach() {}

    public static String sanitize(String name) {
        if (name == null || name.trim().isEmpty()) name = "file";
        String s = name.replaceAll("[\\\\/:*?\"<>|\\r\\n\\t]", "_")
                .replaceAll("[\\p{Cntrl}]", "").trim();
        // «.» й «..» — це не імʼя файлу, а посилання на теку
        s = s.replaceAll("^[.]+", "");
        if (s.isEmpty()) s = "file";
        if (s.length() > 90) {
            int dot = s.lastIndexOf('.');
            if (dot > 0 && dot > s.length() - 12) {
                s = s.substring(0, 80) + s.substring(dot);
            } else {
                s = s.substring(0, 90);
            }
        }
        return s;
    }

    public static String mime(String name) {
        String n = name == null ? "" : name.toLowerCase(Locale.US);
        String m = URLConnection.guessContentTypeFromName(n);
        if (m != null && !m.isEmpty()) return m;
        if (n.endsWith(".md") || n.endsWith(".log") || n.endsWith(".kt")
                || n.endsWith(".py") || n.endsWith(".java") || n.endsWith(".js")
                || n.endsWith(".ts") || n.endsWith(".css") || n.endsWith(".json")
                || n.endsWith(".xml") || n.endsWith(".yml") || n.endsWith(".yaml")) {
            return "text/plain";
        }
        if (n.endsWith(".heic") || n.endsWith(".heif")) return "image/heic";
        if (n.endsWith(".webp")) return "image/webp";
        if (n.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (n.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        if (n.endsWith(".pptx")) return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        if (n.endsWith(".apk")) return "application/vnd.android.package-archive";
        return "application/octet-stream";
    }

    public static String thumbFile(File f, int max) {
        if (f == null || !f.exists() || f.length() <= 0 || f.length() > 14L * 1024 * 1024) return "";
        try (InputStream in = new FileInputStream(f); ByteArrayOutputStream bo = new ByteArrayOutputStream()) {
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            return thumb(bo.toByteArray(), max);
        } catch (Exception e) {
            return "";
        }
    }

    public static String imageData(File f, int max) {
        if (f == null || !f.exists() || f.length() <= 0 || f.length() > 24L * 1024 * 1024) return "";
        try (InputStream in = new FileInputStream(f); ByteArrayOutputStream bo = new ByteArrayOutputStream()) {
            byte[] buf = new byte[32768];
            int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            return scaled(bo.toByteArray(), max, true);
        } catch (Exception e) {
            return "";
        }
    }

    public static String thumb(byte[] data, int max) {
        return scaled(data, max, false);
    }

    private static Bitmap orient(Bitmap bm, byte[] data) {
        int or = ExifInterface.ORIENTATION_NORMAL;
        try (InputStream in = new ByteArrayInputStream(data)) {
            or = new ExifInterface(in)
                    .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        } catch (Exception ignored) {}
        if (or == ExifInterface.ORIENTATION_NORMAL) return bm;
        Matrix m = new Matrix();
        switch (or) {
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL: m.setScale(-1f, 1f); break;
            case ExifInterface.ORIENTATION_ROTATE_180: m.setRotate(180f); break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL: m.setScale(1f, -1f); break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                m.setRotate(90f); m.postScale(-1f, 1f); break;
            case ExifInterface.ORIENTATION_ROTATE_90: m.setRotate(90f); break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                m.setRotate(270f); m.postScale(-1f, 1f); break;
            case ExifInterface.ORIENTATION_ROTATE_270: m.setRotate(270f); break;
            default: return bm;
        }
        try {
            Bitmap out = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), m, true);
            if (out != bm) bm.recycle();
            return out;
        } catch (Throwable e) {
            // OOM/некоректний EXIF: краще неповернута, ніж жодної
            return bm;
        }
    }

    private static String scaled(byte[] data, int max, boolean png) {
        if (data == null || data.length == 0) return "";
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, o);
            if (o.outWidth <= 0 || o.outHeight <= 0) return "";
            int scale = 1;
            while (o.outWidth / scale > max * 2 || o.outHeight / scale > max * 2) scale *= 2;
            o.inJustDecodeBounds = false;
            o.inSampleSize = scale;
            Bitmap bm = BitmapFactory.decodeByteArray(data, 0, data.length, o);
            if (bm == null) return "";
            // Камера майже завжди пише орієнтацію в EXIF, а не повертає
            // пікселі. Без цього мініатюри й перегляд лежали на боці.
            bm = orient(bm, data);
            int w = bm.getWidth(), h = bm.getHeight();
            float r = Math.min(1f, (float) max / Math.max(1, Math.max(w, h)));
            Bitmap scaledBm = bm;
            if (r < 1f) {
                scaledBm = Bitmap.createScaledBitmap(bm, Math.max(1, Math.round(w * r)), Math.max(1, Math.round(h * r)), true);
                if (scaledBm != bm) bm.recycle();
            }
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            boolean alpha = png && scaledBm.hasAlpha();
            scaledBm.compress(alpha ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG, alpha ? 100 : 82, bo);
            scaledBm.recycle();
            return (alpha ? "data:image/png;base64," : "data:image/jpeg;base64,")
                    + Base64.encodeToString(bo.toByteArray(), Base64.NO_WRAP);
        } catch (Throwable e) {
            return "";
        }
    }
}
