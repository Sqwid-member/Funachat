package com.funa.chat;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Локальні папки чатів. Самі сесії живуть на сервері, тут лише назви й прив'язка. */
public class Folders {
    private static final String K_MIGRATED = "folders_owner_migrated";

    private final SharedPreferences p;
    private final Store store;

    public Folders(Context c) { this(c, null); }

    /** Папки належать активному акаунту DeepSeek. */
    public Folders(Context c, Store store) {
        p = c.getSharedPreferences("funa", Context.MODE_PRIVATE);
        this.store = store;
        adoptLegacy();
    }

    private String owner() {
        return store == null ? "" : store.activeId();
    }

    private String kList() {
        String o = owner();
        return o.isEmpty() ? "folders" : "folders_" + o;
    }

    private String kMap() {
        String o = owner();
        return o.isEmpty() ? "folder_map" : "folder_map_" + o;
    }

    /** Переносить спільні папки першому акаунту; решта починає з чистих. */
    private void adoptLegacy() {
        String o = owner();
        if (o.isEmpty() || p.getBoolean(K_MIGRATED, false)) return;
        p.edit()
                .putString(kList(), p.getString("folders", "[]"))
                .putString(kMap(), p.getString("folder_map", "{}"))
                .putBoolean(K_MIGRATED, true)
                .apply();
    }

    public synchronized JSONArray list() {
        adoptLegacy();
        try {
            JSONArray a = new JSONArray(p.getString(kList(), "[]"));
            return a;
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    public synchronized JSONObject map() {
        adoptLegacy();
        try {
            return new JSONObject(p.getString(kMap(), "{}"));
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    private void saveList(JSONArray a) { p.edit().putString(kList(), a.toString()).apply(); }
    private void saveMap(JSONObject o) { p.edit().putString(kMap(), o.toString()).apply(); }

    public synchronized String add(String name) {
        JSONArray a = list();
        // 200 папок — стеля, за якою список перестає бути корисним
        if (a.length() >= 200) return "";
        if (name == null) name = "";
        name = name.trim();
        if (name.length() > 60) name = name.substring(0, 60);
        String id = "f" + System.currentTimeMillis();
        // дві папки за одну мілісекунду не мають злипнутися в одну
        for (int n = 1; hasId(a, id); n++) id = "f" + System.currentTimeMillis() + "-" + n;
        try {
            JSONObject o = new JSONObject();
            o.put("id", id);
            o.put("name", name == null ? "" : name.trim());
            o.put("created", System.currentTimeMillis());
            a.put(o);
            saveList(a);
        } catch (Exception ignored) {}
        return id;
    }

    private static boolean hasId(JSONArray a, String id) {
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o != null && id.equals(o.optString("id"))) return true;
        }
        return false;
    }

    public synchronized void rename(String id, String name) {
        if (name == null) name = "";
        name = name.trim();
        if (name.length() > 60) name = name.substring(0, 60);
        JSONArray a = list();
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o == null || !id.equals(o.optString("id"))) continue;
            try { o.put("name", name == null ? "" : name.trim()); saveList(a); } catch (Exception ignored) {}
            return;
        }
    }

    public synchronized void remove(String id) {
        JSONArray a = list();
        JSONArray out = new JSONArray();
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o != null && !id.equals(o.optString("id"))) out.put(o);
        }
        saveList(out);

        JSONObject m = map();
        List<String> drop = new ArrayList<>();
        Iterator<String> it = m.keys();
        while (it.hasNext()) {
            String k = it.next();
            if (id.equals(m.optString(k))) drop.add(k);
        }
        for (String k : drop) m.remove(k);
        saveMap(m);
    }

    public synchronized void move(String sid, String fid) {
        JSONObject m = map();
        try {
            if (fid == null || fid.isEmpty() || "0".equals(fid) || "none".equals(fid)) m.remove(sid);
            else m.put(sid, fid);
            saveMap(m);
        } catch (Exception ignored) {}
    }

    /** Прибирає папки й привʼязки видаленого акаунта. */
    public synchronized void purge(String id) {
        if (id == null || id.isEmpty()) return;
        p.edit().remove("folders_" + id).remove("folder_map_" + id).apply();
    }

    public synchronized void forget(String sid) {
        JSONObject m = map();
        m.remove(sid);
        saveMap(m);
    }
}
