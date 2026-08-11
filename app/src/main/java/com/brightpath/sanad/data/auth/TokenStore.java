package com.brightpath.sanad.data.auth;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Local session store. Clearing the token notifies {@link #setSessionListener} so the UI
 * can leave to Login immediately when a deleted/expired account is detected.
 */
public class TokenStore {
    public interface SessionListener {
        void onSessionCleared();
    }

    private static volatile SessionListener sessionListener;

    private static final String PREF = "sanad_prefs";
    private static final String KEY = "access_token";
    private static final String KEY_ROLE = "user_role";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private final SharedPreferences sp;

    public TokenStore(Context context) {
        sp = context.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static void setSessionListener(SessionListener listener) {
        sessionListener = listener;
    }

    public void saveToken(String token) {
        if (token == null) {
            sp.edit().remove(KEY).apply();
            return;
        }
        String clean = token.trim();
        if (clean.isEmpty() || "null".equalsIgnoreCase(clean)) {
            sp.edit().remove(KEY).apply();
            return;
        }
        sp.edit().putString(KEY, clean).apply();
    }
    public String getToken() {
        String t = sp.getString(KEY, null);
        if (t == null) { return null; }
        String clean = t.trim();
        if (clean.isEmpty() || "null".equalsIgnoreCase(clean)) { return null; }
        return clean;
    }
    public void saveRole(String role) { sp.edit().putString(KEY_ROLE, role).apply(); }
    public String getRole() { return sp.getString(KEY_ROLE, null); }
    public void saveUserName(String name) { sp.edit().putString(KEY_USER_NAME, name).apply(); }
    public String getUserName() { return sp.getString(KEY_USER_NAME, null); }
    public void saveUserEmail(String email) { sp.edit().putString(KEY_USER_EMAIL, email).apply(); }
    public String getUserEmail() { return sp.getString(KEY_USER_EMAIL, null); }
    public void saveUserId(int id) { sp.edit().putInt(KEY_USER_ID, id).apply(); }
    public int getUserId() { return sp.getInt(KEY_USER_ID, 0); }

    public void clear() {
        boolean hadToken = hasToken();
        sp.edit()
                .remove(KEY)
                .remove(KEY_ROLE)
                .remove(KEY_USER_ID)
                .remove(KEY_USER_NAME)
                .remove(KEY_USER_EMAIL)
                .apply();
        try {
            SessionGuard.invalidateCache();
        } catch (Throwable ignored) {}
        if (hadToken) {
            SessionListener listener = sessionListener;
            if (listener != null) {
                try {
                    listener.onSessionCleared();
                } catch (Throwable ignored) {}
            }
        }
    }

    public void clearRole() { sp.edit().remove(KEY_ROLE).apply(); }
    public boolean hasToken() { return getToken() != null; }
}
