package com.brightpath.sanad.feature.specialist;
import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.*;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.brightpath.sanad.feature.sessions.SessionRealtimeClient;
import com.brightpath.sanad.feature.sessions.SessionReminderWorker;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.concurrent.TimeUnit;
public class SpecialistViewModels {
  public static class HomeVM extends AndroidViewModel {
    public MutableLiveData<SpecialistModels.Dashboard> state = new MutableLiveData<>();
    private final SpecialistRepository repo;
    private final SessionRealtimeClient realtime;
    private final SessionRealtimeClient.Listener realtimeListener = (sessionId, status) -> load();
    public HomeVM(@NonNull Application app){
      super(app);
      repo = new SpecialistRepository(app);
      realtime = SessionRealtimeClient.get(app);
      realtime.addListener(realtimeListener);
    }
    public void load(){ repo.dashboard(new SpecialistRepository.Cb<SpecialistModels.Dashboard>(){ @Override public void ok(SpecialistModels.Dashboard t){ state.postValue(t);} @Override public void err(Throwable e){} }); }
    @Override protected void onCleared(){ realtime.removeListener(realtimeListener); super.onCleared(); }
  }
  public static class SessionsVM extends AndroidViewModel {
    public MutableLiveData<java.util.List<SpecialistModels.Appointment>> list = new MutableLiveData<>(new java.util.ArrayList<>()); private final SpecialistRepository repo;
    private final Application app;
    public MutableLiveData<String> toast = new MutableLiveData<>();
    private String currentScope = "pending";
    private final SessionRealtimeClient realtime;
    private final SessionRealtimeClient.Listener realtimeListener = (sessionId, status) -> load(currentScope);
    public SessionsVM(@NonNull Application app){
      super(app);
      this.app = app;
      repo = new SpecialistRepository(app);
      realtime = SessionRealtimeClient.get(app);
      realtime.addListener(realtimeListener);
    }
    public void load(String scope){
      this.currentScope = scope;
      repo.sessions(scope, new SpecialistRepository.Cb<SpecialistModels.Appointments>(){
        @Override public void ok(SpecialistModels.Appointments t){
          list.postValue(t.data);
          scheduleReminders(t.data);
        }
        @Override public void err(Throwable e){}
      });
    }
    public void accept(int id){ repo.accept(id, new SpecialistRepository.Cb<SpecialistModels.Simple>(){ @Override public void ok(SpecialistModels.Simple t){ SpecialistActionNotifier.notify(app, "تم قبول الجلسة", "تم تأكيد الموعد رقم " + id); toast.postValue("تم قبول الجلسة"); load(currentScope); } @Override public void err(Throwable e){} }); }
    public void reject(int id, String reason){
      repo.reject(id, reason, new SpecialistRepository.Cb<SpecialistModels.Simple>(){
        @Override public void ok(SpecialistModels.Simple t){
          SpecialistActionNotifier.notify(app, "تم رفض الجلسة", "تم إبلاغ المريض بتحديث حالة الموعد #" + id);
          toast.postValue("تم رفض الجلسة");
          load(currentScope);
        }
        @Override public void err(Throwable e){}
      });
    }
    public void reschedule(int id, String starts, String ends){ repo.reschedule(id, starts, ends, new SpecialistRepository.Cb<SpecialistModels.Simple>(){ @Override public void ok(SpecialistModels.Simple t){ SpecialistActionNotifier.notify(app, "إعادة جدولة", "تم طلب موعد جديد للموعد #" + id); toast.postValue("تم تحديث موعد الجلسة"); load(currentScope); } @Override public void err(Throwable e){} }); }
    public void extend(int id, int minutes){ repo.extend(id, minutes, new SpecialistRepository.Cb<SpecialistModels.Simple>(){ @Override public void ok(SpecialistModels.Simple t){ toast.postValue("تم تمديد الجلسة"); load(currentScope); } @Override public void err(Throwable e){} }); }
    public void complete(int id){ repo.complete(id, new SpecialistRepository.Cb<SpecialistModels.Simple>(){ @Override public void ok(SpecialistModels.Simple t){ toast.postValue("تم إنهاء الجلسة"); load(currentScope); } @Override public void err(Throwable e){} }); }
    public void refresh(){ load(currentScope); }

