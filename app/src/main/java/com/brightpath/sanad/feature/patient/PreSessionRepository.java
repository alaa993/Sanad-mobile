package com.brightpath.sanad.feature.patient;

import android.content.Context;
import androidx.annotation.NonNull;
import com.brightpath.sanad.data.ApiClient;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PreSessionRepository {
    private final PreSessionApi api;

    public PreSessionRepository(Context ctx) {
        api = ApiClient.get(ctx).create(PreSessionApi.class);
    }

    public interface StatusCb { void ok(PreSessionModels.Status s); void err(Throwable t); }
    public interface SubmitCb { void ok(); void err(Throwable t); }

    public void fetchStatus(StatusCb cb) {
        api.status().enqueue(new Callback<PreSessionModels.Status>() {
            @Override public void onResponse(@NonNull Call<PreSessionModels.Status> call, @NonNull Response<PreSessionModels.Status> r) {
                if (!r.isSuccessful() || r.body() == null) { cb.err(new RuntimeException("HTTP " + r.code())); return; }
                cb.ok(r.body());
            }
            @Override public void onFailure(@NonNull Call<PreSessionModels.Status> call, @NonNull Throwable t) { cb.err(t); }
        });
    }

    public void submit(Map<String, Object> answers, SubmitCb cb) {
        Map<String, Object> body = new HashMap<>();
        body.put("answers", answers);
        api.submit(body).enqueue(new Callback<PreSessionModels.SubmitResult>() {
            @Override public void onResponse(@NonNull Call<PreSessionModels.SubmitResult> call, @NonNull Response<PreSessionModels.SubmitResult> r) {
                if (!r.isSuccessful()) { cb.err(new RuntimeException("HTTP " + r.code())); return; }
                cb.ok();
            }
            @Override public void onFailure(@NonNull Call<PreSessionModels.SubmitResult> call, @NonNull Throwable t) { cb.err(t); }
        });
    }
}
