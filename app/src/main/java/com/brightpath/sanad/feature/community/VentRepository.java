package com.brightpath.sanad.feature.community;

import android.content.Context;

import com.brightpath.sanad.data.ApiClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VentRepository {
    private final VentApi api;

    public VentRepository(Context ctx) {
        api = ApiClient.get(ctx).create(VentApi.class);
    }

    public interface Cb<T> {
        void ok(T t);
        void err(Throwable e);
    }

    private static <T> Callback<T> wrap(Cb<T> cb) {
        return new Callback<T>() {
            @Override
            public void onResponse(Call<T> c, Response<T> r) {
                if (r.isSuccessful() && r.body() != null) cb.ok(r.body());
                else cb.err(new Exception("HTTP " + r.code()));
            }

            @Override
            public void onFailure(Call<T> c, Throwable t) {
                cb.err(t);
            }
        };
    }

    public void list(Cb<VentModels.VentList> cb) {
        api.list().enqueue(wrap(cb));
    }

    public void create(String body, Cb<VentModels.VentPost> cb) {
        api.create(new VentModels.VentCreate(body)).enqueue(wrap(cb));
    }

    public void react(int postId, String type, Cb<VentModels.ReactResponse> cb) {
        api.react(postId, new VentModels.ReactRequest(type)).enqueue(wrap(cb));
    }

    public void report(int postId, String reason, Cb<VentModels.ReportResponse> cb) {
        api.report(postId, new VentModels.ReportRequest(reason)).enqueue(wrap(cb));
    }
}
