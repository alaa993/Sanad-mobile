package com.brightpath.sanad.feature.specialist;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.brightpath.sanad.R;
import com.brightpath.sanad.feature.patient.PatientIntakeForm;
import com.brightpath.sanad.feature.sessions.SessionModels;
import com.brightpath.sanad.ui.IntakeLabelHelper;

public class SpecialistPatientFileFragment extends Fragment {
    private SpecialistSessionDetailViewModel vm;
    private View progress, content, errorContainer;
    private TextView tvError, tvPatientName, tvSpecialistName, tvPatientIntakeDetails, tvSpecialistAssessmentDetails, tvSessionHistory;
    private int sessionId = -1;
    private int patientId = -1;
    private SpecialistDetailRepository detailRepo;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_specialist_patient_file, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        sessionId = getArguments()!=null ? getArguments().getInt("sessionId", -1) : -1;
        patientId = getArguments()!=null ? getArguments().getInt("patientId", -1) : -1;

        content = v.findViewById(R.id.content);
        progress = v.findViewById(R.id.progress);
        errorContainer = v.findViewById(R.id.errorContainer);
        tvError = v.findViewById(R.id.tvError);
        tvPatientName = v.findViewById(R.id.tvPatientName);
        tvSpecialistName = v.findViewById(R.id.tvSpecialistName);
        tvPatientIntakeDetails = v.findViewById(R.id.tvPatientIntakeDetails);
        tvSpecialistAssessmentDetails = v.findViewById(R.id.tvSpecialistAssessmentDetails);
        tvSessionHistory = v.findViewById(R.id.tvSessionHistory);
        detailRepo = new SpecialistDetailRepository(requireContext());

        View btnRetry = v.findViewById(R.id.btnRetry);
        if (btnRetry != null) {
            btnRetry.setOnClickListener(x -> {
                vm.load(sessionId, patientId);
                if (patientId > 0) loadSessionHistory(patientId);
            });
        }

