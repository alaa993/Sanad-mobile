package com.brightpath.sanad.feature.chat;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.brightpath.sanad.data.AppConfig;
import com.brightpath.sanad.data.auth.TokenStore;
import com.brightpath.sanad.data.realtime.RealtimeSocketOptions;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

import io.socket.client.IO;
import io.socket.client.Socket;

/**
 * Socket.IO client for chat rooms.
 */
public class ChatRealtime {

    public interface RoomListener {
        void onMessage(ChatModels.Message message);
        default void onTyping(int userId) {}
    }

    private static ChatRealtime instance;

    public static synchronized ChatRealtime get(@NonNull Context ctx) {
        if (instance == null) {
            instance = new ChatRealtime(ctx.getApplicationContext());
        }
        return instance;
    }

    private final Context appCtx;
    private final TokenStore tokenStore;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Gson gson = new Gson();
    private final Map<Integer, Set<RoomListener>> listeners = new ConcurrentHashMap<>();
    private final Set<String> joinedRooms = ConcurrentHashMap.newKeySet();
    private final Set<String> pendingRooms = ConcurrentHashMap.newKeySet();
    private Socket socket;
    private boolean connecting = false;

    private ChatRealtime(Context ctx) {
        this.appCtx = ctx;
        this.tokenStore = new TokenStore(ctx);
    }

    public void register(int chatId, RoomListener listener) {
        register(chatId, listener, false);
    }

    public void register(int chatId, RoomListener listener, boolean allowSelfEcho) {
        if (chatId <= 0 || listener == null) return;
        listeners.computeIfAbsent(chatId, c -> new CopyOnWriteArraySet<>()).add(listener);
        joinRoom(chatId);
    }

    public void unregister(int chatId, RoomListener listener) {
        if (chatId <= 0) return;
        Set<RoomListener> set = listeners.get(chatId);
        if (set != null) {
            set.remove(listener);
            if (set.isEmpty()) {
                listeners.remove(chatId);
                leaveRoom(chatId);
            }
        }
    }

    public void emitTyping(int chatId) {
        if (chatId <= 0 || socket == null || !socket.connected()) return;
        try {
            JSONObject payload = new JSONObject();
            payload.put("room", roomName(chatId));
            socket.emit("chat:typing", payload);
        } catch (Exception ignored) {}
    }

    /**
     * إرسال رسالة عبر الـ socket مباشرة لتحديث الطرف الآخر فوراً حتى قبل وصول REST.
     */
    public void emitMessage(int chatId, String content) {
        emitMessage(chatId, content, "text");
    }

    public void emitMessage(int chatId, String content, String type) {
        if (chatId <= 0 || socket == null || !socket.connected()) return;
        try {
            JSONObject payload = new JSONObject();
            payload.put("room", roomName(chatId));
            payload.put("content", content);
            payload.put("type", type);
            socket.emit("chat:message", payload, (Object) null);
        } catch (Exception ignored) {}
    }

    private void joinRoom(int chatId) {
        ensureConnected();
        final String room = roomName(chatId);
        pendingRooms.add(room);
        if (socket != null && socket.connected()) {
            try {
                JSONObject payload = new JSONObject();
                payload.put("room", room);
                socket.emit("join", payload, (Object) null);
                pendingRooms.remove(room);
                joinedRooms.add(room);
            } catch (Exception ignored) {}
        }
    }

    private void leaveRoom(int chatId) {
        final String room = roomName(chatId);
        pendingRooms.remove(room);
        joinedRooms.remove(room);
        if (socket != null && socket.connected()) {
            try {
                JSONObject payload = new JSONObject();
                payload.put("room", room);
                socket.emit("leave", payload, (Object) null);
            } catch (Exception ignored) {}
        }
    }

    private void ensureConnected() {
        if (socket != null && (socket.connected() || connecting)) return;
        try {
            String base = AppConfig.BASE_URL;
            if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
            IO.Options options = RealtimeSocketOptions.create(tokenStore);
            socket = IO.socket(base, options);
            connecting = true;
            socket.on(Socket.EVENT_CONNECT, args -> {
                connecting = false;
                Set<String> roomsToJoin = new HashSet<>(joinedRooms);
                roomsToJoin.addAll(pendingRooms);
                for (String room : roomsToJoin) {
                    try {
                        JSONObject payload = new JSONObject();
                        payload.put("room", room);
                        socket.emit("join", payload, (Object) null);
                        joinedRooms.add(room);
                    } catch (Exception ignored) {}
                }
                pendingRooms.clear();
            });
            socket.on(Socket.EVENT_CONNECT_ERROR, args -> connecting = false);
            socket.on("chat:message", this::handleMessageEvent);
            socket.on("chat:typing", this::handleTypingEvent);
            socket.connect();
        } catch (Exception ignored) {
            connecting = false;
        }
    }

    private void handleTypingEvent(Object... args) {
        JsonObject json = toJson(args);
        if (json == null) return;
        int chatId = extractChatId(json);
        int userId = safeInt(json, "userId", "from");
        if (chatId <= 0 || userId <= 0) return;
        notifyListeners(chatId, listener -> listener.onTyping(userId));
    }

