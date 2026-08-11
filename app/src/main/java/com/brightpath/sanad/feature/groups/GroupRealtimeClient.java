package com.brightpath.sanad.feature.groups;

import android.content.Context;
import android.util.Log;

import com.brightpath.sanad.data.AppConfig;
import com.brightpath.sanad.data.auth.TokenStore;
import com.brightpath.sanad.data.realtime.RealtimeSocketOptions;

import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import io.socket.client.IO;
import io.socket.client.Socket;

/**
 * Minimal realtime presence for group sessions using the existing Socket.IO server.
 * Room name convention: group_{id}
 */
public class GroupRealtimeClient {
    public interface Listener {
        void onPresence(int count);
    }

    private static GroupRealtimeClient instance;
    private final TokenStore tokenStore;
    private Socket socket;
    private boolean connecting;
    private Listener listener;
    private int currentGroupId = -1;

    public static synchronized GroupRealtimeClient get(Context ctx){
        if (instance == null) instance = new GroupRealtimeClient(ctx.getApplicationContext());
        return instance;
    }

    private GroupRealtimeClient(Context ctx){
        this.tokenStore = new TokenStore(ctx);
    }

    public void connect(int groupId, Listener l){
        listener = l;
        currentGroupId = groupId;
        if (socket != null && (socket.connected() || connecting)) {
            joinRoom();
            return;
        }
        try {
            String base = AppConfig.BASE_URL;
            if (base.endsWith("/")) {
                base = base.substring(0, base.length() - 1);
            }
            IO.Options options = RealtimeSocketOptions.create(tokenStore);
            socket = IO.socket(base, options);
            connecting = true;
            socket.on(Socket.EVENT_CONNECT, args -> {
                connecting = false;
                joinRoom();
            });
            socket.on(Socket.EVENT_CONNECT_ERROR, args -> connecting = false);
            socket.on("group:presence", args -> {
                if (listener == null || args == null || args.length == 0 || !(args[0] instanceof JSONObject)) return;
                JSONObject obj = (JSONObject) args[0];
                int count = obj.optInt("count", -1);
                if (count >= 0) listener.onPresence(count);
            });
            socket.connect();
        } catch (Exception e){
            Log.e("GroupRealtime", "connect error", e);
            connecting = false;
        }
    }

    public void disconnect(){
        leaveRoom();
        if (socket != null){
            socket.off("group:presence");
            socket.disconnect();
            socket = null;
        }
        listener = null;
        currentGroupId = -1;
        connecting = false;
    }

    private void joinRoom(){
        if (socket == null || !socket.connected() || currentGroupId <= 0) return;
        try {
            JSONObject payload = new JSONObject();
            payload.put("groupId", currentGroupId);
            socket.emit("group:join", payload);
        } catch (Exception ignore){}
    }

    private void leaveRoom(){
        if (socket == null || !socket.connected() || currentGroupId <= 0) return;
        try {
            JSONObject payload = new JSONObject();
            payload.put("groupId", currentGroupId);
            socket.emit("group:leave", payload);
        } catch (Exception ignore){}
    }

    private String buildQuery() {
        String token = tokenStore.getToken();
        String role = tokenStore.getRole();
        int userId = tokenStore.getUserId();
        StringBuilder builder = new StringBuilder();
        appendQuery(builder, "userId", userId > 0 ? String.valueOf(userId) : null);
        appendQuery(builder, "role", role);
        appendQuery(builder, "token", token);
        return builder.toString();
    }

    private void appendQuery(StringBuilder builder, String key, String value) {
        if (value == null || value.isEmpty()) return;
        if (builder.length() > 0) builder.append("&");
        builder.append(key).append("=")
                .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
    }
}