        vm = new ViewModelProvider(this).get(SpecialistSessionDetailViewModel.class);
        vm.getState().observe(getViewLifecycleOwner(), this::render);
        vm.load(sessionId, patientId);
        if (patientId > 0) loadSessionHistory(patientId);
    }

    private void loadSessionHistory(int pid) {
        detailRepo.fetchSessions(pid, new SpecialistDetailRepository.SessionsCb() {
            @Override public void ok(java.util.List<SpecialistDetailRepository.PatientSessionRow> sessions) {
                if (!isAdded() || tvSessionHistory == null) return;
                if (sessions == null || sessions.isEmpty()) {
                    tvSessionHistory.setText(getString(R.string.specialist_patient_file_no_history));
                    return;
                }
                StringBuilder sb = new StringBuilder();
                for (SpecialistDetailRepository.PatientSessionRow row : sessions) {
                    if (sb.length() > 0) sb.append("\n\n");
                    sb.append("#").append(row.id)
                            .append(" — ")
                            .append(IntakeLabelHelper.sessionStatus(requireContext(), row.status));
                    if (row.starts_at != null) {
                        sb.append("\n").append(IntakeLabelHelper.formatDate(row.starts_at));
                    }
                    if (row.specialist_notes != null && !row.specialist_notes.isEmpty()) {
                        sb.append("\n").append(row.specialist_notes);
                    }
                    if (row.rating != null) sb.append("\n★ ").append(row.rating);
                }
                tvSessionHistory.setText(sb.toString());
            }
            @Override public void err(Throwable t) {
                if (isAdded() && tvSessionHistory != null) {
                    tvSessionHistory.setText(getString(R.string.specialist_patient_file_no_history));
                }
            }
        });
    }

    private void render(SpecialistSessionDetailViewModel.UIState state){
        if (state == null) return;
        if (state.loading){
            show(progress);
            return;
        }
        if (state.error != null){
            tvError.setText(getString(R.string.specialist_patient_file_load_failed));
            show(errorContainer);
            return;
        }
        if (state.session != null) {
            bindSession(state.session);
        }
        if (state.intake != null) {
            String fallbackName = (state.session != null && state.session.user != null && !TextUtils.isEmpty(state.session.user.name))
                    ? state.session.user.name : getString(R.string.patient_title);
            tvPatientName.setText(state.intake.fullName != null ? state.intake.fullName : fallbackName);
            tvPatientIntakeDetails.setText(formatIntakeDetails(state.intake));
            tvSpecialistAssessmentDetails.setText(formatAssessmentDetails(state.intake));
        } else {
            tvPatientIntakeDetails.setText(getString(R.string.specialist_patient_file_no_intake));
            tvSpecialistAssessmentDetails.setText("-");
        }
        show(content);
    }

    private void bindSession(SessionModels.Session session) {
        if (session == null) return;
        String specialistName = session.specialist != null && !TextUtils.isEmpty(session.specialist.name)
                ? session.specialist.name : getString(R.string.role_specialist);
        tvSpecialistName.setText(getString(R.string.session_specialist_name_format, specialistName));
    }

    private void show(View target){
        content.setVisibility(target==content?View.VISIBLE:View.GONE);
        progress.setVisibility(target==progress?View.VISIBLE:View.GONE);
        errorContainer.setVisibility(target==errorContainer?View.VISIBLE:View.GONE);
    }

    private String formatIntakeDetails(@NonNull PatientIntakeForm intake){
        StringBuilder info = new StringBuilder();
        info.append(getString(R.string.patient_intake_label_age, safe(intake.age))).append("\n");
        if (!TextUtils.isEmpty(intake.ageGender)) {
            info.append(getString(R.string.patient_intake_label_age_gender, safe(intake.ageGender))).append("\n");
        }
        info.append(getString(R.string.patient_intake_label_occupation, safe(intake.occupation))).append("\n");
        info.append(getString(R.string.patient_intake_label_primary_issue, safe(intake.primaryIssue))).append("\n");
        info.append(getString(R.string.patient_intake_label_duration,
                IntakeLabelHelper.duration(requireContext(), intake.duration))).append("\n");
        if (intake.symptoms != null && !intake.symptoms.isEmpty()) {
            info.append(getString(R.string.patient_intake_label_symptoms,
                    IntakeLabelHelper.joinTokens(requireContext(), intake.symptoms))).append("\n");
        }
        info.append(getString(R.string.patient_intake_label_previous_consult, intake.hadConsultation
                ? getString(R.string.yes)
                : getString(R.string.no))).append("\n");
        if (!TextUtils.isEmpty(intake.consultationNotes)) {
            info.append(getString(R.string.patient_intake_label_consult_notes, intake.consultationNotes)).append("\n");
        }
        info.append(getString(R.string.patient_intake_label_benefit, String.valueOf(intake.benefitScore))).append("\n");
        if (!TextUtils.isEmpty(intake.notes)) {
            info.append(getString(R.string.patient_intake_label_notes, intake.notes));
        }
        return info.toString().trim();
    }

    private String formatAssessmentDetails(@NonNull PatientIntakeForm intake){
        java.util.List<String> tagSource = intake.triageTags;
        if ((tagSource == null || tagSource.isEmpty()) && intake.riskFlags != null) {
            tagSource = intake.riskFlags;
        }
        String tags = (tagSource != null && !tagSource.isEmpty())
                ? IntakeLabelHelper.joinTokens(requireContext(), tagSource)
                : getString(R.string.session_triage_empty);
        String reason = null;
        if (intake.triageRecommendation != null) {
            reason = intake.triageRecommendation.get("reason");
        }
        StringBuilder info = new StringBuilder();
        if (intake.referralPhysicianRecommended) {
            info.append(getString(R.string.physician_referral_banner)).append("\n\n");
        }
        info.append(getString(R.string.session_specialist_assessment_tags, tags)).append("\n");
        info.append(getString(R.string.session_specialist_assessment_category,
                IntakeLabelHelper.symptomOrTag(requireContext(), intake.triageCategory))).append("\n");
        info.append(getString(R.string.session_specialist_assessment_reason, safe(reason)));
        return info.toString().trim();
    }

    private String safe(String value){
        return value != null && !value.trim().isEmpty() ? value : "-";
    }
}
