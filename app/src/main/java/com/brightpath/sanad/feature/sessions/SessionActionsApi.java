package com.brightpath.sanad.feature.sessions;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface SessionActionsApi {
    @POST("api/v1/sessions/{id}/extend")
    Call<SessionActionsRepository.ExtendResponse> extend(@Path("id") int sessionId, @Body java.util.Map<String,Integer> body);

    @GET("api/v1/sessions/{id}/tasks")
    Call<SessionActionsRepository.TaskListResponse> listTasks(@Path("id") int sessionId);

    @POST("api/v1/sessions/{id}/tasks")
    Call<SessionActionsRepository.TaskResponse> addTask(@Path("id") int sessionId, @Body java.util.Map<String,Object> body);

    @POST("api/v1/sessions/tasks/{taskId}/complete")
    Call<SessionActionsRepository.TaskResponse> completeTask(@Path("taskId") int taskId, @Body java.util.Map<String,String> body);

    @POST("api/v1/sessions/{id}/rate-specialist")
    Call<java.util.Map<String,Boolean>> rateSpecialist(@Path("id") int sessionId, @Body java.util.Map<String,Object> body);

    @POST("api/v1/sessions/{id}/rate-patient")
    Call<java.util.Map<String,Boolean>> ratePatient(@Path("id") int sessionId, @Body java.util.Map<String,Object> body);

    @POST("api/v1/sessions/{id}/cancel")
    Call<java.util.Map<String,Boolean>> cancel(@Path("id") int sessionId, @Body java.util.Map<String,Object> body);

    @POST("api/v1/sessions/{id}/complete")
    Call<java.util.Map<String,Object>> complete(@Path("id") int sessionId, @Body java.util.Map<String,Object> body);

    @POST("api/v1/sessions/{id}/survey")
    Call<java.util.Map<String,Object>> survey(@Path("id") int sessionId, @Body java.util.Map<String,Object> body);
}
