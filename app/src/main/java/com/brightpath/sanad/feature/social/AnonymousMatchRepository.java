package com.brightpath.sanad.feature.social;

import android.content.Context;
import com.brightpath.sanad.data.ApiClient;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AnonymousMatchRepository {
    private final AnonymousMatchApi api;

    public AnonymousMatchRepository(Context ctx) {
        api = ApiClient.get(ctx).create(AnonymousMatchApi.class);
    }

    public interface Cb<T> { void ok(T t); void err(Throwable e); }

    private static <T> Callback<T> wrap(Cb<T> cb) {
        return new Callback<T>() {
            @Override public void onResponse(Call<T> c, Response<T> r) {
                if (r.isSuccessful() && r.body() != null) cb.ok(r.body());
                else cb.err(new Exception("HTTP " + r.code()));
            }
            @Override public void onFailure(Call<T> c, Throwable t) { cb.err(t); }
        };
    }

    public void status(Cb<AnonymousMatchModels.StatusResponse> cb) {
        api.status().enqueue(wrap(cb));
    }

    public void join(String gender, String matchGender, String mode, Cb<AnonymousMatchModels.StatusResponse> cb) {
        Map<String, String> body = new HashMap<>();
        body.put("gender", gender);
        body.put("match_gender", matchGender);
        body.put("mode", mode);
        api.join(body).enqueue(wrap(cb));
    }

    public void leave(Cb<Map<String, Boolean>> cb) {
        api.leave().enqueue(wrap(cb));
    }

    public void end(int id, Cb<Map<String, Boolean>> cb) {
        api.end(id).enqueue(wrap(cb));
    }

    public void report(int id, Cb<Map<String, Boolean>> cb) {
        Map<String, String> body = new HashMap<>();
        api.report(id, body).enqueue(wrap(cb));
    }
}
