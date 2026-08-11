package com.brightpath.sanad.feature.specialist;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.brightpath.sanad.feature.patient.PatientIntakeForm;
import com.brightpath.sanad.feature.patient.PatientTask;
import com.brightpath.sanad.feature.sessions.SessionModels;
import com.brightpath.sanad.feature.sessions.SessionsRepository;
import com.brightpath.sanad.feature.specialist.SpecialistModels;
import java.util.List;

public class SpecialistSessionDetailViewModel extends AndroidViewModel {
    public static class UIState {
        public boolean loading;
        public String error;
        public PatientIntakeForm intake;
        public List<PatientTask> tasks;
        public SessionModels.Session session;
    }

    private final SpecialistDetailRepository repo;
    private final SessionsRepository sessionsRepo;
    private final SpecialistRepository specialistRepo;
    private final MutableLiveData<UIState> state = new MutableLiveData<>();
    private final MutableLiveData<String> toast = new MutableLiveData<>();
    private boolean intakeLoaded = false;
    private boolean tasksLoaded = false;
    private int lastSessionId = -1;
    private int lastPatientId = -1;

    public SpecialistSessionDetailViewModel(@NonNull Application application) {
        super(application);
        repo = new SpecialistDetailRepository(application);
        sessionsRepo = new SessionsRepository(application);
        specialistRepo = new SpecialistRepository(application);
        UIState initial = new UIState();
        initial.loading = true;
        state.setValue(initial);
    }

    public LiveData<UIState> getState(){ return state; }
    public LiveData<String> getToast(){ return toast; }

    public void load(int sessionId, int patientId){
        intakeLoaded = false;
        tasksLoaded = false;
        lastSessionId = sessionId;
        lastPatientId = patientId;
        UIState current = new UIState();
        current.loading = true;
        state.setValue(current);

        if (sessionId <= 0 && patientId <= 0) {
            UIState s = state.getValue();
            if (s == null) s = new UIState();
            s.error = "invalid_session";
            s.loading = false;
            state.postValue(s);
            return;
        }

        if (sessionId > 0) {
            sessionsRepo.show(sessionId, new SessionsRepository.OneListener() {
                @Override public void onSuccess(SessionModels.Session d) {
                    UIState s = state.getValue();
                    if (s == null) s = new UIState();
                    s.session = d;
                    s.loading = false;
                    state.postValue(s);
                    int derivedPatientId = d != null && d.user != null ? d.user.id : -1;
                    int resolvedPatientId = patientId > 0 ? patientId : derivedPatientId;
                    if (resolvedPatientId > 0) {
                        lastPatientId = resolvedPatientId;
                    }
                    if (resolvedPatientId > 0) {
                        loadPatientData(resolvedPatientId);
                    }
                }
                @Override public void onError(Throwable t) {
                    UIState s = state.getValue();
                    if (s == null) s = new UIState();
                    s.error = t.getMessage();
                    s.loading = false;
                    state.postValue(s);
                }
            });
        } else if (patientId > 0) {
            loadPatientData(patientId);
        }
    }

    public void accept(){
        performAction(true, null);
    }

    public void reject(@Nullable String reason){
        performAction(false, reason);
    }

    public void reschedule(String startsAt, String endsAt){
        if (lastSessionId <= 0) {
            UIState s = state.getValue();
            if (s == null) s = new UIState();
            s.error = "invalid_session";
            s.loading = false;
            state.postValue(s);
            return;
        }
        UIState s = state.getValue();
        if (s == null) s = new UIState();
        s.loading = true;
        state.postValue(s);
        specialistRepo.reschedule(lastSessionId, startsAt, endsAt, new SpecialistRepository.Cb<SpecialistModels.Simple>() {
            @Override public void ok(SpecialistModels.Simple t) {
                toast.postValue("تم إعادة جدولة الجلسة");
                load(lastSessionId, lastPatientId);
            }
            @Override public void err(Throwable e) {
                UIState s = state.getValue();
                if (s == null) s = new UIState();
                s.error = e.getMessage();
                s.loading = false;
                state.postValue(s);
            }
        });
    }

