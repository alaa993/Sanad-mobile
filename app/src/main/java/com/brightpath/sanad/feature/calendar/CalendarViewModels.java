
package com.brightpath.sanad.feature.calendar;
import android.app.Application; import androidx.annotation.NonNull; import androidx.lifecycle.*; import java.util.*;
public class CalendarViewModels {
  public static class AvailabilityVM extends AndroidViewModel {
    private final CalendarRepository repo; public final MutableLiveData<CalendarModels.Availability> state = new MutableLiveData<>();
    public AvailabilityVM(@NonNull Application app){ super(app); repo = new CalendarRepository(app); }
    public void load(){ repo.availability(new CalendarRepository.Cb<CalendarModels.Availability>(){ @Override public void ok(CalendarModels.Availability t){ state.postValue(t); } @Override public void err(Throwable e){} }); }
    public void addSlot(int weekday,String s,String e){ repo.addSlot(weekday,s,e, new CalendarRepository.Cb<CalendarModels.Simple>(){ @Override public void ok(CalendarModels.Simple t){ load(); } @Override public void err(Throwable e){} }); }
    public void delSlot(int id){ repo.delSlot(id, new CalendarRepository.Cb<CalendarModels.Simple>(){ @Override public void ok(CalendarModels.Simple t){ load(); } @Override public void err(Throwable e){} }); }
    public void addBlock(String s,String e,String r){ repo.addBlock(s,e,r, new CalendarRepository.Cb<CalendarModels.Simple>(){ @Override public void ok(CalendarModels.Simple t){ load(); } @Override public void err(Throwable e){} }); }
    public void delBlock(int id){ repo.delBlock(id, new CalendarRepository.Cb<CalendarModels.Simple>(){ @Override public void ok(CalendarModels.Simple t){ load(); } @Override public void err(Throwable e){} }); }
  }
  public static class AppointmentsVM extends AndroidViewModel {
    private final CalendarRepository repo; public final MutableLiveData<java.util.List<CalendarModels.Appointment>> list = new MutableLiveData<>(new java.util.ArrayList<>());
    public AppointmentsVM(@NonNull Application app){ super(app); repo = new CalendarRepository(app); }
    public void load(String scope,String from,String to){ repo.appointments(scope,from,to, new CalendarRepository.Cb<CalendarModels.Appointments>(){ @Override public void ok(CalendarModels.Appointments t){ list.postValue(t.data); } @Override public void err(Throwable e){} }); }
    public void create(int specialistId,String s,String e,String notes){ repo.createAppointment(specialistId,s,e,notes, new CalendarRepository.Cb<CalendarModels.Simple>(){ @Override public void ok(CalendarModels.Simple t){ /* reload by caller */ } @Override public void err(Throwable e){} }); }
    public void accept(int id){ repo.accept(id, new CalendarRepository.Cb<CalendarModels.Simple>(){ @Override public void ok(CalendarModels.Simple t){ /* reload by caller */ } @Override public void err(Throwable e){} }); }
    public void reject(int id){ repo.reject(id, new CalendarRepository.Cb<CalendarModels.Simple>(){ @Override public void ok(CalendarModels.Simple t){ } @Override public void err(Throwable e){} }); }
    public void cancel(int id){ repo.cancel(id, new CalendarRepository.Cb<CalendarModels.Simple>(){ @Override public void ok(CalendarModels.Simple t){ } @Override public void err(Throwable e){} }); }
    public void reschedule(int id,String s,String e){ repo.reschedule(id,s,e, new CalendarRepository.Cb<CalendarModels.Simple>(){ @Override public void ok(CalendarModels.Simple t){ } @Override public void err(Throwable e){} }); }
  }
  public static class SuggestedSlotsVM extends AndroidViewModel {
    private final CalendarRepository repo; public final MutableLiveData<java.util.List<CalendarModels.Suggestion>> data = new MutableLiveData<>(new java.util.ArrayList<>());
    public SuggestedSlotsVM(@NonNull Application app){ super(app); repo = new CalendarRepository(app); }
    public void load(int specialistId,String date){ repo.suggested(specialistId,date, new CalendarRepository.Cb<CalendarModels.Suggested>(){ @Override public void ok(CalendarModels.Suggested t){ data.postValue(t.data); } @Override public void err(Throwable e){} }); }
  }
}
