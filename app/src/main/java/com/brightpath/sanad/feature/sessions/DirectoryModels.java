package com.brightpath.sanad.feature.sessions;

import java.util.Locale;

public class DirectoryModels {
    public static class Item {
        public int id;
        public String name;
        public String avatar;
        public String specialty;
        public String category;
        public java.util.List<String> tags;
        public java.util.List<String> languages;
        public Integer years_exp;
        public Double rating;
        public java.util.List<String> session_types;
        public java.util.Map<String,String> bio;
        public Object accepting_new;
    }
    public static class Paged { public java.util.List<Item> data; }
    public static class Detail { public Item data; }

    public static boolean isAccepting(Object value) {
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).intValue() != 0;
        if (value instanceof String) {
            String s = ((String) value).trim().toLowerCase(Locale.US);
            return "1".equals(s) || "true".equals(s) || "yes".equals(s);
        }
        return false;
    }
}
