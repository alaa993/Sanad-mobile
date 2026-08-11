
package com.brightpath.sanad.feature.org;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.brightpath.sanad.R;
import com.brightpath.sanad.feature.home.AppNavigator;
import com.brightpath.sanad.ui.tour.CoachMarkManager;
import com.brightpath.sanad.ui.tour.CoachMarkStep;

import java.util.ArrayList;
import java.util.List;

public class OrgHomeFragment extends Fragment {
    private OrgViewModels.HomeVM vm;
    private ProgressBar progress;
    private LinearLayout contentGroup;
    private TextView tvError;
    private TextView tvBeneficiaries, tvSessionsTotal, tvUpcoming, tvActiveSpecialists, tvRiskCases;
    private QuickActionsAdapter quickAdapter;
    private AlertsAdapter alertsAdapter;
    private String mode = "dashboard";
    private TextView tvTitle, tvSubtitle, tvQuickActionsLabel;
    private RecyclerView rvQuick;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_org_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() != null) {
            String argMode = getArguments().getString("mode", "dashboard");
            mode = argMode != null ? argMode : "dashboard";
        }
        progress = view.findViewById(R.id.progress);
        contentGroup = view.findViewById(R.id.contentGroup);
        tvError = view.findViewById(R.id.tvError);
        tvTitle = view.findViewById(R.id.tvTitle);
        tvSubtitle = view.findViewById(R.id.tvSubtitle);
        tvQuickActionsLabel = view.findViewById(R.id.tvQuickActionsLabel);
        tvBeneficiaries = view.findViewById(R.id.tvBeneficiaries);
        tvSessionsTotal = view.findViewById(R.id.tvSessionsTotal);
        tvUpcoming = view.findViewById(R.id.tvUpcoming);
        tvActiveSpecialists = view.findViewById(R.id.tvActiveSpecialists);
        tvRiskCases = view.findViewById(R.id.tvRiskCases);

        rvQuick = view.findViewById(R.id.rvQuickActions);
        RecyclerView rvAlerts = view.findViewById(R.id.rvAlerts);
        quickAdapter = new QuickActionsAdapter(this::handleQuickAction);
        alertsAdapter = new AlertsAdapter();
        if ("shortcuts".equalsIgnoreCase(mode)) {
            rvQuick.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(requireContext(), 2));
        } else {
            rvQuick.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false));
        }
        rvQuick.setAdapter(quickAdapter);
        rvAlerts.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvAlerts.setAdapter(alertsAdapter);

        Button btnSpecs = view.findViewById(R.id.btnSpecs);
        Button btnSessions = view.findViewById(R.id.btnSessions);
        btnSpecs.setOnClickListener(v -> AppNavigator.go(this, R.id.orgSpecialistsFragment));
        btnSessions.setOnClickListener(v -> AppNavigator.go(this, R.id.orgSessionsFragment));

        vm = new ViewModelProvider(this).get(OrgViewModels.HomeVM.class);
        vm.state().observe(getViewLifecycleOwner(), this::renderState);
        vm.load();
        applyMode();

        view.post(() -> {
            java.util.List<CoachMarkStep> steps = new java.util.ArrayList<>();
            if (rvQuick != null) steps.add(CoachMarkManager.step(rvQuick, R.string.tour_org_quick_title, R.string.tour_org_quick_desc));
            if (tvBeneficiaries != null) steps.add(CoachMarkManager.step(tvBeneficiaries, R.string.tour_org_beneficiaries_title, R.string.tour_org_beneficiaries_desc));
            if (tvSessionsTotal != null) steps.add(CoachMarkManager.step(tvSessionsTotal, R.string.tour_org_sessions_title, R.string.tour_org_sessions_desc));
            CoachMarkManager.showIfNeeded(OrgHomeFragment.this, "tour_org_home", steps);
        });
    }

    private void renderState(OrgViewModels.HomeVM.UiState state) {
        if (state == null) return;
        progress.setVisibility(state.loading ? View.VISIBLE : View.GONE);
        if (state.error != null) {
            tvError.setVisibility(View.VISIBLE);
            tvError.setText(state.error);
        } else {
            tvError.setVisibility(View.GONE);
        }
        boolean showContent = !state.loading && state.data != null;
        contentGroup.setVisibility(showContent ? View.VISIBLE : View.GONE);
        if (!showContent) return;

        OrgModels.Counters counters = state.data.counters;
        if (counters != null) {
            int beneficiaries = counters.beneficiaries != 0 ? counters.beneficiaries : counters.pending;
            tvBeneficiaries.setText(String.valueOf(beneficiaries));
            int sessions = counters.sessions_total != 0 ? counters.sessions_total : counters.upcoming;
            tvSessionsTotal.setText(String.valueOf(sessions));
            int upcoming = counters.upcoming_48h != 0 ? counters.upcoming_48h : counters.upcoming;
            tvUpcoming.setText(String.valueOf(upcoming));
            int specialists = counters.specialists_active;
            tvActiveSpecialists.setText(String.valueOf(specialists));
            tvRiskCases.setText(String.valueOf(counters.high_risk_cases));
        }

        List<OrgModels.QuickAction> quickActions;
        if ("shortcuts".equalsIgnoreCase(mode)) {
            quickActions = defaultQuickActions();
        } else if (state.data.quick_actions != null && !state.data.quick_actions.isEmpty()) {
            quickActions = state.data.quick_actions;
        } else {
            quickActions = defaultQuickActions();
        }
        quickAdapter.submit(quickActions);

        List<OrgModels.Alert> alerts = state.data.alerts;
        alertsAdapter.submit(alerts == null ? new ArrayList<>() : alerts);
        applyMode();
    }

    private void applyMode() {
        boolean shortcutsOnly = "shortcuts".equalsIgnoreCase(mode);
        boolean hideShortcuts = "dashboard".equalsIgnoreCase(mode);
        if (tvTitle != null) tvTitle.setVisibility(shortcutsOnly ? View.GONE : View.VISIBLE);
        if (tvSubtitle != null) tvSubtitle.setVisibility(shortcutsOnly ? View.GONE : View.VISIBLE);
        if (contentGroup != null && shortcutsOnly) {
            for (int i = 0; i < contentGroup.getChildCount(); i++) {
                View child = contentGroup.getChildAt(i);
                if (child != null) child.setVisibility(View.GONE);
            }
            if (tvQuickActionsLabel != null) tvQuickActionsLabel.setVisibility(View.VISIBLE);
            if (rvQuick != null) rvQuick.setVisibility(View.VISIBLE);
        } else if (contentGroup != null) {
            for (int i = 0; i < contentGroup.getChildCount(); i++) {
                View child = contentGroup.getChildAt(i);
                if (child != null) child.setVisibility(View.VISIBLE);
            }
        }
        if (hideShortcuts) {
            if (tvQuickActionsLabel != null) tvQuickActionsLabel.setVisibility(View.VISIBLE);
            if (rvQuick != null) rvQuick.setVisibility(View.VISIBLE);
        }
    }

    private List<OrgModels.QuickAction> defaultQuickActions() {
        List<OrgModels.QuickAction> fallback = new ArrayList<>();
        fallback.add(createAction("beneficiaries", getString(R.string.fragment_org_beneficiaries_text_1)));
        fallback.add(createAction("group_session", getString(R.string.fragment_org_home_text_15)));
        fallback.add(createAction("reports", getString(R.string.org_reports_title)));
        fallback.add(createAction("community_room", getString(R.string.org_dashboard_action_support_room)));
        return fallback;
    }

    private OrgModels.QuickAction createAction(String id, String label) {
        OrgModels.QuickAction q = new OrgModels.QuickAction();
        q.id = id;
        q.label = label;
        return q;
    }

    private void handleQuickAction(String id) {
        if (id == null) return;
        switch (id) {
            case "add_beneficiary": {
                Bundle args = new Bundle();
                args.putBoolean("openAdd", true);
                AppNavigator.go(this, R.id.orgBeneficiariesFragment, args);
                break;
            }
            case "group_session":
                AppNavigator.go(this, R.id.orgSessionsFragment);
                break;
            case "beneficiaries":
                AppNavigator.go(this, R.id.orgBeneficiariesFragment);
                break;
            case "reports":
                AppNavigator.go(this, R.id.orgReportsFragment);
                break;
            case "billing":
                AppNavigator.go(this, R.id.orgBillingFragment);
                break;
            case "community_room":
                openSupportRoom();
                break;
            default:
                Toast.makeText(requireContext(), R.string.shortcut_not_supported, Toast.LENGTH_SHORT).show();
        }
    }

    private void openSupportRoom() {
        OrgRepository repo = new OrgRepository(requireContext());
        repo.supportRoom(new OrgRepository.Cb<OrgModels.SupportRoom>() {
            @Override public void ok(OrgModels.SupportRoom room) {
                if (!isAdded() || room == null) return;
                Bundle b = new Bundle();
                b.putInt("communityId", room.community_id);
                String title = getString(R.string.org_dashboard_action_support_room);
                if (room.name != null && !room.name.isEmpty()) {
                    title = room.name;
                } else if (room.slug != null) {
                    title = room.slug;
                }
                b.putString("communityTitle", title);
                AppNavigator.go(OrgHomeFragment.this, R.id.communityFeedFragment, b);
            }
            @Override public void err(Throwable e) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), R.string.org_dashboard_support_room_error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private static class QuickActionsAdapter extends RecyclerView.Adapter<QuickActionsAdapter.VH> {
        interface OnQuickActionClick { void onClick(String id); }
        private final List<OrgModels.QuickAction> data = new ArrayList<>();
        private final OnQuickActionClick listener;
        QuickActionsAdapter(OnQuickActionClick listener){ this.listener=listener; }
        void submit(List<OrgModels.QuickAction> list){
            data.clear();
            if(list!=null) data.addAll(list);
            notifyDataSetChanged();
        }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent,int viewType){
            View v=LayoutInflater.from(parent.getContext()).inflate(R.layout.item_quick_action,parent,false);
            return new VH(v);
        }
        @Override public void onBindViewHolder(@NonNull VH holder,int position){
            OrgModels.QuickAction action=data.get(position);
            holder.label.setText(action.label);
            holder.itemView.setOnClickListener(v->{
                if(listener!=null) listener.onClick(action.id);
            });
        }
        @Override public int getItemCount(){ return data.size(); }
        static class VH extends RecyclerView.ViewHolder{
            final TextView label;
            VH(@NonNull View itemView){
                super(itemView);
                label=itemView.findViewById(R.id.tvLabel);
            }
        }
    }

    private static class AlertsAdapter extends RecyclerView.Adapter<AlertsAdapter.VH>{
        private final List<OrgModels.Alert> data=new ArrayList<>();
        void submit(List<OrgModels.Alert> alerts){
            data.clear();
            if(alerts!=null) data.addAll(alerts);
            notifyDataSetChanged();
        }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent,int viewType){
            View v=LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alert_message,parent,false);
            return new VH(v);
        }
        @Override public void onBindViewHolder(@NonNull VH holder,int position){
            OrgModels.Alert alert=data.get(position);
            holder.title.setText(alert.title);
            holder.message.setText(alert.message);
        }
        @Override public int getItemCount(){ return data.size(); }
        static class VH extends RecyclerView.ViewHolder{
            final TextView title,message;
            VH(@NonNull View itemView){
                super(itemView);
                title=itemView.findViewById(R.id.tvAlertTitle);
                message=itemView.findViewById(R.id.tvAlertMessage);
            }
        }
    }

    @Override
    public void onDestroyView() {
        try { CoachMarkManager.dismissActive(); } catch (Throwable ignored) {}
        super.onDestroyView();
    }

}
