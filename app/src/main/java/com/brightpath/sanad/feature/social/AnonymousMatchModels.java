package com.brightpath.sanad.feature.social;

public class AnonymousMatchModels {
    public static class MatchData {
        public int id;
        public String status;
        public String mode;
        public String gender;
        public String match_gender;
        public Integer chat_id;
        public String alias_self;
        public String alias_partner;
        public String matched_at;
        public String expires_at;
    }
    public static class StatusResponse {
        public MatchData data;
    }
}
