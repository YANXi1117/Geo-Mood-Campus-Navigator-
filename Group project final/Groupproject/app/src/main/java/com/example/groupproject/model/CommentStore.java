package com.example.groupproject.model;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CommentStore {

    private static final CommentStore instance = new CommentStore();
    private final List<Comment> comments = new ArrayList<>();
    private final Set<String> unlockedBuildings = new HashSet<>();
    private SharedPreferences prefs;

    private CommentStore() {}

    public static CommentStore getInstance() { return instance; }

    public void init(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences("artgeo_comments", Context.MODE_PRIVATE);
        loadFromPrefs();
    }

    // ── 评论 ──────────────────────────────────────────

    public void addComment(Comment comment) {
        comments.add(comment);
        saveToPrefs();
    }

    // 根据 buildingId + index 删除单条评论
    public void removeComment(String buildingId, int indexWithinBuilding) {
        int count = 0;
        for (int i = 0; i < comments.size(); i++) {
            if (comments.get(i).getBuildingId().equals(buildingId)) {
                if (count == indexWithinBuilding) {
                    comments.remove(i);
                    saveToPrefs();
                    return;
                }
                count++;
            }
        }
    }

    public List<Comment> getCommentsForBuilding(String buildingId) {
        List<Comment> result = new ArrayList<>();
        for (Comment c : comments) {
            if (c.getBuildingId().equals(buildingId)) result.add(c);
        }
        return result;
    }

    // ── 情绪 ──────────────────────────────────────────

    public int getMoodColor(String buildingId) {
        List<Comment> list = getCommentsForBuilding(buildingId);
        if (list.isEmpty()) return 0x00000000; // 透明

        int positive = 0, negative = 0;
        for (Comment c : list) {
            String e = c.getEmoji();
            if (e.equals("😊") || e.equals("😍") || e.equals("👍")) positive++;
            else if (e.equals("😡") || e.equals("😞") || e.equals("👎")) negative++;
        }
        if (positive > negative)      return 0x4400CC44;
        else if (negative > positive) return 0x44CC2200;
        else                          return 0x44FFCC00;
    }

    public String getDominantEmoji(String buildingId) {
        List<Comment> list = getCommentsForBuilding(buildingId);
        if (list.isEmpty()) return null;

        Map<String, Integer> count = new HashMap<>();
        for (Comment c : list) {
            count.put(c.getEmoji(), count.getOrDefault(c.getEmoji(), 0) + 1);
        }
        String dominant = null;
        int max = 0;
        for (Map.Entry<String, Integer> entry : count.entrySet()) {
            if (entry.getValue() > max) {
                max = entry.getValue();
                dominant = entry.getKey();
            }
        }
        return dominant;
    }

    // ── 解锁 ──────────────────────────────────────────

    public void unlockBuilding(String buildingId) {
        unlockedBuildings.add(buildingId);
        saveToPrefs();
    }

    public boolean isUnlocked(String buildingId) {
        return unlockedBuildings.contains(buildingId);
    }

    // ── 持久化 ────────────────────────────────────────

    private void saveToPrefs() {
        if (prefs == null) return;
        try {
            JSONArray array = new JSONArray();
            for (Comment c : comments) {
                JSONObject obj = new JSONObject();
                obj.put("buildingId", c.getBuildingId());
                obj.put("emoji",      c.getEmoji());
                obj.put("text",       c.getText());
                array.put(obj);
            }
            prefs.edit()
                    .putString("comments_data", array.toString())
                    .putStringSet("unlocked_ids", unlockedBuildings)
                    .apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void loadFromPrefs() {
        if (prefs == null) return;
        String data = prefs.getString("comments_data", "[]");
        try {
            JSONArray array = new JSONArray(data);
            comments.clear();
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                comments.add(new Comment(
                        obj.getString("buildingId"),
                        obj.getString("emoji"),
                        obj.getString("text")
                ));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Set<String> saved = prefs.getStringSet("unlocked_ids", new HashSet<>());
        unlockedBuildings.clear();
        unlockedBuildings.addAll(saved);
    }
}
