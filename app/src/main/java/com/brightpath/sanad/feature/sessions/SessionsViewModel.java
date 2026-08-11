package com.brightpath.sanad.feature.sessions;
import android.app.Application;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.annotation.NonNull;
import androidx.lifecycle.*;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Sessions list UI state. Soft reload on resume/realtime; schedules WorkManager reminders from starts_at.
 * Realtime listener always calls load(..., true) so the spinner is skipped on socket updates.
 */
public class SessionsViewModel extends AndroidViewModel {
    private final SessionsRepository repo;
    private final SessionRealtimeClient realtime;
    private Map<String, String> lastFilters;
    private final SessionRealtimeClient.Listener realtimeListener = (sessionId, status) -> load(lastFilters, true);

    public SessionsViewModel(@NonNull Application app){
        super(app);
        repo=new SessionsRepository(app);
        realtime = SessionRealtimeClient.get(app);
        realtime.addListener(realtimeListener);
    }

    public static class UIState {
        public boolean loading; public String error; public SessionModels.SessionList data;
        public static UIState loading(){ UIState s=new UIState(); s.loading=true; return s; }
        public static UIState error(String e){ UIState s=new UIState(); s.error=e; return s; }
        public static UIState data(SessionModels.SessionList d){ UIState s=new UIState(); s.data=d; return s; }
    }

    private final MutableLiveData<UIState> state = new MutableLiveData<>(UIState.loading());
    public LiveData<UIState> getState(){ return state; }

    public void load(){
        load(null, false);
    }

    public void load(java.util.Map<String,String> filters){
        load(filters, false);
    }

    /** Soft refresh keeps current list visible while refetching. */
    public void load(java.util.Map<String,String> filters, boolean soft){
        lastFilters = filters;
        UIState current = state.getValue();
        boolean keepFrame = soft && current != null && current.data != null && !current.loading;
        if (!keepFrame) {
            state.postValue(UIState.loading());
        }
        final UIState retained = keepFrame ? current : null;
        repo.list(filters, new SessionsRepository.ListListener() {
            @Override public void onSuccess(SessionModels.SessionList d){
                state.postValue(UIState.data(d));
                scheduleReminders(d);
            }
            @Override public void onError(Throwable t){
                if (retained != null && retained.data != null) {
                    // Keep last good frame on soft refresh failures.
                    return;
                }
                state.postValue(UIState.error(t.getMessage()));
            }
        });
    }

    private void scheduleReminders(SessionModels.SessionList list){
        if (list == null) return;
        Set<Integer> scheduled = new HashSet<>();
        scheduleFrom(list.accepted, scheduled);
        scheduleFrom(list.upcoming, scheduled);
        scheduleFrom(list.pending, scheduled);
    }

    private void scheduleFrom(java.util.List<SessionModels.Session> sessions, Set<Integer> scheduled){
        if (sessions == null) return;
        for (SessionModels.Session s : sessions){
            if (s == null || s.id <= 0 || scheduled.contains(s.id)) continue;
            long scheduledAt = parseMillis(s.scheduled_at);
            if (scheduledAt <= 0) continue;
            long triggerAt = scheduledAt - TimeUnit.MINUTES.toMillis(10);
            long delay = triggerAt - System.currentTimeMillis();
            if (delay <= 0) continue;
            Data input = new Data.Builder()
                    .putString(SessionReminderWorker.KEY_TITLE, null)
                    .putString(SessionReminderWorker.KEY_BODY, null)
                    .putInt(SessionReminderWorker.KEY_SESSION_ID, s.id)
                    .build();
            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(SessionReminderWorker.class)
                    .setInputData(input)
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .addTag(reminderTag(s.id))
                    .build();
            WorkManager.getInstance(getApplication())
                    .enqueueUniqueWork(reminderTag(s.id), ExistingWorkPolicy.REPLACE, request);
            scheduled.add(s.id);
        }
    }

    private String reminderTag(int sessionId){
        return "session_reminder_" + sessionId;
    }

    private long parseMillis(String raw){
        if (raw == null || raw.isEmpty()) return -1;
        try {
            return Instant.parse(raw).toEpochMilli();
        } catch (DateTimeParseException ignored){}
        try {
            return OffsetDateTime.parse(raw, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant().toEpochMilli();
        } catch (DateTimeParseException ignored){}
        return -1;
    }

    @Override
    protected void onCleared() {
        realtime.removeListener(realtimeListener);
        super.onCleared();
    }
}
