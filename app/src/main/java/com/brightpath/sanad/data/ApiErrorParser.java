package com.brightpath.sanad.data;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.brightpath.sanad.R;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class ApiErrorParser {
    private ApiErrorParser() {}

    public static final class ParsedError {
        public final String code;
        public final String message;

        public ParsedError(String code, String message) {
            this.code = code;
            this.message = message;
        }
    }

    public static ParsedError parseDetailed(@NonNull Context context, @StringRes int fallbackRes, String raw, int httpCode) {
        String fallback = context.getString(fallbackRes);
        String code = null;
        if (TextUtils.isEmpty(raw)) {
            return new ParsedError(null, fallback);
        }

        String trimmed = raw.trim();
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            code = trimmed;
            String mapped = mapKnownCode(context, code);
            return new ParsedError(code, mapped != null ? mapped : trimmed);
        }

        try {
            JsonObject json = JsonParser.parseString(trimmed).getAsJsonObject();
            code = firstNonEmpty(json, "message", "error", "msg");
            String mapped = mapKnownCode(context, code);
            if (mapped != null) {
                return new ParsedError(code, mapped);
            }
            if (!TextUtils.isEmpty(code) && !looksLikeJson(code)) {
                return new ParsedError(code, code);
            }
            String validation = firstValidationError(json);
            if (!TextUtils.isEmpty(validation)) {
                return new ParsedError(code, validation);
            }
        } catch (Exception ignored) {
            return new ParsedError(code, fallback);
        }

        if (httpCode == 402) {
            code = "insufficient_points";
            String mapped = mapKnownCode(context, code);
            if (mapped != null) {
                return new ParsedError(code, mapped);
            }
        }

        return new ParsedError(code, fallback);
    }

    public static String parse(@NonNull Context context, @StringRes int fallbackRes, String raw) {
        return parse(context, fallbackRes, raw, 0);
    }

    public static String parse(@NonNull Context context, @StringRes int fallbackRes, String raw, int httpCode) {
        return parseDetailed(context, fallbackRes, raw, httpCode).message;
    }

    private static String mapKnownCode(@NonNull Context context, String code) {
        if (TextUtils.isEmpty(code)) {
            return null;
        }
        switch (code.trim()) {
            case "intake_required":
                return context.getString(R.string.book_session_error_intake_required);
            case "pre_session_required":
                return context.getString(R.string.book_session_error_pre_session_required);
            case "insufficient_points":
                return context.getString(R.string.book_session_error_insufficient_points);
            default:
                return null;
        }
    }

    private static String firstNonEmpty(JsonObject json, String... keys) {
        for (String key : keys) {
            if (!json.has(key) || json.get(key).isJsonNull()) {
                continue;
            }
            String value = json.get(key).getAsString();
            if (!TextUtils.isEmpty(value)) {
                return value;
            }
        }
        return null;
    }

    private static String firstValidationError(JsonObject json) {
        if (!json.has("errors") || !json.get("errors").isJsonObject()) {
            return null;
        }
        JsonObject errors = json.getAsJsonObject("errors");
        for (String key : errors.keySet()) {
            JsonElement value = errors.get(key);
            if (value != null && value.isJsonArray()) {
                JsonArray array = value.getAsJsonArray();
                if (array.size() > 0 && !array.get(0).isJsonNull()) {
                    return array.get(0).getAsString();
                }
            }
        }
        return null;
    }

    private static boolean looksLikeJson(String value) {
        String trimmed = value.trim();
        return trimmed.startsWith("{") || trimmed.startsWith("[");
    }
}
