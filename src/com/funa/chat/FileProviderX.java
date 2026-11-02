package com.funa.chat;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;

/** Мінімальний FileProvider без AndroidX: віддає файли з /sdcard і кешу. */
public class FileProviderX extends ContentProvider {
    public static final String AUTHORITY = "com.funa.chat.files";

    public static Uri uriFor(File f) {
        return new Uri.Builder().scheme("content").authority(AUTHORITY)
                .appendPath("f").appendQueryParameter("p", f.getAbsolutePath()).build();
    }

    private File fileFor(Uri uri) throws FileNotFoundException {
        String p = uri.getQueryParameter("p");
        if (p == null || p.isEmpty()) throw new FileNotFoundException("no path");
        try {
            File f = new File(p).getCanonicalFile();
            String path = f.getAbsolutePath();
            boolean ok = path.startsWith("/sdcard/") || path.startsWith("/storage/emulated/0/");
            if (!ok && getContext() != null) {
                String cache = getContext().getCacheDir().getCanonicalPath();
                String files = getContext().getFilesDir().getCanonicalPath();
                ok = path.startsWith(cache + "/") || path.startsWith(files + "/");
            }
            if (!ok || !f.isFile()) throw new FileNotFoundException(path);
            return f;
        } catch (FileNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    @Override public boolean onCreate() { return true; }

    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        return ParcelFileDescriptor.open(fileFor(uri), ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override public String getType(Uri uri) {
        try { return Attach.mime(fileFor(uri).getName()); } catch (Exception e) { return "application/octet-stream"; }
    }

    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
        try {
            File f = fileFor(uri);
            String[] cols = projection == null ? new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE} : projection;
            MatrixCursor c = new MatrixCursor(cols, 1);
            Object[] row = new Object[cols.length];
            for (int i = 0; i < cols.length; i++) {
                if (OpenableColumns.DISPLAY_NAME.equals(cols[i])) row[i] = f.getName();
                else if (OpenableColumns.SIZE.equals(cols[i])) row[i] = f.length();
                else row[i] = null;
            }
            c.addRow(row);
            return c;
        } catch (Exception e) {
            return null;
        }
    }

    @Override public Uri insert(Uri uri, ContentValues v) { return null; }
    @Override public int delete(Uri uri, String s, String[] a) { return 0; }
    @Override public int update(Uri uri, ContentValues v, String s, String[] a) { return 0; }
}
