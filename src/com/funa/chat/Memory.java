package com.funa.chat;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONObject;

/** Локальна база «карток знань», які підмішуються в промт. */
public class Memory extends SQLiteOpenHelper {
    private static final String DB = "funa_memory.db";
    private static final int VER = 2;

    private final Store store;

    public Memory(Context c) { this(c, null); }

    /**
     * Картки належать активному акаунту DeepSeek: різні акаунти не бачать
     * факти один одного. Старі картки (owner='') забирає перший акаунт.
     */
    public Memory(Context c, Store store) {
        super(c, DB, null, VER);
        this.store = store;
        claim();
    }

    private String owner() {
        return store == null ? "" : store.activeId();
    }

    private void claim() {
        String o = owner();
        if (o.isEmpty()) return;
        try {
            getWritableDatabase().execSQL("UPDATE cards SET owner=? WHERE owner=''", new Object[]{o});
        } catch (Exception ignored) {}
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE cards (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT NOT NULL DEFAULT ''," +
                "body TEXT NOT NULL DEFAULT ''," +
                "tags TEXT NOT NULL DEFAULT ''," +
                "enabled INTEGER NOT NULL DEFAULT 1," +
                "created_at INTEGER NOT NULL DEFAULT 0," +
                "updated_at INTEGER NOT NULL DEFAULT 0," +
                "owner TEXT NOT NULL DEFAULT '')");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        if (oldV < 1) { onCreate(db); return; }
        if (oldV < 2) {
            try { db.execSQL("ALTER TABLE cards ADD COLUMN owner TEXT NOT NULL DEFAULT ''"); }
            catch (Exception ignored) {}
        }
    }

    @Override public void onDowngrade(SQLiteDatabase db, int oldV, int newV) {
        // Не втрачаємо картки, якщо користувач відкотився на старішу збірку.
    }

