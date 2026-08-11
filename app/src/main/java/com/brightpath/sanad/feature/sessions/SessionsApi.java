package com.brightpath.sanad.feature.sessions;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;
public interface SessionsApi {
    @GET("api/v1/sessions") Call<SessionModels.SessionList> getSessions();
    @GET("api/v1/sessions") Call<SessionModels.SessionList> getSessions(@QueryMap Map<String, String> params);
    @GET("api/v1/sessions/{id}") Call<SessionModels.Session> getSession(@Path("id") int id);

    @POST("api/v1/sessions")
    Call<SessionModels.Session> book(@Body BookRequest req);
}
