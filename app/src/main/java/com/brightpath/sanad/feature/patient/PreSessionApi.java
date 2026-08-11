package com.brightpath.sanad.feature.patient;

import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface PreSessionApi {
    @GET("api/v1/patient/pre-session-survey")
    Call<PreSessionModels.Status> status();

    @POST("api/v1/patient/pre-session-survey")
    Call<PreSessionModels.SubmitResult> submit(@Body Map<String, Object> body);
}
