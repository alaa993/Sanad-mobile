package com.brightpath.sanad.push;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.HTTP;
import retrofit2.http.POST;
import retrofit2.http.PUT;

public interface PushDeviceApi {
    class RegisterBody {
        public String token;
        public String platform;
        public String device_id;
    }

    class UnregisterBody {
        public String token;
    }

    class PreferencesBody {
        public boolean push_enabled;
    }

    class PreferencesResponse {
        public boolean push_enabled;
    }

    class OkResponse {
        public boolean ok;
    }

    @GET("api/v1/push-preferences")
    Call<PreferencesResponse> getPreferences();

    @PUT("api/v1/push-preferences")
    Call<PreferencesResponse> updatePreferences(@Body PreferencesBody body);

    @POST("api/v1/devices")
    Call<OkResponse> register(@Body RegisterBody body);

    @HTTP(method = "DELETE", path = "api/v1/devices", hasBody = true)
    Call<OkResponse> unregister(@Body UnregisterBody body);
}
