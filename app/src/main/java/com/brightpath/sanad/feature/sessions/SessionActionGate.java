package com.brightpath.sanad.feature.sessions;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Shared session CTA rules for patient + specialist (accept / reject / join / complete)
 * with a unified T−5 early-join window.
 */
public final class SessionActionGate {

    public static final long EARLY_JOIN_MS = 5L * 60L * 1000L;

    public enum Phase {
        PENDING,
        WAITING_WINDOW,
        JOINABLE,
        IN_PROGRESS,
        COMPLETED,
        REJECTED,
        CANCELLED,
        UNKNOWN
    }

    public final Phase phase;
    public final boolean canJoin;
    public final boolean canAccept;
    public final boolean canReject;
    public final boolean canComplete;
    public final boolean canCancel;
    /** String resource hint key name used by callers (map in UI). */
    public final String joinHintKey;
    public final long millisUntilJoin;

    private SessionActionGate(
            Phase phase,
            boolean canJoin,
            boolean canAccept,
            boolean canReject,
            boolean canComplete,
            boolean canCancel,
            String joinHintKey,
            long millisUntilJoin
    ) {
        this.phase = phase;
        this.canJoin = canJoin;
        this.canAccept = canAccept;
        this.canReject = canReject;
        this.canComplete = canComplete;
        this.canCancel = canCancel;
        this.joinHintKey = joinHintKey;
        this.millisUntilJoin = millisUntilJoin;
    }

    @NonNull
    public static SessionActionGate evaluate(
            @Nullable String status,
            @Nullable String scheduledAtIso,
            boolean isSpecialist
    ) {
        return evaluate(status, parseMillis(scheduledAtIso), isSpecialist, System.currentTimeMillis());
    }

    @NonNull
    public static SessionActionGate evaluate(
            @Nullable String status,
            long scheduledAtMs,
            boolean isSpecialist,
            long nowMs
    ) {
        String key = status == null ? "" : status.toLowerCase();
        boolean closedRejected = key.contains("rejected");
        boolean closedCancelled = key.contains("cancel");
        boolean closedCompleted = key.contains("completed");
        boolean pending = key.contains("pending") || key.contains("requested");
        boolean inProgress = key.contains("in_progress") || key.contains("started");
        boolean acceptedLike = key.contains("accepted")
                || key.contains("confirmed")
                || key.contains("scheduled")
                || key.contains("upcoming")
                || inProgress;

        if (closedCompleted) {
            return new SessionActionGate(Phase.COMPLETED, false, false, false, false, false,
                    "session_join_unavailable", 0);
        }
        if (closedRejected) {
            return new SessionActionGate(Phase.REJECTED, false, false, false, false, false,
                    "session_join_unavailable", 0);
        }
        if (closedCancelled) {
            return new SessionActionGate(Phase.CANCELLED, false, false, false, false, false,
                    "session_join_unavailable", 0);
        }
        if (pending) {
            return new SessionActionGate(Phase.PENDING, false, isSpecialist, isSpecialist, false, !isSpecialist,
                    "session_join_wait_accept", 0);
        }

        boolean withinWindow;
        long millisUntil = 0L;
        if (scheduledAtMs > 0) {
            long openAt = scheduledAtMs - EARLY_JOIN_MS;
            withinWindow = nowMs >= openAt;
            millisUntil = Math.max(0L, openAt - nowMs);
        } else {
            withinWindow = true;
        }

        if (inProgress) {
            return new SessionActionGate(Phase.IN_PROGRESS, true, false, false, isSpecialist, false,
                    "session_join_available_now", 0);
        }
        if (acceptedLike) {
            if (withinWindow) {
                return new SessionActionGate(Phase.JOINABLE, true, false, false, isSpecialist, !isSpecialist,
                        "session_join_available_now", 0);
            }
            return new SessionActionGate(Phase.WAITING_WINDOW, false, false, false, false, !isSpecialist,
                    "session_join_wait", millisUntil);
        }
        return new SessionActionGate(Phase.UNKNOWN, false, false, false, false, false,
                "session_join_unavailable", 0);
    }

    public static long parseMillis(@Nullable String raw) {
        if (raw == null || raw.trim().isEmpty()) return -1L;
        try {
            return Instant.parse(raw).toEpochMilli();
        } catch (DateTimeParseException ignored) {}
        try {
            return OffsetDateTime.parse(raw, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant().toEpochMilli();
        } catch (DateTimeParseException ignored) {}
        try {
            // "yyyy-MM-dd HH:mm" style used by some specialist APIs
            String normalized = raw.trim().replace(' ', 'T');
            if (!normalized.endsWith("Z") && !normalized.contains("+")) {
                normalized = normalized + ":00Z";
            }
            return Instant.parse(normalized).toEpochMilli();
        } catch (Exception ignored) {}
        return -1L;
    }

    @NonNull
    public static String normalizeBucket(@Nullable String status) {
        String key = status == null ? "" : status.toLowerCase();
        if (key.contains("pending") || key.contains("requested")) return "pending";
        if (key.contains("completed")) return "completed";
        if (key.contains("canceled") || key.contains("cancelled") || key.contains("rejected")) return "canceled";
        if (key.contains("scheduled") || key.contains("upcoming") || key.contains("confirmed")
                || key.contains("in_progress") || key.contains("started") || key.contains("accepted")) {
            return "accepted";
        }
        return "pending";
    }
}
