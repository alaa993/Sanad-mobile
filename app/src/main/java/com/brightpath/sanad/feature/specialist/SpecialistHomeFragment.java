
package com.brightpath.sanad.feature.specialist;

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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.button.MaterialButton;
import com.brightpath.sanad.R;
import com.brightpath.sanad.ui.tour.CoachMarkManager;
import com.brightpath.sanad.ui.tour.CoachMarkStep;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class SpecialistHomeFragment extends Fragment {
    private SpecialistViewModels.HomeVM homeVM;
    private SpecialistViewModels.SessionsVM sessionsVM;
    private UpcomingAdapter adapter;
    private final java.util.Calendar calendar = java.util.Calendar.getInstance();
    private SwipeRefreshLayout swipeRefresh;
    private String mode = "dashboard";
    private View hero, cardStats, cardQuickActions, upcomingLabel;
    private RecyclerView rvUpcoming;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_specialist_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        if (getArguments() != null) {
            String argMode = getArguments().getString("mode", "dashboard");
            mode = argMode != null ? argMode : "dashboard";
        }
        MaterialButton btnSessions = v.findViewById(R.id.btnSessions);
        MaterialButton btnCommunity = v.findViewById(R.id.btnCommunity);
        MaterialButton btnLibrary = v.findViewById(R.id.btnLibrary);
        MaterialButton btnWallet = v.findViewById(R.id.btnWallet);
        MaterialButton btnProfile = v.findViewById(R.id.btnProfile);
        TextView tvUpcoming = v.findViewById(R.id.tvUpcoming);
        TextView tvToday = v.findViewById(R.id.tvToday);
        TextView tvPending = v.findViewById(R.id.tvPending);
        View cardToday = v.findViewById(R.id.cardToday);
        View cardUpcoming = v.findViewById(R.id.cardUpcoming);
        View cardPending = v.findViewById(R.id.cardPending);
        rvUpcoming = v.findViewById(R.id.rvUpcomingSessions);
        hero = v.findViewById(R.id.heroSpecialist);
        cardStats = v.findViewById(R.id.cardStats);
        cardQuickActions = v.findViewById(R.id.cardQuickActions);
        upcomingLabel = v.findViewById(R.id.upcomingLabel);
        rvUpcoming.setLayoutManager(new LinearLayoutManager(requireContext()));

        swipeRefresh = v.findViewById(R.id.swipeRefresh);
        homeVM = new ViewModelProvider(this).get(SpecialistViewModels.HomeVM.class);
        sessionsVM = new ViewModelProvider(this).get(SpecialistViewModels.SessionsVM.class);
        adapter = new UpcomingAdapter(this, sessionsVM);
        rvUpcoming.setAdapter(adapter);

        homeVM.state.observe(getViewLifecycleOwner(), d -> {
            if (d != null && d.counters != null) {
                tvUpcoming.setText(String.valueOf(d.counters.upcoming));
                tvToday.setText(String.valueOf(d.counters.today));
                tvPending.setText(String.valueOf(d.counters.pending));
            }
            if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
        });

        sessionsVM.list.observe(getViewLifecycleOwner(), sessions -> {
            adapter.submit(sessions);
            if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
        });
        sessionsVM.toast.observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                sessionsVM.toast.postValue(null);
            }
        });
        sessionsVM.load("upcoming");

        cardToday.setOnClickListener(x -> openSessionsScope("today"));
        cardUpcoming.setOnClickListener(x -> openSessionsScope("upcoming"));
        cardPending.setOnClickListener(x -> openSessionsScope("pending"));
        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(() -> {
                homeVM.load();
                sessionsVM.load("upcoming");
            });
        }
        homeVM.load();

        // Quick-action buttons stay GONE in layout (nav already covers them).
        // Keep click wiring so they work if visibility is re-enabled later.
        if (btnWallet != null) {
            btnWallet.setOnClickListener(x ->
                    NavHostFragment.findNavController(this).navigate(R.id.action_global_wallet));
        }
        if (btnProfile != null) {
            btnProfile.setOnClickListener(x ->
                    NavHostFragment.findNavController(this).navigate(R.id.specialistProfileFragment));
        }
        if (btnSessions != null) {
            btnSessions.setOnClickListener(x -> openSessionsScope("upcoming"));
        }
        if (btnCommunity != null) {
            btnCommunity.setOnClickListener(x ->
                    NavHostFragment.findNavController(this).navigate(R.id.communityFeedFragment));
        }
        if (btnLibrary != null) {
            btnLibrary.setOnClickListener(x ->
                    NavHostFragment.findNavController(this).navigate(R.id.libraryFragment));
        }

        applyMode();

        v.post(() -> {
            java.util.List<CoachMarkStep> steps = new java.util.ArrayList<>();
            if (cardToday != null) steps.add(CoachMarkManager.step(cardToday, R.string.tour_spec_today_title, R.string.tour_spec_today_desc));
            if (cardUpcoming != null) steps.add(CoachMarkManager.step(cardUpcoming, R.string.tour_spec_upcoming_title, R.string.tour_spec_upcoming_desc));
            if (cardPending != null) steps.add(CoachMarkManager.step(cardPending, R.string.tour_spec_pending_title, R.string.tour_spec_pending_desc));
            if (rvUpcoming != null) steps.add(CoachMarkManager.step(rvUpcoming, R.string.tour_spec_list_title, R.string.tour_spec_list_desc));
            CoachMarkManager.showIfNeeded(SpecialistHomeFragment.this, "tour_specialist_home", steps);
        });
    }

    private void applyMode() {
        if (hero != null) hero.setVisibility(View.VISIBLE);
        if (cardStats != null) cardStats.setVisibility(View.VISIBLE);
        if (upcomingLabel != null) upcomingLabel.setVisibility(View.VISIBLE);
        if (rvUpcoming != null) rvUpcoming.setVisibility(View.VISIBLE);
        if (cardQuickActions != null) cardQuickActions.setVisibility(View.GONE);
    }

    private void openSessionsScope(String scope){
        Bundle b = new Bundle();
        b.putString("scope", scope);
        NavHostFragment.findNavController(this).navigate(R.id.specialistSessionsFragment, b);
    }

    void showReschedulePicker(int sessionId){
        final java.util.Calendar cal = (java.util.Calendar) calendar.clone();
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
                sessionsVM.reschedule(sessionId, fmt.format(cal.getTime()), fmt.format(end.getTime()));
            }, hour, minute, true);
            timePicker.show();
        }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH));
        datePicker.show();
    }

    void showRejectDialog(int sessionId){
        final android.widget.EditText input = new android.widget.EditText(requireContext());
        input.setHint(R.string.session_rejection_reason_hint);
        input.setPadding(32, 32, 32, 32);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.session_rejection_reason_title)
                .setView(input)
                .setPositiveButton(R.string.session_rejection_confirm, (d, w) -> {
                    String reason = input.getText() != null ? input.getText().toString().trim() : null;
                    sessionsVM.reject(sessionId, reason);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    static class UpcomingAdapter extends RecyclerView.Adapter<UpcomingAdapter.VH> {
        private final List<SpecialistModels.Appointment> data = new ArrayList<>();
        private final SpecialistHomeFragment host;
        private final SpecialistViewModels.SessionsVM vm;
        UpcomingAdapter(SpecialistHomeFragment host, SpecialistViewModels.SessionsVM vm){
            this.host = host; this.vm = vm;
        }
        private int extendMinutesDefault = 15;
        void submit(List<SpecialistModels.Appointment> list) {
            data.clear();
            if (list != null) {
                data.addAll(list.size() > 3 ? list.subList(0, 3) : list);
            }
            notifyDataSetChanged();
        }

        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_specialist_upcoming, parent, false);
            return new VH(view);
        }

        @Override public void onBindViewHolder(@NonNull VH holder, int position) {
            SpecialistModels.Appointment appt = data.get(position);
            holder.time.setText(appt.starts_at != null ? formatSchedule(appt.starts_at) : "--");
            holder.status.setText(com.brightpath.sanad.ui.IntakeLabelHelper.sessionStatus(holder.itemView.getContext(), appt.status));
            boolean pending = appt.status != null && appt.status.equalsIgnoreCase("pending");
            boolean active = appt.status != null && (appt.status.equalsIgnoreCase("accepted") || appt.status.equalsIgnoreCase("in_progress"));
            holder.btnAccept.setVisibility(pending ? View.VISIBLE : View.GONE);
            holder.btnReject.setVisibility(pending ? View.VISIBLE : View.GONE);
            holder.btnReschedule.setVisibility(View.VISIBLE);
            holder.btnExtend.setVisibility(active ? View.VISIBLE : View.GONE);
            holder.btnComplete.setVisibility(active ? View.VISIBLE : View.GONE);

            holder.btnAccept.setOnClickListener(v -> vm.accept(appt.id));
            holder.btnReject.setOnClickListener(v -> host.showRejectDialog(appt.id));
            holder.btnReschedule.setOnClickListener(v -> host.showReschedulePicker(appt.id));
            holder.btnExtend.setOnClickListener(v -> host.sessionsVM.extend(appt.id, extendMinutesDefault));
            holder.btnComplete.setOnClickListener(v -> host.sessionsVM.complete(appt.id));
        }

        @Override public int getItemCount() { return data.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView time, status;
            MaterialButton btnAccept, btnReject, btnReschedule, btnExtend, btnComplete;
            VH(@NonNull View itemView) {
                super(itemView);
                time = itemView.findViewById(R.id.tvSessionTime);
                status = itemView.findViewById(R.id.tvSessionStatus);
                btnAccept = itemView.findViewById(R.id.btnAccept);
                btnReject = itemView.findViewById(R.id.btnReject);
                btnReschedule = itemView.findViewById(R.id.btnReschedule);
                btnExtend = itemView.findViewById(R.id.btnExtend);
                btnComplete = itemView.findViewById(R.id.btnComplete);
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
    }

    @Override
    public void onDestroyView() {
        try { CoachMarkManager.dismissActive(); } catch (Throwable ignored) {}
        super.onDestroyView();
    }

}
