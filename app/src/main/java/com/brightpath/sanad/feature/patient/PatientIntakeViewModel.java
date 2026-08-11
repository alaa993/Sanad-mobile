package com.brightpath.sanad.feature.patient;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class PatientIntakeViewModel extends AndroidViewModel {
    private final PatientIntakeRepository repo;
    private final MutableLiveData<PatientIntakeForm> form = new MutableLiveData<>();
    private final MutableLiveData<TriageRecommendation> recommendation = new MutableLiveData<>();

    public PatientIntakeViewModel(@NonNull Application application) {
        super(application);
        repo = new PatientIntakeRepository(application);
        PatientIntakeForm saved = repo.load();
        if (saved == null) saved = new PatientIntakeForm();
        form.setValue(saved);
        recommendation.setValue(TriageRecommendation.evaluate(saved));
    }

    public LiveData<PatientIntakeForm> getForm(){ return form; }
    public LiveData<TriageRecommendation> getRecommendation(){ return recommendation; }
    public LiveData<PatientIntakeStatus> getStatus(){ return repo.getStatus(); }

    public void resetStatus(){ repo.resetStatus(); }

    public void save(PatientIntakeForm intakeForm){
        repo.save(intakeForm);
        form.setValue(intakeForm);
        recommendation.setValue(TriageRecommendation.evaluate(intakeForm));
    }
}
