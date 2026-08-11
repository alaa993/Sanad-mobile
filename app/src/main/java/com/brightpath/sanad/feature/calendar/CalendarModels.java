
package com.brightpath.sanad.feature.calendar;
import java.util.List;
public class CalendarModels {
  public static class Slot { public int id; public int weekday; public String start_time; public String end_time; public boolean active; }
  public static class Block { public int id; public String start_at; public String end_at; public String reason; }
  public static class Availability { public List<Slot> slots; public List<Block> blocks; }
  public static class Appointment { public int id; public int patient_id; public int specialist_id; public String status; public String starts_at; public String ends_at; public String notes; }
  public static class Appointments { public java.util.List<Appointment> data; }
  public static class Suggested { public java.util.List<Suggestion> data; }
  public static class Suggestion { public String starts_at; public String ends_at; }
  public static class Simple { public Boolean ok; public Integer id; public String status; public Boolean deleted; }
  public static class RecurringOccurrence { public int id; public String starts_at; }
  public static class RecurringResponse { public int series_id; public java.util.List<RecurringOccurrence> occurrences; }
}
