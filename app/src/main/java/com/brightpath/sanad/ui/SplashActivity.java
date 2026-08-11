package com.brightpath.sanad.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.splashscreen.SplashScreen;

import com.brightpath.sanad.R;
import com.brightpath.sanad.data.ThemeStore;
import com.brightpath.sanad.data.auth.SessionGuard;
import com.brightpath.sanad.data.auth.TokenStore;
import com.brightpath.sanad.push.PushRegistrar;

/**
 * Splash validates a cached token before entering Main.
 * Deleted DB users get 401 → token cleared → Login (no crash).
 * Network check is budgeted so cold start stays responsive.
 * Visual continuity: OS SplashScreen API + matching light canvas layout.
 */
public class SplashActivity extends AppCompatActivity {

    private static final long MIN_SPLASH_MS = 350L;
    private static final long AUTH_BUDGET_MS = 4500L;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile boolean contentReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        splashScreen.setKeepOnScreenCondition(() -> !contentReady);

        super.onCreate(savedInstanceState);
        try {
            new ThemeStore(this).applySavedTheme(this);
        } catch (Throwable ignored) {}
        try {
            setContentView(R.layout.activity_splash);
        } catch (Throwable t) {
            try {
                setContentView(new android.widget.FrameLayout(this));
            } catch (Throwable ignored) {}
        }

        int chrome = ThemeStore.chromeStatusBarColor(this);
        try {
            android.view.View content = findViewById(android.R.id.content);
            if (content != null) content.setBackgroundColor(chrome);
            android.view.View splashRoot = findViewById(R.id.splashRoot);
            if (splashRoot != null) {
                splashRoot.setBackgroundColor(chrome);
            }
        } catch (Throwable ignored) {}
        ThemeStore.applyLightSystemBars(this);

        ImageView logo = findViewById(R.id.imgSplashLogo);
        if (logo != null) {
            try {
                logo.setImageResource(new ThemeStore(this).getLogoRes(true));
            } catch (OutOfMemoryError | Exception e) {
                try {
                    logo.setImageResource(R.drawable.logobluenotbackgraound);
                } catch (Throwable ignored) {}
            }
        }
        TextView title = findViewById(R.id.txtSplashTitle);
        if (title != null) {
            try {
                title.setTextColor(ThemeStore.primaryColor(this));
            } catch (Throwable ignored) {
                title.setTextColor(ContextCompat.getColor(this, R.color.sanad_blue_primary));
            }
        }

        contentReady = true;

        final Context appCtx = getApplicationContext();
        final long startedAt = System.currentTimeMillis();
        final Bundle pushExtras = getIntent() != null ? getIntent().getExtras() : null;
        final String pushType = firstExtra(getIntent(), PushRegistrar.EXTRA_PUSH_TYPE, "type");
        final String sessionId = firstExtra(getIntent(), PushRegistrar.EXTRA_SESSION_ID, "session_id");
        final String specialistId = firstExtra(getIntent(), PushRegistrar.EXTRA_SPECIALIST_ID, "specialist_id");

        new Thread(() -> {
            TokenStore tokenStore = new TokenStore(appCtx);
            if (tokenStore.hasToken()) {
                validateSessionBudgeted(appCtx);
            }
            // Re-read after validation — deleted accounts must go to Login.
            tokenStore = new TokenStore(appCtx);
            final Intent next = resolveNextIntent(appCtx, tokenStore, pushExtras, pushType, sessionId, specialistId);
            long elapsed = System.currentTimeMillis() - startedAt;
            long delay = Math.max(0L, MIN_SPLASH_MS - elapsed);
            mainHandler.postDelayed(() -> {
                if (isFinishing() || isDestroyed()) return;
                try {
                    startActivity(next);
                } catch (Throwable ignored) {
                    try {
                        startActivity(new Intent(SplashActivity.this, LoginActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    } catch (Throwable ignored2) {}
                }
                finish();
            }, delay);
        }, "splash-auth-check").start();
    }

    /** Clears stale/deleted sessions; never blocks longer than {@link #AUTH_BUDGET_MS}. */
    private void validateSessionBudgeted(Context appCtx) {
        try {
            SessionGuard.validateBlocking(appCtx, AUTH_BUDGET_MS);
        } catch (Throwable ignored) {}
    }

    private Intent resolveNextIntent(
            Context appCtx,
            TokenStore tokenStore,
            Bundle pushExtras,
            String pushType,
            String sessionId,
            String specialistId
    ) {
        Intent target;
        if (tokenStore.hasToken()) {
            if (!isOnboardingDone()) {
                target = new Intent(appCtx, OnboardingActivity.class);
            } else {
                target = new Intent(appCtx, MainActivity.class);
            }
        } else {
            target = new Intent(appCtx, LoginActivity.class);
        }
        target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (pushExtras != null) {
            try {
                target.putExtras(pushExtras);
            } catch (Throwable ignored) {}
        }
        if (!TextUtils.isEmpty(pushType)) {
            target.putExtra(PushRegistrar.EXTRA_PUSH_TYPE, pushType);
        }
        if (sessionId != null) {
            target.putExtra(PushRegistrar.EXTRA_SESSION_ID, sessionId);
        }
        if (specialistId != null) {
            target.putExtra(PushRegistrar.EXTRA_SPECIALIST_ID, specialistId);
        }
        return target;
    }

    private static String firstExtra(Intent source, String primary, String fallback) {
        if (source == null) return null;
        String value = source.getStringExtra(primary);
        if (TextUtils.isEmpty(value)) {
            value = source.getStringExtra(fallback);
        }
        if (TextUtils.isEmpty(value) && source.hasExtra(primary)) {
            try {
                value = String.valueOf(source.getIntExtra(primary, -1));
                if ("-1".equals(value)) value = null;
            } catch (Exception ignored) {
                value = null;
            }
        }
        return value;
    }

    private boolean isOnboardingDone() {
        return getSharedPreferences("onboarding_prefs", MODE_PRIVATE)
                .getBoolean("onboarding_done", false);
    }
}
