package com.brightpath.sanad.data.realtime;

import com.brightpath.sanad.data.auth.TokenStore;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import io.socket.client.IO;

/**
 * Shared Socket.IO IO.Options for all Android realtime clients (chat/community/session/library/group).
 * Puts Sanctum token + userId + role on both auth and query so the Node handshake accepts either.
 */
public final class RealtimeSocketOptions {
    private RealtimeSocketOptions() {}

    public static IO.Options create(TokenStore tokenStore) {
        IO.Options options = new IO.Options();
        options.path = "/socket/";
        options.reconnection = true;
        options.forceNew = false;
        options.timeout = 10_000;
        options.transports = new String[]{"polling", "websocket"};

        int userId = tokenStore.getUserId();
        String role = tokenStore.getRole();
        String token = tokenStore.getToken();

        Map<String, String> auth = new HashMap<>();
        if (userId > 0) auth.put("userId", String.valueOf(userId));
        if (role != null && !role.isEmpty()) auth.put("role", role);
        if (token != null && !token.isEmpty()) auth.put("token", token);
        options.auth = auth;
        options.query = buildQuery(userId, role, token);
        return options;
    }

    private static String buildQuery(int userId, String role, String token) {
        StringBuilder builder = new StringBuilder();
        appendQuery(builder, "userId", userId > 0 ? String.valueOf(userId) : null);
        appendQuery(builder, "role", role);
        appendQuery(builder, "token", token);
        return builder.toString();
    }

    private static void appendQuery(StringBuilder builder, String key, String value) {
        if (value == null || value.isEmpty()) return;
        if (builder.length() > 0) builder.append('&');
        builder.append(key).append('=')
                .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
    }
}
