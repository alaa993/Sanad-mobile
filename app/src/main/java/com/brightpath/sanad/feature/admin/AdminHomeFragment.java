package com.brightpath.sanad.feature.admin;

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
import com.brightpath.sanad.feature.home.AppNavigator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.brightpath.sanad.R;
import com.brightpath.sanad.feature.home.AppNavigator;
import com.brightpath.sanad.ui.tour.CoachMarkManager;
import com.brightpath.sanad.ui.tour.CoachMarkStep;

import java.util.ArrayList;
import java.util.List;

public class AdminHomeFragment extends Fragment {

    private ProgressBar progress;
    private TextView errorView;
    private LinearLayout contentGroup;
    private TextView tvUsers, tvSpecs, tvOrgs, tvSessionsWeek, tvSpecPending, tvOrgPending, tvAppointmentsToday, tvLibraryPosts;
    private QuickAdapter quickAdapter;
    private AlertsAdapter alertsAdapter;
    private AdminViewModels.HomeVM vm;
    private String mode = "dashboard";
    private View headerCard;
    private TextView tvQuickLabel, tvAlertsLabel;
    private RecyclerView rvQuick;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() != null) {
            String argMode = getArguments().getString("mode", "dashboard");
            mode = argMode != null ? argMode : "dashboard";
        }
        headerCard = view.findViewById(R.id.adminHeaderCard);
        progress = view.findViewById(R.id.adminProgress);
        errorView = view.findViewById(R.id.adminError);
        contentGroup = view.findViewById(R.id.adminContent);
        tvUsers = view.findViewById(R.id.tvUsers);
        tvSpecs = view.findViewById(R.id.tvSpecs);
        tvOrgs = view.findViewById(R.id.tvOrgs);
        tvSessionsWeek = view.findViewById(R.id.tvSessionsWeek);
        tvSpecPending = view.findViewById(R.id.tvSpecPending);
        tvOrgPending = view.findViewById(R.id.tvOrgPending);
        tvAppointmentsToday = view.findViewById(R.id.tvAppointmentsToday);
        tvLibraryPosts = view.findViewById(R.id.tvLibraryPosts);
        tvQuickLabel = view.findViewById(R.id.tvAdminQuickActionsLabel);
        tvAlertsLabel = view.findViewById(R.id.tvAdminAlertsLabel);
        rvQuick = view.findViewById(R.id.rvAdminQuickActions);
        RecyclerView rvAlerts = view.findViewById(R.id.rvAdminAlerts);
        quickAdapter = new QuickAdapter(this::handleQuickAction);
        alertsAdapter = new AlertsAdapter(this::handleAlert);
        if ("shortcuts".equalsIgnoreCase(mode)) {
            rvQuick.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        } else {
            rvQuick.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false));
        }
        rvQuick.setAdapter(quickAdapter);
        rvAlerts.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvAlerts.setAdapter(alertsAdapter);

        Button btnSpecs = view.findViewById(R.id.btnAdminSpecs);
        Button btnOrgs = view.findViewById(R.id.btnAdminOrgs);
        btnSpecs.setOnClickListener(v -> AppNavigator.go(this, R.id.adminSpecialistsFragment));
        btnOrgs.setOnClickListener(v -> AppNavigator.go(this, R.id.adminOrganizationsFragment));

        vm = new ViewModelProvider(this).get(AdminViewModels.HomeVM.class);
        vm.state().observe(getViewLifecycleOwner(), this::renderState);
        vm.load();
        applyMode();

        view.post(() -> {
            java.util.List<CoachMarkStep> steps = new java.util.ArrayList<>();
            if (rvQuick != null) steps.add(CoachMarkManager.step(rvQuick, R.string.tour_admin_quick_title, R.string.tour_admin_quick_desc));
            if (tvUsers != null) steps.add(CoachMarkManager.step(tvUsers, R.string.tour_admin_users_title, R.string.tour_admin_users_desc));
            if (tvSessionsWeek != null) steps.add(CoachMarkManager.step(tvSessionsWeek, R.string.tour_admin_sessions_title, R.string.tour_admin_sessions_desc));
            CoachMarkManager.showIfNeeded(AdminHomeFragment.this, "tour_admin_home", steps);
        });
    }

    private void renderState(AdminViewModels.HomeVM.UiState state) {
        try {
            if (state == null) return;
            if (progress != null) progress.setVisibility(state.loading ? View.VISIBLE : View.GONE);
            if (errorView != null) {
                if (state.error != null) {
                    errorView.setText(state.error);
                    errorView.setVisibility(View.VISIBLE);
                } else {
                    errorView.setVisibility(View.GONE);
                }
            }
            boolean showContent = !state.loading && state.data != null;
            if (contentGroup != null) contentGroup.setVisibility(showContent ? View.VISIBLE : View.GONE);
            if (!showContent) return;

            AdminModels.Counters counters = state.data.counters;
            if (counters != null) {
                if (tvUsers != null) tvUsers.setText(String.valueOf(counters.users));
                if (tvSpecs != null) tvSpecs.setText(String.valueOf(counters.specialists));
                if (tvOrgs != null) tvOrgs.setText(String.valueOf(counters.organizations));
                if (tvSessionsWeek != null) tvSessionsWeek.setText(String.valueOf(counters.sessions_week));
                if (tvSpecPending != null) tvSpecPending.setText(String.valueOf(counters.specialists_pending));
                if (tvOrgPending != null) tvOrgPending.setText(String.valueOf(counters.organizations_pending));
                if (tvAppointmentsToday != null) tvAppointmentsToday.setText(String.valueOf(counters.appointments_today));
                if (tvLibraryPosts != null) tvLibraryPosts.setText(String.valueOf(counters.posts));
            }

            List<AdminModels.QuickAction> quickActions = mergeQuickActions(state.data.quick_actions);
            if (quickAdapter != null) quickAdapter.submit(quickActions);

            List<AdminModels.Alert> alerts = state.data.alerts;
            if (alertsAdapter != null) alertsAdapter.submit(alerts == null ? new ArrayList<>() : alerts);
            applyMode();
        } catch (Throwable ignored) {
            // Never crash the admin home on bad/partial payloads.
        }
    }

    private void applyMode() {
        boolean shortcutsOnly = "shortcuts".equalsIgnoreCase(mode);
        if (headerCard != null) headerCard.setVisibility(shortcutsOnly ? View.GONE : View.VISIBLE);
        if (shortcutsOnly && contentGroup != null) {
            for (int i = 0; i < contentGroup.getChildCount(); i++) {
                View child = contentGroup.getChildAt(i);
                if (child == null) continue;
                boolean isQuick = child.getId() == R.id.tvAdminQuickActionsLabel
                        || child.getId() == R.id.rvAdminQuickActions;
                child.setVisibility(isQuick ? View.VISIBLE : View.GONE);
            }
        } else if (contentGroup != null) {
            for (int i = 0; i < contentGroup.getChildCount(); i++) {
                View child = contentGroup.getChildAt(i);
                if (child != null) child.setVisibility(View.VISIBLE);
            }
        }
        if (shortcutsOnly && tvAlertsLabel != null) tvAlertsLabel.setVisibility(View.GONE);
        if (!shortcutsOnly && rvQuick != null) {
            rvQuick.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false));
        }
    }

    private List<AdminModels.QuickAction> mergeQuickActions(List<AdminModels.QuickAction> fromApi) {
        List<AdminModels.QuickAction> defaults = defaultQuickActions();
        if (fromApi == null || fromApi.isEmpty()) {
            return defaults;
        }
        java.util.Set<String> seen = new java.util.HashSet<>();
        List<AdminModels.QuickAction> merged = new ArrayList<>();
        for (AdminModels.QuickAction action : fromApi) {
            if (action == null || action.id == null) continue;
            seen.add(action.id);
            AdminModels.QuickAction localized = findQuickAction(action.id, defaults);
            merged.add(localized != null ? localized : action);
        }
        for (AdminModels.QuickAction action : defaults) {
            if (!seen.contains(action.id)) {
                merged.add(action);
            }
        }
        return merged;
    }

    private AdminModels.QuickAction findQuickAction(String id, List<AdminModels.QuickAction> defaults) {
        for (AdminModels.QuickAction action : defaults) {
            if (id.equals(action.id)) return action;
        }
        return null;
    }

    private void handleAlert(String alertId) {
        if (alertId == null) return;
        switch (alertId) {
            case "pending_specialists":
                AppNavigator.go(this, R.id.adminSpecialistsFragment);
                break;
            case "pending_orgs":
                AppNavigator.go(this, R.id.adminOrganizationsFragment);
                break;
            default:
                break;
        }
    }

    private List<AdminModels.QuickAction> defaultQuickActions() {
        List<AdminModels.QuickAction> fallback = new ArrayList<>();
        fallback.add(createAction("approve_specialists", getString(R.string.admin_specialists_title)));
        fallback.add(createAction("approve_orgs", getString(R.string.admin_orgs_title)));
        fallback.add(createAction("sessions", getString(R.string.admin_sessions_view)));
        fallback.add(createAction("users", getString(R.string.admin_users_title)));
        fallback.add(createAction("library", getString(R.string.nav_library)));
        fallback.add(createAction("wallet", getString(R.string.admin_wallet_title)));
        fallback.add(createAction("reports", getString(R.string.org_reports_title)));
        fallback.add(createAction("community", getString(R.string.nav_groups)));
        fallback.add(createAction("vent", getString(R.string.admin_dashboard_action_vent)));
        fallback.add(createAction("daily_tips", getString(R.string.admin_dashboard_action_daily_tips)));
        fallback.add(createAction("settings", getString(R.string.admin_profile_title)));
        return fallback;
    }

    private AdminModels.QuickAction createAction(String id, String label) {
        AdminModels.QuickAction qa = new AdminModels.QuickAction();
        qa.id = id;
        qa.label = label;
        return qa;
    }

    private void handleQuickAction(String id) {
        if (id == null) return;
        switch (id) {
            case "approve_specialists":
                AppNavigator.go(this, R.id.adminSpecialistsFragment);
                break;
            case "approve_orgs":
                AppNavigator.go(this, R.id.adminOrganizationsFragment);
                break;
            case "sessions":
                AppNavigator.go(this, R.id.adminSessionsFragment);
                break;
            case "users":
                AppNavigator.go(this, R.id.adminUsersFragment);
                break;
            case "library":
                AppNavigator.go(this, R.id.adminLibraryFragment);
                break;
            case "reports":
                AppNavigator.go(this, R.id.reportsFragment);
                break;
            case "community":
                AppNavigator.go(this, R.id.communityListFragment);
                break;
            case "wallet":
                AppNavigator.go(this, R.id.adminWalletFragment);
                break;
            case "vent":
                AppNavigator.go(this, R.id.adminVentFragment);
                break;
            case "daily_tips":
                AppNavigator.go(this, R.id.adminDailyTipsFragment);
                break;
            case "settings":
                AppNavigator.go(this, R.id.adminProfileFragment);
                break;
            default:
                Toast.makeText(requireContext(), R.string.shortcut_not_supported, Toast.LENGTH_SHORT).show();
        }
    }

    private static class QuickAdapter extends RecyclerView.Adapter<QuickAdapter.VH> {
        interface OnQuickClick { void onClick(String id); }
        private final List<AdminModels.QuickAction> data = new ArrayList<>();
        private final OnQuickClick listener;
        QuickAdapter(OnQuickClick l){ listener = l; }
        void submit(List<AdminModels.QuickAction> list){
            data.clear();
            if(list!=null) data.addAll(list);
            notifyDataSetChanged();
        }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent,int viewType){
            View v=LayoutInflater.from(parent.getContext()).inflate(R.layout.item_quick_action,parent,false);
            return new VH(v);
        }
        @Override public void onBindViewHolder(@NonNull VH holder,int position){
            AdminModels.QuickAction action=data.get(position);
            holder.label.setText(action.label);
            if (holder.icon != null) {
                holder.icon.setImageResource(iconFor(action.id));
            }
            holder.itemView.setOnClickListener(v->{
                if(listener!=null) listener.onClick(action.id);
            });
        }
        @Override public int getItemCount(){ return data.size(); }
        private int iconFor(String id) {
            if (id == null) return R.drawable.ic_home;
            switch (id) {
                case "approve_specialists":
                    return R.drawable.ic_approve_specialist;
                case "approve_orgs":
                    return R.drawable.ic_approve_org;
                case "sessions":
                    return R.drawable.ic_sessions;
                case "users":
                    return R.drawable.ic_patients;
                case "library":
                    return R.drawable.ic_library;
                case "reports":
                    return R.drawable.ic_reports;
                case "wallet":
                    return R.drawable.ic_wallet;
                case "community":
                    return R.drawable.ic_community;
                case "vent":
                    return R.drawable.ic_vent;
                case "daily_tips":
                    return R.drawable.ic_daily_tips;
                case "settings":
                    return R.drawable.ic_settings;
                default:
                    return R.drawable.ic_placeholder;
            }
        }
        static class VH extends RecyclerView.ViewHolder{
            final TextView label;
            final android.widget.ImageView icon;
            VH(@NonNull View itemView){
                super(itemView);
                label = itemView.findViewById(R.id.tvLabel);
                icon = itemView.findViewById(R.id.ivIcon);
            }
        }
    }

    private static class AlertsAdapter extends RecyclerView.Adapter<AlertsAdapter.VH>{
        interface OnAlertClick { void onClick(String alertId); }
        private final List<AdminModels.Alert> data=new ArrayList<>();
        private final OnAlertClick listener;
        AlertsAdapter(OnAlertClick listener){ this.listener = listener; }
        void submit(List<AdminModels.Alert> alerts){
            data.clear();
            if(alerts!=null) data.addAll(alerts);
            notifyDataSetChanged();
        }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent,int viewType){
            View v=LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alert_message,parent,false);
            return new VH(v);
        }
        @Override public void onBindViewHolder(@NonNull VH holder,int position){
            AdminModels.Alert alert=data.get(position);
            holder.title.setText(alert.title);
            holder.message.setText(alert.message);
            holder.itemView.setOnClickListener(v -> {
                if (listener != null && alert.id != null) listener.onClick(alert.id);
            });
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
