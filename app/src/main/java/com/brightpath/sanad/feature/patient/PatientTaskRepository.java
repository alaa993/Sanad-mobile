package com.brightpath.sanad.feature.patient;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.Nullable;
import com.brightpath.sanad.data.ApiClient;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public class PatientTaskRepository {
    private interface PatientTaskApi {
        @GET("api/v1/patient/tasks")
        Call<TaskListResponse> list();

        @POST("api/v1/patient/tasks")
        Call<RemoteTask> create(@Body Map<String, Object> body);

        @PUT("api/v1/patient/tasks/{id}")
        Call<RemoteTask> update(@Path("id") int id, @Body Map<String, Object> body);
    }

    static class TaskListResponse {
        List<RemoteTask> upcoming;
        List<RemoteTask> completed;
    }

    static class RemoteTask {
        int id;
        String title;
        String description;
        String status;
        String due_at;
        String completed_at;
        String completion_note;
        Integer appointment_id;
    }

    private static final String PREF = "patient_tasks";
    private static final String KEY = "tasks";
    private final SharedPreferences prefs;
    private final PatientTaskApi api;

    public PatientTaskRepository(Context ctx) {
        prefs = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        api = ApiClient.get(ctx).create(PatientTaskApi.class);
    }

    public List<PatientTask> load() {
        String raw = prefs.getString(KEY, null);
        if (raw == null) return new ArrayList<>();
        List<PatientTask> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                PatientTask t = fromJson(o);
                if (t != null) list.add(t);
            }
        } catch (JSONException ignored) {
        }
        return list;
    }

    public void save(List<PatientTask> tasks) {
        JSONArray arr = new JSONArray();
        if (tasks != null) {
            for (PatientTask t : tasks) {
                arr.put(toJson(t));
            }
        }
        prefs.edit().putString(KEY, arr.toString()).apply();
    }

    public void add(PatientTask task) {
        List<PatientTask> tasks = load();
        tasks.add(task);
        save(tasks);
    }

    public void updateStatus(String sessionId, PatientTask.Status status) {
        List<PatientTask> tasks = load();
        boolean changed = false;
        for (PatientTask t : tasks) {
            if (sessionId != null && sessionId.equals(t.sessionId)) {
                t.status = status;
                changed = true;
            }
        }
        if (changed) save(tasks);
    }

    public void completeTask(PatientTask task, @Nullable String note, long completedAt) {
        if (task == null) return;
        List<PatientTask> tasks = load();
        boolean changed = false;
        for (PatientTask local : tasks) {
            if (matches(local, task)) {
                local.status = PatientTask.Status.COMPLETED;
                local.completionNote = note;
                local.completedAt = completedAt;
                changed = true;
            }
        }
        if (changed) save(tasks);
    }

    @Nullable
    private PatientTask fromJson(JSONObject o) {
        if (o == null) return null;
        PatientTask t = new PatientTask();
        t.id = o.optString("id", null);
        t.title = o.optString("title");
        t.description = o.optString("description");
        t.dueAt = o.optLong("dueAt");
        String status = o.optString("status");
        if ("COMPLETED".equalsIgnoreCase(status)) t.status = PatientTask.Status.COMPLETED;
        t.sessionId = o.optString("sessionId");
        t.completedAt = o.optLong("completedAt");
        t.completionNote = o.optString("completionNote", null);
        return t;
    }

    private JSONObject toJson(PatientTask t) {
        JSONObject o = new JSONObject();
        try {
            o.put("id", t.id);
            o.put("title", t.title);
            o.put("description", t.description);
            o.put("dueAt", t.dueAt);
            o.put("status", t.status.name());
            o.put("sessionId", t.sessionId);
            o.put("completedAt", t.completedAt);
            o.put("completionNote", t.completionNote);
        } catch (JSONException ignored) {
        }
        return o;
    }

    public interface SyncListener {
        void onResult(List<PatientTask> remote);

        void onError(Throwable t);
    }

    public void syncRemote(@Nullable SyncListener listener) {
        if (api == null) {
            if (listener != null) listener.onError(new IllegalStateException("api null"));
            return;
        }
        api.list().enqueue(new Callback<TaskListResponse>() {
            @Override
            public void onResponse(Call<TaskListResponse> call, Response<TaskListResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<PatientTask> merged = mergeRemote(response.body());
                    save(merged);
                    if (listener != null) listener.onResult(merged);
                } else if (listener != null) {
                    listener.onError(new RuntimeException("sync_failed"));
                }
            }

            @Override
            public void onFailure(Call<TaskListResponse> call, Throwable t) {
                if (listener != null) listener.onError(t);
            }
        });
    }

    public void createRemote(PatientTask task) {
        if (api == null || task == null) return;
        Map<String, Object> body = new HashMap<>();
        body.put("title", task.title != null ? task.title : "واجب");
        if (task.description != null && !task.description.isEmpty()) {
            body.put("description", task.description);
        }
        Integer appointmentId = parseAppointmentId(task.sessionId);
        if (appointmentId == null) return;
        body.put("appointment_id", appointmentId);
        if (task.dueAt > 0) {
            body.put("due_at", Instant.ofEpochMilli(task.dueAt).toString());
        }
        api.create(body).enqueue(new Callback<RemoteTask>() {
            @Override
            public void onResponse(Call<RemoteTask> call, Response<RemoteTask> response) {
                if (response.isSuccessful() && response.body() != null) {
                    RemoteTask remote = response.body();
                    List<PatientTask> current = load();
                    for (PatientTask t : current) {
                        if (matchesLocalDraft(t, task)) {
                            t.id = String.valueOf(remote.id);
                            if (remote.appointment_id != null) {
                                t.sessionId = String.valueOf(remote.appointment_id);
                            }
                        }
                    }
                    save(current);
                }
            }

            @Override
            public void onFailure(Call<RemoteTask> call, Throwable t) {
            }
        });
    }

    public void completeRemote(@Nullable String taskId, @Nullable String note) {
        if (api == null || taskId == null || taskId.isEmpty()) return;
        int id;
        try {
            id = Integer.parseInt(taskId);
        } catch (NumberFormatException e) {
            return;
        }
        Map<String, Object> body = new HashMap<>();
        body.put("status", "completed");
        if (note != null && !note.isEmpty()) {
            body.put("notes", note);
        }
        api.update(id, body).enqueue(new Callback<RemoteTask>() {
            @Override
            public void onResponse(Call<RemoteTask> call, Response<RemoteTask> response) {
            }

            @Override
            public void onFailure(Call<RemoteTask> call, Throwable t) {
            }
        });
    }

    private List<PatientTask> mergeRemote(TaskListResponse response) {
        List<PatientTask> merged = new ArrayList<>();
        if (response.upcoming != null) {
            for (RemoteTask remote : response.upcoming) {
                PatientTask mapped = fromRemote(remote);
                if (mapped != null) merged.add(mapped);
            }
        }
        if (response.completed != null) {
            for (RemoteTask remote : response.completed) {
                PatientTask mapped = fromRemote(remote);
                if (mapped != null) merged.add(mapped);
            }
        }
        return merged;
    }

    @Nullable
    private PatientTask fromRemote(RemoteTask remote) {
        if (remote == null) return null;
        PatientTask t = new PatientTask();
        t.id = String.valueOf(remote.id);
        t.title = remote.title;
        t.description = remote.description;
        t.dueAt = parseInstant(remote.due_at);
        t.completedAt = parseInstant(remote.completed_at);
        t.completionNote = remote.completion_note;
        if (remote.appointment_id != null) {
            t.sessionId = String.valueOf(remote.appointment_id);
        }
        if ("completed".equalsIgnoreCase(remote.status)) {
            t.status = PatientTask.Status.COMPLETED;
        } else {
            t.status = PatientTask.Status.PENDING;
        }
        return t;
    }

    private long parseInstant(@Nullable String raw) {
        if (raw == null || raw.isEmpty()) return 0L;
        try {
            return Instant.parse(raw).toEpochMilli();
        } catch (DateTimeParseException ignored) {
        }
        return 0L;
    }

    @Nullable
    private Integer parseAppointmentId(@Nullable String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) return null;
        try {
            return Integer.parseInt(sessionId.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean matches(PatientTask local, PatientTask target) {
        if (local == null || target == null) return false;
        if (local.id != null && target.id != null) {
            return local.id.equals(target.id);
        }
        if (local.sessionId != null && target.sessionId != null) {
            return local.sessionId.equals(target.sessionId);
        }
        return false;
    }

    private boolean matchesLocalDraft(PatientTask local, PatientTask draft) {
        if (local == null || draft == null) return false;
        if (local.id != null && !local.id.isEmpty()) return false;
        if (draft.sessionId != null && draft.sessionId.equals(local.sessionId)) {
            return draft.title != null && draft.title.equals(local.title);
        }
        return draft.title != null && draft.title.equals(local.title);
    }
}
