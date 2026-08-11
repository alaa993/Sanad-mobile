package com.brightpath.sanad.feature.coach;

import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.*;

public interface CoachApi {
    @GET("api/v1/coach/programs") Call<CoachModels.ProgramListResponse> programs();
    @POST("api/v1/coach/programs") Call<CoachModels.ProgramDetail> create(@Body Map<String, Object> body);
    @GET("api/v1/coach/programs/{id}") Call<CoachModels.ProgramDetail> show(@Path("id") int id);
    @POST("api/v1/coach/programs/{id}/checkins") Call<CoachModels.Checkin> checkin(@Path("id") int id, @Body Map<String, Object> body);
    @POST("api/v1/coach/items/{itemId}/complete") Call<Map<String, Object>> completeItem(@Path("itemId") int itemId);
}
