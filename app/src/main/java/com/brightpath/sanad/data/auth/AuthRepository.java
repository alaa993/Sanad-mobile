package com.brightpath.sanad.data.auth;

import android.content.Context;
import android.content.pm.ApplicationInfo;

import androidx.annotation.Nullable;
import com.brightpath.sanad.models.LoginRequest;
import com.brightpath.sanad.models.LoginResponse;
import com.brightpath.sanad.models.User;
import com.brightpath.sanad.models.RegisterRequest;
import com.brightpath.sanad.data.SafeGson;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Auth REST client + shared OkHttp for login/register/me/logout.
 * Holds Sanctum tokens in TokenStore; separate from ApiClient so auth works before the main API graph is ready.
 */
public class AuthRepository {

    private static final Object CLIENT_LOCK = new Object();
    private static volatile OkHttpClient sharedClient;

    private final AuthApi api;
    private final TokenStore tokens;

    public AuthRepository(Context ctx, String baseUrl) {
        this.tokens = new TokenStore(ctx);
        OkHttpClient client = sharedClient(ctx.getApplicationContext());

        Gson gson = SafeGson.get();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ensureTrailingSlash(baseUrl))
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        this.api = retrofit.create(AuthApi.class);
    }

    /** Shared client so login reuses TLS/DNS after cold start (fewer flaky first attempts). */
    private static OkHttpClient sharedClient(Context appCtx) {
        OkHttpClient existing = sharedClient;
        if (existing != null) return existing;
        synchronized (CLIENT_LOCK) {
            if (sharedClient != null) return sharedClient;
            OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                    .connectTimeout(20, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .callTimeout(45, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .connectionPool(new okhttp3.ConnectionPool(5, 5, TimeUnit.MINUTES))
                    .addInterceptor(new AuthRouteInterceptor(new TokenStore(appCtx)));
            // No TokenAuthenticator here — login 401 must not clear/retry auth handshake.
            boolean isDebug = (appCtx.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
            if (isDebug) {
                HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
                clientBuilder.addInterceptor(logging);
            }
            sharedClient = clientBuilder.build();
            return sharedClient;
        }
    }

    /** Warm DNS/TLS off the UI path so the first login is less likely to flake. */
    public static void warmUp(Context context) {
        try {
            sharedClient(context.getApplicationContext());
        } catch (Throwable ignored) {}
    }

    private static String ensureTrailingSlash(String baseUrl) {
        if (baseUrl == null || baseUrl.isEmpty()) return "https://dashboard.sanadhub.cloud/";
        return baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }

    /**
     * Adds Accept + Bearer for normal auth-api calls, but strips Authorization on
     * public login/register so stale tokens cannot interfere with the handshake.
     */
    private static final class AuthRouteInterceptor implements Interceptor {
        private final TokenStore tokenStore;

        AuthRouteInterceptor(TokenStore tokenStore) {
            this.tokenStore = tokenStore;
        }

        @Override
        public okhttp3.Response intercept(Chain chain) throws IOException {
            Request original = chain.request();
            String path = original.url().encodedPath();
            boolean publicAuth = path.contains("/api/auth/login")
                    || path.contains("/api/auth/register")
                    || path.contains("/api/auth/phone/")
                    || path.contains("/api/auth/forgot")
                    || path.contains("/api/auth/reset");

            Request.Builder builder = original.newBuilder()
                    .header("Accept", "application/json")
                    .header("User-Agent", "SanadAndroid/1.0.14")
                    .header("Connection", "keep-alive");
            if (publicAuth) {
                builder.removeHeader("Authorization");
            } else {
                String token = tokenStore.getToken();
                if (token != null) {
                    token = token.trim();
                    if (!token.isEmpty()) {
                        String auth = token.regionMatches(true, 0, "Bearer ", 0, 7)
                                ? token
                                : "Bearer " + token;
                        builder.header("Authorization", auth);
                    }
                }
            }
            okhttp3.Response response = chain.proceed(builder.build());
            // Only definitive identity checks clear the session — not every auth 401.
            if (!publicAuth && response.code() == 401) {
                String pathSafe = path != null ? path : "";
                if (pathSafe.contains("/api/auth/me")) {
                    String t = tokenStore.getToken();
                    if (t != null && !t.isEmpty()) {
                        tokenStore.clear();
                    }
                }
            }
            return response;
        }
    }


    public @Nullable LoginResponse login(String username, String password) throws Exception {
        LoginRequest body = new LoginRequest(username, password);
        Exception lastNetwork = null;
        // Transient mobile/TLS flakes: retry a few times before surfacing "Unable to connect".
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                Response<LoginResponse> r = api.login(body).execute();
                LoginResponse resp = r.body();
                String token = resp != null ? resp.resolveToken() : null;
                if (r.isSuccessful() && token != null) {
                    persistAuth(resp);
                    return resp;
                }
                // Auth errors (401/422/429) — do not retry.
                throw buildAuthException(r);
            } catch (AuthException auth) {
                throw auth;
            } catch (IOException network) {
                lastNetwork = network;
                if (attempt < 3) {
                    try {
                        Thread.sleep(400L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw network;
                    }
                }
            }
        }
        throw lastNetwork != null ? lastNetwork : new IOException("login_failed");
    }

    public @Nullable LoginResponse register(RegisterRequest request) throws Exception {
        Exception lastNetwork = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                Response<LoginResponse> r = api.register(request).execute();
                LoginResponse resp = r.body();
                String token = resp != null ? resp.resolveToken() : null;
                if (r.isSuccessful() && isRegisterSuccess(resp, token)) {
                    persistAuth(resp);
                    return resp;
                }
                throw buildAuthException(r);
            } catch (AuthException auth) {
                throw auth;
            } catch (IOException network) {
                lastNetwork = network;
                if (attempt < 3) {
                    try {
                        Thread.sleep(400L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw network;
                    }
                }
            }
        }
        throw lastNetwork != null ? lastNetwork : new IOException("register_failed");
    }

    private boolean isRegisterSuccess(@Nullable LoginResponse resp, @Nullable String token) {
        if (token != null && !token.isEmpty()) return true;
        if (resp == null) return false;
        if ("success".equalsIgnoreCase(resp.status)) return true;
        return resp.message != null && resp.message.toLowerCase(java.util.Locale.US).contains("registered");
    }

    public @Nullable User me() throws Exception {
        Response<User> r = api.me().execute();
        if (r.isSuccessful()) {
            User body = r.body();
            if (body == null || body.id <= 0) {
                throw new IOException("empty_user");
            }
            return body;
        }
        throw buildAuthException(r);
    }

    public void logout() throws Exception {
        // Clear local session immediately so UI can leave auth screens without waiting.
        String token = tokens.getToken();
        tokens.clear();
        if (token == null || token.isEmpty()) return;
        try {
            api.logout().execute();
        } catch (Exception ignored) {
            // Best-effort remote revoke.
        }
    }

    /** Remote revoke without touching a local store that was already cleared. */
    public void logoutRemoteOnly() {
        try {
            api.logout().execute();
        } catch (Exception ignored) {}
    }

    public void clearLocalSession() {
        tokens.clear();
    }

    public void updateProfile(@Nullable String name, @Nullable String locale, @Nullable String phone) throws Exception {
        java.util.Map<String, String> body = new java.util.HashMap<>();
        if (name != null && !name.trim().isEmpty()) body.put("name", name.trim());
        if (locale != null && !locale.trim().isEmpty()) body.put("locale", locale.trim());
        if (phone != null && !phone.trim().isEmpty()) body.put("phone", phone.trim());
        if (body.isEmpty()) return;
        Response<java.util.Map<String, Object>> r = api.updateProfile(body).execute();
        if (!r.isSuccessful()) {
            throw new Exception("Update profile failed: " + r.code());
        }
    }

    public void updatePassword(String current, String password, String confirm) throws Exception {
        java.util.Map<String,String> body = new java.util.HashMap<>();
        body.put("current_password", current != null ? current : "");
        body.put("new_password", password != null ? password : "");
        body.put("new_password_confirmation", confirm != null ? confirm : "");
        Response<java.util.Map<String,Boolean>> r = api.updatePassword(body).execute();
        if (!r.isSuccessful()) {
            throw new Exception("Update password failed: " + r.code());
        }
    }

    public void saveSecurityAnswer(String username, String answer) throws Exception {
        java.util.Map<String,String> body = new java.util.HashMap<>();
        body.put("username", username != null ? username : "");
        body.put("security_answer", answer != null ? answer : "");
        Response<java.util.Map<String,Boolean>> r = api.saveSecurityAnswer(body).execute();
        if (!r.isSuccessful()) {
            throw new Exception("Save security answer failed: " + r.code());
        }
    }

    public static class ForgotLookup {
        public boolean exists;
        public String name;
        public String account_hint;
        public String security_question;
        public boolean has_security_answer;
    }

    public @Nullable ForgotLookup forgotLookup(String username) throws Exception {
        java.util.Map<String, String> body = new java.util.HashMap<>();
        body.put("username", username != null ? username : "");
        Response<java.util.Map<String, Object>> r = api.forgotLookup(body).execute();
        if (r.code() == 404) {
            ForgotLookup miss = new ForgotLookup();
            miss.exists = false;
            return miss;
        }
        if (!r.isSuccessful() || r.body() == null) {
            throw new Exception("Lookup failed: " + r.code());
        }
        java.util.Map<String, Object> data = r.body();
        ForgotLookup out = new ForgotLookup();
        out.exists = Boolean.TRUE.equals(data.get("exists"));
        out.name = data.get("name") != null ? String.valueOf(data.get("name")) : null;
        out.account_hint = data.get("account_hint") != null ? String.valueOf(data.get("account_hint")) : null;
        out.security_question = data.get("security_question") != null ? String.valueOf(data.get("security_question")) : null;
        Object has = data.get("has_security_answer");
        out.has_security_answer = has instanceof Boolean ? (Boolean) has : Boolean.parseBoolean(String.valueOf(has));
        return out;
    }

    public void resetPasswordWithAnswer(String username, String answer, String password, String confirm) throws Exception {
        java.util.Map<String,String> body = new java.util.HashMap<>();
        body.put("username", username != null ? username : "");
        body.put("security_answer", answer != null ? answer : "");
        body.put("new_password", password != null ? password : "");
        body.put("new_password_confirmation", confirm != null ? confirm : "");
        Response<java.util.Map<String,Boolean>> r = api.resetPasswordWithAnswer(body).execute();
        if (!r.isSuccessful()) {
            throw new Exception("Reset password failed: " + r.code());
        }
    }

    public boolean hasToken() { return tokens.hasToken(); }

    public void refreshProfileIfNeeded() {
        if (!tokens.hasToken()) return;
        try {
            Response<User> response = api.me().execute();
            if (!response.isSuccessful()) {
                // Only a true unauthenticated response ends the session.
                // 403/404/HTML from OEM proxies must not bounce active users to login.
                if (response.code() == 401) {
                    tokens.clear();
                }
                return;
            }
            User user = response.body();
            if (user == null || user.id <= 0) {
                return;
            }
            if (user.role != null) tokens.saveRole(user.role);
            tokens.saveUserId(user.id);
            if (user.name != null) tokens.saveUserName(user.name);
            if (user.email != null) tokens.saveUserEmail(user.email);
        } catch (Throwable t) {
            // Network / parse glitches (common on HyperOS): keep the token.
        }
    }

    private void persistAuth(LoginResponse response){
        String token = response.resolveToken();
        if (token != null) {
            tokens.saveToken(token);
        }
        User user = response.resolveUser();
        if (user != null) {
            if (user.role != null) tokens.saveRole(user.role);
            if (user.id > 0) tokens.saveUserId(user.id);
            if (user.name != null) tokens.saveUserName(user.name);
            if (user.email != null) tokens.saveUserEmail(user.email);
        } else {
            tokens.clearRole();
        }
    }

    private AuthException buildAuthException(Response<?> response) {
        String body = null;
        try {
            if (response.errorBody() != null) {
                body = response.errorBody().string();
            }
        } catch (Exception ignored) {}

        String serverMessage = null;
        java.util.Map<String, java.util.List<String>> fieldErrors = null;
        if (body != null && !body.isEmpty()) {
            try {
                Gson g = new GsonBuilder().setLenient().create();
                ApiError err = g.fromJson(body, ApiError.class);
                if (err != null) {
                    serverMessage = err.message;
                    fieldErrors = err.errors;
                }
            } catch (Exception ignored) {}
            if (fieldErrors == null) {
                try {
                    com.google.gson.JsonObject root = com.google.gson.JsonParser.parseString(body).getAsJsonObject();
                    if (serverMessage == null && root.has("message") && root.get("message").isJsonPrimitive()) {
                        serverMessage = root.get("message").getAsString();
                    }
                    if (root.has("errors") && root.get("errors").isJsonObject()) {
                        fieldErrors = new java.util.LinkedHashMap<>();
                        com.google.gson.JsonObject errorsObj = root.getAsJsonObject("errors");
                        for (java.util.Map.Entry<String, com.google.gson.JsonElement> entry : errorsObj.entrySet()) {
                            java.util.List<String> list = new java.util.ArrayList<>();
                            com.google.gson.JsonElement value = entry.getValue();
                            if (value != null && value.isJsonArray()) {
                                for (com.google.gson.JsonElement item : value.getAsJsonArray()) {
                                    if (item != null && item.isJsonPrimitive()) {
                                        list.add(item.getAsString());
                                    }
                                }
                            } else if (value != null && value.isJsonPrimitive()) {
                                list.add(value.getAsString());
                            }
                            if (!list.isEmpty()) {
                                fieldErrors.put(entry.getKey(), list);
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        return new AuthException(response.code(), body, serverMessage, fieldErrors);
    }

    private static class ApiError {
        String message;
        java.util.Map<String, java.util.List<String>> errors;
    }
}
