package com.brightpath.sanad.data.auth;

import android.content.Context;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Attaches Bearer token. Clears the local session only on definitive auth failures
 * from {@code /api/auth/me} — not on every 401 (push prefs / unrelated routes).
 */
public class AuthInterceptor implements Interceptor {
    private final TokenStore tokenStore;

    public AuthInterceptor(Context context) {
        this.tokenStore = new TokenStore(context);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
        String token = tokenStore.getToken();
        Request.Builder builder = original.newBuilder()
                .header("Accept", "application/json")
                .header("User-Agent", "SanadAndroid/1.0.18");
        if (token != null) {
            token = token.trim();
            if (!token.isEmpty()) {
                String auth = token.regionMatches(true, 0, "Bearer ", 0, 7)
                        ? token
                        : "Bearer " + token;
                builder.header("Authorization", auth);
            }
        }
        Response response = chain.proceed(builder.build());
        if (response.code() == 401 && token != null && !token.isEmpty()) {
            String path = original.url().encodedPath();
            if (path != null && path.contains("/api/auth/me")) {
                tokenStore.clear();
            }
        }
        return response;
    }
}
