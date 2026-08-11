package com.brightpath.sanad.data;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface LibraryApi {
    @GET("api/v1/library")
    Call<List<LibraryModels.Category>> getLibrary(@retrofit2.http.Query("tag") String tag);

    @GET("api/v1/library/tags")
    Call<LibraryModels.TagsResponse> getTags();

    @GET("api/v1/library/daily-tip")
    Call<LibraryModels.DailyTip> getDailyTip();

    @GET("api/v1/library/curated/syria-europe")
    Call<LibraryModels.CuratedResponse> getCuratedSyriaEurope();

    @GET("api/v1/library/{id}")
    Call<LibraryModels.ArticleDetail> getArticle(@Path("id") int id);

    @POST("api/v1/library/{id}/favorite")
    Call<LibraryModels.FavoriteResponse> favoriteArticle(@Path("id") int id);

    @retrofit2.http.DELETE("api/v1/library/{id}/favorite")
    Call<LibraryModels.FavoriteResponse> unfavoriteArticle(@Path("id") int id);

    @POST("api/v1/library/articles")
    Call<LibraryModels.ArticleDetail> createArticle(@Body java.util.Map<String, Object> body);
}
