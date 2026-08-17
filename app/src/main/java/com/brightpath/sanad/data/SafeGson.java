package com.brightpath.sanad.data;

import com.brightpath.sanad.models.User;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

/**
 * Lenient Gson so null/string numbers from the API cannot crash screens
 * on any OEM (Xiaomi/Samsung/Huawei) or Android version.
 */
public final class SafeGson {
    private static final Gson INSTANCE = build();

    private SafeGson() {}

    public static Gson get() {
        return INSTANCE;
    }

    private static Gson build() {
        return new GsonBuilder()
                .setLenient()
                .registerTypeAdapter(User.class, new User.Deserializer())
                .registerTypeAdapter(int.class, new IntAdapter())
                .registerTypeAdapter(boolean.class, new BooleanAdapter())
                .registerTypeAdapter(double.class, new DoubleAdapter())
                .registerTypeAdapter(long.class, new LongAdapter())
                .create();
    }

    private static final class IntAdapter implements JsonDeserializer<Integer> {
        @Override
        public Integer deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            if (json == null || json.isJsonNull() || !json.isJsonPrimitive()) return 0;
            try {
                if (json.getAsJsonPrimitive().isBoolean()) {
                    return json.getAsBoolean() ? 1 : 0;
                }
                if (json.getAsJsonPrimitive().isNumber()) {
                    return json.getAsInt();
                }
                String raw = json.getAsString();
                if (raw == null || raw.isEmpty() || "null".equalsIgnoreCase(raw)) return 0;
                return (int) Double.parseDouble(raw.trim());
            } catch (Exception ignored) {
                return 0;
            }
        }
    }

    private static final class LongAdapter implements JsonDeserializer<Long> {
        @Override
        public Long deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            if (json == null || json.isJsonNull() || !json.isJsonPrimitive()) return 0L;
            try {
                if (json.getAsJsonPrimitive().isNumber()) {
                    return json.getAsLong();
                }
                String raw = json.getAsString();
                if (raw == null || raw.isEmpty()) return 0L;
                return (long) Double.parseDouble(raw.trim());
            } catch (Exception ignored) {
                return 0L;
            }
        }
    }

    private static final class DoubleAdapter implements JsonDeserializer<Double> {
        @Override
        public Double deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            if (json == null || json.isJsonNull() || !json.isJsonPrimitive()) return 0d;
            try {
                if (json.getAsJsonPrimitive().isNumber()) {
                    return json.getAsDouble();
                }
                String raw = json.getAsString();
                if (raw == null || raw.isEmpty()) return 0d;
                return Double.parseDouble(raw.trim());
            } catch (Exception ignored) {
                return 0d;
            }
        }
    }

    private static final class BooleanAdapter implements JsonDeserializer<Boolean> {
        @Override
        public Boolean deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            if (json == null || json.isJsonNull() || !json.isJsonPrimitive()) return false;
            try {
                if (json.getAsJsonPrimitive().isBoolean()) return json.getAsBoolean();
                if (json.getAsJsonPrimitive().isNumber()) return json.getAsInt() != 0;
                String raw = json.getAsString();
                if (raw == null) return false;
                raw = raw.trim().toLowerCase();
                return "1".equals(raw) || "true".equals(raw) || "yes".equals(raw);
            } catch (Exception ignored) {
                return false;
            }
        }
    }
}
