package com.brightpath.sanad.feature.community;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface VentApi {
    @GET("api/v1/vent") Call<VentModels.VentList> list();
    @POST("api/v1/vent") Call<VentModels.VentPost> create(@Body VentModels.VentCreate body);
    @POST("api/v1/vent/{id}/react") Call<VentModels.ReactResponse> react(@retrofit2.http.Path("id") int id, @Body VentModels.ReactRequest body);
    @POST("api/v1/vent/{id}/report") Call<VentModels.ReportResponse> report(@retrofit2.http.Path("id") int id, @Body VentModels.ReportRequest body);
    @POST("api/v1/vent/chat") Call<VentModels.ChatResponse> chat(@Body VentModels.ChatRequest body);
}
