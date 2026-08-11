package com.brightpath.sanad.feature.notifications;

import android.content.Context;

import com.brightpath.sanad.data.ApiClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationsRepository {
    private final NotificationsApi api;

    public NotificationsRepository(Context ctx) {
        api = ApiClient.get(ctx).create(NotificationsApi.class);
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

    public void list(Cb<NotificationsApi.NotificationList> cb) {
        api.list().enqueue(wrap(cb));
    }
}
