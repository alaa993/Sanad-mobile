package com.brightpath.sanad.feature.library;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.brightpath.sanad.data.AppConfig;
import com.brightpath.sanad.data.auth.TokenStore;
import com.brightpath.sanad.data.realtime.RealtimeSocketOptions;
import com.google.gson.JsonObject;

import org.json.JSONObject;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * Listens for library:updated from realtime-server so Library screens invalidate and refetch categories.
 */
public class LibraryRealtimeClient {
    public interface Listener {
        void onLibraryUpdated();
    }

    private static LibraryRealtimeClient instance;

    public static synchronized LibraryRealtimeClient get(@NonNull Context ctx) {
        if (instance == null) {
            instance = new LibraryRealtimeClient(ctx.getApplicationContext());
        }
        return instance;
    }

    private final TokenStore tokenStore;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private final Emitter.Listener updatedListener = args -> notifyListeners();
    private final Emitter.Listener notifyListener = this::handleNotify;
    private Socket socket;
    private boolean connecting = false;

    private LibraryRealtimeClient(Context ctx) {
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

    private void ensureConnected() {
        if (socket != null && (socket.connected() || connecting)) return;
        try {
            String base = AppConfig.BASE_URL;
            if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
            IO.Options options = RealtimeSocketOptions.create(tokenStore);
            socket = IO.socket(base, options);
            connecting = true;
            socket.on(Socket.EVENT_CONNECT, args -> connecting = false);
            socket.on(Socket.EVENT_CONNECT_ERROR, args -> connecting = false);
            socket.on("library:updated", updatedListener);
            socket.on("notify:event", notifyListener);
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
            if ("library:updated".equals(type) || type.startsWith("library")) {
                notifyListeners();
            }
        } catch (Exception ignored) {}
    }

    private void notifyListeners() {
        mainHandler.post(() -> {
            for (Listener listener : listeners) {
                listener.onLibraryUpdated();
            }
        });
    }

    private void disconnect() {
        if (socket != null) {
            socket.off("library:updated", updatedListener);
            socket.off("notify:event", notifyListener);
            socket.disconnect();
            socket = null;
            connecting = false;
        }
    }
}
