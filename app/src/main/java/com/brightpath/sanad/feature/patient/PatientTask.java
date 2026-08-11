package com.brightpath.sanad.feature.patient;

public class PatientTask {
    public enum Status { PENDING, COMPLETED }
    public String id;
    public String title;
    public String description;
    public long dueAt;
    public Status status = Status.PENDING;
    public String sessionId;
    public long completedAt;
    public String completionNote;

    public PatientTask copy(){
        PatientTask t = new PatientTask();
        t.id = id;
        t.title = title;
        t.description = description;
        t.dueAt = dueAt;
        t.status = status;
        t.sessionId = sessionId;
        t.completedAt = completedAt;
        t.completionNote = completionNote;
        return t;
    }
}
