
package com.brightpath.sanad.feature.org;
public class OrgModels {
  public static class Dashboard {
    public Integer org_id;
    public Counters counters;
    public Summary summary;
    public java.util.List<QuickAction> quick_actions;
    public java.util.List<Alert> alerts;
    public java.util.List<Activity> activity;
  }

  public static class SupportRoom {
    public int community_id;
    public String slug;
    public String name;
    public String visibility;
  }

  public static class Counters{
    public int beneficiaries;
    public int sessions_total;
    public int upcoming_48h;
    public int specialists_active;
    public int high_risk_cases;
    // حقول قديمة لضمان التوافق مع النسخ السابقة
    public int upcoming;
    public int pending;
  }

  public static class Summary {
    public int completed_sessions;
    public int cancelled_sessions;
    public float avg_satisfaction;
  }

  public static class QuickAction {
    public String id;
    public String label;
  }

  public static class Alert {
    public String id;
    public String title;
    public String level;
    public String message;
  }

  public static class Activity {
    public String title;
    public String value;
    public String meta;
  }

  public static class Specialist {
    public int id;
    public String name;
    public String role;
    public String email;
    public int sessions_count;
    public float commitment_rate;
    public float avg_rating;
    public String next_session_at;
  }
  public static class Specialists { public java.util.List<Specialist> data; }
  public static class Appointment { public int id; public String status; public String starts_at; public String ends_at; public int specialist_id; public int patient_id; }
  public static class Appointments { public java.util.List<Appointment> data; }

  public static class Beneficiary {
    public int id;
    public String name;
    public String email;
    public String status;
    public String risk_level;
    public String primary_issue;
    public String specialist_name;
    public String last_session_at;
  }

  public static class Beneficiaries { public java.util.List<Beneficiary> data; }

  public static class BeneficiaryDetail {
    public Detail data;
    public static class Detail {
      public OrganizationBeneficiary beneficiary;
      public Patient patient;
      public SpecialistSummary assigned_specialist;
      public Object intake;
      public java.util.List<Appointment> sessions;
    }
  }

  public static class OrganizationBeneficiary {
    public int id;
    public int organization_id;
    public int patient_id;
    public Integer assigned_specialist_id;
    public String status;
    public String risk_level;
    public String primary_issue;
    public String notes;
    public String last_session_at;
  }

  public static class Patient {
    public int id;
    public String name;
    public String email;
    public String phone;
  }

  public static class SpecialistSummary {
    public int id;
    public String name;
    public String email;
    public String phone;
  }

  public static class BeneficiaryForm {
    public Integer patient_id;
    public String name;
    public String email;
    public String phone;
    public String risk_level;
    public String primary_issue;
    public String notes;
  }

  public static class ReportSummary {
    public Period period;
    public Metrics metrics;
    public java.util.List<TopBeneficiary> top_beneficiaries;

    public static class Period {
      public String from;
      public String to;
    }

    public static class Metrics {
      public int beneficiaries_total;
      public int beneficiaries_active;
      public int high_risk_cases;
      public int sessions_completed;
      public int sessions_cancelled;
      public int sessions_upcoming_week;
      public float engagement_rate;
    }

    public static class TopBeneficiary {
      public int id;
      public String name;
      public String risk_level;
      public String primary_issue;
      public String last_session_at;
    }
  }

  public static class BillingOverview {
    public Plan plan;
    public SeatUsage seats;
    public SessionUsage sessions;
    public Wallet wallet;
    public java.util.List<Invoice> invoices;

    public static class Plan {
      public String name;
      public String status;
      public String renews_at;
    }

    public static class SeatUsage {
      public int limit;
      public int used;
    }

    public static class SessionUsage {
      public int limit;
      public int used;
    }

    public static class Wallet {
      public int balance;
      public int points;
      public String currency;
    }

    public static class Invoice {
      public long id;
      public int total;
      public String currency;
      public String status;
      public String pdf_url;
      public String created_at;
    }
  }

  public static class SpecialistDetail {
    public Detail data;
    public static class Detail {
      public SpecialistSummary specialist;
      public SpecialistStats stats;
      public java.util.List<Appointment> sessions;
      public java.util.List<BeneficiaryMini> beneficiaries;
    }
    public static class SpecialistStats {
      public int sessions_count;
      public float commitment_rate;
      public String next_session_at;
    }
    public static class BeneficiaryMini {
      public int id;
      public String name;
      public String risk_level;
    }
  }
}
