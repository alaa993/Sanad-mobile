package com.brightpath.sanad.feature.home;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.brightpath.sanad.data.DashboardRepository;
import com.brightpath.sanad.data.DashboardResponse;
import com.brightpath.sanad.feature.sessions.SessionRealtimeClient;

import java.util.List;

public class HomeViewModel extends AndroidViewModel {

    private final MutableLiveData<UIState> state = new MutableLiveData<>(UIState.loading());
    private final DashboardRepository repo;
    private final SessionRealtimeClient realtime;
    private final SessionRealtimeClient.Listener realtimeListener = (sessionId, status) -> load(true);

    public HomeViewModel(@NonNull Application application) {
        super(application);
        repo = new DashboardRepository(application.getApplicationContext());
        realtime = SessionRealtimeClient.get(application);
        realtime.addListener(realtimeListener);
    }

    public LiveData<UIState> getState() {
        return state;
    }

    public void load() {
        load(false);
    }

    /** Soft refresh keeps last UI frame visible (no full-screen loading flash). */
    public void load(boolean soft) {
        UIState current = state.getValue();
        boolean keepFrame = soft && current != null && current.role != null && !current.loading;
        if (!keepFrame) {
            state.postValue(UIState.loading());
        }
        repo.fetch(new DashboardRepository.Listener() {
            @Override
            public void onSuccess(DashboardResponse d) {
                int up = d.stats != null ? d.stats.upcoming_sessions : 0;
                int un = d.stats != null ? d.stats.unread_messages : 0;
                int pt = d.stats != null ? d.stats.points : 0;
                DashboardResponse.SessionSummary next = d.next_session;
                boolean canJoin = next != null && next.can_join;
                List<DashboardResponse.Shortcut> shortcuts = d.shortcuts;
                state.postValue(UIState.data(d.role, up, un, pt, shortcuts, d.intake, next, canJoin, d.onboarding));
            }

            @Override
            public void onError(Throwable t) {
                state.postValue(UIState.error(t));
            }
        });
    }

    private boolean isPatientRole(String role) {
        return role == null || role.isEmpty() || role.equalsIgnoreCase("patient");
    }

    @Override
    protected void onCleared() {
        realtime.removeListener(realtimeListener);
        super.onCleared();
    }
}
