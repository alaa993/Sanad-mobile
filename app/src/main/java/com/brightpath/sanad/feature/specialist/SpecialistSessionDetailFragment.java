package com.brightpath.sanad.feature.specialist;

import android.os.Bundle;
import android.text.TextUtils;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.brightpath.sanad.R;
import com.brightpath.sanad.feature.patient.PatientTask;
import com.brightpath.sanad.feature.sessions.SessionActionGate;
import com.brightpath.sanad.feature.sessions.SessionModels;
import com.google.android.material.button.MaterialButton;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SpecialistSessionDetailFragment extends Fragment {
    private SpecialistSessionDetailViewModel vm;
    private View progress, content, errorContainer;
    private TextView tvError, tvSessionInfo, tvPatientName, tvIntakeSummary, tvTriageSummary, tvSteps, tvStatus, tvSchedule, tvNotesValue, tvRejectReason;
    private TextView tvPatientIntakeDetails, tvSpecialistAssessmentDetails;
    private com.google.android.material.button.MaterialButton btnTriage, btnAccept, btnReject, btnJoin, btnReschedule, btnComplete, btnAddSessionTask, btnApplyTemplates;
    private View actionsPrimary, actionsSecondary;
    private TaskAdapter adapter;
    private SpecialistDetailRepository detailRepo;
    private int sessionId = -1;
    private int patientId = -1;
    private SessionModels.Session currentSession;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_specialist_session_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        sessionId = getArguments()!=null ? getArguments().getInt("sessionId", -1) : -1;
        patientId = getArguments()!=null ? getArguments().getInt("patientId", -1) : -1;

        content = v.findViewById(R.id.content);
        progress = v.findViewById(R.id.progress);
        errorContainer = v.findViewById(R.id.errorContainer);
        tvError = v.findViewById(R.id.tvError);
        tvSessionInfo = v.findViewById(R.id.tvSessionInfo);
        tvPatientName = v.findViewById(R.id.tvPatientName);
        tvStatus = v.findViewById(R.id.tvStatus);
        tvSchedule = v.findViewById(R.id.tvSchedule);
        tvIntakeSummary = v.findViewById(R.id.tvIntakeSummary);
        tvTriageSummary = v.findViewById(R.id.tvTriageSummary);
        tvSteps = v.findViewById(R.id.tvSteps);
        tvNotesValue = v.findViewById(R.id.tvNotesValue);
        tvRejectReason = v.findViewById(R.id.tvRejectReason);
        actionsPrimary = v.findViewById(R.id.actionsPrimary);
        actionsSecondary = v.findViewById(R.id.actionsSecondary);
        btnTriage = v.findViewById(R.id.btnTriage);
        btnAccept = v.findViewById(R.id.btnAccept);
        btnReject = v.findViewById(R.id.btnReject);
        btnJoin = v.findViewById(R.id.btnJoin);
        btnReschedule = v.findViewById(R.id.btnReschedule);
        btnComplete = v.findViewById(R.id.btnComplete);
        btnAddSessionTask = v.findViewById(R.id.btnAddSessionTask);
        btnApplyTemplates = v.findViewById(R.id.btnApplyTemplates);
        tvPatientIntakeDetails = v.findViewById(R.id.tvPatientIntakeDetails);
        tvSpecialistAssessmentDetails = v.findViewById(R.id.tvSpecialistAssessmentDetails);
        RecyclerView rvTasks = v.findViewById(R.id.rvTasks);
        rvTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TaskAdapter();
        rvTasks.setAdapter(adapter);

        tvSessionInfo.setText("جلسة #" + sessionId);

        vm = new ViewModelProvider(this).get(SpecialistSessionDetailViewModel.class);
        detailRepo = new SpecialistDetailRepository(requireContext());
        vm.getState().observe(getViewLifecycleOwner(), this::render);
        vm.getToast().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                if (msg.contains("تم")) {
                    NavHostFragment.findNavController(this).navigate(R.id.specialistHomeFragment);
                }
            }
        });
        if (btnAccept != null) {
            btnAccept.setOnClickListener(x -> vm.accept());
        }
        if (btnReject != null) {
            btnReject.setOnClickListener(x -> showRejectDialog());
        }
        if (btnJoin != null) {
            btnJoin.setOnClickListener(x -> openJoinLink(currentSession));
        }
        if (btnReschedule != null) {
            btnReschedule.setOnClickListener(x -> showReschedulePicker());
        }
        if (btnComplete != null) {
            btnComplete.setOnClickListener(x -> vm.complete());
        }
        if (btnAddSessionTask != null) {
            btnAddSessionTask.setOnClickListener(x -> openSessionTasks());
        }
        if (btnApplyTemplates != null) {
            btnApplyTemplates.setOnClickListener(x -> showApplyTemplatesDialog());
        }
        vm.load(sessionId, patientId);
    }

    private void render(SpecialistSessionDetailViewModel.UIState state){
        if (state == null) return;
        if (state.loading){
            show(progress);
            return;
        }
        if (state.error != null){
            tvError.setText(state.error);
            show(errorContainer);
            return;
        }
        if (state.session != null) {
            bindSession(state.session);
        }
        if (state.intake != null){
            String fallbackName = (state.session != null && state.session.user != null && !TextUtils.isEmpty(state.session.user.name))
                    ? state.session.user.name : "مريض";
            tvPatientName.setText(state.intake.fullName != null ? state.intake.fullName : fallbackName);
            tvIntakeSummary.setText("المشكلة الأساسية: " + safe(state.intake.primaryIssue));
            if (tvTriageSummary != null) {
                String tags = (state.intake.triageTags != null && !state.intake.triageTags.isEmpty())
                        ? TextUtils.join("، ", state.intake.triageTags)
                        : getString(R.string.session_triage_empty);
                tvTriageSummary.setText(getString(R.string.session_triage_summary, tags));
            }
            if (tvSteps != null) {
                tvSteps.setText(getString(R.string.session_steps_specialist));
            }
            if (btnTriage != null) {
                btnTriage.setVisibility(View.VISIBLE);
                btnTriage.setOnClickListener(x -> showTriageDialog(state.intake));
            }
            if (tvPatientIntakeDetails != null) {
                tvPatientIntakeDetails.setText(formatIntakeDetails(state.intake));
            }
            if (tvSpecialistAssessmentDetails != null) {
                tvSpecialistAssessmentDetails.setText(formatAssessmentDetails(state.intake));
            }
        } else {
            if (btnTriage != null) btnTriage.setVisibility(View.GONE);
            if (tvPatientIntakeDetails != null) tvPatientIntakeDetails.setText("-");
            if (tvSpecialistAssessmentDetails != null) tvSpecialistAssessmentDetails.setText("-");
        }
        adapter.submit(state.tasks);
        show(content);
    }

    private void bindSession(SessionModels.Session session) {
        if (session == null) return;
        currentSession = session;
        if (session.user != null && session.user.id > 0) {
            patientId = session.user.id;
        }
        if (tvSessionInfo != null) {
            tvSessionInfo.setText("جلسة #" + session.id);
        }
        if (tvStatus != null) {
            tvStatus.setText(mapStatus(session.status));
        }
        if (tvSchedule != null) {
            String schedule = formatSchedule(session.scheduled_at);
            if (TextUtils.isEmpty(schedule)) {
                schedule = getString(R.string.session_unknown_schedule);
            }
            tvSchedule.setText(schedule);
        }
        if (tvNotesValue != null) {
            tvNotesValue.setText(!TextUtils.isEmpty(session.notes) ? session.notes : "-");
        }
        if (tvRejectReason != null) {
            if (!TextUtils.isEmpty(session.rejection_reason)) {
                tvRejectReason.setVisibility(View.VISIBLE);
                tvRejectReason.setText(getString(R.string.session_rejection_reason_label, session.rejection_reason));
            } else {
                tvRejectReason.setVisibility(View.GONE);
            }
        }
        SessionActionGate gate = SessionActionGate.evaluate(session.status, session.scheduled_at, true);
        if (actionsPrimary != null) actionsPrimary.setVisibility(gate.canAccept || gate.canReject ? View.VISIBLE : View.GONE);
        if (btnAccept != null) btnAccept.setVisibility(gate.canAccept ? View.VISIBLE : View.GONE);
        if (btnReject != null) btnReject.setVisibility(gate.canReject ? View.VISIBLE : View.GONE);

        boolean showSecondary = gate.phase == SessionActionGate.Phase.JOINABLE
                || gate.phase == SessionActionGate.Phase.WAITING_WINDOW
                || gate.phase == SessionActionGate.Phase.IN_PROGRESS;
        if (actionsSecondary != null) actionsSecondary.setVisibility(showSecondary || gate.canComplete ? View.VISIBLE : View.GONE);

        if (btnJoin != null) {
            btnJoin.setVisibility(showSecondary ? View.VISIBLE : View.GONE);
            btnJoin.setEnabled(gate.canJoin);
            btnJoin.setAlpha(gate.canJoin ? 1f : 0.45f);
        }
        if (btnReschedule != null) btnReschedule.setVisibility(showSecondary ? View.VISIBLE : View.GONE);
        if (btnComplete != null) btnComplete.setVisibility(gate.canComplete ? View.VISIBLE : View.GONE);
        if (btnApplyTemplates != null) {
            boolean completed = session.status != null && session.status.toLowerCase().contains("completed");
            btnApplyTemplates.setVisibility(completed && patientId > 0 ? View.VISIBLE : View.GONE);
        }
    }

    private void show(View target){
        content.setVisibility(target==content?View.VISIBLE:View.GONE);
        progress.setVisibility(target==progress?View.VISIBLE:View.GONE);
        errorContainer.setVisibility(target==errorContainer?View.VISIBLE:View.GONE);
    }

    private void showApplyTemplatesDialog() {
        if (patientId <= 0) return;
        String[] labels = new String[]{
                getString(R.string.task_template_resistance),
                getString(R.string.task_template_self_esteem),
                getString(R.string.task_template_encouragement)
        };
        String[] ids = new String[]{"resistance", "self_esteem", "encouragement"};
        boolean[] checked = new boolean[]{true, true, true};
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.specialist_apply_task_templates)
                .setMultiChoiceItems(labels, checked, (d, which, isChecked) -> checked[which] = isChecked)
                .setPositiveButton(R.string.session_task_add_save, (d, w) -> {
                    java.util.List<String> selected = new java.util.ArrayList<>();
                    for (int i = 0; i < ids.length; i++) {
                        if (checked[i]) selected.add(ids[i]);
                    }
                    if (selected.isEmpty()) return;
                    detailRepo.applyTaskTemplates(patientId, sessionId, selected, new SpecialistDetailRepository.ApplyTemplatesCb() {
                        @Override public void ok() {
                            Toast.makeText(requireContext(), R.string.task_templates_applied, Toast.LENGTH_SHORT).show();
                            vm.load(sessionId, patientId);
                        }
                        @Override public void err(Throwable t) {
                            Toast.makeText(requireContext(), R.string.task_templates_failed, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void openSessionTasks(){
        if (sessionId <= 0) {
            Toast.makeText(requireContext(), R.string.error_fetch_data, Toast.LENGTH_SHORT).show();
            return;
        }
        Bundle args = new Bundle();
        args.putInt("sessionId", sessionId);
        args.putBoolean("isSpecialist", true);
        androidx.navigation.fragment.NavHostFragment.findNavController(this).navigate(R.id.sessionDetailFragment, args);
    }

    private void showRejectDialog(){
        final android.widget.EditText input = new android.widget.EditText(requireContext());
        input.setHint(R.string.session_rejection_reason_hint);
        input.setPadding(32, 32, 32, 32);
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.session_rejection_reason_title)
                .setView(input)
                .setPositiveButton(R.string.session_rejection_confirm, (d, w) -> {
                    String reason = input.getText() != null ? input.getText().toString().trim() : null;
                    vm.reject(reason);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showTriageDialog(@NonNull com.brightpath.sanad.feature.patient.PatientIntakeForm intake){
        android.widget.LinearLayout container = new android.widget.LinearLayout(requireContext());
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.setPadding(32, 24, 32, 8);

        java.util.List<android.widget.CheckBox> boxes = new java.util.ArrayList<>();
        String[] tags = new String[]{"bipolar", "anx_dep", "schizophrenia", "children", "mild", "identity"};
        int[] labels = new int[]{
                R.string.fragment_patient_intake_text_9,
                R.string.fragment_patient_intake_text_10,
                R.string.fragment_patient_intake_text_11,
                R.string.fragment_patient_intake_text_12,
                R.string.fragment_patient_intake_text_13,
                R.string.fragment_patient_intake_text_14
        };
        for (int i = 0; i < tags.length; i++){
            android.widget.CheckBox cb = new android.widget.CheckBox(requireContext());
            cb.setText(labels[i]);
            cb.setTag(tags[i]);
            cb.setChecked(intake.triageTags != null && intake.triageTags.contains(tags[i]));
            boxes.add(cb);
            container.addView(cb);
        }

        android.widget.EditText reasonInput = new android.widget.EditText(requireContext());
        reasonInput.setHint(R.string.session_triage_reason_hint);
        reasonInput.setPadding(8, 24, 8, 8);
        container.addView(reasonInput);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.patient_intake_triage)
                .setView(container)
                .setPositiveButton(R.string.session_rejection_confirm, (d, w) -> {
                    java.util.List<String> selected = new java.util.ArrayList<>();
                    for (android.widget.CheckBox cb : boxes){
                        if (cb.isChecked()){
                            selected.add(String.valueOf(cb.getTag()));
                        }
                    }
                    String reason = reasonInput.getText() != null ? reasonInput.getText().toString().trim() : null;
                    vm.updateTriage(selected, reason);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showError(String msg){
        tvError.setText(msg);
        show(errorContainer);
    }

    private String safe(String value){
        return value != null ? value : "-";
    }

    private boolean isPending(@Nullable String status){
        return status != null && status.equalsIgnoreCase("pending");
    }

    private boolean isClosed(@Nullable String status) {
        if (status == null) return false;
        String value = status.toLowerCase();
        return value.contains("pending")
                || value.contains("rejected")
                || value.contains("canceled")
                || value.contains("cancelled")
                || value.contains("completed");
    }

    private boolean canJoin(@Nullable String status, @Nullable String startsAt){
        boolean statusOk = status != null && (status.equalsIgnoreCase("accepted")
                || status.equalsIgnoreCase("upcoming")
                || status.equalsIgnoreCase("confirmed")
                || status.equalsIgnoreCase("in_progress")
                || status.equalsIgnoreCase("started"));
        return statusOk || canJoinNow(startsAt);
    }

    private boolean canComplete(@Nullable String status, @Nullable String startsAt){
        if (status == null) return false;
        String value = status.toLowerCase();
        if (value.contains("completed") || value.contains("rejected") || value.contains("canceled") || value.contains("cancelled")) {
            return false;
        }
        if (value.contains("in_progress") || value.contains("started")) {
            return true;
        }
        return canJoinNow(startsAt);
    }

    private boolean canJoinNow(@Nullable String startsAt){
        if (startsAt == null) return false;
        try {
            Instant start = parseInstant(startsAt);
            Instant now = Instant.now();
            return !now.isBefore(start.minusSeconds(5 * 60));
        } catch (Exception ignored){
            return false;
        }
    }

    private String mapStatus(String raw) {
        if (raw == null) return getString(R.string.session_detail_status);
        switch (raw.toLowerCase()) {
            case "pending":
                return getString(R.string.session_status_pending);
            case "scheduled":
            case "upcoming":
            case "accepted":
            case "confirmed":
            case "in_progress":
            case "started":
                return getString(R.string.session_status_upcoming);
            case "completed":
                return getString(R.string.session_status_completed);
            case "rejected":
                return getString(R.string.session_status_rejected);
            case "cancelled":
            case "canceled":
                return getString(R.string.session_status_cancelled);
            default:
                return raw;
        }
    }

    private String formatSchedule(String raw){
        if (TextUtils.isEmpty(raw)) return raw;
        long ms = parseMillis(raw);
        if (ms <= 0) return raw;
        try {
            ZonedDateTime dt = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault());
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd - hh:mm a");
            return dt.format(fmt);
        } catch (Exception e){
            return raw;
        }
    }

    private long parseMillis(String raw){
        if (TextUtils.isEmpty(raw)) return -1;
        try {
            return Instant.parse(raw).toEpochMilli();
        } catch (DateTimeParseException ignored){}
        try {
            return OffsetDateTime.parse(raw, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant().toEpochMilli();
        } catch (DateTimeParseException ignored){}
        return -1;
    }

    private Instant parseInstant(String raw){
        long ms = parseMillis(raw);
        if (ms <= 0) throw new IllegalArgumentException("invalid time");
        return Instant.ofEpochMilli(ms);
    }

    private void showReschedulePicker(){
        if (sessionId <= 0) return;
        final java.util.Calendar cal = java.util.Calendar.getInstance();
        DatePickerDialog datePicker = new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            cal.set(java.util.Calendar.YEAR, year);
            cal.set(java.util.Calendar.MONTH, month);
            cal.set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth);
            int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
            int minute = cal.get(java.util.Calendar.MINUTE);
            TimePickerDialog timePicker = new TimePickerDialog(requireContext(), (tp, h, m) -> {
                cal.set(java.util.Calendar.HOUR_OF_DAY, h);
                cal.set(java.util.Calendar.MINUTE, m);
                java.util.Calendar end = (java.util.Calendar) cal.clone();
                end.add(java.util.Calendar.MINUTE, 60);
                SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                vm.reschedule(fmt.format(cal.getTime()), fmt.format(end.getTime()));
            }, hour, minute, true);
            timePicker.show();
        }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH));
        datePicker.show();
    }

    private void openJoinLink(@Nullable SessionModels.Session session){
        if (session == null) return;
        if (sessionId <= 0) {
            Toast.makeText(requireContext(), R.string.error_fetch_data, Toast.LENGTH_SHORT).show();
            return;
        }
        SessionActionGate gate = SessionActionGate.evaluate(session.status, session.scheduled_at, true);
        if (!gate.canJoin) {
            Toast.makeText(requireContext(), R.string.session_join_waiting, Toast.LENGTH_SHORT).show();
            return;
        }
        Bundle args = new Bundle();
        args.putInt("sessionId", sessionId);
        args.putString("sessionEndsAt", session.ends_at);
        args.putBoolean("canExtend", true);
        args.putInt("chatId", session.chat_id != null ? session.chat_id : -1);
        args.putString("chatTitle", mapTypeLabel(session.type));
        args.putString("joinUrl", session.join_url);
        args.putString("type", session.type != null ? session.type : "video");
        args.putString("scheduledAt", session.scheduled_at);
        String type = session.type != null ? session.type.toLowerCase() : "";
        if (type.contains("chat") && session.chat_id != null && session.chat_id > 0) {
            NavHostFragment.findNavController(this).navigate(R.id.chatRoomFragment, args);
            return;
        }
        NavHostFragment.findNavController(this).navigate(R.id.sessionCallFragment, args);
    }

    private String mapTypeLabel(String raw){
        if (raw == null) return getString(R.string.next_session_type_placeholder);
        String value = raw.toLowerCase();
        if (value.contains("video")) return getString(R.string.session_type_video);
        if (value.contains("voice") || value.contains("audio")) return getString(R.string.session_type_voice);
        if (value.contains("chat")) return getString(R.string.session_type_chat);
        return raw;
    }

    private String formatIntakeDetails(@NonNull com.brightpath.sanad.feature.patient.PatientIntakeForm intake){
        StringBuilder info = new StringBuilder();
        info.append(getString(R.string.patient_intake_label_age, safe(intake.age))).append("\n");
        if (!TextUtils.isEmpty(intake.ageGender)) {
            info.append(getString(R.string.patient_intake_label_age_gender, safe(intake.ageGender))).append("\n");
        }
        info.append(getString(R.string.patient_intake_label_occupation, safe(intake.occupation))).append("\n");
        info.append(getString(R.string.patient_intake_label_primary_issue, safe(intake.primaryIssue))).append("\n");
        info.append(getString(R.string.patient_intake_label_duration,
                com.brightpath.sanad.ui.IntakeLabelHelper.duration(requireContext(), intake.duration))).append("\n");
        if (intake.symptoms != null && !intake.symptoms.isEmpty()) {
            info.append(getString(R.string.patient_intake_label_symptoms,
                    com.brightpath.sanad.ui.IntakeLabelHelper.joinTokens(requireContext(), intake.symptoms))).append("\n");
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

    private String formatAssessmentDetails(@NonNull com.brightpath.sanad.feature.patient.PatientIntakeForm intake){
        String tags = (intake.triageTags != null && !intake.triageTags.isEmpty())
                ? com.brightpath.sanad.ui.IntakeLabelHelper.joinTokens(requireContext(), intake.triageTags)
                : getString(R.string.session_triage_empty);
        String reason = null;
        if (intake.triageRecommendation != null) {
            reason = intake.triageRecommendation.get("reason");
        }
        StringBuilder info = new StringBuilder();
        info.append(getString(R.string.session_specialist_assessment_tags, tags)).append("\n");
        info.append(getString(R.string.session_specialist_assessment_category, safe(intake.triageCategory))).append("\n");
        info.append(getString(R.string.session_specialist_assessment_reason, safe(reason)));
        return info.toString().trim();
    }

    static class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.VH> {
        private final List<PatientTask> data = new ArrayList<>();
        void submit(List<PatientTask> list){
            data.clear();
            if (list != null) data.addAll(list);
            notifyDataSetChanged();
        }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_patient_task, parent, false);
            return new VH(view);
        }
        @Override public int getItemCount() { return data.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView title, desc, due, status, note, completedAt;
            MaterialButton btnComplete;
            VH(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.tvTaskTitle);
                desc = itemView.findViewById(R.id.tvTaskDesc);
                due = itemView.findViewById(R.id.tvTaskDue);
                status = itemView.findViewById(R.id.tvTaskStatus);
                note = itemView.findViewById(R.id.tvTaskNote);
                completedAt = itemView.findViewById(R.id.tvTaskCompletedAt);
                btnComplete = itemView.findViewById(R.id.btnCompleteTask);
            }
        }
        @Override public void onBindViewHolder(@NonNull VH holder, int position) {
            PatientTask task = data.get(position);
            holder.title.setText(task.title);
            if (!TextUtils.isEmpty(task.description)){
                holder.desc.setVisibility(View.VISIBLE);
                holder.desc.setText(task.description);
            } else {
                holder.desc.setVisibility(View.GONE);
            }
            String date = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date(task.dueAt));
            holder.due.setText(holder.itemView.getResources().getString(R.string.task_due_at, date));
            boolean completed = task.status == PatientTask.Status.COMPLETED;
            holder.status.setText(holder.itemView.getResources().getString(
                    completed ? R.string.task_status_completed : R.string.task_status_pending));
            if (completed && !TextUtils.isEmpty(task.completionNote)){
                holder.note.setVisibility(View.VISIBLE);
                holder.note.setText(holder.itemView.getResources().getString(R.string.task_completion_note_label, task.completionNote));
            } else {
                holder.note.setVisibility(View.GONE);
            }
            if (completed && task.completedAt > 0){
                SimpleDateFormat finish = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
                holder.completedAt.setVisibility(View.VISIBLE);
                holder.completedAt.setText(holder.itemView.getResources().getString(R.string.task_completed_at, finish.format(new Date(task.completedAt))));
            } else {
                holder.completedAt.setVisibility(View.GONE);
            }
            holder.btnComplete.setVisibility(View.GONE);
        }
    }
}
