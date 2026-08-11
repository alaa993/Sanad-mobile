
package com.brightpath.sanad.feature.reports;
import okhttp3.ResponseBody;
import retrofit2.*; import retrofit2.http.*; import java.util.*;
public interface ReportsApi {
  @GET("api/v1/reports/overview") Call<OverviewResponse> overview(@Query("from") String from, @Query("to") String to);
  @GET("api/v1/reports/timeseries/sessions") Call<SeriesResponse> sessions(@Query("from") String from, @Query("to") String to);
  @GET("api/v1/reports/timeseries/users") Call<SeriesResponse> users(@Query("from") String from, @Query("to") String to);
  @GET("api/v1/reports/timeseries/revenue") Call<SeriesResponse> revenue(@Query("from") String from, @Query("to") String to);
  @GET("api/v1/reports/top/specialists") Call<TopResponse> topSpecialists(@Query("from") String from, @Query("to") String to);
  @GET("api/v1/reports/top/organizations") Call<TopResponse> topOrganizations(@Query("from") String from, @Query("to") String to);
  @GET("api/v1/reports/retention") Call<CohortsResponse> retention(@Query("from") String from, @Query("to") String to);
  @GET("api/v1/reports/conversion") Call<FunnelResponse> conversion(@Query("from") String from, @Query("to") String to);
  @Streaming @GET("api/v1/reports/export/csv") Call<ResponseBody> exportCsv(@Query("type") String type, @Query("from") String from, @Query("to") String to);
  class OverviewResponse { public java.util.List<Card> cards; public Period period; static class Period { public String from; public String to; } static class Card { public String key; public String value; } }
  class SeriesResponse { public java.util.List<Point> data; static class Point { public String d; public double v; } }
  class TopResponse { public java.util.List<Top> data; static class Top { public int id; public String name; public double sessions; public double avg_rating; } }
  class CohortsResponse { public java.util.List<Row> data; static class Row { public String week; public int users; public int retained; } }
  class FunnelResponse { public java.util.List<Stage> data; static class Stage { public String stage; public int value; } }
}
