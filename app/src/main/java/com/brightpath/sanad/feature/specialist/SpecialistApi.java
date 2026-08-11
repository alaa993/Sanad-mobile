
package com.brightpath.sanad.feature.specialist;
import retrofit2.*; import retrofit2.http.*; import java.util.*; 
public interface SpecialistApi {
  @GET("api/v1/specialist/dashboard") Call<SpecialistModels.Dashboard> dashboard();
  @GET("api/v1/specialist/sessions") Call<SpecialistModels.Appointments> sessions(@Query("scope") String scope);
  @POST("api/v1/specialist/sessions/{id}/accept") Call<SpecialistModels.Simple> accept(@Path("id") int id);
  @POST("api/v1/specialist/sessions/{id}/reject") Call<SpecialistModels.Simple> reject(@Path("id") int id, @Body Map<String,Object> body);
  @POST("api/v1/specialist/sessions/{id}/reschedule") Call<SpecialistModels.Simple> reschedule(@Path("id") int id, @Body Map<String,Object> body);
  @POST("api/v1/specialist/sessions/{id}/extend") Call<SpecialistModels.Simple> extend(@Path("id") int id, @Body Map<String,Object> body);
  @POST("api/v1/specialist/sessions/{id}/complete") Call<SpecialistModels.Simple> complete(@Path("id") int id);
  @GET("api/v1/specialist/profile") Call<SpecialistModels.Profile> profile();
  @PUT("api/v1/specialist/profile") Call<SpecialistModels.Simple> update(@Body Map<String,Object> body);
  @GET("api/v1/specialist/documents") Call<SpecialistModels.DocumentList> documents();
  @GET("api/v1/specialist/patients") Call<SpecialistModels.Patients> patients();
  @Multipart @POST("api/v1/specialist/documents")
  Call<SpecialistModels.Document> uploadDocument(@Part("type") okhttp3.RequestBody type,
                                                 @Part("title") okhttp3.RequestBody title,
                                                 @Part okhttp3.MultipartBody.Part file);
  @Multipart @POST("api/v1/specialist/profile/avatar")
  Call<java.util.Map<String,String>> uploadAvatar(@Part okhttp3.MultipartBody.Part avatar);
  @DELETE("api/v1/specialist/documents/{id}") Call<SpecialistModels.Simple> deleteDocument(@Path("id") int id);
}
