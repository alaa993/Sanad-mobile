package com.brightpath.sanad.data;

import com.brightpath.sanad.feature.patient.PreSessionModels;
import java.util.List;

public class CatalogModels {
    public static class Catalog {
        public List<CaseType> case_types;
        public List<CommunityCategory> community_categories;
        public List<CommunityCategory> group_age_categories;
        public List<CommunityCategory> group_disorder_tags;
        public List<TaskTemplate> task_templates;
        public List<PreSessionModels.Question> pre_session_questions;
    }

    public static class CaseType {
        public String id;
        public String label_ar;
        public String label_en;
        public String specialist;
    }

    public static class CommunityCategory {
        public String id;
        public String label_ar;
        public String label_en;
    }

    public static class TaskTemplate {
        public String id;
        public String title_ar;
        public String title_en;
        public String description_ar;
    }
}