    /**
     * Ремонт схеми на кожному відкритті: якщо в базі немає таблиці або потрібних
     * колонок (так було в збірках, де версію підняли без міграції), додаємо їх.
     * Без цього будь-яке збереження падало з «no such column: owner».
     */
    @Override public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        try {
            db.execSQL("CREATE TABLE IF NOT EXISTS cards (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "title TEXT NOT NULL DEFAULT ''," +
                    "body TEXT NOT NULL DEFAULT ''," +
                    "tags TEXT NOT NULL DEFAULT ''," +
                    "enabled INTEGER NOT NULL DEFAULT 1," +
                    "created_at INTEGER NOT NULL DEFAULT 0," +
                    "updated_at INTEGER NOT NULL DEFAULT 0," +
                    "owner TEXT NOT NULL DEFAULT '')");
        } catch (Exception e) {
            Log.e("mem", "create table failed", e);
        }
        addColumn(db, "title", "TEXT NOT NULL DEFAULT ''");
        addColumn(db, "body", "TEXT NOT NULL DEFAULT ''");
        addColumn(db, "tags", "TEXT NOT NULL DEFAULT ''");
        addColumn(db, "enabled", "INTEGER NOT NULL DEFAULT 1");
        addColumn(db, "created_at", "INTEGER NOT NULL DEFAULT 0");
        addColumn(db, "updated_at", "INTEGER NOT NULL DEFAULT 0");
        addColumn(db, "owner", "TEXT NOT NULL DEFAULT ''");
    }

    private void addColumn(SQLiteDatabase db, String name, String def) {
        if (hasColumn(db, name)) return;
        try {
            db.execSQL("ALTER TABLE cards ADD COLUMN " + name + " " + def);
            Log.add("mem", "schema repaired: cards." + name);
        } catch (Exception e) {
            Log.e("mem", "add column " + name + " failed", e);
        }
    }

    private boolean hasColumn(SQLiteDatabase db, String name) {
        try (Cursor c = db.rawQuery("PRAGMA table_info(cards)", null)) {
            while (c.moveToNext()) {
                if (name.equalsIgnoreCase(c.getString(1))) return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    /** Прибирає картки видаленого акаунта: його id більше не повернеться. */
    public void purge(String id) {
        if (id == null || id.isEmpty()) return;
        try {
            getWritableDatabase().delete("cards", "owner=?", new String[]{id});
        } catch (Exception e) {
            Log.e("mem", "purge failed", e);
        }
    }

    public JSONArray list() {
        claim();
        JSONArray out = new JSONArray();
        SQLiteDatabase db = getReadableDatabase();
        String own = owner();
        String sql = "SELECT id,title,body,tags,enabled,created_at,updated_at FROM cards"
                + (own.isEmpty() ? "" : " WHERE owner=?")
                + " ORDER BY enabled DESC, updated_at DESC, id DESC LIMIT 1000";
        try (Cursor c = db.rawQuery(sql, own.isEmpty() ? null : new String[]{own})) {
            while (c.moveToNext()) {
                JSONObject o = new JSONObject();
                try {
                    o.put("id", c.getLong(0));
                    o.put("title", c.getString(1));
                    o.put("body", c.getString(2));
                    o.put("tags", c.getString(3));
                    o.put("enabled", c.getInt(4) == 1);
                    o.put("created", c.getLong(5));
                    o.put("updated", c.getLong(6));
                } catch (Exception ignored) {}
                out.put(o);
            }
        } catch (Exception ignored) {}
        return out;
    }

    /**
     * Лише потрібні промту поля й лише ввімкнені картки: інакше кожне
     * повідомлення читало б усю базу памʼяті (тисячі карток по 20 КБ).
     */
    private JSONArray promptCards(int limit) {
        claim();
        JSONArray out = new JSONArray();
        String own = owner();
        String sql = "SELECT title,body,tags FROM cards WHERE enabled=1"
                + (own.isEmpty() ? "" : " AND owner=?")
                + " ORDER BY updated_at DESC, id DESC LIMIT " + Math.max(1, limit);
        try (Cursor c = getReadableDatabase().rawQuery(sql,
                own.isEmpty() ? null : new String[]{own})) {
            while (c.moveToNext()) {
                JSONObject o = new JSONObject();
                try {
                    o.put("title", c.getString(0));
                    o.put("body", c.getString(1));
                    o.put("tags", c.getString(2));
                } catch (Exception ignored) {}
                out.put(o);
            }
        } catch (Exception e) {
            Log.e("mem", "promptCards failed", e);
        }
        return out;
    }

    public long save(long id, String title, String body, String tags) {
        SQLiteDatabase db = getWritableDatabase();
        long now = System.currentTimeMillis();
        // картка памʼяті — це кілька абзаців, а не книга: не роздуваємо
        // базу й промт випадково вставленим текстом
        if (title != null && title.length() > 200) title = title.substring(0, 200);
        if (body != null && body.length() > 20000) body = body.substring(0, 20000) + "\n… (обрізано)";
        if (tags != null && tags.length() > 200) tags = tags.substring(0, 200);
        ContentValues v = new ContentValues();
        v.put("title", title == null ? "" : title.trim());
        v.put("body", body == null ? "" : body);
        v.put("tags", tags == null ? "" : tags.trim());
        v.put("updated_at", now);
        String o = owner();
        if (id > 0) {
            String where = "id=?" + (o.isEmpty() ? "" : " AND owner=?");
            String[] args = o.isEmpty()
                    ? new String[]{String.valueOf(id)}
                    : new String[]{String.valueOf(id), o};
            db.update("cards", v, where, args);
            return id;
        }
        v.put("owner", o);
        v.put("enabled", 1);
        v.put("created_at", now);
        return db.insert("cards", null, v);
    }

    public void delete(long id) {
        String o = owner();
        getWritableDatabase().delete("cards",
                "id=?" + (o.isEmpty() ? "" : " AND owner=?"),
                o.isEmpty() ? new String[]{String.valueOf(id)}
                        : new String[]{String.valueOf(id), o});
    }

    public void toggle(long id, boolean on) {
        ContentValues v = new ContentValues();
        v.put("enabled", on ? 1 : 0);
        v.put("updated_at", System.currentTimeMillis());
        String o = owner();
        getWritableDatabase().update("cards", v,
                "id=?" + (o.isEmpty() ? "" : " AND owner=?"),
                o.isEmpty() ? new String[]{String.valueOf(id)}
                        : new String[]{String.valueOf(id), o});
    }

    public int count() {
        String o = owner();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM cards" + (o.isEmpty() ? "" : " WHERE owner=?"),
                o.isEmpty() ? null : new String[]{o})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /** Блок знань для промту. Порожній рядок, якщо карток немає. */
    public String promptBlock(String lang, int maxChars) {
        JSONArray cards = promptCards(300);
        StringBuilder sb = new StringBuilder();
        int used = 0;
        for (int i = 0; i < cards.length(); i++) {
            JSONObject c = cards.optJSONObject(i);
            if (c == null || !c.optBoolean("enabled", true)) continue;
            String title = c.optString("title", "").trim();
            String body = c.optString("body", "").trim();
            String tags = c.optString("tags", "").trim();
            if (title.isEmpty() && body.isEmpty()) continue;
            StringBuilder line = new StringBuilder("• ");
            if (!title.isEmpty()) line.append(title).append(": ");
            line.append(body.replace("\n", " "));
            if (!tags.isEmpty()) line.append(" [").append(tags).append("]");
            int left = maxChars - used;
            if (left <= 40) break;
            if (line.length() > left) {
                // обрізаємо саму картку, а не викидаємо її й усе після неї
                line.setLength(left - 1);
                line.append('…');
            }
            used += line.length();
            sb.append(line).append('\n');
        }
        if (sb.length() == 0) return "";
        String head;
        switch (lang == null ? "uk" : lang) {
            case "en": head = "Saved facts (use when relevant, do not repeat verbatim):\n"; break;
            case "ru": head = "Сохранённые факты (используй по необходимости, не цитируй дословно):\n"; break;
            default: head = "Збережені факти (використовуй за потреби, не цитуй дослівно):\n";
        }
        return head + sb;
    }
}
