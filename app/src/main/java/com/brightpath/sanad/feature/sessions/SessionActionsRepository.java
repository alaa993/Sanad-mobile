package com.brightpath.sanad.feature.sessions;

import android.content.Context;
import com.brightpath.sanad.data.ApiClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SessionActionsRepository {
    private final SessionActionsApi api;
    public SessionActionsRepository(Context ctx){
        api = ApiClient.get(ctx).create(SessionActionsApi.class);
    }

    public static class ExtendResponse {
        public boolean ok;
        public Integer extended_minutes;
        public String ends_at;
    }
    public static class Task {
        public int id;
        public String title;
        public String description;
        public String type;
        public String status;
        public String patient_answer;
        public String completed_at;
    }
    public static class TaskListResponse { public java.util.List<Task> data; }
    public static class TaskResponse { public boolean ok; public Task task; }

    public interface ExtendCb { void ok(ExtendResponse resp); void err(Throwable t); }
    public interface TaskListCb { void ok(java.util.List<Task> tasks); void err(Throwable t); }
    public interface TaskCb { void ok(Task task); void err(Throwable t); }
    public interface SimpleCb { void ok(); void err(Throwable t); }

    public void extend(int sessionId, int minutes, ExtendCb cb){
        java.util.Map<String,Integer> b = new java.util.HashMap<>();
        b.put("minutes", minutes);
        api.extend(sessionId, b).enqueue(new Callback<ExtendResponse>() {
            @Override public void onResponse(Call<ExtendResponse> call, Response<ExtendResponse> response) {
                if (response.isSuccessful()) {
                    cb.ok(response.body());
                } else {
                    cb.err(new Exception("HTTP "+response.code()));
                }
            }
            @Override public void onFailure(Call<ExtendResponse> call, Throwable t) { cb.err(t); }
        });
    }

    public void listTasks(int sessionId, TaskListCb cb){
        api.listTasks(sessionId).enqueue(new Callback<TaskListResponse>() {
            @Override public void onResponse(Call<TaskListResponse> call, Response<TaskListResponse> response) {
                if (response.isSuccessful() && response.body()!=null) {
                    cb.ok(response.body().data);
                } else { cb.err(new Exception("HTTP "+response.code())); }
            }
            @Override public void onFailure(Call<TaskListResponse> call, Throwable t) { cb.err(t); }
        });
    }

    public void addTask(int sessionId, String title, String description, String type, String dueAt, TaskCb cb){
        java.util.Map<String,Object> b = new java.util.HashMap<>();
        b.put("title", title);
        if (description!=null) b.put("description", description);
        if (type!=null) b.put("type", type);
        if (dueAt != null && !dueAt.isEmpty()) b.put("due_at", dueAt);
        b.put("create_follow_up", true);
        api.addTask(sessionId, b).enqueue(new Callback<TaskResponse>() {
            @Override public void onResponse(Call<TaskResponse> call, Response<TaskResponse> response) {
                if (response.isSuccessful() && response.body()!=null) cb.ok(response.body().task);
                else cb.err(new Exception("HTTP "+response.code()));
            }
            @Override public void onFailure(Call<TaskResponse> call, Throwable t) { cb.err(t); }
        });
    }

    public void completeTask(int taskId, String answer, TaskCb cb){
        java.util.Map<String,String> b = new java.util.HashMap<>();
        if (answer!=null) b.put("answer", answer);
        api.completeTask(taskId, b).enqueue(new Callback<TaskResponse>() {
            @Override public void onResponse(Call<TaskResponse> call, Response<TaskResponse> response) {
                if (response.isSuccessful() && response.body()!=null) cb.ok(response.body().task);
                else cb.err(new Exception("HTTP "+response.code()));
            }
            @Override public void onFailure(Call<TaskResponse> call, Throwable t) { cb.err(t); }
        });
    }

    public void rateSpecialist(int sessionId, int score, String comment, SimpleCb cb){
        java.util.Map<String,Object> b = new java.util.HashMap<>();
        b.put("score", score);
        if (comment!=null) b.put("comment", comment);
        api.rateSpecialist(sessionId, b).enqueue(new Callback<java.util.Map<String,Boolean>>() {
            @Override public void onResponse(Call<java.util.Map<String,Boolean>> call, Response<java.util.Map<String,Boolean>> response) {
                if (response.isSuccessful()) cb.ok(); else cb.err(new Exception("HTTP "+response.code()));
            }
            @Override public void onFailure(Call<java.util.Map<String,Boolean>> call, Throwable t) { cb.err(t); }
        });
    }

    public void ratePatient(int sessionId, int score, String comment, SimpleCb cb){
        java.util.Map<String,Object> b = new java.util.HashMap<>();
        b.put("score", score);
        if (comment!=null) b.put("comment", comment);
        api.ratePatient(sessionId, b).enqueue(new Callback<java.util.Map<String,Boolean>>() {
            @Override public void onResponse(Call<java.util.Map<String,Boolean>> call, Response<java.util.Map<String,Boolean>> response) {
                if (response.isSuccessful()) cb.ok(); else cb.err(new Exception("HTTP "+response.code()));
            }
            @Override public void onFailure(Call<java.util.Map<String,Boolean>> call, Throwable t) { cb.err(t); }
        });
    }

    public void cancelSession(int sessionId, String reason, SimpleCb cb){
        java.util.Map<String,Object> b = new java.util.HashMap<>();
        if (reason != null && !reason.trim().isEmpty()) {
            b.put("reason", reason.trim());
        }
        api.cancel(sessionId, b).enqueue(new Callback<java.util.Map<String,Boolean>>() {
            @Override public void onResponse(Call<java.util.Map<String,Boolean>> call, Response<java.util.Map<String,Boolean>> response) {
                if (response.isSuccessful()) cb.ok(); else cb.err(new Exception("HTTP "+response.code()));
            }
            @Override public void onFailure(Call<java.util.Map<String,Boolean>> call, Throwable t) { cb.err(t); }
        });
    }

    public void completeSession(int sessionId, String diagnosisNotes, SimpleCb cb){
        java.util.Map<String,Object> b = new java.util.HashMap<>();
        if (diagnosisNotes != null && !diagnosisNotes.trim().isEmpty()) {
            b.put("diagnosis_notes", diagnosisNotes.trim());
        }
        api.complete(sessionId, b).enqueue(new Callback<java.util.Map<String,Object>>() {
            @Override public void onResponse(Call<java.util.Map<String,Object>> call, Response<java.util.Map<String,Object>> response) {
                if (response.isSuccessful()) cb.ok(); else cb.err(new Exception("HTTP "+response.code()));
            }
            @Override public void onFailure(Call<java.util.Map<String,Object>> call, Throwable t) { cb.err(t); }
        });
    }

    public void submitSurvey(int sessionId, int feedback, String comment, SimpleCb cb){
        java.util.Map<String,Object> b = new java.util.HashMap<>();
        b.put("patient_feedback", feedback);
        if (comment != null && !comment.trim().isEmpty()) {
            b.put("comment", comment.trim());
        }
        api.survey(sessionId, b).enqueue(new Callback<java.util.Map<String,Object>>() {
            @Override public void onResponse(Call<java.util.Map<String,Object>> call, Response<java.util.Map<String,Object>> response) {
                if (response.isSuccessful()) cb.ok(); else cb.err(new Exception("HTTP "+response.code()));
            }
            @Override public void onFailure(Call<java.util.Map<String,Object>> call, Throwable t) { cb.err(t); }
        });
    }
}
