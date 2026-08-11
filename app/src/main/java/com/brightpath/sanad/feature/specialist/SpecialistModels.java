
package com.brightpath.sanad.feature.specialist;
import java.util.Locale;
public class SpecialistModels {
  public static class Dashboard { public Counters counters; public static class Counters{ public int upcoming; public int today; public int pending; } }
  public static class Appointment {
    public int id;
    public String status;
    public String starts_at;
    public String ends_at;
    public int patient_id;
    public String patient_name;
    public String organization_name;
    public String type;
    public String join_url;
    public String notes;
    public String rejection_reason;
    public String rejection_by;
  }
  public static class Appointments { public java.util.List<Appointment> data; }
  public static class Profile { public int id; public int user_id; public String name; public String email; public String avatar; public boolean requires_avatar; public String specialty; public java.util.List<String> languages; public int years_exp; public Object accepting_new; public java.util.Map<String,String> bio; public int rate_cents; public String currency; public String status; public String verification_notes; }
  public static class DocumentList { public String status; public String verification_notes; public java.util.List<Document> documents; }
  public static class Document { public int id; public String type; public String title; public String file_path; public String verified_at; public Meta meta; }
  public static class Meta { public String original_name; public String mime; }
  public static class PatientMini { public int id; public String name; public String avatar; }
  public static class Patients { public java.util.List<PatientMini> data; }
  public static class Simple { public Boolean ok; }

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
