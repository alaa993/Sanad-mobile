package com.brightpath.sanad.models;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

/**
 * Auth/profile user. Parsed leniently so Xiaomi/HyperOS (and schema drift)
 * cannot turn a 200 response into a forced logout.
 */
public class User {
    public int id;
    public String name;
    public String email;
    public String phone;
    public String locale;
    public String role;
    public String approval_status;
    public String organization_status;
    public String org_rejection_reason;
    public OrgProfile org_profile;

    public static class OrgProfile {
        public Integer id;
        public String name;
        public String status;
        public String review_notes;
        public String about;
        public Integer members;
        public Integer specialists;
        public Integer beneficiaries;
        public Integer wallet_points;
    }

    public static final class Deserializer implements JsonDeserializer<User> {
        @Override
        public User deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            User user = new User();
            if (json == null || json.isJsonNull() || !json.isJsonObject()) {
                return user;
            }
            JsonObject root = json.getAsJsonObject();
            JsonObject object = unwrap(root);
            user.id = readInt(object, "id");
            user.name = readString(object, "name");
            user.email = readString(object, "email");
            user.phone = readString(object, "phone");
            user.locale = readString(object, "locale");
            user.role = readString(object, "role");
            user.approval_status = readString(object, "approval_status");
            user.organization_status = readString(object, "organization_status");
            user.org_rejection_reason = readString(object, "org_rejection_reason");
            if (object.has("org_profile") && object.get("org_profile").isJsonObject()) {
                user.org_profile = parseOrg(object.getAsJsonObject("org_profile"));
            }
            return user;
        }

        private static JsonObject unwrap(JsonObject root) {
            if (root.has("user") && root.get("user").isJsonObject()) {
                return root.getAsJsonObject("user");
            }
            if (root.has("data") && root.get("data").isJsonObject()) {
                JsonObject data = root.getAsJsonObject("data");
                if (data.has("id") || data.has("user")) {
                    if (data.has("user") && data.get("user").isJsonObject()) {
                        return data.getAsJsonObject("user");
                    }
                    return data;
                }
            }
            return root;
        }

        private static OrgProfile parseOrg(JsonObject object) {
            OrgProfile org = new OrgProfile();
            int id = readInt(object, "id");
            org.id = id > 0 ? id : null;
            org.name = readString(object, "name");
            org.status = readString(object, "status");
            org.review_notes = readString(object, "review_notes");
            org.about = readString(object, "about");
            org.members = readInteger(object, "members");
            org.specialists = readInteger(object, "specialists");
            org.beneficiaries = readInteger(object, "beneficiaries");
            org.wallet_points = readInteger(object, "wallet_points");
            return org;
        }

        private static String readString(JsonObject object, String key) {
            if (object == null || !object.has(key) || object.get(key).isJsonNull()) return null;
            JsonElement el = object.get(key);
            if (!el.isJsonPrimitive()) return null;
            String value = el.getAsString();
            return value != null && !value.isEmpty() && !"null".equalsIgnoreCase(value) ? value : null;
        }

        private static int readInt(JsonObject object, String key) {
            Integer value = readInteger(object, key);
            return value != null ? value : 0;
        }

        private static Integer readInteger(JsonObject object, String key) {
            if (object == null || !object.has(key) || object.get(key).isJsonNull()) return null;
            JsonElement el = object.get(key);
            if (!el.isJsonPrimitive()) return null;
            try {
                if (el.getAsJsonPrimitive().isNumber()) {
                    return el.getAsInt();
                }
                String raw = el.getAsString();
                if (raw == null || raw.isEmpty()) return null;
                return (int) Double.parseDouble(raw.trim());
            } catch (Exception ignored) {
                return null;
            }
        }
    }
}
