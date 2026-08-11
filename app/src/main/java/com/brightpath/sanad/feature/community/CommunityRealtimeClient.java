package com.brightpath.sanad.feature.community;

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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import io.socket.client.IO;
import io.socket.client.Socket;

/**
 * Socket.IO client for community:post / community:comment / community:like.
 * Feed ViewModels patch lists incrementally from Listener callbacks instead of always refetching.
 */
public class CommunityRealtimeClient {

    public interface Listener {
        void onPost(int communityId, CommunityModels.Post post);
        void onComment(CommentPayload payload);
        void onLike(LikePayload payload);
    }

    public static class CommentPayload {
        public final int communityId;
        public final int postId;
        public final CommunityModels.Comment comment;

        public CommentPayload(int communityId, int postId, CommunityModels.Comment comment) {
            this.communityId = communityId;
            this.postId = postId;
            this.comment = comment;
        }
    }

    public static class LikePayload {
        public final int communityId;
        public final int postId;
        public final int likesCount;
        public final Boolean liked;

        public LikePayload(int communityId, int postId, int likesCount, Boolean liked) {
            this.communityId = communityId;
            this.postId = postId;
            this.likesCount = likesCount;
            this.liked = liked;
        }
    }

    private static CommunityRealtimeClient instance;
    public static synchronized CommunityRealtimeClient get(@NonNull Context ctx) {
        if (instance == null) {
            instance = new CommunityRealtimeClient(ctx.getApplicationContext());
        }
        return instance;
    }

    private final Context appCtx;
    private final TokenStore tokenStore;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private final Gson gson = new Gson();
    private Socket socket;
    private boolean connecting = false;

    private CommunityRealtimeClient(Context ctx) {
        this.appCtx = ctx;
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

    private synchronized void ensureConnected() {
        if (socket != null && (socket.connected() || connecting)) return;
        try {
            String base = AppConfig.BASE_URL;
            if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
            IO.Options options = RealtimeSocketOptions.create(tokenStore);
            socket = IO.socket(base, options);
            connecting = true;
            socket.on(Socket.EVENT_CONNECT, args -> connecting = false);
            socket.on(Socket.EVENT_CONNECT_ERROR, args -> connecting = false);
            socket.on("community:post", this::handlePost);
            socket.on("community:comment", this::handleComment);
            socket.on("community:like", this::handleLike);
            socket.connect();
        } catch (Exception e) {
            e.printStackTrace();
            connecting = false;
        }
    }

    private void disconnect() {
        if (socket != null) {
            socket.off("community:post", this::handlePost);
            socket.off("community:comment", this::handleComment);
            socket.off("community:like", this::handleLike);
            socket.disconnect();
            socket = null;
            connecting = false;
        }
    }

    private void handlePost(Object... args) {
        JsonObject obj = toJson(args);
        if (obj == null) return;
        int communityId = readInt(obj, "communityId", "community_id");
        JsonElement postElement = obj.has("post") ? obj.get("post") : obj;
        try {
            CommunityModels.Post post = gson.fromJson(postElement, CommunityModels.Post.class);
            if (post == null) return;
            mainHandler.post(() -> {
                for (Listener listener : listeners) {
                    listener.onPost(communityId, post);
                }
            });
        } catch (Exception ignored) { }
    }

    private void handleComment(Object... args) {
        JsonObject obj = toJson(args);
        if (obj == null) return;
        int communityId = readInt(obj, "communityId", "community_id");
        int postId = readInt(obj, "postId", "post_id");
        JsonElement commentElement = obj.get("comment");
        if (commentElement == null || !commentElement.isJsonObject()) return;
        try {
            CommunityModels.Comment comment = gson.fromJson(commentElement, CommunityModels.Comment.class);
            if (comment == null) return;
            CommentPayload payload = new CommentPayload(communityId, postId, comment);
            mainHandler.post(() -> {
                for (Listener listener : listeners) {
                    listener.onComment(payload);
                }
            });
        } catch (Exception ignored) { }
    }

    private void handleLike(Object... args) {
        JsonObject obj = toJson(args);
        if (obj == null) return;
        int communityId = readInt(obj, "communityId", "community_id");
        int postId = readInt(obj, "postId", "post_id");
        int likesCount = obj.has("likesCount") && obj.get("likesCount").isJsonPrimitive()
                ? obj.get("likesCount").getAsInt() : -1;
        Boolean liked = obj.has("liked") && obj.get("liked").isJsonPrimitive()
                ? obj.get("liked").getAsBoolean() : null;
        LikePayload payload = new LikePayload(communityId, postId, likesCount, liked);
        mainHandler.post(() -> {
            for (Listener listener : listeners) {
                listener.onLike(payload);
            }
        });
    }

    private JsonObject toJson(Object[] args) {
        if (args == null || args.length == 0 || args[0] == null) return null;
        Object data = args[0];
        try {
            if (data instanceof JSONObject) {
                return gson.fromJson(data.toString(), JsonObject.class);
            } else if (data instanceof String) {
                return gson.fromJson((String) data, JsonObject.class);
            } else {
                return gson.fromJson(gson.toJson(data), JsonObject.class);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private int readInt(JsonObject obj, String... keys) {
        for (String key : keys) {
            if (obj.has(key) && obj.get(key).isJsonPrimitive()) {
                try { return obj.get(key).getAsInt(); } catch (Exception ignored) { }
            }
        }
        return -1;
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
