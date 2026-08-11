package com.brightpath.sanad.feature.sessions;

import retrofit2.Call;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface LiveKitApi {
    @POST("api/v1/sessions/{id}/livekit-token")
    Call<LiveKitRepository.TokenResponse> sessionToken(@Path("id") int sessionId);

    @POST("api/v1/group-sessions/{id}/livekit-token")
    Call<LiveKitRepository.TokenResponse> groupToken(@Path("id") int groupId);
}
