
package com.brightpath.sanad.feature.calendar;
import android.content.Context; import com.brightpath.sanad.data.ApiClient;
import retrofit2.*; import java.util.*;
public class CalendarRepository {
  private final CalendarApi api;
  public CalendarRepository(Context ctx){ api = ApiClient.get(ctx).create(CalendarApi.class); }
  public interface Cb<T>{ void ok(T t); void err(Throwable e); }
  private static <T> Callback<T> wrap(Cb<T> cb){ return new Callback<T>(){
    @Override public void onResponse(Call<T> c, Response<T> r){ if(r.isSuccessful() && r.body()!=null) cb.ok(r.body()); else cb.err(new Exception("HTTP "+r.code())); }
    @Override public void onFailure(Call<T> c, Throwable t){ cb.err(t); }
  };}

  public void availability(Cb<CalendarModels.Availability> cb){ api.availability().enqueue(wrap(cb)); }
  public void addSlot(int weekday, String start, String end, Cb<CalendarModels.Simple> cb){
    Map<String,Object> b=new HashMap<>(); b.put("weekday",weekday); b.put("start_time",start); b.put("end_time",end); api.addSlot(b).enqueue(wrap(cb));
  }
  public void delSlot(int id, Cb<CalendarModels.Simple> cb){ api.delSlot(id).enqueue(wrap(cb)); }
  public void addBlock(String start, String end, String reason, Cb<CalendarModels.Simple> cb){
    Map<String,Object> b=new HashMap<>(); b.put("start_at",start); b.put("end_at",end); if(reason!=null) b.put("reason",reason); api.addBlock(b).enqueue(wrap(cb));
  }
  public void delBlock(int id, Cb<CalendarModels.Simple> cb){ api.delBlock(id).enqueue(wrap(cb)); }
  public void appointments(String scope,String from,String to,Cb<CalendarModels.Appointments> cb){ api.appointments(scope,from,to).enqueue(wrap(cb)); }
  public void createAppointment(int specialistId,String starts,String ends,String notes,Cb<CalendarModels.Simple> cb){
    Map<String,Object> b=new HashMap<>(); b.put("specialist_id",specialistId); b.put("starts_at",starts); b.put("ends_at",ends); if(notes!=null) b.put("notes",notes); api.createAppointment(b).enqueue(wrap(cb));
  }
  public void cancel(int id, Cb<CalendarModels.Simple> cb){ api.cancel(id).enqueue(wrap(cb)); }
  public void accept(int id, Cb<CalendarModels.Simple> cb){ api.accept(id).enqueue(wrap(cb)); }
  public void reject(int id, Cb<CalendarModels.Simple> cb){ api.reject(id).enqueue(wrap(cb)); }
  public void reschedule(int id,String starts,String ends,Cb<CalendarModels.Simple> cb){ Map<String,Object> b=new HashMap<>(); b.put("starts_at",starts); b.put("ends_at",ends); api.reschedule(id,b).enqueue(wrap(cb)); }
  public void suggested(int specialistId,String date,Cb<CalendarModels.Suggested> cb){ api.suggested(specialistId,date).enqueue(wrap(cb)); }
}
