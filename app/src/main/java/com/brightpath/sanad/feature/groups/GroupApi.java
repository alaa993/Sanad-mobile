package com.brightpath.sanad.feature.groups;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import java.util.Map;

public interface GroupApi {
    @GET("api/v1/group-sessions")
    Call<GroupModels.GroupSessionList> list(@Query("age_category") String ageCategory, @Query("disorder_tag") String disorderTag);

    @GET("api/v1/group-sessions/{id}")
    Call<GroupModels.GroupSession> detail(@Path("id") int id);

    @POST("api/v1/group-sessions")
    Call<GroupModels.GroupSession> create(@Body Map<String,Object> body);

    @POST("api/v1/group-sessions/{id}/join")
    Call<GroupModels.GroupSession> join(@Path("id") int id, @Body Map<String,Object> body);

    @POST("api/v1/group-sessions/{id}/leave")
    Call<GroupModels.GroupSession> leave(@Path("id") int id, @Body Map<String,Object> body);
}
