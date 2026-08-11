package com.brightpath.sanad.feature.specialist;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
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
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.brightpath.sanad.R;
import com.brightpath.sanad.feature.sessions.SessionActionGate;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class SpecialistSessionsFragment extends Fragment {
    private SpecialistViewModels.SessionsVM vm;
    private View progress;
    private TextView tvEmpty;
    private Adapter adapter;
    private String currentScope = "pending";
    private String requestedScope;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_specialist_sessions, container, false);
    }

    @Override public void onViewCreated(@NonNull View v,@Nullable Bundle s){
        super.onViewCreated(v,s);
        RecyclerView rv = v.findViewById(R.id.rv);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new Adapter(this);
        rv.setAdapter(adapter);
        progress = v.findViewById(R.id.progress);
        tvEmpty = v.findViewById(R.id.tvEmpty);
        MaterialButton btnCreateGroup = v.findViewById(R.id.btnCreateGroupSession);
        if (btnCreateGroup != null) {
            btnCreateGroup.setOnClickListener(view -> NavHostFragment.findNavController(this).navigate(R.id.groupCreateFragment));
        }

        ChipGroup chipGroup = v.findViewById(R.id.chipScopes);
        requestedScope = getArguments() != null ? getArguments().getString("scope") : null;
        chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            Chip chip = group.findViewById(checkedId);
            if (chip != null) {
                currentScope = String.valueOf(chip.getTag());
                loadScope(currentScope);
            }
        });

        vm=new ViewModelProvider(this).get(SpecialistViewModels.SessionsVM.class);
        vm.list.observe(getViewLifecycleOwner(), list -> {
            progress.setVisibility(View.GONE);
            if (list == null || list.isEmpty()){
                tvEmpty.setVisibility(View.VISIBLE);
            } else {
                tvEmpty.setVisibility(View.GONE);
            }
            adapter.submit(list);
        });
        vm.toast.observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                vm.toast.postValue(null);
                if (msg.contains("تم")) {
                    NavHostFragment.findNavController(this).navigate(R.id.specialistHomeFragment);
                }
            }
        });

        if (requestedScope != null && chipGroup != null) {
            String normalized = normalizeScope(requestedScope);
            selectScopeChip(chipGroup, normalized);
            currentScope = normalized;
        }
        loadScope(currentScope);
    }

    private void loadScope(String scope){
        progress.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        vm.load(scope);
    }

    private String normalizeScope(String scope) {
        if (scope == null) return "pending";
        String value = scope.trim().toLowerCase();
        if ("upcoming".equals(value)) return "accepted";
        if ("done".equals(value)) return "completed";
        if ("today".equals(value)) return "accepted";
        return value;
    }

    private void selectScopeChip(ChipGroup chipGroup, String scope) {
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            View child = chipGroup.getChildAt(i);
            if (child instanceof Chip) {
                Object tag = child.getTag();
                if (tag != null && scope.equalsIgnoreCase(String.valueOf(tag))) {
                    chipGroup.check(child.getId());
                    return;
                }
            }
        }
    }

    static class Adapter extends RecyclerView.Adapter<Adapter.VH>{
        private final Fragment host;
        private final List<SpecialistModels.Appointment> data=new ArrayList<>();
        Adapter(Fragment host){ this.host = host; }
        void submit(List<SpecialistModels.Appointment> d){
            data.clear();
            if(d!=null) data.addAll(d);
            notifyDataSetChanged();
        }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p,int v){
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_specialist_session_card,p,false));
        }
        @Override public void onBindViewHolder(@NonNull VH h,int i){
            SpecialistModels.Appointment a=data.get(i);
            h.patient.setText(a.patient_name != null && !a.patient_name.isEmpty()
                    ? a.patient_name : host.getString(R.string.patient_title) + " #" + a.patient_id);
            if (a.organization_name != null && !a.organization_name.isEmpty()){
                h.org.setVisibility(View.VISIBLE);
                h.org.setText(a.organization_name);
            } else {
                h.org.setVisibility(View.GONE);
            }
            h.time.setText(formatWindow(a.starts_at, a.ends_at));
            h.type.setText(typeLabel(host, a.type));
            h.status.setText(statusLabel(host, a.status));
            if (a.notes != null && !a.notes.isEmpty()){
                h.notes.setVisibility(View.VISIBLE);
                h.notes.setText(host.getString(R.string.specialist_session_notes_label, a.notes));
            } else {
                h.notes.setVisibility(View.GONE);
            }
            String scheduleIso = a.starts_at;
            SessionActionGate gate = SessionActionGate.evaluate(a.status, scheduleIso, true);
            h.btnAccept.setVisibility(gate.canAccept ? View.VISIBLE : View.GONE);
            h.btnReject.setVisibility(gate.canReject ? View.VISIBLE : View.GONE);
            h.btnReschedule.setVisibility(!gate.canAccept && gate.phase != SessionActionGate.Phase.COMPLETED
                    && gate.phase != SessionActionGate.Phase.REJECTED
                    && gate.phase != SessionActionGate.Phase.CANCELLED ? View.VISIBLE : View.GONE);
            boolean showJoin = gate.phase == SessionActionGate.Phase.JOINABLE
                    || gate.phase == SessionActionGate.Phase.WAITING_WINDOW
                    || gate.phase == SessionActionGate.Phase.IN_PROGRESS;
            h.btnJoin.setVisibility(showJoin ? View.VISIBLE : View.GONE);
            h.btnJoin.setEnabled(gate.canJoin);
            h.btnJoin.setAlpha(gate.canJoin ? 1f : 0.45f);
            h.btnPatientFile.setVisibility(View.VISIBLE);
            h.btnDetails.setOnClickListener(v -> {
                Bundle b = new Bundle();
                b.putInt("sessionId", a.id);
                b.putInt("patientId", a.patient_id);
                NavHostFragment.findNavController(host).navigate(R.id.specialistSessionDetailFragment, b);
            });
            h.btnPatientFile.setOnClickListener(v -> {
                Bundle b = new Bundle();
                b.putInt("sessionId", a.id);
                b.putInt("patientId", a.patient_id);
                NavHostFragment.findNavController(host).navigate(R.id.specialistPatientFileFragment, b);
            });
            h.btnAccept.setOnClickListener(v -> {
                Fragment parent = host;
                if (parent instanceof SpecialistSessionsFragment) {
                    ((SpecialistSessionsFragment) parent).vm.accept(a.id);
                }
            });
            h.btnReject.setOnClickListener(v -> {
                Fragment parent = host;
                if (parent instanceof SpecialistSessionsFragment) {
                    ((SpecialistSessionsFragment) parent).showRejectDialog(a.id);
                }
            });
            h.btnReschedule.setOnClickListener(v -> {
                if (host instanceof SpecialistSessionsFragment) {
                    ((SpecialistSessionsFragment) host).showReschedulePicker(a.id);
                }
            });
            h.btnJoin.setOnClickListener(v -> {
                if (host instanceof SpecialistSessionsFragment) {
                    ((SpecialistSessionsFragment) host).openJoinInApp(a.id, a.join_url, a.type, scheduleIso);
                }
            });
        }
        @Override public int getItemCount(){ return data.size(); }

        static class VH extends RecyclerView.ViewHolder{
            TextView patient, org, time, type, status, notes;
            MaterialButton btnDetails, btnAccept, btnReject, btnReschedule, btnJoin;
            MaterialButton btnPatientFile;
            VH(@NonNull View v){
                super(v);
                patient=v.findViewById(R.id.tvSessionPatient);
                org=v.findViewById(R.id.tvSessionOrg);
                time=v.findViewById(R.id.tvSessionTime);
                type=v.findViewById(R.id.tvSessionType);
                status=v.findViewById(R.id.tvSessionStatus);
                notes=v.findViewById(R.id.tvSessionNotes);
                btnDetails=v.findViewById(R.id.btnDetails);
                btnPatientFile=v.findViewById(R.id.btnPatientFile);
                btnAccept=v.findViewById(R.id.btnAccept);
                btnReject=v.findViewById(R.id.btnReject);
                btnReschedule=v.findViewById(R.id.btnReschedule);
                btnJoin=v.findViewById(R.id.btnJoin);
            }
        }
    }

    private void showReschedulePicker(int sessionId){
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
                    java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault());
                    vm.reschedule(sessionId, fmt.format(cal.getTime()), fmt.format(end.getTime()));
                }, hour, minute, true);
                timePicker.show();
            }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH));
        datePicker.show();
    }

    private void showRejectDialog(int sessionId){
        final android.widget.EditText input = new android.widget.EditText(requireContext());
        input.setHint(R.string.session_rejection_reason_hint);
        input.setPadding(32, 32, 32, 32);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.session_rejection_reason_title)
                .setView(input)
                .setPositiveButton(R.string.session_rejection_confirm, (d, w) -> {
                    String reason = input.getText() != null ? input.getText().toString().trim() : null;
                    vm.reject(sessionId, reason);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void openJoinInApp(int sessionId, @Nullable String joinUrl, @Nullable String type, @Nullable String scheduledAt) {
        Bundle b = new Bundle();
        b.putInt("sessionId", sessionId);
        b.putString("joinUrl", joinUrl);
        b.putString("type", type != null ? type : "video");
        b.putString("scheduledAt", scheduledAt);
        b.putBoolean("canExtend", true);
        NavHostFragment.findNavController(this).navigate(R.id.sessionCallFragment, b);
    }

        private static String formatWindow(@Nullable String start, @Nullable String end){
            if (start == null && end == null) return "--";
            String s = start != null ? formatSchedule(start) : "--";
            String e = end != null ? formatSchedule(end) : "--";
            if (end == null) return s;
            if (start == null) return e;
            return s + " → " + e;
        }

    private static boolean isPending(@Nullable String status){
        return status != null && status.equalsIgnoreCase("pending");
    }

    private static boolean canJoin(@Nullable String status){
        if (status == null) return false;
        switch (status.toLowerCase()){
            case "accepted":
            case "upcoming":
            case "confirmed":
            case "in_progress":
            case "started":
                return true;
            default:
                return false;
        }
    }

    private static boolean isClosed(@Nullable String status) {
        if (status == null) return false;
        String value = status.toLowerCase();
        return value.contains("pending")
                || value.contains("reject")
                || value.contains("canceled")
                || value.contains("cancelled")
                || value.contains("completed");
    }

    private static boolean canJoinNow(@Nullable String startsAt){
        if (startsAt == null) return false;
        try {
            Instant start = parseInstant(startsAt);
            Instant now = Instant.now();
            // السماح بالانضمام قبل 5 دقائق من الموعد
            return !now.isBefore(start.minusSeconds(5 * 60));
        } catch (Exception ignored){
            return false;
        }
    }

    private static String statusLabel(Fragment host, @Nullable String status){
        if (status == null) return "";
        switch (status.toLowerCase()){
            case "pending":
                return host.getString(R.string.specialist_session_status_pending);
            case "accepted":
            case "confirmed":
            case "upcoming":
                return host.getString(R.string.specialist_session_status_accepted);
            case "rejected":
                return host.getString(R.string.specialist_session_status_rejected);
            case "completed":
                return host.getString(R.string.specialist_session_status_completed);
            case "cancelled":
            case "canceled":
                return host.getString(R.string.specialist_session_status_canceled);
            default:
                return status;
        }
    }

    private static String typeLabel(Fragment host, @Nullable String raw){
        if (raw == null) return host.getString(R.string.session_type_label);
        String v = raw.toLowerCase();
        if (v.contains("video")) return host.getString(R.string.specialist_session_type_video);
        if (v.contains("voice") || v.contains("audio")) return host.getString(R.string.specialist_session_type_voice);
        if (v.contains("chat")) return host.getString(R.string.specialist_session_type_chat);
        return raw;
    }

    private static String formatSchedule(String raw){
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

    private static long parseMillis(String raw){
        if (raw == null || raw.isEmpty()) return -1;
        try {
            return Instant.parse(raw).toEpochMilli();
        } catch (DateTimeParseException ignored){}
        try {
            return OffsetDateTime.parse(raw, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant().toEpochMilli();
        } catch (DateTimeParseException ignored){}
        return -1;
    }

    private static Instant parseInstant(String raw){
        long ms = parseMillis(raw);
        if (ms <= 0) throw new IllegalArgumentException("invalid time");
        return Instant.ofEpochMilli(ms);
    }
}
