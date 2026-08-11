package com.brightpath.sanad.feature.profile;

import android.content.Context;
import androidx.annotation.NonNull;
import com.brightpath.sanad.data.ApiClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.GET;

public class SettingsRepository {
    interface SettingsApi {
        @GET("api/v1/settings") Call<SettingsResponse> settings();
    }

    public interface Listener {
        void onSuccess(SettingsResponse settings);
        void onError(Throwable t);
    }

    private final SettingsApi api;

    public SettingsRepository(Context context) {
        api = ApiClient.get(context).create(SettingsApi.class);
    }

    public void fetch(Listener listener) {
        api.settings().enqueue(new Callback<SettingsResponse>() {
            @Override
            public void onResponse(@NonNull Call<SettingsResponse> call, @NonNull Response<SettingsResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    listener.onError(new RuntimeException("bad_response"));
                    return;
                }
                listener.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<SettingsResponse> call, @NonNull Throwable t) {
                listener.onError(t);
            }
        });
    }
}
