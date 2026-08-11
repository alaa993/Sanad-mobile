
package com.brightpath.sanad.feature.admin;
public class AdminModels {
  public static class Dashboard {
    public Counters counters;
    public java.util.List<QuickAction> quick_actions;
    public java.util.List<Alert> alerts;
    public java.util.List<Metric> metrics;
  }

  public static class Counters{
    public int users,specialists,organizations,appointments,appointments_today,posts;
    public int sessions_week;
    public int organizations_pending;
    public int specialists_pending;
  }

  public static class QuickAction { public String id,label; }
  public static class Alert { public String id,title,message,level; }
  public static class Metric { public String title,value,trend; }

  public static class Users {
    public java.util.List<User> data;
    public static class User{
      public int id;
      public String name,email,role,status,created_at,phone;
      public boolean banned;
    }
  }
  public static class Specialists {
    public java.util.List<Specialist> data;
    public static class Specialist{
      public int id,years_exp;
      public String name,specialty,status;
      // backend sometimes sends 0/1, accept both (Object avoids parse errors)
      public Object accepting_new;
    }
  }
  public static class Organizations {
    public java.util.List<Organization> data;
    public static class Organization{ public int id; public String name,status; }
  }
  public static class OrganizationDetail {
    public Detail data;
    public static class Detail {
      public Organizations.Organization organization;
      public Stats stats;
    }
    public static class Stats {
      public int members;
      public int specialists;
      public int beneficiaries;
      public int sessions_total;
      public int upcoming;
    }
  }
  public static class Appointments {
    public java.util.List<Appointment> data;
    public static class Appointment{
      public int id,specialist_id,patient_id;
      public String status,starts_at,ends_at,type;
      public String patient_name,specialist_name,organization_name;
    }
  }
  public static class Posts {
    public java.util.List<Post> data;
    public static class Post{
      public int id;
      public String title,status,type,created_at,author;
      public boolean featured;
      public int comments,likes;
    }
  }
  public static class Toggle { public Boolean ok; public String status; public String msg; }
  public static class SpecialistDocuments { public String status; public String verification_notes; public java.util.List<Document> documents; }
  public static class Document { public int id; public String type,title,file_path,verified_at; public Meta meta; }
  public static class Meta { public String original_name,mime; }
  public static class ReviewRequest { public String status; public String notes; public java.util.List<Integer> verified_documents; }
  public static class RejectRequest { public String reason; }
  public static class AdminProfile {
    public int id; public String name; public String email; public String avatar; public String locale; public String phone;
    public Stats stats; public String privacy_policy; public String contact_info; public Integer platform_fee_percent;
    public static class Stats { public int pending_specialists; public int pending_organizations; public int total_users; public int total_sessions; }
  }
  public static class AdminSettings { public String privacy_policy; public String contact_info; public Integer platform_fee_percent; }

  public static class VentReports { public java.util.List<VentReport> data; }
  public static class VentReport {
    public int id;
    public String reason, status, created_at;
    public VentPost post;
    public Reporter reporter;
    public static class VentPost { public int id; public String alias, body, hidden_at; }
    public static class Reporter { public int id; public String name; }
  }

  public static class DailyTips { public java.util.List<DailyTip> data; }
  public static class DailyTip {
    public int id;
    public String tip_date;
    /** Localized maps — Object values avoid ClassCast when API shape drifts. */
    public java.util.Map<String, Object> title;
    public java.util.Map<String, Object> body;
    public boolean active;
  }
}
