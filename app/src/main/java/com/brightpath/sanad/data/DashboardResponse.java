package com.brightpath.sanad.data;

import java.util.List;

public class DashboardResponse {
    public String role;
    public Stats stats;
    public List<Shortcut> shortcuts;
    public Intake intake;
    public SessionSummary next_session;
    public Onboarding onboarding;

    public static class Onboarding {
        public String step;
        public boolean needs_intake;
        public boolean needs_pre_session;
        public boolean needs_vent;
        public boolean journal_unlocked;
    }

    public static class Stats {
        public int upcoming_sessions;
        public int unread_messages;
        public int points;
    }

    public static class Shortcut {
        public String id;
        public String title;
        public String route;
    }

    public static class SessionSummary {
        public int id;
        public String specialist_name;
        public String specialist_avatar;
        public String organization_name;
        public String type;
        public String status;
        public String scheduled_at;
        public String join_url;
        public boolean can_join;
    }

    public static class Intake {
        public boolean completed;
        public String full_name;
        public String severity_level;
        public String impact_level;
        public String preferred_session_mode;
        public List<String> risk_flags;
        public String primary_issue;
        public Integer benefit_score;
        public String triage_category;
        public String triage_reason;
        public RecommendedSpecialist recommended_specialist;
        public boolean referral_physician_recommended;
        public boolean external_physician_recommended;
        public String updated_at;
    }

    public static class RecommendedSpecialist {
        public int id;
        public String name;
    }
}
