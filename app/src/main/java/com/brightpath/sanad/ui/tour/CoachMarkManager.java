package com.brightpath.sanad.ui.tour;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows one-time educational tips. Skip/Next never crash the host Activity.
 */
public class CoachMarkManager {
    private static final String PREF = "coach_marks";
    private static final long SHOW_DELAY_MS = 450L;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static Runnable pendingShow;
    private static CoachMarkOverlay activeOverlay;
    private static int sessionGeneration;

    private CoachMarkManager() {}

    public static void showIfNeeded(@NonNull Fragment fragment, String key, List<CoachMarkStep> steps) {
        if (!fragment.isAdded() || fragment.getView() == null) return;
        if (!fragment.isResumed()) {
            // Defer until the fragment is interactive — avoids tips during recreate.
            View root = fragment.getView();
            if (root != null) {
                root.post(() -> {
                    if (fragment.isAdded() && fragment.isResumed()) {
                        showIfNeeded(fragment, key, steps);
                    }
                });
            }
            return;
        }
        Activity activity = fragment.getActivity();
        if (!isActivityUsable(activity)) return;
        if (fragment.isRemoving() || fragment.isDetached()) return;
        showIfNeeded(activity, key, steps);
    }

    public static void showIfNeeded(Activity activity, String key, List<CoachMarkStep> steps) {
        if (!isActivityUsable(activity) || key == null || steps == null || steps.isEmpty()) return;
        if (isXiaomiFamily()) {
            // Overlay tips crash some HyperOS GPUs (Redmi Note 14 etc.) and recover to Login.
            markSeen(activity, key);
            return;
        }
        SharedPreferences prefs;
        try {
            prefs = activity.getSharedPreferences(PREF, Context.MODE_PRIVATE);
            if (prefs.getBoolean(key, false)) return;
        } catch (Throwable t) {
            return;
        }

        List<CoachMarkStep> clean = new ArrayList<>();
        for (CoachMarkStep s : steps) {
            if (s != null && s.target != null) clean.add(s);
        }
        if (clean.isEmpty()) return;

        cancelPending();
        dismissActiveQuietly();

        final Activity host = activity;
        final SharedPreferences prefsFinal = prefs;
        final int gen = ++sessionGeneration;
        pendingShow = () -> {
            pendingShow = null;
            if (gen != sessionGeneration) return;
            if (!isActivityUsable(host)) return;
            try {
                if (prefsFinal.getBoolean(key, false)) return;
            } catch (Throwable t) {
                return;
            }

            try {
                ViewGroup decor = (ViewGroup) host.getWindow().getDecorView();
                CoachMarkOverlay overlay = new CoachMarkOverlay(host);
                activeOverlay = overlay;
                decor.addView(overlay);

                final int[] index = {0};
                Runnable bind = () -> {
                    try {
                        if (gen != sessionGeneration) return;
                        if (!isActivityUsable(host) || activeOverlay != overlay) return;
                        if (index[0] < 0 || index[0] >= clean.size()) {
                            markSeenAndRemove(prefsFinal, key, overlay);
                            return;
                        }
                        overlay.resetFinishingForNextStep();
                        CoachMarkStep step = clean.get(index[0]);
                        if (step == null || step.target == null || !step.target.isAttachedToWindow()) {
                            markSeenAndRemove(prefsFinal, key, overlay);
                            return;
                        }
                        overlay.bindStep(step, index[0] == clean.size() - 1);
                    } catch (Throwable t) {
                        markSeenAndRemove(prefsFinal, key, overlay);
                    }
                };

                overlay.setListener(new CoachMarkOverlay.Listener() {
                    @Override public void onNext() {
                        try {
                            if (index[0] < clean.size() - 1) {
                                index[0] += 1;
                                View target = clean.get(index[0]).target;
                                if (target != null) {
                                    target.post(bind);
                                } else {
                                    MAIN.post(bind);
                                }
                            } else {
                                markSeenAndRemove(prefsFinal, key, overlay);
                            }
                        } catch (Throwable t) {
                            markSeenAndRemove(prefsFinal, key, overlay);
                        }
                    }

                    @Override public void onSkip() {
                        markSeenAndRemove(prefsFinal, key, overlay);
                    }
                });

                View first = clean.get(0).target;
                if (first != null) {
                    first.post(bind);
                } else {
                    MAIN.post(bind);
                }
            } catch (Throwable t) {
                try {
                    prefsFinal.edit().putBoolean(key, true).apply();
                } catch (Throwable ignored) {}
                dismissActiveQuietly();
            }
        };
        MAIN.postDelayed(pendingShow, SHOW_DELAY_MS);
    }

