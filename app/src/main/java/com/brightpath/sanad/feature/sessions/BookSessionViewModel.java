package com.brightpath.sanad.feature.sessions;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.*;

import com.brightpath.sanad.R;

public class BookSessionViewModel extends AndroidViewModel {
    private final SessionsRepository_Book repo;

    @Nullable private Integer specialistId;
    @Nullable private String specialistName;
    @Nullable private Integer organizationId;
    @Nullable private String organizationName;

    public BookSessionViewModel(@NonNull Application app){
        super(app);
        repo = new SessionsRepository_Book(app);
    }

    public static class UIState {
        public boolean loading; public String error; public String errorCode; public SessionModels.Session data;
        public static UIState idle(){ return new UIState(); }
        public static UIState loading(){ UIState s=new UIState(); s.loading=true; return s; }
        public static UIState error(String e){ UIState s=new UIState(); s.error=e; return s; }
        public static UIState error(String code, String e){ UIState s=new UIState(); s.errorCode=code; s.error=e; return s; }
        public static UIState data(SessionModels.Session d){ UIState s=new UIState(); s.data=d; return s; }
    }

    private final MutableLiveData<UIState> state = new MutableLiveData<>(UIState.idle());
    public LiveData<UIState> getState(){ return state; }

    public void clearResult() {
        state.setValue(UIState.idle());
    }

    @Nullable public Integer getSpecialistId() { return specialistId; }
    @Nullable public String getSpecialistName() { return specialistName; }
    @Nullable public Integer getOrganizationId() { return organizationId; }
    @Nullable public String getOrganizationName() { return organizationName; }

    public void setSpecialist(int id, @Nullable String name) {
        specialistId = id > 0 ? id : null;
        specialistName = name;
    }

    public void setOrganization(@Nullable Integer id, @Nullable String name) {
        if (id == null || id <= 0) {
            organizationId = null;
            organizationName = null;
            return;
        }
        organizationId = id;
        organizationName = name;
    }

    public void clearOrganization() {
        organizationId = null;
        organizationName = null;
    }

    public void book(String type, String isoDateTime, Integer specialistId, Integer organizationId, String notes,
                     boolean weeklyRecurring, Integer recurrenceCount){
        state.postValue(UIState.loading());
        BookRequest req = new BookRequest();
        req.type = type;
        req.scheduled_at = isoDateTime;
        req.specialist_id = specialistId;
        req.organization_id = organizationId;
        req.notes = notes;
        req.timezone = java.time.ZoneId.systemDefault().getId();
        if (weeklyRecurring) {
            req.weekly_recurring = true;
            req.recurrence_count = recurrenceCount != null ? recurrenceCount : 4;
        }

        repo.book(req, new SessionsRepository_Book.BookListener() {
            @Override public void onSuccess(SessionModels.Session d){ state.postValue(UIState.data(d)); }
            @Override public void onError(Throwable t){
                String message = getApplication().getString(R.string.book_session_failed);
                String code = null;
                if (t instanceof SessionsRepository_Book.BookingException) {
                    SessionsRepository_Book.BookingException bookingError = (SessionsRepository_Book.BookingException) t;
                    code = bookingError.code;
                    if ("past_datetime".equals(code)) {
                        message = getApplication().getString(R.string.book_session_error_past_datetime);
                    } else if ("intake_required".equals(code)) {
                        message = getApplication().getString(R.string.book_session_error_intake_required);
                    } else if ("pre_session_required".equals(code)) {
                        message = getApplication().getString(R.string.book_session_error_pre_session_required);
                    } else if ("insufficient_points".equals(code)) {
                        message = getApplication().getString(R.string.book_session_error_insufficient_points);
                    } else if (bookingError.getMessage() != null && !bookingError.getMessage().trim().isEmpty()) {
                        message = bookingError.getMessage();
                    }
                } else if (t != null && t.getMessage() != null && !t.getMessage().trim().isEmpty()) {
                    message = t.getMessage();
                }
                state.postValue(UIState.error(code, message));
            }
        });
    }
}
