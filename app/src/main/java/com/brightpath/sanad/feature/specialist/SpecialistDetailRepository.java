package com.brightpath.sanad.feature.specialist;

import android.content.Context;
import androidx.annotation.Nullable;
import com.brightpath.sanad.data.ApiClient;
import com.brightpath.sanad.feature.patient.PatientIntakeForm;
import com.brightpath.sanad.feature.patient.PatientTask;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public class SpecialistDetailRepository {
    interface SpecialistDetailApi {
        @GET("api/v1/specialist/patients/{id}/intake")
        Call<PatientIntakeForm> intake(@Path("id") int patientId);
        @PUT("api/v1/specialist/patients/{id}/intake")
        Call<PatientIntakeForm> updateIntake(@Path("id") int patientId, @retrofit2.http.Body java.util.Map<String,Object> body);
        @GET("api/v1/specialist/patients/{id}/tasks")
        Call<List<PatientTask>> tasks(@Path("id") int patientId);
        @GET("api/v1/specialist/patients/{id}/sessions")
        Call<PatientSessionsResponse> sessions(@Path("id") int patientId);
        @POST("api/v1/specialist/patients/{id}/tasks/templates")
        Call<ApplyTemplatesResponse> applyTemplates(@Path("id") int patientId, @retrofit2.http.Body java.util.Map<String,Object> body);
    }

    public static class ApplyTemplatesResponse {
        public java.util.List<Object> created;
    }

    public static class PatientSessionRow {
        public int id;
        public String status;
        public String starts_at;
        public String closed_at;
        public String specialist_notes;
        public Integer rating;
        public String type;
    }
    public static class PatientSessionsResponse {
        public List<PatientSessionRow> data;
    }

    private final SpecialistDetailApi api;
    public SpecialistDetailRepository(Context ctx) {
        api = ApiClient.get(ctx).create(SpecialistDetailApi.class);
    }

    public interface IntakeCb { void ok(PatientIntakeForm form); void err(Throwable t); }
    public interface TasksCb { void ok(List<PatientTask> tasks); void err(Throwable t); }
    public interface SessionsCb { void ok(List<PatientSessionRow> sessions); void err(Throwable t); }
    public interface UpdateIntakeCb { void ok(PatientIntakeForm form); void err(Throwable t); }
    public interface ApplyTemplatesCb { void ok(); void err(Throwable t); }

    public void applyTaskTemplates(int patientId, int appointmentId, java.util.List<String> templateIds, ApplyTemplatesCb cb) {
        java.util.Map<String,Object> body = new java.util.HashMap<>();
        body.put("template_ids", templateIds);
        if (appointmentId > 0) body.put("appointment_id", appointmentId);
        api.applyTemplates(patientId, body).enqueue(new Callback<ApplyTemplatesResponse>() {
            @Override public void onResponse(Call<ApplyTemplatesResponse> call, Response<ApplyTemplatesResponse> response) {
                if (response.isSuccessful()) cb.ok();
                else cb.err(new RuntimeException("templates_failed"));
            }
            @Override public void onFailure(Call<ApplyTemplatesResponse> call, Throwable t) { cb.err(t); }
        });
    }

    public void fetchIntake(int patientId, IntakeCb cb) {
        api.intake(patientId).enqueue(new Callback<PatientIntakeForm>() {
            @Override public void onResponse(Call<PatientIntakeForm> call, Response<PatientIntakeForm> response) {
                if (response.isSuccessful() && response.body()!=null) cb.ok(response.body());
                else cb.err(new RuntimeException("intake_failed"));
            }
            @Override public void onFailure(Call<PatientIntakeForm> call, Throwable t) { cb.err(t); }
        });
    }

    public void fetchTasks(int patientId, TasksCb cb) {
        api.tasks(patientId).enqueue(new Callback<List<PatientTask>>() {
            @Override public void onResponse(Call<List<PatientTask>> call, Response<List<PatientTask>> response) {
                if (response.isSuccessful() && response.body()!=null) cb.ok(response.body());
                else cb.err(new RuntimeException("tasks_failed"));
            }
            @Override public void onFailure(Call<List<PatientTask>> call, Throwable t) { cb.err(t); }
        });
    }

    public void fetchSessions(int patientId, SessionsCb cb) {
        api.sessions(patientId).enqueue(new Callback<PatientSessionsResponse>() {
            @Override public void onResponse(Call<PatientSessionsResponse> call, Response<PatientSessionsResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    cb.ok(response.body().data);
                } else {
                    cb.err(new RuntimeException("sessions_failed"));
                }
            }
            @Override public void onFailure(Call<PatientSessionsResponse> call, Throwable t) { cb.err(t); }
        });
    }

    public void updateIntake(int patientId, java.util.List<String> triageTags, @Nullable String reason, UpdateIntakeCb cb) {
        java.util.Map<String,Object> body = new java.util.HashMap<>();
        if (triageTags != null) body.put("triageTags", triageTags);
        if (reason != null && !reason.trim().isEmpty()) body.put("triage_reason", reason.trim());
        api.updateIntake(patientId, body).enqueue(new Callback<PatientIntakeForm>() {
            @Override public void onResponse(Call<PatientIntakeForm> call, Response<PatientIntakeForm> response) {
                if (response.isSuccessful() && response.body()!=null) cb.ok(response.body());
                else cb.err(new RuntimeException("intake_update_failed"));
            }
            @Override public void onFailure(Call<PatientIntakeForm> call, Throwable t) { cb.err(t); }
        });
    }
}
