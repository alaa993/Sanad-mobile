package com.brightpath.sanad.feature.social;

import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface AnonymousMatchApi {
    @GET("api/v1/anonymous/status") Call<AnonymousMatchModels.StatusResponse> status();
    @POST("api/v1/anonymous/join") Call<AnonymousMatchModels.StatusResponse> join(@Body Map<String, String> body);
    @POST("api/v1/anonymous/leave") Call<Map<String, Boolean>> leave();
    @POST("api/v1/anonymous/{id}/end") Call<Map<String, Boolean>> end(@Path("id") int id);
    @POST("api/v1/anonymous/{id}/report") Call<Map<String, Boolean>> report(@Path("id") int id, @Body Map<String, String> body);
}
