package com.brightpath.sanad.feature.sessions;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;
import com.brightpath.sanad.data.AppConfig;
import com.brightpath.sanad.data.auth.TokenStore;
import com.brightpath.sanad.data.realtime.RealtimeSocketOptions;

import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import io.socket.client.IO;
import io.socket.client.Socket;

/**
 * Dedicated Socket.IO client for session:status events (room session_{id}).
 * Singleton per process; soft-reloads SessionsViewModel when status changes.
 */
public class SessionRealtimeClient {

    public interface Listener {
        void onStatus(int sessionId, String status);
    }

    private static SessionRealtimeClient instance;

    public static synchronized SessionRealtimeClient get(@NonNull Context ctx) {
        if (instance == null) {
            instance = new SessionRealtimeClient(ctx.getApplicationContext());
        }
        return instance;
    }

    private final TokenStore tokenStore;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private final Set<Integer> joinedSessions = new CopyOnWriteArraySet<>();
    private Socket socket;
    private boolean connecting = false;

    private SessionRealtimeClient(Context ctx) {
        this.tokenStore = new TokenStore(ctx);
    }

    public void addListener(Listener listener) {
        if (listener == null) return;
        listeners.add(listener);
        ensureConnected();
    }

    public void removeListener(Listener listener) {
        if (listener == null) return;
        listeners.remove(listener);
        if (listeners.isEmpty()) {
            disconnect();
        }
    }

    public void joinSession(int sessionId) {
        if (sessionId <= 0) return;
        joinedSessions.add(sessionId);
        ensureConnected();
        emitJoin(sessionId);
    }

    public void leaveSession(int sessionId) {
        if (sessionId <= 0) return;
        joinedSessions.remove(sessionId);
        if (socket != null && socket.connected()) {
            try {
                JSONObject payload = new JSONObject();
                payload.put("sessionId", sessionId);
                socket.emit("session:leave", payload);
                JSONObject leaveRoom = new JSONObject();
                leaveRoom.put("room", "session_" + sessionId);
                socket.emit("leave", leaveRoom);
            } catch (Exception ignored) {}
        }
    }

    private void emitJoin(int sessionId) {
        if (socket == null || !socket.connected()) return;
        try {
            JSONObject joinRoom = new JSONObject();
            joinRoom.put("room", "session_" + sessionId);
            socket.emit("join", joinRoom);
            JSONObject payload = new JSONObject();
            payload.put("sessionId", sessionId);
            socket.emit("session:join", payload);
        } catch (Exception ignored) {}
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
                for (Integer sessionId : joinedSessions) {
                    emitJoin(sessionId);
                }
            });
            socket.on(Socket.EVENT_CONNECT_ERROR, args -> connecting = false);
            socket.on("session:status", this::handleStatus);
            socket.on("notify:event", this::handleNotify);
            socket.connect();
        } catch (Exception e) {
            connecting = false;
        }
    }

    private void handleNotify(Object... args) {
        if (args == null || args.length == 0 || args[0] == null) return;
        try {
            JsonObject obj;
            Object payload = args[0];
            if (payload instanceof JSONObject) {
                obj = new com.google.gson.Gson().fromJson(payload.toString(), JsonObject.class);
            } else {
                obj = new com.google.gson.Gson().fromJson(payload.toString(), JsonObject.class);
            }
            String type = obj.has("type") && obj.get("type").isJsonPrimitive()
                    ? obj.get("type").getAsString() : "";
            if (!"session:status".equals(type) && !type.startsWith("session")) return;
            JsonObject data = obj.has("data") && obj.get("data").isJsonObject()
                    ? obj.getAsJsonObject("data") : obj;
            int sessionId = readInt(data, "sessionId", "session_id");
            String status = data.has("status") && data.get("status").isJsonPrimitive()
                    ? data.get("status").getAsString() : "";
            if (sessionId <= 0) sessionId = -1;
            if (status.isEmpty()) status = type;
            final int sid = sessionId;
            final String st = status;
            mainHandler.post(() -> {
                for (Listener listener : listeners) {
                    listener.onStatus(sid, st);
                }
            });
        } catch (Exception ignored) {}
    }

    private void handleStatus(Object... args) {
        if (args == null || args.length == 0 || args[0] == null) return;
        try {
            JsonObject obj;
            Object payload = args[0];
            if (payload instanceof JSONObject) {
                obj = new com.google.gson.Gson().fromJson(payload.toString(), JsonObject.class);
            } else {
                obj = new com.google.gson.Gson().fromJson(payload.toString(), JsonObject.class);
            }
            int sessionId = readInt(obj, "sessionId", "session_id");
            String status = obj.has("status") && obj.get("status").isJsonPrimitive()
                    ? obj.get("status").getAsString() : "";
            if (sessionId <= 0 || status.isEmpty()) return;
            mainHandler.post(() -> {
                for (Listener listener : listeners) {
                    listener.onStatus(sessionId, status);
                }
            });
        } catch (Exception ignored) {}
    }

    private int readInt(JsonObject obj, String... keys) {
        for (String key : keys) {
            if (obj.has(key) && obj.get(key).isJsonPrimitive()) {
                try { return obj.get(key).getAsInt(); } catch (Exception ignored) {}
            }
        }
        return -1;
    }

    private void disconnect() {
        if (socket != null) {
            socket.off("session:status", this::handleStatus);
            socket.off("notify:event", this::handleNotify);
            socket.disconnect();
            socket = null;
            connecting = false;
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
        sb.append(key).append('=').append(URLEncoder.encode(value, StandardCharsets.UTF_8));
    }
}
