package com.brightpath.sanad.ui;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.brightpath.sanad.R;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Maps raw intake / session API tokens to localized display labels. */
public final class IntakeLabelHelper {

    private IntakeLabelHelper() {}

    @NonNull
    public static String duration(@NonNull Context ctx, @Nullable String raw) {
        if (TextUtils.isEmpty(raw)) return "-";
        String v = raw.trim().toLowerCase(Locale.ROOT);
        switch (v) {
            case "less_3":
            case "less_3m":
            case "lt_month":
            case "< شهر":
                return ctx.getString(R.string.patient_intake_duration_less3);
            case "more_3":
            case "more_3m":
            case "1_3":
            case "1_3_months":
                return ctx.getString(R.string.patient_intake_duration_more3);
            case "year":
            case "more_year":
            case "gt_6":
            case "gt_6_months":
                return ctx.getString(R.string.patient_intake_duration_year);
            default:
                return raw.trim();
        }
    }

    @NonNull
    public static String symptomOrTag(@NonNull Context ctx, @Nullable String raw) {
        if (TextUtils.isEmpty(raw)) return "-";
        String v = raw.trim().toLowerCase(Locale.ROOT);
        switch (v) {
            case "anxiety":
            case "قلق":
                return ctx.getString(R.string.fragment_patient_intake_text_1);
            case "depression":
            case "اكتئاب":
                return ctx.getString(R.string.fragment_patient_intake_text_2);
            case "need_med":
            case "medication":
                return ctx.getString(R.string.fragment_patient_intake_text_3);
            case "behavior":
            case "قبول السلوك":
                return ctx.getString(R.string.fragment_patient_intake_text_4);
            case "sleep":
            case "insomnia":
                return ctx.getString(R.string.fragment_patient_intake_text_5);
            case "attention":
            case "low_focus":
            case "تشتت انتباه":
                return ctx.getString(R.string.fragment_patient_intake_text_6);
            case "stress":
            case "توتر":
                return ctx.getString(R.string.intake_label_stress);
            case "bipolar":
                return ctx.getString(R.string.fragment_patient_intake_text_9);
            case "anx_dep":
                return ctx.getString(R.string.fragment_patient_intake_text_10);
            case "schizophrenia":
                return ctx.getString(R.string.fragment_patient_intake_text_11);
            case "children":
                return ctx.getString(R.string.fragment_patient_intake_text_12);
            case "mild":
                return ctx.getString(R.string.fragment_patient_intake_text_13);
            case "identity":
                return ctx.getString(R.string.fragment_patient_intake_text_14);
            default:
                return raw.trim();
        }
    }

    @NonNull
    public static String joinTokens(@NonNull Context ctx, @Nullable List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) return "-";
        List<String> out = new ArrayList<>();
        for (String t : tokens) {
            if (TextUtils.isEmpty(t)) continue;
            out.add(symptomOrTag(ctx, t));
        }
        return out.isEmpty() ? "-" : TextUtils.join(" · ", out);
    }

    @NonNull
    public static String sessionStatus(@NonNull Context ctx, @Nullable String raw) {
        if (TextUtils.isEmpty(raw)) return ctx.getString(R.string.session_status_unknown);
        switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "pending":
                return ctx.getString(R.string.session_status_pending);
            case "accepted":
            case "confirmed":
            case "scheduled":
            case "upcoming":
            case "in_progress":
            case "started":
                return ctx.getString(R.string.session_status_upcoming);
            case "completed":
            case "done":
                return ctx.getString(R.string.session_status_completed);
            case "rejected":
                return ctx.getString(R.string.session_status_rejected);
            case "cancelled":
            case "canceled":
                return ctx.getString(R.string.session_status_cancelled);
            default:
                return raw.trim();
        }
    }

    @NonNull
    public static String formatDate(@Nullable String iso) {
        if (TextUtils.isEmpty(iso)) return "-";
        try {
            Instant instant;
            try {
                instant = OffsetDateTime.parse(iso).toInstant();
            } catch (DateTimeParseException e) {
                instant = Instant.parse(iso);
            }
            DateTimeFormatter fmt = DateTimeFormatter
                    .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                    .withLocale(Locale.getDefault())
                    .withZone(ZoneId.systemDefault());
            return fmt.format(instant);
        } catch (Exception e) {
            return iso;
        }
    }
}
