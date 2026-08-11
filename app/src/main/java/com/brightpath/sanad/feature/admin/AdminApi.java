
package com.brightpath.sanad.feature.admin;
import retrofit2.*; import retrofit2.http.*;
public interface AdminApi {
  @GET("api/v1/admin/dashboard") Call<AdminModels.Dashboard> dashboard();
  @GET("api/v1/admin/users") Call<AdminModels.Users> users();
  @POST("api/v1/admin/specialists") Call<java.util.Map<String,Object>> createSpecialist(@Body java.util.Map<String,Object> body);
  @GET("api/v1/admin/specialists") Call<AdminModels.Specialists> specialists();
  @PUT("api/v1/admin/daily-tips/{id}") Call<AdminModels.DailyTip> updateDailyTip(@Path("id") int id, @Body java.util.Map<String,Object> body);
  @GET("api/v1/admin/organizations") Call<AdminModels.Organizations> orgs();
  @GET("api/v1/admin/appointments") Call<AdminModels.Appointments> appointments();
  @GET("api/v1/admin/library/posts") Call<AdminModels.Posts> posts();
  @POST("api/v1/admin/library/posts/{id}/toggle") Call<AdminModels.Toggle> toggle(@Path("id") int id);
  @POST("api/v1/admin/specialists/{id}/approve") Call<AdminModels.Toggle> approveSpec(@Path("id") int id);
  @POST("api/v1/admin/specialists/{id}/reject")  Call<AdminModels.Toggle> rejectSpec(@Path("id") int id, @Body AdminModels.RejectRequest body);
  @GET("api/v1/admin/specialists/{id}/documents") Call<AdminModels.SpecialistDocuments> specialistDocs(@Path("id") int id);
  @POST("api/v1/admin/specialists/{id}/review") Call<AdminModels.Toggle> reviewSpec(@Path("id") int id, @Body AdminModels.ReviewRequest body);
  @POST("api/v1/admin/organizations/{id}/approve") Call<AdminModels.Toggle> approveOrg(@Path("id") int id);
  @POST("api/v1/admin/organizations/{id}/reject")  Call<AdminModels.Toggle> rejectOrg(@Path("id") int id, @Body AdminModels.RejectRequest body);
  @GET("api/v1/admin/organizations/{id}") Call<AdminModels.OrganizationDetail> orgDetail(@Path("id") int id);
  @GET("api/v1/admin/profile") Call<AdminModels.AdminProfile> profile();
  @PUT("api/v1/admin/profile") Call<AdminModels.Toggle> updateProfile(@Body java.util.Map<String,Object> body);
  @POST("api/v1/admin/profile/password") Call<AdminModels.Toggle> updatePassword(@Body java.util.Map<String,String> body);
  @Multipart @POST("api/v1/admin/profile/avatar")
  Call<java.util.Map<String,Object>> uploadAvatar(@Part okhttp3.MultipartBody.Part avatar);
  @GET("api/v1/admin/settings") Call<AdminModels.AdminSettings> settings();
  @PUT("api/v1/admin/settings") Call<AdminModels.Toggle> saveSettings(@Body java.util.Map<String,String> body);
  @GET("api/v1/admin/vent/reports") Call<AdminModels.VentReports> ventReports();
  @POST("api/v1/admin/vent/posts/{id}/hide") Call<AdminModels.Toggle> hideVentPost(@Path("id") int id);
  @GET("api/v1/admin/daily-tips") Call<AdminModels.DailyTips> dailyTips();
  @POST("api/v1/admin/daily-tips") Call<AdminModels.DailyTip> createDailyTip(@Body java.util.Map<String,Object> body);
  @DELETE("api/v1/admin/daily-tips/{id}") Call<java.util.Map<String,Object>> deleteDailyTip(@Path("id") int id);
  @POST("api/v1/admin/wallet/coupon") Call<AdminModels.Toggle> createCoupon(@Body java.util.Map<String,Object> body);
  @POST("api/v1/admin/wallet/credit") Call<AdminModels.Toggle> credit(@Body java.util.Map<String,Object> body);
}