    public void complete(){
        if (lastSessionId <= 0) {
            UIState s = state.getValue();
            if (s == null) s = new UIState();
            s.error = "invalid_session";
            s.loading = false;
            state.postValue(s);
            return;
        }
        UIState s = state.getValue();
        if (s == null) s = new UIState();
        s.loading = true;
        state.postValue(s);
        specialistRepo.complete(lastSessionId, new SpecialistRepository.Cb<SpecialistModels.Simple>() {
            @Override public void ok(SpecialistModels.Simple t) {
                toast.postValue("تم إنهاء الجلسة");
                load(lastSessionId, lastPatientId);
            }
            @Override public void err(Throwable e) {
                UIState s = state.getValue();
                if (s == null) s = new UIState();
                s.error = e.getMessage();
                s.loading = false;
                state.postValue(s);
            }
        });
    }

    public void updateTriage(java.util.List<String> tags, @Nullable String reason){
        if (lastPatientId <= 0) {
            UIState s = state.getValue();
            if (s == null) s = new UIState();
            s.error = "invalid_patient";
            s.loading = false;
            state.postValue(s);
            return;
        }
        repo.updateIntake(lastPatientId, tags, reason, new SpecialistDetailRepository.UpdateIntakeCb() {
            @Override public void ok(PatientIntakeForm form) {
                UIState s = state.getValue();
                if (s == null) s = new UIState();
                s.intake = form;
                s.loading = false;
                state.postValue(s);
                toast.postValue("تم حفظ التقييم الأولي");
            }
            @Override public void err(Throwable t) {
                toast.postValue("تعذر حفظ التقييم الأولي");
            }
        });
    }

    private void performAction(boolean accept, @Nullable String reason){
        if (lastSessionId <= 0) {
            UIState s = state.getValue();
            if (s == null) s = new UIState();
            s.error = "invalid_session";
            s.loading = false;
            state.postValue(s);
            return;
        }
        UIState s = state.getValue();
        if (s == null) s = new UIState();
        s.loading = true;
        state.postValue(s);
        SpecialistRepository.Cb<SpecialistModels.Simple> cb = new SpecialistRepository.Cb<SpecialistModels.Simple>() {
            @Override public void ok(SpecialistModels.Simple t) {
                toast.postValue(accept ? "تم قبول الجلسة" : "تم رفض الجلسة");
                load(lastSessionId, lastPatientId);
            }
            @Override public void err(Throwable e) {
                UIState s = state.getValue();
                if (s == null) s = new UIState();
                s.error = e.getMessage();
                s.loading = false;
                state.postValue(s);
            }
        };
        if (accept) {
            specialistRepo.accept(lastSessionId, cb);
        } else {
            specialistRepo.reject(lastSessionId, reason, cb);
        }
    }

    private void loadPatientData(int patientId) {
        repo.fetchIntake(patientId, new SpecialistDetailRepository.IntakeCb() {
            @Override public void ok(PatientIntakeForm form) {
                UIState s = state.getValue();
                if (s == null) s = new UIState();
                s.intake = form;
                intakeLoaded = true;
                s.loading = false;
                state.postValue(s);
            }
            @Override public void err(Throwable t) {
                UIState s = state.getValue();
                if (s == null) s = new UIState();
                s.error = t.getMessage();
                s.loading = false;
                state.postValue(s);
            }
        });

        repo.fetchTasks(patientId, new SpecialistDetailRepository.TasksCb() {
            @Override public void ok(List<PatientTask> tasks) {
                UIState s = state.getValue();
                if (s == null) s = new UIState();
                tasksLoaded = true;
                s.tasks = tasks;
                if (!intakeLoaded) {
                    s.loading = false;
                }
                state.postValue(s);
            }
            @Override public void err(Throwable t) {
                UIState s = state.getValue();
                if (s == null) s = new UIState();
                tasksLoaded = true;
                s.tasks = java.util.Collections.emptyList();
                if (!intakeLoaded) {
                    s.loading = false;
                }
                state.postValue(s);
            }
        });
    }
}
