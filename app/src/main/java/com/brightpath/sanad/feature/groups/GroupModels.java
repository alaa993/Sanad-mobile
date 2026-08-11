package com.brightpath.sanad.feature.groups;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class GroupModels {
    public static class GroupSession {
        public int id;
        public String title;
        public String topic;
        @SerializedName("age_category") public String ageCategory;
        @SerializedName("disorder_tag") public String disorderTag;
        public String type; // video | voice | chat
        @SerializedName("start_at") public String startAt;
        @SerializedName("end_at") public String endAt;
        public String status; // scheduled | ongoing | finished | canceled
        @SerializedName("participants_count") public int participantsCount;
        @SerializedName("specialist_name") public String specialistName;
        @SerializedName("join_url") public String joinUrl;
        @SerializedName("chat_id") public Integer chatId;
        @SerializedName("max_capacity") public int maxCapacity;
        @SerializedName("is_public") public boolean isPublic;
        @SerializedName("spots_left") public int spotsLeft;
        public String description;
        public boolean joined; // هل المستخدم الحالي منضم
    }

    public static class GroupSessionList {
        public List<GroupSession> data;
    }
}
