package com.brightpath.sanad.data.auth;

import android.content.Context;

import com.brightpath.sanad.data.AppConfig;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Validates a cached token before opening authenticated UI.
 * Deleted DB users get 401 → token cleared → caller should open Login.
 *
 * Single-flight: Splash + Main share one /me call.
 * Never interrupts OkHttp (avoids InterruptedIOException noise and races).
 */
public final class SessionGuard {
    private static final Object LOCK = new Object();
    private static CountDownLatch inFlight;
    private static volatile long lastValidatedAtMs;

    private SessionGuard() {}

    /**
     * Blocks the calling thread up to {@code budgetMs} while checking /me.
     * Safe to call from a background thread. Never throws.
     *
     * @return true if a usable token remains after the check
     */
    public static boolean validateBlocking(Context context, long budgetMs) {
        Context app = context.getApplicationContext();
        TokenStore tokens = new TokenStore(app);
        if (!tokens.hasToken()) return false;

        // Recent successful validation — skip another round trip.
        if (System.currentTimeMillis() - lastValidatedAtMs < 8_000L && tokens.hasToken()) {
            return true;
        }

        CountDownLatch latch;
        final AtomicReference<Boolean> keep = new AtomicReference<>(null);
        synchronized (LOCK) {
            if (inFlight != null) {
                latch = inFlight;
            } else {
                latch = new CountDownLatch(1);
                inFlight = latch;
                final CountDownLatch mine = latch;
                new Thread(() -> {
                    try {
                        new AuthRepository(app, AppConfig.BASE_URL).refreshProfileIfNeeded();
                    } catch (Throwable ignored) {
                    } finally {
                        boolean has = new TokenStore(app).hasToken();
                        keep.set(has);
                        if (has) {
                            lastValidatedAtMs = System.currentTimeMillis();
                        } else {
                            lastValidatedAtMs = 0L;
                        }
                        synchronized (LOCK) {
                            if (inFlight == mine) {
                                inFlight = null;
                            }
                        }
                        mine.countDown();
                    }
                }, "session-guard").start();
            }
        }

        try {
            // Wait only — do NOT interrupt the OkHttp thread on timeout.
            latch.await(Math.max(500L, budgetMs), TimeUnit.MILLISECONDS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }

        Boolean result = keep.get();
        if (result != null) return result;
        // Timed out while request still running: keep token for offline; in-flight /me
        // will clear + notify SessionListener if the account is gone.
        return new TokenStore(app).hasToken();
    }

    /** After a successful login so Main does not immediately re-hit /me. */
    public static void markFresh() {
        lastValidatedAtMs = System.currentTimeMillis();
    }

    /** Call after logout / deleted-session clear. */
    public static void invalidateCache() {
        lastValidatedAtMs = 0L;
    }
}
