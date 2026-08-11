package com.brightpath.sanad.feature.sessions;
import java.util.List;
public class SessionModels {
    public static class Person { public int id; public String name; public String avatar; }
    public static class Session {
        public int id;
        public String type;
        public String status;
        public String scheduled_at;
        public String ends_at;
        public String notes;
        public String rejection_reason;
        public String rejection_by;
        public String specialist_notes;
        public Integer rating;
        public Boolean survey_submitted;
        public String transferred_at;
        public String transfer_reason;
        public String join_url;
        public Integer chat_id;
        public Integer duration_minutes;
        public Integer extended_minutes;
        public Person specialist;
        public Person organization;
        public Person user;
    }
    public static class SessionList {
        public List<Session> pending;
        public List<Session> accepted;
        public List<Session> completed;
        public List<Session> rejected;
        public List<Session> upcoming;
        public List<Session> history;
    }
}
