package com.brightpath.sanad.feature.profile;

import java.util.Map;
import retrofit2.Call;
import retrofit2.http.POST;

public interface ProfileApi {
    @POST("api/v1/specialist/resubmit")
    Call<Map<String, Object>> resubmitSpecialist();

    @POST("api/v1/org/resubmit")
    Call<Map<String, Object>> resubmitOrg();
}
