package com.brightpath.sanad.data;

import android.content.Context;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardRepository {
    private final ApiService api;

    public DashboardRepository(Context context) {
        api = ApiClient.get(context).create(ApiService.class);
    }

    public interface Listener {
        void onSuccess(DashboardResponse d);
        void onError(Throwable t);
    }

    public void fetch(Listener l) {
        api.getDashboard().enqueue(new Callback<DashboardResponse>() {
            @Override
            public void onResponse(Call<DashboardResponse> call, Response<DashboardResponse> r) {
                if (!r.isSuccessful() || r.body() == null) {
                    l.onError(new RuntimeException("bad_response"));
                    return;
                }
                l.onSuccess(r.body());
            }

            @Override
            public void onFailure(Call<DashboardResponse> call, Throwable t) {
                l.onError(t);
            }
        });
    }
}
