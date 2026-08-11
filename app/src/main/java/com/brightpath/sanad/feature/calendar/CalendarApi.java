
package com.brightpath.sanad.feature.calendar;
import retrofit2.Call; import retrofit2.http.*; import java.util.Map;
public interface CalendarApi {
  @GET("api/v1/cal/availability") Call<CalendarModels.Availability> availability();
  @POST("api/v1/cal/availability") Call<CalendarModels.Simple> addSlot(@Body Map<String,Object> body);
  @DELETE("api/v1/cal/availability/{id}") Call<CalendarModels.Simple> delSlot(@Path("id") int id);
  @POST("api/v1/cal/block") Call<CalendarModels.Simple> addBlock(@Body Map<String,Object> body);
  @DELETE("api/v1/cal/block/{id}") Call<CalendarModels.Simple> delBlock(@Path("id") int id);

  @GET("api/v1/cal/appointments") Call<CalendarModels.Appointments> appointments(@Query("scope") String scope, @Query("from") String from, @Query("to") String to);
  @POST("api/v1/cal/appointments") Call<CalendarModels.Simple> createAppointment(@Body Map<String,Object> body);
  @POST("api/v1/cal/appointments/recurring") Call<CalendarModels.RecurringResponse> createRecurring(@Body Map<String,Object> body);
  @POST("api/v1/cal/appointments/{id}/cancel") Call<CalendarModels.Simple> cancel(@Path("id") int id);
  @POST("api/v1/cal/appointments/{id}/accept") Call<CalendarModels.Simple> accept(@Path("id") int id);
  @POST("api/v1/cal/appointments/{id}/reject") Call<CalendarModels.Simple> reject(@Path("id") int id);
  @POST("api/v1/cal/appointments/{id}/reschedule") Call<CalendarModels.Simple> reschedule(@Path("id") int id, @Body Map<String,Object> body);

  @GET("api/v1/cal/suggested-slots") Call<CalendarModels.Suggested> suggested(@Query("specialist_id") int specialistId, @Query("date") String date);
}
