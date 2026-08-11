package com.brightpath.sanad.feature.sessions;

public class BookRequest {
    public String type;
    public String scheduled_at; // ISO 8601, e.g., 2025-11-02T16:00:00Z
    public Integer specialist_id;
    public Integer organization_id;
    public String notes;
    public String timezone;
    public Boolean weekly_recurring;
    public Integer recurrence_count;
}
