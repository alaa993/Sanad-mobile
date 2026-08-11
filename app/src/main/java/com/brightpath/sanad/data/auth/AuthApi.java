package com.brightpath.sanad.data.auth;

import com.brightpath.sanad.models.LoginRequest;
import com.brightpath.sanad.models.LoginResponse;
import com.brightpath.sanad.models.User;
import com.brightpath.sanad.models.RegisterRequest;

import retrofit2.Call;
import retrofit2.http.Headers;
import retrofit2.http.PUT;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface AuthApi {
    @Headers("Accept: application/json")
    @POST("/api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest body);

    @Headers("Accept: application/json")
    @POST("/api/auth/register")
    Call<LoginResponse> register(@Body RegisterRequest body);

    @Headers("Accept: application/json")
    @GET("/api/auth/me")
    Call<User> me();

    @Headers("Accept: application/json")
    @POST("/api/auth/logout")
    Call<Void> logout();

    @Headers("Accept: application/json")
    @PUT("/api/v1/profile")
    Call<java.util.Map<String, Object>> updateProfile(@Body java.util.Map<String, String> body);

    @Headers("Accept: application/json")
    @POST("/api/v1/profile/password")
    Call<java.util.Map<String,Boolean>> updatePassword(@Body java.util.Map<String,String> body);

    @Headers("Accept: application/json")
    @POST("/api/auth/security-answer")
    Call<java.util.Map<String,Boolean>> saveSecurityAnswer(@Body java.util.Map<String,String> body);

    @Headers("Accept: application/json")
    @POST("/api/auth/forgot/lookup")
    Call<java.util.Map<String, Object>> forgotLookup(@Body java.util.Map<String, String> body);

    @Headers("Accept: application/json")
    @POST("/api/auth/forgot/reset")
    Call<java.util.Map<String,Boolean>> resetPasswordWithAnswer(@Body java.util.Map<String,String> body);
}
