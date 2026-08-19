package com.brightpath.sanad.router;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;

import com.brightpath.sanad.R;

/**
 * Language/theme recreate used to dump the user on Home because RoleBoot
 * always navigates to the role start tab. Remember the current destination
 * for a few seconds and restore it.
 */
public final class NavRestore {
    private static final String PREF = "nav_restore";
    private static final String KEY_DEST = "pending_dest";
    private static final String KEY_AT = "pending_at";
    private static final long TTL_MS = 20_000L;

    private NavRestore() {}

    public static void remember(Activity activity) {
        if (activity == null) return;
        try {
            if (!(activity instanceof AppCompatActivity)) return;
            Fragment host = ((AppCompatActivity) activity)
                    .getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment);
            if (!(host instanceof NavHostFragment)) return;
            NavDestination dest = ((NavHostFragment) host).getNavController().getCurrentDestination();
            if (dest == null) return;
            rememberDest(activity, dest.getId());
        } catch (Throwable ignored) {}
    }

    /** Persist last screen so a crash restart can reopen Profile instead of Home. */
    public static void rememberDest(Context context, @IdRes int destId) {
        if (context == null || destId == 0 || destId == R.id.roleBootFragment) return;
        try {
            context.getApplicationContext()
                    .getSharedPreferences(PREF, Context.MODE_PRIVATE)
                    .edit()
                    .putInt(KEY_DEST, destId)
                    .putLong(KEY_AT, System.currentTimeMillis())
                    .commit();
        } catch (Throwable ignored) {}
    }

    @IdRes
    public static int takePending(Context context, @IdRes int fallback) {
        if (context == null) return fallback;
        try {
            SharedPreferences prefs = context.getApplicationContext()
                    .getSharedPreferences(PREF, Context.MODE_PRIVATE);
            int dest = prefs.getInt(KEY_DEST, 0);
            long at = prefs.getLong(KEY_AT, 0L);
            prefs.edit().remove(KEY_DEST).remove(KEY_AT).apply();
            if (dest != 0 && System.currentTimeMillis() - at < TTL_MS) {
                return dest;
            }
        } catch (Throwable ignored) {}
        return fallback;
    }
}
