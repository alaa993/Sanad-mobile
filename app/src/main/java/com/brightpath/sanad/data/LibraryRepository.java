package com.brightpath.sanad.data;

import android.content.Context;
import androidx.annotation.NonNull;

import com.brightpath.sanad.feature.community.CommunityModels;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Library categories, article detail, daily tip, and curated lists via LibraryApi. */
public class LibraryRepository {
    private final LibraryApi api;

    public LibraryRepository(Context ctx) {
        api = ApiClient.get(ctx).create(LibraryApi.class);
    }

    public interface ListListener {
        void onSuccess(List<LibraryModels.Category> data);
        void onError(Throwable t);
    }
    public interface ArticleListener {
        void onSuccess(LibraryModels.ArticleDetail detail);
        void onError(Throwable t);
    }
    public interface CreateListener {
        void onSuccess();
        void onError(Throwable t);
    }
    public interface DailyTipListener {
        void onSuccess(LibraryModels.DailyTip tip);
        void onError(Throwable t);
    }
    public interface TagsListener {
        void onSuccess(java.util.List<String> tags);
        void onError(Throwable t);
    }
    public interface CuratedListener {
        void onSuccess(LibraryModels.CuratedResponse data);
        void onError(Throwable t);
    }

    public void fetchLibrary(String tag, ListListener l){
        api.getLibrary(tag).enqueue(new Callback<List<LibraryModels.Category>>() {
            @Override public void onResponse(@NonNull Call<List<LibraryModels.Category>> call, @NonNull Response<List<LibraryModels.Category>> r) {
                if (!r.isSuccessful() || r.body()==null) { l.onError(new RuntimeException("bad_response")); return; }
                l.onSuccess(r.body());
            }
            @Override public void onFailure(@NonNull Call<List<LibraryModels.Category>> call, @NonNull Throwable t) { l.onError(t); }
        });
    }

    public void fetchTags(TagsListener l){
        api.getTags().enqueue(new Callback<LibraryModels.TagsResponse>() {
            @Override public void onResponse(@NonNull Call<LibraryModels.TagsResponse> call, @NonNull Response<LibraryModels.TagsResponse> r) {
                if (!r.isSuccessful() || r.body()==null) { l.onError(new RuntimeException("bad_response")); return; }
                l.onSuccess(r.body().data != null ? r.body().data : new java.util.ArrayList<>());
            }
            @Override public void onFailure(@NonNull Call<LibraryModels.TagsResponse> call, @NonNull Throwable t) { l.onError(t); }
        });
    }

    public void fetchArticle(int id, ArticleListener l){
        api.getArticle(id).enqueue(new Callback<LibraryModels.ArticleDetail>() {
            @Override public void onResponse(@NonNull Call<LibraryModels.ArticleDetail> call, @NonNull Response<LibraryModels.ArticleDetail> r) {
                if (!r.isSuccessful() || r.body()==null) { l.onError(new RuntimeException("bad_response")); return; }
                l.onSuccess(r.body());
            }
            @Override public void onFailure(@NonNull Call<LibraryModels.ArticleDetail> call, @NonNull Throwable t) { l.onError(t); }
        });
    }

    public interface FavoriteListener {
        void onSuccess(boolean favorited);
        void onError(Throwable t);
    }

    public void favoriteArticle(int id, FavoriteListener l) {
        api.favoriteArticle(id).enqueue(new Callback<LibraryModels.FavoriteResponse>() {
            @Override public void onResponse(@NonNull Call<LibraryModels.FavoriteResponse> call, @NonNull Response<LibraryModels.FavoriteResponse> r) {
                if (!r.isSuccessful()) { l.onError(new RuntimeException("bad_response")); return; }
                boolean fav = r.body() != null && r.body().favorited != null ? r.body().favorited : true;
                l.onSuccess(fav);
            }
            @Override public void onFailure(@NonNull Call<LibraryModels.FavoriteResponse> call, @NonNull Throwable t) { l.onError(t); }
        });
    }

    public void unfavoriteArticle(int id, FavoriteListener l) {
        api.unfavoriteArticle(id).enqueue(new Callback<LibraryModels.FavoriteResponse>() {
            @Override public void onResponse(@NonNull Call<LibraryModels.FavoriteResponse> call, @NonNull Response<LibraryModels.FavoriteResponse> r) {
                if (!r.isSuccessful()) { l.onError(new RuntimeException("bad_response")); return; }
                boolean fav = r.body() != null && r.body().favorited != null ? r.body().favorited : false;
                l.onSuccess(fav);
            }
            @Override public void onFailure(@NonNull Call<LibraryModels.FavoriteResponse> call, @NonNull Throwable t) { l.onError(t); }
        });
    }

    public void fetchCuratedSyriaEurope(CuratedListener l) {
        api.getCuratedSyriaEurope().enqueue(new Callback<LibraryModels.CuratedResponse>() {
            @Override public void onResponse(@NonNull Call<LibraryModels.CuratedResponse> call, @NonNull Response<LibraryModels.CuratedResponse> r) {
                if (!r.isSuccessful() || r.body() == null) { l.onError(new RuntimeException("bad_response")); return; }
                l.onSuccess(r.body());
            }
            @Override public void onFailure(@NonNull Call<LibraryModels.CuratedResponse> call, @NonNull Throwable t) { l.onError(t); }
        });
    }

    public void fetchDailyTip(DailyTipListener l){
        api.getDailyTip().enqueue(new Callback<LibraryModels.DailyTip>() {
            @Override public void onResponse(@NonNull Call<LibraryModels.DailyTip> call, @NonNull Response<LibraryModels.DailyTip> r) {
                if (!r.isSuccessful() || r.body()==null) { l.onError(new RuntimeException("bad_response")); return; }
                l.onSuccess(r.body());
            }
            @Override public void onFailure(@NonNull Call<LibraryModels.DailyTip> call, @NonNull Throwable t) { l.onError(t); }
        });
    }

    public void createArticle(String title, String body, boolean publish, int categoryId, CreateListener l){
        Map<String,Object> payload = new HashMap<>();
        Map<String,String> titleMap = new HashMap<>();
        titleMap.put("ar", title);
        payload.put("title", titleMap);
        Map<String,String> bodyMap = new HashMap<>();
        bodyMap.put("ar", body);
        payload.put("body", bodyMap);
        payload.put("active", publish);
        if (categoryId > 0) {
            payload.put("category_id", categoryId);
        }
        api.createArticle(payload).enqueue(new Callback<LibraryModels.ArticleDetail>() {
            @Override public void onResponse(@NonNull Call<LibraryModels.ArticleDetail> call, @NonNull Response<LibraryModels.ArticleDetail> response) {
                if (response.isSuccessful()) {
                    l.onSuccess();
                } else {
                    String msg = "HTTP " + response.code();
                    if (response.errorBody() != null) {
                        try {
                            String body = response.errorBody().string();
                            if (body != null && !body.isEmpty()) msg = body;
                        } catch (Exception ignored) {}
                    }
                    l.onError(new RuntimeException(msg));
                }
            }
            @Override public void onFailure(@NonNull Call<LibraryModels.ArticleDetail> call, @NonNull Throwable t) { l.onError(t); }
        });
    }
}
