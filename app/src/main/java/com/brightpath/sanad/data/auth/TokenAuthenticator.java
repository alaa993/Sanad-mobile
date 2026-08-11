package com.brightpath.sanad.data.auth;

import android.content.Context;
import androidx.annotation.Nullable;
import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

/**
 * Clears the local session only when {@code /api/auth/me} returns 401.
 * Other endpoints (push prefs, wallet, …) must not force logout for active users.
 */
public class TokenAuthenticator implements Authenticator {
    private final TokenStore tokenStore;

    public TokenAuthenticator(Context context) {
        this.tokenStore = new TokenStore(context.getApplicationContext());
    }

    @Nullable
    @Override
    public Request authenticate(@Nullable Route route, Response response) {
        if (responseCount(response) > 1) {
            return null;
        }
        String authorization = response.request().header("Authorization");
        if (authorization == null || authorization.isEmpty()) {
            return null;
        }
        String path = response.request().url().encodedPath();
        if (path != null && path.contains("/api/auth/me")) {
            tokenStore.clear();
        }
        return null;
    }

    private int responseCount(Response response) {
        int result = 1;
        while ((response = response.priorResponse()) != null) {
            result++;
        }
        return result;
    }
}
