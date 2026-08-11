package com.brightpath.sanad.feature.sessions;
import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.brightpath.sanad.R;
import com.brightpath.sanad.data.auth.TokenStore;
import com.brightpath.sanad.ui.tour.CoachMarkManager;
import com.brightpath.sanad.ui.tour.CoachMarkStep;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SessionsFragment extends Fragment {
    private SessionsViewModel vm;
    private View progress, error, content;
    private RecyclerView rvPending, rvAccepted, rvCompleted, rvRejected;
    private TextView emptyPending, emptyAccepted, emptyCompleted, emptyRejected;
    private ChipGroup groupStatus;
    private TextView tvDateRange;
    private View btnPickDate, btnClearFilters;
    private String filterStatus = null;
    private String filterFrom = null;
    private String filterTo = null;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sessions, container, false);
    }

    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        progress = v.findViewById(R.id.progress);
        error    = v.findViewById(R.id.errorContainer);
        content  = v.findViewById(R.id.content);

        rvPending = v.findViewById(R.id.rvPending);
        rvAccepted = v.findViewById(R.id.rvAccepted);
        rvCompleted = v.findViewById(R.id.rvCompleted);
        rvRejected = v.findViewById(R.id.rvRejected);
        emptyPending = v.findViewById(R.id.emptyPending);
        emptyAccepted = v.findViewById(R.id.emptyAccepted);
        emptyCompleted = v.findViewById(R.id.emptyCompleted);
        emptyRejected = v.findViewById(R.id.emptyRejected);
        View btnBookSession = v.findViewById(R.id.btnBookSession);
        View btnGroupSessions = v.findViewById(R.id.btnGroupSessions);
        groupStatus = v.findViewById(R.id.groupStatus);
        tvDateRange = v.findViewById(R.id.tvDateRange);
        btnPickDate = v.findViewById(R.id.btnPickDate);
        btnClearFilters = v.findViewById(R.id.btnClearFilters);

        rvPending.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvAccepted.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCompleted.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvRejected.setLayoutManager(new LinearLayoutManager(requireContext()));

        SessionAdapter pendingAdapter = new SessionAdapter(this);
        SessionAdapter acceptedAdapter = new SessionAdapter(this);
        SessionAdapter completedAdapter = new SessionAdapter(this);
        SessionAdapter rejectedAdapter = new SessionAdapter(this);
        rvPending.setAdapter(pendingAdapter);
        rvAccepted.setAdapter(acceptedAdapter);
        rvCompleted.setAdapter(completedAdapter);
        rvRejected.setAdapter(rejectedAdapter);

        vm = new ViewModelProvider(this).get(SessionsViewModel.class);
        vm.getState().observe(getViewLifecycleOwner(), st -> {
            if (st==null){ show(progress); return; }
            if (st.loading && st.data == null){ show(progress); return; }
            if (st.error!=null && st.data == null){ show(error); return; }
            if (st.data == null){ show(progress); return; }
            show(content);
            SessionModels.SessionList data = st.data;
            List<SessionModels.Session> pending = data != null ? data.pending : null;
            List<SessionModels.Session> accepted = data != null ? data.accepted : null;
            List<SessionModels.Session> completed = data != null ? data.completed : null;
            List<SessionModels.Session> rejected = data != null ? data.rejected : null;
            if (pending == null && data != null) {
                pending = data.upcoming;
            }
            if (accepted == null && data != null) {
                accepted = data.upcoming;
            }
            if (completed == null && data != null) {
                completed = data.history;
            }
            if (rejected == null && data != null) {
                rejected = data.history;
            }
            pendingAdapter.submit(pending);
            acceptedAdapter.submit(accepted);
            completedAdapter.submit(completed);
            rejectedAdapter.submit(rejected);
            emptyPending.setVisibility(pending==null || pending.isEmpty() ? View.VISIBLE : View.GONE);
            emptyAccepted.setVisibility(accepted==null || accepted.isEmpty() ? View.VISIBLE : View.GONE);
            emptyCompleted.setVisibility(completed==null || completed.isEmpty() ? View.VISIBLE : View.GONE);
            emptyRejected.setVisibility(rejected==null || rejected.isEmpty() ? View.VISIBLE : View.GONE);
        });

        v.findViewById(R.id.btnRetry).setOnClickListener(x -> vm.load());
        boolean canBook = isPatientRole();
        if (btnBookSession != null) {
            if (canBook) {
                btnBookSession.setVisibility(View.VISIBLE);
                btnBookSession.setOnClickListener(x -> NavHostFragment.findNavController(this).navigate(R.id.bookSessionFragment));
            } else {
                btnBookSession.setVisibility(View.GONE);
            }
        }
        if (btnGroupSessions != null) {
            btnGroupSessions.setOnClickListener(x -> NavHostFragment.findNavController(this).navigate(R.id.groupsFragment));
        }
        bindFilters();
        applyInitialStatusFilter(getArguments());
        vm.load(buildFilters());

        v.post(() -> {
            java.util.List<CoachMarkStep> steps = new java.util.ArrayList<>();
            if (canBook && btnBookSession != null) {
                steps.add(CoachMarkManager.step(btnBookSession, R.string.tour_sessions_book_title, R.string.tour_sessions_book_desc));
            }
            if (btnGroupSessions != null) steps.add(CoachMarkManager.step(btnGroupSessions, R.string.tour_sessions_groups_title, R.string.tour_sessions_groups_desc));
            if (groupStatus != null) steps.add(CoachMarkManager.step(groupStatus, R.string.tour_sessions_filter_title, R.string.tour_sessions_filter_desc));
            if (btnPickDate != null) steps.add(CoachMarkManager.step(btnPickDate, R.string.tour_sessions_date_title, R.string.tour_sessions_date_desc));
            CoachMarkManager.showIfNeeded(SessionsFragment.this, "tour_sessions", steps);
        });


    }

    @Override
    public void onResume() {
        super.onResume();
        if (vm != null) {
            vm.load(buildFilters(), true);
        }
    }

    private void bindFilters() {
        if (groupStatus != null) {
            groupStatus.setOnCheckedChangeListener((group, checkedId) -> {
                String tag = null;
                View chip = checkedId != View.NO_ID ? group.findViewById(checkedId) : null;
                if (chip != null && chip.getTag() != null) {
                    tag = String.valueOf(chip.getTag());
                }
                filterStatus = (tag == null || "all".equalsIgnoreCase(tag)) ? null : tag;
                vm.load(buildFilters());
            });
        }
        if (btnPickDate != null) {
            btnPickDate.setOnClickListener(x -> openDateRangePicker());
        }
        if (btnClearFilters != null) {
            btnClearFilters.setOnClickListener(x -> {
                filterStatus = null;
                filterFrom = null;
                filterTo = null;
                if (groupStatus != null) {
                    groupStatus.check(R.id.chipAll);
                }
                updateDateRangeLabel();
                vm.load(buildFilters());
            });
        }
        updateDateRangeLabel();
    }

    private void updateDateRangeLabel() {
        if (tvDateRange == null) return;
        if (filterFrom == null || filterTo == null) {
            tvDateRange.setVisibility(View.GONE);
            tvDateRange.setText("");
            return;
        }
        tvDateRange.setVisibility(View.VISIBLE);
        tvDateRange.setText(filterFrom + " - " + filterTo);
    }

    private void applyInitialStatusFilter(@Nullable Bundle args) {
        if (args == null || groupStatus == null) return;
        String status = args.getString("filterStatus");
        if (status == null || status.isEmpty()) return;
        filterStatus = status;
        int chipId = R.id.chipAll;
        if ("pending".equalsIgnoreCase(status)) {
            chipId = R.id.chipPending;
        } else if ("accepted".equalsIgnoreCase(status)) {
            chipId = R.id.chipAccepted;
        } else if ("completed".equalsIgnoreCase(status)) {
            chipId = R.id.chipCompleted;
        }
        groupStatus.check(chipId);
    }

    private void openDateRangePicker() {
        MaterialDatePicker.Builder<androidx.core.util.Pair<Long, Long>> builder =
                MaterialDatePicker.Builder.dateRangePicker();
        builder.setTitleText(R.string.sessions_filter_date_pick);
        MaterialDatePicker<androidx.core.util.Pair<Long, Long>> picker = builder.build();
        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection == null) return;
            Long start = selection.first;
            Long end = selection.second;
            if (start == null || end == null) return;
            filterFrom = toDate(start);
            filterTo = toDate(end);
            updateDateRangeLabel();
            vm.load(buildFilters());
        });
        picker.show(getParentFragmentManager(), "sessions_date_picker");
    }

    private String toDate(Long ms){
        if (ms == null) return null;
        ZonedDateTime dt = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault());
        return dt.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private Map<String,String> buildFilters(){
        Map<String,String> map = new HashMap<>();
        if (filterStatus != null) map.put("status", filterStatus);
        if (filterFrom != null) map.put("from", filterFrom);
        if (filterTo != null) map.put("to", filterTo);
        return map;
    }

    private void show(View t){
        progress.setVisibility(t==progress?View.VISIBLE:View.GONE);
        error.setVisibility(t==error?View.VISIBLE:View.GONE);
        content.setVisibility(t==content?View.VISIBLE:View.GONE);
    }

    static class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.VH> {
        private final List<SessionModels.Session> data = new ArrayList<>();
        private final Fragment host;
        SessionAdapter(Fragment host){ this.host = host; }
        void submit(List<SessionModels.Session> d){ data.clear(); if (d!=null) data.addAll(d); notifyDataSetChanged(); }

        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int v) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_session, p, false));
        }
        @Override public void onBindViewHolder(@NonNull VH h, int i) {
            SessionModels.Session s = data.get(i);
            h.type.setText(mapType(s.type));
            h.status.setText(mapStatus(s.status));
            h.schedule.setText(formatSchedule(s.scheduled_at));
            h.participants.setText(buildParticipants(s));

            View.OnClickListener openDetail = v -> {
                Bundle b = new Bundle(); b.putInt("sessionId", s.id);
                NavController nav = NavHostFragment.findNavController(host);
                nav.navigate(R.id.sessionDetailFragment, b);
            };
            h.itemView.setOnClickListener(openDetail);
            h.btnDetails.setOnClickListener(openDetail);

            SessionActionGate gate = SessionActionGate.evaluate(s.status, s.scheduled_at, false);
            h.btnJoin.setEnabled(gate.canJoin);
            h.btnJoin.setClickable(gate.canJoin);
            h.btnJoin.setAlpha(gate.canJoin ? 1f : 0.45f);
            h.btnJoin.setOnClickListener(v -> {
                if (!gate.canJoin) return;
                Bundle b = new Bundle();
                b.putInt("sessionId", s.id);
                // Hub path: detail owns actions; quick join goes straight toward the room stack.
                NavHostFragment.findNavController(host).navigate(R.id.sessionDetailFragment, b);
            });
        }
        @Override public int getItemCount(){ return data.size(); }
        static class VH extends RecyclerView.ViewHolder {
            TextView type, status, schedule, participants;
            com.google.android.material.button.MaterialButton btnJoin, btnDetails;
            VH(@NonNull View v){
                super(v);
                type = v.findViewById(R.id.tvType);
                status = v.findViewById(R.id.tvStatus);
                schedule = v.findViewById(R.id.tvSchedule);
                participants = v.findViewById(R.id.tvParticipants);
                btnJoin = v.findViewById(R.id.btnQuickJoin);
                btnDetails = v.findViewById(R.id.btnOpenDetails);
            }
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
            if (raw == null || raw.isEmpty()) return -1;
            try {
                return Instant.parse(raw).toEpochMilli();
            } catch (DateTimeParseException ignored){}
            try {
                return OffsetDateTime.parse(raw, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant().toEpochMilli();
            } catch (DateTimeParseException ignored){}
            return -1;
        }

        private String mapType(String raw){
            if (raw == null) return host.getString(R.string.next_session_type_placeholder);
            String value = raw.toLowerCase();
            if (value.contains("video")) return host.getString(R.string.session_type_video);
            if (value.contains("voice") || value.contains("audio")) return host.getString(R.string.session_type_voice);
            if (value.contains("chat")) return host.getString(R.string.session_type_chat);
            return raw;
        }

        private String mapStatus(String raw) {
            if (raw == null) return host.getString(R.string.session_status_unknown);
            switch (raw.toLowerCase()) {
                case "pending":
                    return host.getString(R.string.session_status_pending);
                case "scheduled":
                case "upcoming":
                case "accepted":
                case "confirmed":
                case "in_progress":
                case "started":
                    return host.getString(R.string.session_status_upcoming);
                case "completed":
                    return host.getString(R.string.session_status_completed);
                case "rejected":
                    return host.getString(R.string.session_status_rejected);
                case "cancelled":
                case "canceled":
                    return host.getString(R.string.session_status_cancelled);
                default:
                    return raw;
            }
        }

        private boolean canJoin(SessionModels.Session session) {
            if (session == null) return false;
            return SessionActionGate.evaluate(session.status, session.scheduled_at, false).canJoin;
        }

        private String buildParticipants(SessionModels.Session s){
            String specialist = s.specialist != null ? s.specialist.name : null;
            String org = s.organization != null ? s.organization.name : null;
            if (specialist != null && org != null) {
                return host.getString(R.string.session_participants_both, specialist, org);
            }
            if (specialist != null) {
                return host.getString(R.string.session_participants_specialist, specialist);
            }
            if (org != null) {
                return host.getString(R.string.session_participants_org, org);
            }
            return host.getString(R.string.session_participants_unknown);
        }
    }

    private boolean isPatientRole() {
        String role = new TokenStore(requireContext()).getRole();
        return role == null || role.isEmpty() || "patient".equalsIgnoreCase(role);
    }

    @Override
    public void onDestroyView() {
        try { CoachMarkManager.dismissActive(); } catch (Throwable ignored) {}
        super.onDestroyView();
    }

}
