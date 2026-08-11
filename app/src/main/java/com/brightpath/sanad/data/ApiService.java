package com.brightpath.sanad.data;

import java.util.Map;
import retrofit2.Call;
import retrofit2.http.GET;

public interface ApiService {
    @GET("api/ping")
    Call<Map<String,Object>> ping();

    @GET("api/bootstrap")
    Call<Map<String,Object>> bootstrap();

    @GET("api/v1/dashboard")
    Call<DashboardResponse> getDashboard();
}

