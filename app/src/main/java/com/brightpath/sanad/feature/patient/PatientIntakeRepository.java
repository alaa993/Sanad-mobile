package com.brightpath.sanad.feature.patient;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.brightpath.sanad.R;
import com.brightpath.sanad.data.ApiClient;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public class PatientIntakeRepository {
    private interface IntakeApi {
        @Headers({"Accept: application/json", "Content-Type: application/json"})
        @POST("api/v1/patient/intake")
        Call<Map<String, Object>> submit(@Body Map<String, Object> payload);
    }

    private static final String PREF = "patient_intake";
    private static final String KEY_FORM = "form";
    private final SharedPreferences prefs;
    private final IntakeApi api;
    private final Context context;
    private final MutableLiveData<PatientIntakeStatus> status = new MutableLiveData<>(PatientIntakeStatus.idle());

    public PatientIntakeRepository(Context context){
        this.context = context.getApplicationContext();
        prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        api = ApiClient.get(context).create(IntakeApi.class);
    }

    public PatientIntakeForm load(){
        String raw = prefs.getString(KEY_FORM, null);
        return PatientIntakeForm.fromJson(raw);
    }

    public LiveData<PatientIntakeStatus> getStatus() {
        return status;
    }

    public void resetStatus() {
        status.postValue(PatientIntakeStatus.idle());
    }

    public void save(PatientIntakeForm form){
        if (form == null) return;
        status.postValue(PatientIntakeStatus.loading(context.getString(R.string.patient_intake_saving_message)));
        prefs.edit().putString(KEY_FORM, form.toJson().toString()).apply();
        tryUpload(form);
    }

    private void tryUpload(PatientIntakeForm form){
        if (api == null || form == null) return;
        Map<String, Object> payload = form.toApiPayload();
        api.submit(payload).enqueue(new Callback<Map<String, Object>>() {
            @Override public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    status.postValue(PatientIntakeStatus.success(context.getString(R.string.patient_intake_saved)));
                } else {
                    status.postValue(PatientIntakeStatus.error(readErrorMessage(response)));
                }
            }

            @Override public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                status.postValue(PatientIntakeStatus.error(context.getString(R.string.patient_intake_submit_failed)));
            }
        });
    }

    private String readErrorMessage(@NonNull Response<?> response) {
        String fallback = context.getString(R.string.patient_intake_submit_failed);
        try {
            if (response.errorBody() == null) return fallback;
            String raw = response.errorBody().string();
            if (raw == null || raw.trim().isEmpty()) return fallback;
            JsonObject json = JsonParser.parseString(raw).getAsJsonObject();
            if (json.has("message") && !json.get("message").isJsonNull()) {
                return json.get("message").getAsString();
            }
            if (json.has("errors") && json.get("errors").isJsonObject()) {
                JsonObject errors = json.getAsJsonObject("errors");
                for (String key : errors.keySet()) {
                    if (errors.get(key).isJsonArray() && errors.getAsJsonArray(key).size() > 0) {
                        return errors.getAsJsonArray(key).get(0).getAsString();
                    }
                }
            }
        } catch (Exception ignored) {
            return fallback;
        }
        return fallback;
    }
}
