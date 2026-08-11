package com.brightpath.sanad.feature.sessions;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.brightpath.sanad.R;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class SessionDetailFragment extends Fragment {
    private SessionDetailViewModel vm;
    private View loadingView, errorView;
    private NestedScrollView contentView;
    private TextView tvErrorMessage, tvSchedule, tvNotes, tvJoinHint, tvSpecialist, tvOrg, tvRejectReason;
    private TextView tvDiagnosisNotes, tvTransferBanner, tvTransferReason;
    private View labelDiagnosis;
    private android.widget.ImageView ivAvatar;
    private Chip chipType, chipStatus;
    private MaterialButton btnJoin, btnRetry, btnAddTask, btnRateSpecialist, btnRatePatient, btnCancelSession, btnToggleSteps;
    private MaterialButton btnCompleteSession, btnSessionSurvey;
    private RecyclerView rvTasks;
    private TextView tvTasksEmpty, tvSteps;
    private View cardSessionSteps;
    private TaskAdapter taskAdapter;
    private SessionModels.Session currentSession;
    private int sessionId = -1;
    private boolean isSpecialist = false;
    private SessionActionsRepository actionsRepo;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_session_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        sessionId = getArguments()!=null ? getArguments().getInt("sessionId", -1) : -1;
        isSpecialist = getArguments()!=null && getArguments().getBoolean("isSpecialist", false);

        loadingView  = v.findViewById(R.id.viewLoading);
        errorView    = v.findViewById(R.id.viewError);
        contentView  = v.findViewById(R.id.contentGroup);
        tvErrorMessage = v.findViewById(R.id.tvErrorMessage);
        tvSchedule   = v.findViewById(R.id.tvSchedule);
        tvNotes      = v.findViewById(R.id.tvNotes);
        tvDiagnosisNotes = v.findViewById(R.id.tvDiagnosisNotes);
        labelDiagnosis = v.findViewById(R.id.labelDiagnosis);
        tvTransferBanner = v.findViewById(R.id.tvTransferBanner);
        tvTransferReason = v.findViewById(R.id.tvTransferReason);
        tvJoinHint   = v.findViewById(R.id.tvJoinHint);
        tvSpecialist = v.findViewById(R.id.tvSpecialistName);
        tvOrg        = v.findViewById(R.id.tvOrgName);
        tvRejectReason = v.findViewById(R.id.tvRejectReason);
        ivAvatar     = v.findViewById(R.id.ivAvatar);
        chipType     = v.findViewById(R.id.chipType);
        chipStatus   = v.findViewById(R.id.chipStatus);
        btnJoin      = v.findViewById(R.id.btnJoinSession);
        btnCancelSession = v.findViewById(R.id.btnCancelSession);
        btnRetry     = v.findViewById(R.id.btnRetry);
        btnAddTask   = v.findViewById(R.id.btnAddTask);
        btnRateSpecialist = v.findViewById(R.id.btnRateSpecialist);
        btnRatePatient    = v.findViewById(R.id.btnRatePatient);
        btnCompleteSession = v.findViewById(R.id.btnCompleteSession);
        btnSessionSurvey = v.findViewById(R.id.btnSessionSurvey);
        btnToggleSteps = v.findViewById(R.id.btnToggleSteps);
        cardSessionSteps = v.findViewById(R.id.cardSessionSteps);
        rvTasks = v.findViewById(R.id.rvSessionTasks);
        tvTasksEmpty = v.findViewById(R.id.tvTasksEmpty);
        tvSteps = v.findViewById(R.id.tvSteps);
        rvTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        taskAdapter = new TaskAdapter();
        rvTasks.setAdapter(taskAdapter);
        actionsRepo = new SessionActionsRepository(requireContext());

        btnRetry.setOnClickListener(x -> {
            if (sessionId > 0) vm.load(sessionId);
        });
        btnJoin.setOnClickListener(x -> openJoinScreen());
        if (btnCancelSession != null) {
            btnCancelSession.setOnClickListener(x -> showCancelDialog());
        }
        btnAddTask.setOnClickListener(x -> showAddTaskDialog());
        btnRateSpecialist.setOnClickListener(x -> showRatingDialog(false));
        btnRatePatient.setOnClickListener(x -> showRatingDialog(true));
        if (btnCompleteSession != null) {
            btnCompleteSession.setOnClickListener(x -> showCompleteSessionDialog());
        }
        if (btnSessionSurvey != null) {
            btnSessionSurvey.setOnClickListener(x -> showSessionSurveyDialog());
        }
        if (btnToggleSteps != null && cardSessionSteps != null) {
            btnToggleSteps.setOnClickListener(v1 -> toggleSteps());
        }

        vm = new ViewModelProvider(this).get(SessionDetailViewModel.class);
        vm.getState().observe(getViewLifecycleOwner(), state -> {
            if (state == null || state.loading) {
                show(loadingView);
                return;
            }
            if (state.error != null) {
                tvErrorMessage.setText(state.error);
                show(errorView);
                return;
            }
            currentSession = state.data;
            bindSession(state.data);
            show(contentView);
            loadTasks();
        });

        if (sessionId > 0) {
            vm.load(sessionId);
        } else {
            tvErrorMessage.setText(R.string.error_fetch_data);
            show(errorView);
        }
    }

    private void show(View visible) {
        loadingView.setVisibility(visible == loadingView ? View.VISIBLE : View.GONE);
        errorView.setVisibility(visible == errorView ? View.VISIBLE : View.GONE);
        contentView.setVisibility(visible == contentView ? View.VISIBLE : View.GONE);
    }

    private void bindSession(SessionModels.Session session) {
        tvSpecialist.setText(session.specialist != null && !TextUtils.isEmpty(session.specialist.name)
                ? session.specialist.name : getString(R.string.session_detail_header));
        tvOrg.setText(session.organization != null && !TextUtils.isEmpty(session.organization.name)
                ? session.organization.name : getString(R.string.session_org_unknown));
        if (ivAvatar != null) {
            if (session.specialist != null && !TextUtils.isEmpty(session.specialist.avatar)) {
                ivAvatar.setVisibility(View.VISIBLE);
                Glide.with(ivAvatar.getContext())
                        .load(session.specialist.avatar)
                        .placeholder(R.drawable.ic_specialists)
                        .circleCrop()
                        .into(ivAvatar);
            } else {
                ivAvatar.setVisibility(View.GONE);
            }
        }

        chipType.setText(mapType(session.type));
        chipStatus.setText(mapStatus(session.status));

        String schedule = !TextUtils.isEmpty(session.scheduled_at)
                ? formatSchedule(session.scheduled_at)
                : getString(R.string.session_unknown_schedule);
        tvSchedule.setText(schedule);

        String notes = !TextUtils.isEmpty(session.notes) ? session.notes : "-";
        tvNotes.setText(notes);

        boolean hasDiagnosis = !TextUtils.isEmpty(session.specialist_notes);
        if (labelDiagnosis != null) labelDiagnosis.setVisibility(hasDiagnosis ? View.VISIBLE : View.GONE);
        if (tvDiagnosisNotes != null) {
            tvDiagnosisNotes.setVisibility(hasDiagnosis ? View.VISIBLE : View.GONE);
            tvDiagnosisNotes.setText(hasDiagnosis ? session.specialist_notes : "");
        }
        if (tvTransferBanner != null) {
            if (!TextUtils.isEmpty(session.transferred_at)) {
                tvTransferBanner.setVisibility(View.VISIBLE);
                tvTransferBanner.setText(getString(R.string.session_transferred_banner));
            } else {
                tvTransferBanner.setVisibility(View.GONE);
            }
        }
        if (tvTransferReason != null) {
            if (!TextUtils.isEmpty(session.transfer_reason)) {
                tvTransferReason.setVisibility(View.VISIBLE);
                tvTransferReason.setText(getString(R.string.session_transfer_reason_label,
                        localizeTransferReason(session.transfer_reason)));
            } else {
                tvTransferReason.setVisibility(View.GONE);
            }
        }
        if (tvRejectReason != null) {
            if (!TextUtils.isEmpty(session.rejection_reason)) {
                tvRejectReason.setVisibility(View.VISIBLE);
                tvRejectReason.setText(getString(R.string.session_rejection_reason_label, session.rejection_reason));
            } else {
                tvRejectReason.setVisibility(View.GONE);
            }
        }

        SessionActionGate gate = SessionActionGate.evaluate(session.status, session.scheduled_at, isSpecialist);
        btnJoin.setEnabled(gate.canJoin);
        btnJoin.setClickable(gate.canJoin);
        btnJoin.setAlpha(gate.canJoin ? 1f : 0.45f);
        tvJoinHint.setText(hintResForGate(gate));
        if (tvSteps != null) {
            if (isSpecialist) {
                tvSteps.setText(getString(R.string.session_steps_specialist));
            } else {
                tvSteps.setText(getString(R.string.session_steps_patient));
            }
        }

        // إظهار الأزرار حسب الدور والحالة
        btnAddTask.setVisibility(isSpecialist ? View.VISIBLE : View.GONE);
        boolean completed = gate.phase == SessionActionGate.Phase.COMPLETED;
        btnRateSpecialist.setVisibility(!isSpecialist && completed ? View.VISIBLE : View.GONE);
        btnRatePatient.setVisibility(isSpecialist && completed ? View.VISIBLE : View.GONE);
        if (btnCompleteSession != null) {
            btnCompleteSession.setVisibility(gate.canComplete ? View.VISIBLE : View.GONE);
        }
        if (btnSessionSurvey != null) {
            boolean surveyDone = session.survey_submitted != null && session.survey_submitted;
            btnSessionSurvey.setVisibility(!isSpecialist && completed && !surveyDone ? View.VISIBLE : View.GONE);
        }
        if (btnCancelSession != null) {
            btnCancelSession.setVisibility(gate.canCancel ? View.VISIBLE : View.GONE);
        }
    }

    private int hintResForGate(SessionActionGate gate) {
        if (gate == null) return R.string.session_join_disabled;
        switch (gate.joinHintKey) {
            case "session_join_available_now":
                return R.string.session_join_ready;
            case "session_join_wait_accept":
                return R.string.session_join_wait_accept;
            case "session_join_wait":
                return R.string.session_join_waiting;
            default:
                return R.string.session_join_disabled;
        }
    }

    private String localizeTransferReason(String raw) {
        if (raw == null) return "";
        if ("no_response".equalsIgnoreCase(raw.trim())) {
            return getString(R.string.transfer_reason_no_response);
        }
        return raw;
    }

    private String mapType(String raw) {
        if (raw == null) return getString(R.string.next_session_type_placeholder);
        String value = raw.toLowerCase();
        if (value.contains("video")) return getString(R.string.session_type_video);
        if (value.contains("voice") || value.contains("audio")) return getString(R.string.session_type_voice);
        if (value.contains("chat")) return getString(R.string.session_type_chat);
        return raw;
    }

    private String formatSchedule(String raw){
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

    private boolean canJoin(SessionModels.Session session) {
        if (session == null) return false;
        return SessionActionGate.evaluate(session.status, session.scheduled_at, isSpecialist).canJoin;
    }

    private boolean canCancel(@Nullable String status) {
        return SessionActionGate.evaluate(status, (String) null, isSpecialist).canCancel;
    }

    private void showCancelDialog(){
        int resolvedId = sessionId;
        if (resolvedId <= 0 && currentSession != null) {
            resolvedId = currentSession.id;
        }
        if (resolvedId <= 0) {
            Toast.makeText(requireContext(), R.string.error_fetch_data, Toast.LENGTH_SHORT).show();
            return;
        }
        final int targetId = resolvedId;
        final android.widget.EditText input = new android.widget.EditText(requireContext());
        input.setHint(R.string.session_rejection_reason_hint);
        input.setPadding(32, 32, 32, 32);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.session_cancel_title)
                .setView(input)
                .setPositiveButton(R.string.session_rejection_confirm, (d, w) -> {
                    String reason = input.getText() != null ? input.getText().toString().trim() : null;
                    actionsRepo.cancelSession(targetId, reason, new SessionActionsRepository.SimpleCb() {
                        @Override public void ok() {
                            Toast.makeText(requireContext(), R.string.session_cancelled_toast, Toast.LENGTH_SHORT).show();
                            NavHostFragment.findNavController(SessionDetailFragment.this).navigate(R.id.homeFragment);
                        }
                        @Override public void err(Throwable t) {
                            Toast.makeText(requireContext(), R.string.session_cancel_failed, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void openJoinScreen(){
        if (sessionId <= 0 && currentSession != null) {
            sessionId = currentSession.id;
        }
        if (sessionId <= 0) {
            tvErrorMessage.setText(R.string.error_fetch_data);
            show(errorView);
            return;
        }
        SessionActionGate gate = SessionActionGate.evaluate(
                currentSession != null ? currentSession.status : null,
                currentSession != null ? currentSession.scheduled_at : null,
                isSpecialist
        );
        if (!gate.canJoin) {
            Toast.makeText(requireContext(), hintResForGate(gate), Toast.LENGTH_SHORT).show();
            return;
        }
        Bundle args = new Bundle();
        args.putInt("sessionId", sessionId);
        if (currentSession != null) {
            args.putString("sessionEndsAt", currentSession.ends_at);
            args.putBoolean("canExtend", isSpecialist);
            args.putInt("chatId", currentSession.chat_id != null ? currentSession.chat_id : -1);
            args.putString("chatTitle", mapType(currentSession.type));
            args.putString("joinUrl", currentSession.join_url);
            args.putString("type", currentSession.type != null ? currentSession.type : "video");
            args.putString("scheduledAt", currentSession.scheduled_at);
            String type = currentSession.type != null ? currentSession.type.toLowerCase() : "";
            if (type.contains("chat") && currentSession.chat_id != null && currentSession.chat_id > 0) {
                NavHostFragment.findNavController(this).navigate(R.id.chatRoomFragment, args);
                return;
            }
            NavHostFragment.findNavController(this).navigate(R.id.sessionCallFragment, args);
            return;
        }
        NavHostFragment.findNavController(this).navigate(R.id.sessionJoinFragment, args);
    }

    private void loadTasks(){
        if (sessionId <= 0) return;
        actionsRepo.listTasks(sessionId, new SessionActionsRepository.TaskListCb() {
            @Override public void ok(List<SessionActionsRepository.Task> tasks) {
                taskAdapter.submit(tasks);
                tvTasksEmpty.setVisibility(tasks==null || tasks.isEmpty()?View.VISIBLE:View.GONE);
            }
            @Override public void err(Throwable t) {
                tvTasksEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    private void showAddTaskDialog(){
        android.widget.LinearLayout container = new android.widget.LinearLayout(requireContext());
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.setPadding(32, 32, 32, 16);

        final android.widget.EditText etTitle = new android.widget.EditText(requireContext());
        etTitle.setHint(getString(R.string.task_title_hint));
        container.addView(etTitle);

        final android.widget.EditText etDesc = new android.widget.EditText(requireContext());
        etDesc.setHint(getString(R.string.task_desc_hint));
        etDesc.setPadding(0, 24, 0, 0);
        container.addView(etDesc);

        android.widget.TextView typeLabel = new android.widget.TextView(requireContext());
        typeLabel.setPadding(0, 24, 0, 8);
        typeLabel.setText(getString(R.string.session_task_type_label));
        container.addView(typeLabel);

        final android.widget.RadioGroup group = new android.widget.RadioGroup(requireContext());
        group.setOrientation(android.widget.RadioGroup.VERTICAL);
        android.widget.RadioButton rbTask = new android.widget.RadioButton(requireContext());
        rbTask.setText(getString(R.string.session_task_type_task));
        rbTask.setId(View.generateViewId());
        group.addView(rbTask);
        android.widget.RadioButton rbTreatment = new android.widget.RadioButton(requireContext());
        rbTreatment.setText(getString(R.string.session_task_type_treatment));
        rbTreatment.setId(View.generateViewId());
        group.addView(rbTreatment);
        android.widget.RadioButton rbQuestion = new android.widget.RadioButton(requireContext());
        rbQuestion.setText(getString(R.string.session_task_type_question));
        rbQuestion.setId(View.generateViewId());
        group.addView(rbQuestion);
        group.check(rbTask.getId());
        container.addView(group);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.fragment_session_detail_text_2))
                .setView(container)
                .setPositiveButton(getString(R.string.session_task_add_save), (d, w) -> {
                    String title = etTitle.getText()!=null ? etTitle.getText().toString().trim() : "";
                    String desc = etDesc.getText()!=null ? etDesc.getText().toString().trim() : null;
                    if (TextUtils.isEmpty(title)) return;
                    String type = "task";
                    int checked = group.getCheckedRadioButtonId();
                    if (checked == rbTreatment.getId()) type = "treatment";
                    else if (checked == rbQuestion.getId()) type = "question";
                    actionsRepo.addTask(sessionId, title, desc, type, null, new SessionActionsRepository.TaskCb() {
                        @Override public void ok(SessionActionsRepository.Task task) { loadTasks(); }
                        @Override public void err(Throwable t) { Toast.makeText(requireContext(), getString(R.string.session_task_add_failed), Toast.LENGTH_SHORT).show(); }
                    });
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void toggleSteps() {
        if (cardSessionSteps == null || btnToggleSteps == null) return;
        boolean show = cardSessionSteps.getVisibility() != View.VISIBLE;
        cardSessionSteps.setVisibility(show ? View.VISIBLE : View.GONE);
        btnToggleSteps.setText(show
                ? R.string.session_steps_toggle_hide
                : R.string.session_steps_toggle_show);
    }

    private void showCompleteTaskDialog(SessionActionsRepository.Task task){
        final android.widget.EditText etAnswer = new android.widget.EditText(requireContext());
        etAnswer.setHint("إجابتك / إنجازك");
        etAnswer.setPadding(32,32,32,32);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("إنهاء المهمة")
                .setView(etAnswer)
                .setPositiveButton("إرسال", (d, w) -> {
                    String ans = etAnswer.getText()!=null ? etAnswer.getText().toString().trim() : null;
                    actionsRepo.completeTask(task.id, ans, new SessionActionsRepository.TaskCb() {
                        @Override public void ok(SessionActionsRepository.Task t) { loadTasks(); }
                        @Override public void err(Throwable t) { Toast.makeText(requireContext(), "فشل إكمال المهمة", Toast.LENGTH_SHORT).show(); }
                    });
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showCompleteSessionDialog() {
        final android.widget.EditText etDiagnosis = new android.widget.EditText(requireContext());
        etDiagnosis.setHint(getString(R.string.session_diagnosis_notes_hint));
        etDiagnosis.setMinLines(3);
        etDiagnosis.setPadding(32, 32, 32, 32);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.session_complete_title)
                .setView(etDiagnosis)
                .setPositiveButton(R.string.session_complete_confirm, (d, w) -> {
                    String notes = etDiagnosis.getText() != null ? etDiagnosis.getText().toString().trim() : "";
                    int targetId = sessionId > 0 ? sessionId : (currentSession != null ? currentSession.id : -1);
                    if (targetId <= 0) return;
                    actionsRepo.completeSession(targetId, notes, new SessionActionsRepository.SimpleCb() {
                        @Override public void ok() {
                            Toast.makeText(requireContext(), R.string.session_completed_toast, Toast.LENGTH_SHORT).show();
                            vm.load(targetId);
                        }
                        @Override public void err(Throwable t) {
                            Toast.makeText(requireContext(), R.string.session_complete_failed, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showSessionSurveyDialog() {
        final android.widget.EditText etComment = new android.widget.EditText(requireContext());
        etComment.setHint(getString(R.string.session_survey_comment_hint));
        etComment.setPadding(32, 32, 32, 32);
        int targetId = sessionId > 0 ? sessionId : (currentSession != null ? currentSession.id : -1);
        if (targetId <= 0) return;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.session_survey_title)
                .setView(etComment)
                .setItems(new String[]{"1", "2", "3", "4", "5"}, (d, which) -> {
                    int score = which + 1;
                    String comment = etComment.getText() != null ? etComment.getText().toString().trim() : null;
                    actionsRepo.submitSurvey(targetId, score, comment, new SessionActionsRepository.SimpleCb() {
                        @Override public void ok() {
                            Toast.makeText(requireContext(), R.string.session_survey_saved, Toast.LENGTH_SHORT).show();
                            if (btnSessionSurvey != null) btnSessionSurvey.setVisibility(View.GONE);
                        }
                        @Override public void err(Throwable t) {
                            Toast.makeText(requireContext(), R.string.session_survey_failed, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showRatingDialog(boolean ratePatient){
        final android.widget.EditText etComment = new android.widget.EditText(requireContext());
        etComment.setHint("ملاحظات (اختياري)");
        etComment.setPadding(32,32,32,32);
        String title = ratePatient ? "قيّم المريض" : "قيّم الأخصائي";
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setView(etComment)
                .setItems(new String[]{"1","2","3","4","5"}, (d, which) -> {
                    int score = which+1;
                    String comment = etComment.getText()!=null ? etComment.getText().toString().trim() : null;
                    SessionActionsRepository.SimpleCb cb = new SessionActionsRepository.SimpleCb() {
                        @Override public void ok() { Toast.makeText(requireContext(), "تم حفظ التقييم", Toast.LENGTH_SHORT).show(); }
                        @Override public void err(Throwable t) { Toast.makeText(requireContext(), "فشل حفظ التقييم", Toast.LENGTH_SHORT).show(); }
                    };
                    if (ratePatient) actionsRepo.ratePatient(sessionId, score, comment, cb);
                    else actionsRepo.rateSpecialist(sessionId, score, comment, cb);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.VH>{
        private final List<SessionActionsRepository.Task> data = new ArrayList<>();
        void submit(List<SessionActionsRepository.Task> list){
            data.clear();
            if (list != null) data.addAll(list);
            notifyDataSetChanged();
        }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new VH(view);
        }
        @Override public int getItemCount(){ return data.size(); }
        @Override public void onBindViewHolder(@NonNull VH h, int pos){
            SessionActionsRepository.Task t = data.get(pos);
            h.title.setText(t.title);
            String status = "open".equalsIgnoreCase(t.status) ? "مفتوحة" : "مكتملة";
            h.subtitle.setText(status + (t.patient_answer!=null ? " • " + t.patient_answer : ""));
            if (!isSpecialist && "open".equalsIgnoreCase(t.status)) {
                h.itemView.setOnClickListener(v -> showCompleteTaskDialog(t));
            } else {
                h.itemView.setOnClickListener(null);
            }
        }
        class VH extends RecyclerView.ViewHolder{
            TextView title, subtitle;
            VH(@NonNull View v){ super(v); title=v.findViewById(android.R.id.text1); subtitle=v.findViewById(android.R.id.text2); }
        }
    }
}
