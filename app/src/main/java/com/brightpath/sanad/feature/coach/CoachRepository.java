package com.brightpath.sanad.feature.coach;

import android.content.Context;
import com.brightpath.sanad.data.ApiClient;
import java.util.HashMap;
import java.util.Map;
import retrofit2.*;

public class CoachRepository {
    private final CoachApi api;
    public CoachRepository(Context ctx) { api = ApiClient.get(ctx).create(CoachApi.class); }
    public interface Cb<T> { void ok(T t); void err(Throwable e); }
    private static <T> Callback<T> wrap(Cb<T> cb) {
        return new Callback<T>() {
            @Override public void onResponse(Call<T> c, Response<T> r) {
                if (r.isSuccessful() && r.body() != null) cb.ok(r.body()); else cb.err(new Exception("HTTP "+r.code()));
            }
            @Override public void onFailure(Call<T> c, Throwable t) { cb.err(t); }
        };
    }
    public void list(Cb<CoachModels.ProgramListResponse> cb) { api.programs().enqueue(wrap(cb)); }
    public void create(String category, String title, Cb<CoachModels.ProgramDetail> cb) {
        Map<String, Object> body = new HashMap<>();
        body.put("category", category);
        body.put("title", title);
        api.create(body).enqueue(wrap(cb));
    }
    public void show(int id, Cb<CoachModels.ProgramDetail> cb) { api.show(id).enqueue(wrap(cb)); }
    public void checkin(int id, String mood, String note, Double weightKg, Cb<CoachModels.Checkin> cb) {
        Map<String, Object> body = new HashMap<>();
        if (mood != null) body.put("mood", mood);
        if (note != null) body.put("note", note);
        if (weightKg != null) body.put("weight_kg", weightKg);
        api.checkin(id, body).enqueue(wrap(cb));
    }
    public void toggleItem(int itemId, Cb<Map<String, Object>> cb) { api.completeItem(itemId).enqueue(wrap(cb)); }
}