    private void scheduleReminders(java.util.List<SpecialistModels.Appointment> sessions){
      if (sessions == null) return;
      for (SpecialistModels.Appointment s : sessions){
        if (s == null || s.id <= 0) continue;
        if (s.status == null) continue;
        String status = s.status.toLowerCase();
        if (status.contains("rejected") || status.contains("canceled") || status.contains("cancelled") || status.contains("completed")) {
          continue;
        }
        long scheduledAt = parseMillis(s.starts_at);
        if (scheduledAt <= 0) continue;
        long triggerAt = scheduledAt - TimeUnit.MINUTES.toMillis(10);
        long delay = triggerAt - System.currentTimeMillis();
        if (delay <= 0) continue;
        Data input = new Data.Builder()
            .putInt(SessionReminderWorker.KEY_SESSION_ID, s.id)
            .build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(SessionReminderWorker.class)
            .setInputData(input)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag(reminderTag(s.id))
            .build();
        WorkManager.getInstance(getApplication())
            .enqueueUniqueWork(reminderTag(s.id), ExistingWorkPolicy.REPLACE, request);
      }
    }

    private String reminderTag(int sessionId){
      return "session_reminder_" + sessionId;
    }

    private long parseMillis(String raw){
      if (raw == null || raw.isEmpty()) return -1;
      try { return Instant.parse(raw).toEpochMilli(); } catch (DateTimeParseException ignored){}
      try { return OffsetDateTime.parse(raw, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant().toEpochMilli(); } catch (DateTimeParseException ignored){}
      return -1;
    }

    @Override protected void onCleared(){ realtime.removeListener(realtimeListener); super.onCleared(); }
  }
  public static class ProfileVM extends AndroidViewModel {
    public MutableLiveData<SpecialistModels.Profile> state = new MutableLiveData<>(); private final SpecialistRepository repo;
    public MutableLiveData<String> toast = new MutableLiveData<>();
    public MutableLiveData<String> error = new MutableLiveData<>();
    public ProfileVM(@NonNull Application app){ super(app); repo = new SpecialistRepository(app); }
    public void load(){ repo.profile(new SpecialistRepository.Cb<SpecialistModels.Profile>(){ @Override public void ok(SpecialistModels.Profile t){ state.postValue(t);} @Override public void err(Throwable e){ error.postValue(e!=null? e.getMessage() : "error"); state.postValue(null); } }); }
    public void update(java.util.Map<String,Object> body){ repo.update(body, new SpecialistRepository.Cb<SpecialistModels.Simple>(){ @Override public void ok(SpecialistModels.Simple t){ toast.postValue("تم حفظ التعديلات"); load(); } @Override public void err(Throwable e){ toast.postValue("تعذر الحفظ: "+e.getMessage()); } }); }
    public void uploadAvatar(android.net.Uri uri, SpecialistRepository.Cb<String> cb){ repo.uploadAvatar(uri, cb); }
  }
  public static class DocumentsVM extends AndroidViewModel {
    public MutableLiveData<SpecialistModels.DocumentList> state = new MutableLiveData<>();
    private final SpecialistRepository repo;
    public DocumentsVM(@NonNull Application app){ super(app); repo = new SpecialistRepository(app); }
    public void load(){ repo.documents(new SpecialistRepository.Cb<SpecialistModels.DocumentList>(){ @Override public void ok(SpecialistModels.DocumentList t){ state.postValue(t);} @Override public void err(Throwable e){} }); }
    public void upload(String type, android.net.Uri uri, SpecialistRepository.Cb<SpecialistModels.Document> cb){ repo.uploadDocument(type, uri, new SpecialistRepository.Cb<SpecialistModels.Document>(){ @Override public void ok(SpecialistModels.Document t){ load(); cb.ok(t);} @Override public void err(Throwable e){ cb.err(e);} }); }
    public void delete(int id){ repo.deleteDocument(id, new SpecialistRepository.Cb<SpecialistModels.Simple>(){ @Override public void ok(SpecialistModels.Simple t){ load(); } @Override public void err(Throwable e){} }); }
  }
}
