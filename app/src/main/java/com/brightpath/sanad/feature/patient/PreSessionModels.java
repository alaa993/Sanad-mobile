package com.brightpath.sanad.feature.patient;

import java.util.List;
import java.util.Map;

public class PreSessionModels {
    public static class Question {
        public String id;
        public String label_ar;
        public String label_en;
        public String type;
    }

    public static class Status {
        public List<Question> questions;
        public boolean completed;
        public Map<String, Object> answers;
    }

    public static class SubmitResult {
        public boolean saved;
        public String completed_at;
    }
}
