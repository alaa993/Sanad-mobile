package com.brightpath.sanad.feature.sessions;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.http.Path;

public interface DirectoryApi {
    @GET("api/v1/specialists")
    Call<DirectoryModels.Paged> specialists(
            @Query("search") String search,
            @Query("page") Integer page,
            @Query("specialty") String specialty,
            @Query("language") String language,
            @Query("min_rating") String minRating);
    @GET("api/v1/specialists/{id}")
    Call<DirectoryModels.Detail> specialist(@Path("id") int id);
    @GET("api/v1/organizations")
    Call<DirectoryModels.Paged> organizations(@Query("search") String search, @Query("page") Integer page);
}