    /** Mark a tip key as seen without showing it. */
    public static void markSeen(Context context, String key) {
        if (context == null || key == null) return;
        try {
            context.getApplicationContext()
                    .getSharedPreferences(PREF, Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(key, true)
                    .apply();
        } catch (Throwable ignored) {}
    }

    /** Dismiss any visible tip (e.g. Activity/Fragment teardown). */
    public static void dismissActive() {
        sessionGeneration++;
        cancelPending();
        dismissActiveQuietly();
    }

    private static void markSeenAndRemove(SharedPreferences prefs, String key, CoachMarkOverlay overlay) {
        try {
            prefs.edit().putBoolean(key, true).apply();
        } catch (Throwable ignored) {}
        if (activeOverlay == overlay) {
            activeOverlay = null;
        }
        try {
            if (overlay != null) {
                overlay.prepareForDetach();
                overlay.setVisibility(View.GONE);
            }
        } catch (Throwable ignored) {}
        // Double-post: wait until after the Skip click + current frame fully unwind.
        // Removing mid-dispatch still crashes some HyperOS builds.
        MAIN.post(() -> MAIN.post(() -> removeOverlay(overlay)));
    }

    private static void removeOverlay(View overlay) {
        if (overlay == null) return;
        try {
            if (overlay instanceof CoachMarkOverlay) {
                ((CoachMarkOverlay) overlay).prepareForDetach();
            }
        } catch (Throwable ignored) {}
        try {
            overlay.setOnClickListener(null);
            ViewParent parent = overlay.getParent();
            if (parent instanceof ViewGroup) {
                ((ViewGroup) parent).removeView(overlay);
            }
        } catch (Throwable ignored) {}
    }

    private static void dismissActiveQuietly() {
        CoachMarkOverlay overlay = activeOverlay;
        activeOverlay = null;
        if (overlay == null) return;
        try {
            overlay.prepareForDetach();
            overlay.setVisibility(android.view.View.GONE);
        } catch (Throwable ignored) {}
        // Always defer removal — callers may be mid click / recreate / language change.
        MAIN.post(() -> MAIN.post(() -> removeOverlay(overlay)));
    }

    private static void cancelPending() {
        if (pendingShow != null) {
            MAIN.removeCallbacks(pendingShow);
            pendingShow = null;
        }
    }

    public static CoachMarkStep step(View target, int titleRes, int descRes) {
        return new CoachMarkStep(target, titleRes, descRes);
    }

    private static boolean isActivityUsable(Activity activity) {
        if (activity == null) return false;
        try {
            return !activity.isFinishing() && !activity.isDestroyed();
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean isXiaomiFamily() {
        try {
            String manufacturer = Build.MANUFACTURER != null ? Build.MANUFACTURER.toLowerCase() : "";
            String brand = Build.BRAND != null ? Build.BRAND.toLowerCase() : "";
            return manufacturer.contains("xiaomi")
                    || manufacturer.contains("redmi")
                    || manufacturer.contains("poco")
                    || brand.contains("xiaomi")
                    || brand.contains("redmi")
                    || brand.contains("poco");
        } catch (Throwable t) {
            return false;
        }
    }
}
