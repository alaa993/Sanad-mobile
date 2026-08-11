package com.brightpath.sanad.push;

import android.content.Context;
import com.brightpath.sanad.data.ApiClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PushDeviceRepository {
    private final PushDeviceApi api;

    public PushDeviceRepository(Context ctx) {
        api = ApiClient.get(ctx).create(PushDeviceApi.class);
    }

    public interface SimpleCb {
        void ok();
        void err(Throwable t);
    }

    public interface PrefCb {
        void ok(boolean enabled);
        void err(Throwable t);
    }

    public void registerToken(String token, SimpleCb cb) {
        PushDeviceApi.RegisterBody body = new PushDeviceApi.RegisterBody();
        body.token = token;
        body.platform = "android";
        api.register(body).enqueue(wrapOk(cb));
    }

    public void unregisterToken(String token, SimpleCb cb) {
        PushDeviceApi.UnregisterBody body = new PushDeviceApi.UnregisterBody();
        body.token = token;
        api.unregister(body).enqueue(wrapOk(cb));
    }

    public void loadPreferences(PrefCb cb) {
        api.getPreferences().enqueue(new Callback<PushDeviceApi.PreferencesResponse>() {
            @Override
            public void onResponse(Call<PushDeviceApi.PreferencesResponse> call, Response<PushDeviceApi.PreferencesResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    cb.ok(response.body().push_enabled);
                } else {
                    cb.err(new Exception("HTTP " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<PushDeviceApi.PreferencesResponse> call, Throwable t) {
                cb.err(t);
            }
        });
    }

    public void updatePreferences(boolean enabled, PrefCb cb) {
        PushDeviceApi.PreferencesBody body = new PushDeviceApi.PreferencesBody();
        body.push_enabled = enabled;
        api.updatePreferences(body).enqueue(new Callback<PushDeviceApi.PreferencesResponse>() {
            @Override
            public void onResponse(Call<PushDeviceApi.PreferencesResponse> call, Response<PushDeviceApi.PreferencesResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    cb.ok(response.body().push_enabled);
                } else {
                    cb.err(new Exception("HTTP " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<PushDeviceApi.PreferencesResponse> call, Throwable t) {
                cb.err(t);
            }
        });
    }

    private Callback<PushDeviceApi.OkResponse> wrapOk(SimpleCb cb) {
        return new Callback<PushDeviceApi.OkResponse>() {
            @Override
            public void onResponse(Call<PushDeviceApi.OkResponse> call, Response<PushDeviceApi.OkResponse> response) {
                if (response.isSuccessful()) cb.ok();
                else cb.err(new Exception("HTTP " + response.code()));
            }

            @Override
            public void onFailure(Call<PushDeviceApi.OkResponse> call, Throwable t) {
                cb.err(t);
            }
        };
    }
}
