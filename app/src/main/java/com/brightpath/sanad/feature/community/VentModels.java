package com.brightpath.sanad.feature.community;

import java.util.List;

public class VentModels {
    public static class VentPost {
        public int id;
        public String alias;
        public String body;
        public String created_at;
        public int empathy_count;
        public int support_count;
        public boolean user_empathy;
        public boolean user_support;
    }
    public static class VentList {
        public java.util.List<VentPost> data;
    }
    public static class VentCreate {
        public String body;
        public VentCreate(String body){ this.body = body; }
    }
    public static class ReactRequest {
        public String type;
        public ReactRequest(String type){ this.type = type; }
    }
    public static class ReactResponse {
        public String type;
        public boolean active;
        public int count;
    }
    public static class ReportRequest {
        public String reason;
        public ReportRequest(String reason){ this.reason = reason; }
    }
    public static class ReportResponse {
        public boolean reported;
    }
    public static class ChatRequest {
        public String message;
        public String mood;
        public String stage;
        public ChatRequest(String msg, String mood, String stage){
            this.message = msg;
            this.mood = mood;
            this.stage = stage;
        }
    }
    public static class ChatResponse {
        public String reply;
        public String sent;
        public java.util.List<String> tips;
        public String prompt;
        public String mood;
        public String stage;
        public String next_stage;
        public String next_prompt;
    }
}
