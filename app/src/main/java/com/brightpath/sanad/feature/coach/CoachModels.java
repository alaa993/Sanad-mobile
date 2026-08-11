package com.brightpath.sanad.feature.coach;

import java.util.List;

public class CoachModels {
    public static class ProgramSummary {
        public int id;
        public String category;
        public String title;
        public boolean active;
        public int items_count;
        public int checkins_count;
    }
    public static class PlanItem {
        public int id;
        public String kind;
        public String title;
        public String schedule;
        public boolean is_done;
    }
    public static class Checkin {
        public int id;
        public Double weight_kg;
        public String mood;
        public String note;
        public String logged_at;
    }
    public static class ProgramDetail extends ProgramSummary {
        public List<PlanItem> items;
        public List<Checkin> checkins;
    }
    public static class ProgramListResponse {
        public List<ProgramSummary> data;
    }
}