    private void handleMessageEvent(Object... args) {
        JsonObject json = toJson(args);
        if (json == null) return;
        int senderId = safeInt(json, "from", "userId");
        if (senderId <= 0 && json.has("meta") && json.get("meta").isJsonObject()) {
            JsonObject meta = json.get("meta").getAsJsonObject();
            if (meta.has("message") && meta.get("message").isJsonObject()) {
                JsonObject message = meta.get("message").getAsJsonObject();
                if (message.has("sender") && message.get("sender").isJsonObject()) {
                    senderId = safeInt(message.get("sender").getAsJsonObject(), "id");
                }
            }
        }
        int chatId = extractChatId(json);
        if (chatId <= 0) return;
        if (senderId > 0 && senderId == tokenStore.getUserId()) {
            return;
        }
        ChatModels.Message message = parseMessage(json, chatId);
        if (message == null) return;
        notifyListeners(chatId, listener -> listener.onMessage(message));
    }

    private void notifyListeners(int chatId, java.util.function.Consumer<RoomListener> call) {
        Set<RoomListener> set = listeners.get(chatId);
        if (set == null || set.isEmpty()) return;
        for (RoomListener listener : set) {
            mainHandler.post(() -> call.accept(listener));
        }
    }

    private ChatModels.Message parseMessage(JsonObject json, int chatId) {
        JsonObject meta = json.has("meta") && json.get("meta").isJsonObject()
                ? json.get("meta").getAsJsonObject() : null;
        if (meta != null && meta.has("message")) {
            try {
                ChatModels.Message msg = gson.fromJson(meta.get("message"), ChatModels.Message.class);
                if (msg != null && msg.chat_id == 0) msg.chat_id = chatId;
                return msg;
            } catch (Exception ignored) {}
        }
        ChatModels.Message msg = new ChatModels.Message();
        msg.chat_id = chatId;
        msg.id = safeInt(json, "id");
        msg.type = json.has("type") && json.get("type").isJsonPrimitive()
                ? json.get("type").getAsString() : "text";
        msg.body = json.has("content") && json.get("content").isJsonPrimitive()
                ? json.get("content").getAsString() : null;
        long createdAt = safeLong(json, "createdAt");
        if (createdAt > 0) {
            msg.created_at = formatIso(createdAt);
        } else if (json.has("created_at")) {
            msg.created_at = json.get("created_at").getAsString();
        }
        ChatModels.UserRef sender = new ChatModels.UserRef();
        sender.id = safeInt(json, "from", "userId");
        sender.name = null;
        msg.sender = sender;
        return msg;
    }

    private int extractChatId(JsonObject json) {
        if (json.has("meta") && json.get("meta").isJsonObject()) {
            JsonObject meta = json.get("meta").getAsJsonObject();
            if (meta.has("chatId")) {
                try { return meta.get("chatId").getAsInt(); } catch (Exception ignored) {}
            }
            if (meta.has("chat_id")) {
                try { return meta.get("chat_id").getAsInt(); } catch (Exception ignored) {}
            }
        }
        if (json.has("room")) {
            String room = json.get("room").getAsString();
            if (room != null && room.startsWith("chat_")) {
                try { return Integer.parseInt(room.substring(5)); } catch (Exception ignored) {}
            }
        }
        if (json.has("chatId")) {
            try { return json.get("chatId").getAsInt(); } catch (Exception ignored) {}
        }
        return -1;
    }

    private JsonObject toJson(Object[] args) {
        if (args == null || args.length == 0 || args[0] == null) return null;
        Object payload = args[0];
        try {
            if (payload instanceof JSONObject) {
                return gson.fromJson(payload.toString(), JsonObject.class);
            }
            if (payload instanceof String) {
                return gson.fromJson((String) payload, JsonObject.class);
            }
            return gson.fromJson(gson.toJson(payload), JsonObject.class);
        } catch (Exception e) {
            return null;
        }
    }

    private String buildQuery() {
        int userId = tokenStore.getUserId();
        String role = tokenStore.getRole();
        String token = tokenStore.getToken();
        StringBuilder sb = new StringBuilder();
        appendQuery(sb, "userId", userId > 0 ? String.valueOf(userId) : null);
        appendQuery(sb, "role", role);
        appendQuery(sb, "token", token);
        return sb.toString();
    }

    private void appendQuery(StringBuilder sb, String key, String value) {
        if (value == null || value.isEmpty()) return;
        if (sb.length() > 0) sb.append('&');
        sb.append(key).append('=').append(urlEncode(value));
    }

    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException ignored) {
            return value;
        }
    }

    private String roomName(int chatId) {
        return "chat_" + chatId;
    }

    private int safeInt(JsonObject obj, String... keys) {
        for (String key : keys) {
            if (obj.has(key) && obj.get(key).isJsonPrimitive()) {
                try { return obj.get(key).getAsInt(); } catch (Exception ignored) {}
                try { return Integer.parseInt(obj.get(key).getAsString()); } catch (Exception ignored) {}
            }
        }
        return 0;
    }

    private long safeLong(JsonObject obj, String key) {
        if (obj.has(key) && obj.get(key).isJsonPrimitive()) {
            try { return obj.get(key).getAsLong(); } catch (Exception ignored) {}
        }
        return 0L;
    }

    private String formatIso(long millis) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        fmt.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        return fmt.format(new Date(millis));
    }
}
