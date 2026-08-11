package com.brightpath.sanad.feature.community;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.Nullable;

import com.brightpath.sanad.data.auth.TokenStore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Local-only "vent to self" journal messages keyed by userId in SharedPreferences (not synced to the API).
 */
public class SelfChatStore {
    private static final String PREF = "sanad_self_chat";
    private final SharedPreferences sp;
    private final String key;

    public SelfChatStore(Context context) {
        sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        int userId = new TokenStore(context).getUserId();
        key = "messages_" + userId;
    }

    public static class Message {
        public final int serverId;
        public final String text;
        public final long ts;

        Message(int serverId, String text, long ts) {
            this.serverId = serverId;
            this.text = text;
            this.ts = ts;
        }

        public String formattedTime(Context context) {
            try {
                return new SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(new Date(ts));
            } catch (Exception e) {
                return "";
            }
        }
    }

    public List<Message> load() {
        String raw = sp.getString(key, "[]");
        List<Message> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                String text = o.optString("text", "");
                long ts = o.optLong("ts", 0L);
                int serverId = o.optInt("serverId", 0);
                if (text == null || text.isEmpty()) continue;
                out.add(new Message(serverId, text, ts));
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    public void replaceFromServer(@Nullable List<CommunityModels.Journal> entries) {
        if (entries == null || entries.isEmpty()) return;
        List<Message> messages = new ArrayList<>();
        for (CommunityModels.Journal entry : entries) {
            if (entry == null || entry.entry == null || entry.entry.trim().isEmpty()) continue;
            messages.add(new Message(entry.id, entry.entry.trim(), parseCreatedAt(entry.created_at)));
        }
        Collections.sort(messages, Comparator.comparingLong(m -> m.ts));
        persist(messages);
    }

    public void add(String text) {
        if (text == null || text.trim().isEmpty()) return;
        List<Message> messages = load();
        messages.add(new Message(0, text.trim(), System.currentTimeMillis()));
        persist(messages);
    }

    public void markSynced(String text, int serverId, @Nullable String createdAt) {
        if (text == null || text.trim().isEmpty() || serverId <= 0) return;
        List<Message> messages = load();
        boolean changed = false;
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message message = messages.get(i);
            if (message.serverId == 0 && text.trim().equals(message.text)) {
                messages.set(i, new Message(serverId, message.text, parseCreatedAt(createdAt)));
                changed = true;
                break;
            }
        }
        if (!changed) {
            messages.add(new Message(serverId, text.trim(), parseCreatedAt(createdAt)));
        }
        persist(messages);
    }

    private void persist(List<Message> messages) {
        JSONArray arr = new JSONArray();
        for (Message message : messages) {
            try {
                JSONObject o = new JSONObject();
                o.put("text", message.text);
                o.put("ts", message.ts);
                o.put("serverId", message.serverId);
                arr.put(o);
            } catch (Exception ignored) {
            }
        }
        sp.edit().putString(key, arr.toString()).apply();
    }

    private long parseCreatedAt(@Nullable String raw) {
        if (raw == null || raw.isEmpty()) return System.currentTimeMillis();
        try {
            return Instant.parse(raw).toEpochMilli();
        } catch (DateTimeParseException ignored) {
        }
        return System.currentTimeMillis();
    }
}
