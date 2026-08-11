package com.brightpath.sanad.models;

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
        public int id;
        public String name;
        public String status;
        public String review_notes;
        public String about;
        public int members;
        public int specialists;
        public int beneficiaries;
        public int wallet_points;
    }
}
