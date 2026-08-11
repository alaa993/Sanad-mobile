package com.brightpath.sanad.feature.patient;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PatientTaskViewModel extends AndroidViewModel {
    private final PatientTaskRepository repo;
    private final MutableLiveData<List<PatientTask>> tasks = new MutableLiveData<>(new ArrayList<>());

    public PatientTaskViewModel(@NonNull Application application) {
        super(application);
        repo = new PatientTaskRepository(application);
        tasks.setValue(repo.load());
        syncFromServer();
    }

    public LiveData<List<PatientTask>> getTasks(){
        return tasks;
    }

    public void addTask(PatientTask task){
        repo.add(task);
        repo.createRemote(task);
        scheduleReminder(task);
        tasks.setValue(repo.load());
    }

    public void markCompleted(String sessionId){
        PatientTask match = findBySession(sessionId);
        completeTask(match, null);
    }

    public void completeTask(@Nullable PatientTask task, @Nullable String note){
        if (task == null) return;
        repo.completeTask(task, note, System.currentTimeMillis());
        if (task.sessionId != null){
            WorkManager.getInstance(getApplication()).cancelAllWorkByTag(tagFor(task.sessionId));
        }
        repo.completeRemote(task.id, note);
        tasks.setValue(repo.load());
    }

    private void syncFromServer(){
        repo.syncRemote(new PatientTaskRepository.SyncListener() {
            @Override public void onResult(List<PatientTask> remote) {
                tasks.postValue(remote);
            }
            @Override public void onError(Throwable t) { }
        });
    }

    private PatientTask findBySession(String sessionId){
        List<PatientTask> current = tasks.getValue();
        if (current == null || sessionId == null) return null;
        for (PatientTask t : current){
            if (sessionId.equals(t.sessionId)) return t;
        }
        return null;
    }

    private void scheduleReminder(PatientTask task){
        long delay = task.dueAt - System.currentTimeMillis();
        if (delay < 0) delay = 0;
        Data input = new Data.Builder()
                .putString(PatientTaskReminderWorker.KEY_TITLE, task.title)
                .putString(PatientTaskReminderWorker.KEY_DESC, task.description)
                .build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(PatientTaskReminderWorker.class)
                .setInputData(input)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .addTag(tagFor(task.sessionId))
                .build();
        WorkManager.getInstance(getApplication()).enqueue(request);
    }

    private String tagFor(String sessionId){
        return sessionId != null ? "task_" + sessionId : "task_general";
    }
}
