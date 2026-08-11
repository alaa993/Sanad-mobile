
package com.brightpath.sanad.feature.reports;
import android.content.Context; import com.brightpath.sanad.data.ApiClient; import okhttp3.ResponseBody; import retrofit2.*;
public class ReportsRepository {
  private final ReportsApi api; public ReportsRepository(Context ctx){ api = ApiClient.get(ctx).create(ReportsApi.class); }
  public interface Cb<T>{ void ok(T d); void err(Throwable e); }
  private static <T> Callback<T> wrap(Cb<T> cb){ return new Callback<T>(){ public void onResponse(Call<T> c, retrofit2.Response<T> r){ if(r.isSuccessful()&&r.body()!=null) cb.ok(r.body()); else cb.err(new Exception("HTTP "+r.code())); } public void onFailure(Call<T> c, Throwable t){ cb.err(t);} }; }
  public void overview(String f,String t, Cb<ReportsApi.OverviewResponse> cb){ api.overview(f,t).enqueue(wrap(cb)); }
  public void sessions(String f,String t, Cb<ReportsApi.SeriesResponse> cb){ api.sessions(f,t).enqueue(wrap(cb)); }
  public void users(String f,String t, Cb<ReportsApi.SeriesResponse> cb){ api.users(f,t).enqueue(wrap(cb)); }
  public void revenue(String f,String t, Cb<ReportsApi.SeriesResponse> cb){ api.revenue(f,t).enqueue(wrap(cb)); }
  public void topSpec(String f,String t, Cb<ReportsApi.TopResponse> cb){ api.topSpecialists(f,t).enqueue(wrap(cb)); }
  public void topOrg(String f,String t, Cb<ReportsApi.TopResponse> cb){ api.topOrganizations(f,t).enqueue(wrap(cb)); }
  public void retention(String f,String t, Cb<ReportsApi.CohortsResponse> cb){ api.retention(f,t).enqueue(wrap(cb)); }
  public void conversion(String f,String t, Cb<ReportsApi.FunnelResponse> cb){ api.conversion(f,t).enqueue(wrap(cb)); }
  public void exportCsv(String f, String t, Cb<String> cb){
    api.exportCsv("overview", f, t).enqueue(new Callback<ResponseBody>() {
      public void onResponse(Call<ResponseBody> c, retrofit2.Response<ResponseBody> r){
        if(!r.isSuccessful()||r.body()==null){ cb.err(new Exception("HTTP "+r.code())); return; }
        try { cb.ok(r.body().string()); }
        catch (Exception e){ cb.err(e); }
      }
      public void onFailure(Call<ResponseBody> c, Throwable t){ cb.err(t); }
    });
  }
}
