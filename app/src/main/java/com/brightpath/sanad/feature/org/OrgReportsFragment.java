package com.brightpath.sanad.feature.org;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.brightpath.sanad.R;

import java.util.ArrayList;
import java.util.List;

public class OrgReportsFragment extends Fragment {
    private OrgViewModels.ReportsVM vm;
    private View progress;
    private View errorGroup;
    private TextView tvError;
    private View content;
    private TextView tvPeriod;
    private TextView tvTotalBeneficiaries;
    private TextView tvActiveBeneficiaries;
    private TextView tvHighRisk;
    private TextView tvCompleted;
    private TextView tvCancelled;
    private TextView tvUpcoming;
    private TextView tvEngagement;
    private BeneficiariesAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_org_reports, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        progress = view.findViewById(R.id.progress);
        errorGroup = view.findViewById(R.id.errorGroup);
        tvError = view.findViewById(R.id.tvError);
        content = view.findViewById(R.id.contentGroup);
        tvPeriod = view.findViewById(R.id.tvPeriod);
        tvTotalBeneficiaries = view.findViewById(R.id.tvBeneficiariesTotal);
        tvActiveBeneficiaries = view.findViewById(R.id.tvBeneficiariesActive);
        tvHighRisk = view.findViewById(R.id.tvHighRisk);
        tvCompleted = view.findViewById(R.id.tvSessionsCompleted);
        tvCancelled = view.findViewById(R.id.tvSessionsCancelled);
        tvUpcoming = view.findViewById(R.id.tvSessionsUpcoming);
        tvEngagement = view.findViewById(R.id.tvEngagement);
        RecyclerView rv = view.findViewById(R.id.rvBeneficiaries);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new BeneficiariesAdapter();
        rv.setAdapter(adapter);

        MaterialButton btnRetry = view.findViewById(R.id.btnRetry);
        btnRetry.setOnClickListener(v -> vm.load());

        vm = new ViewModelProvider(this).get(OrgViewModels.ReportsVM.class);
        vm.state().observe(getViewLifecycleOwner(), this::render);
        vm.load();
    }

    private void render(OrgViewModels.ReportsVM.UiState state) {
        progress.setVisibility(state.loading ? View.VISIBLE : View.GONE);
        if (state.error != null) {
            errorGroup.setVisibility(View.VISIBLE);
            tvError.setText(state.error);
            content.setVisibility(View.GONE);
            return;
        } else {
            errorGroup.setVisibility(View.GONE);
        }
        if (state.data == null) {
            content.setVisibility(View.GONE);
            return;
        }
        content.setVisibility(View.VISIBLE);
        if (state.data.period != null) {
            tvPeriod.setText(getString(R.string.org_reports_period,
                    safe(state.data.period.from), safe(state.data.period.to)));
        }
        OrgModels.ReportSummary.Metrics m = state.data.metrics;
        if (m != null) {
            tvTotalBeneficiaries.setText(String.valueOf(m.beneficiaries_total));
            tvActiveBeneficiaries.setText(String.valueOf(m.beneficiaries_active));
            tvHighRisk.setText(String.valueOf(m.high_risk_cases));
            tvCompleted.setText(String.valueOf(m.sessions_completed));
            tvCancelled.setText(String.valueOf(m.sessions_cancelled));
            tvUpcoming.setText(String.valueOf(m.sessions_upcoming_week));
            tvEngagement.setText(String.format("%s%%", m.engagement_rate));
        }
        adapter.submit(state.data.top_beneficiaries);
    }

    private String safe(String text) {
        return text == null ? "--" : text;
    }

    private static class BeneficiariesAdapter extends RecyclerView.Adapter<BeneficiariesAdapter.VH> {
        private final List<OrgModels.ReportSummary.TopBeneficiary> data = new ArrayList<>();
        void submit(List<OrgModels.ReportSummary.TopBeneficiary> list) {
            data.clear();
            if (list != null) data.addAll(list);
            notifyDataSetChanged();
        }
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_org_report_beneficiary, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            OrgModels.ReportSummary.TopBeneficiary item = data.get(position);
            holder.name.setText(item.name);
            holder.risk.setText(item.risk_level == null ? "--" : item.risk_level);
            holder.issue.setText(item.primary_issue == null ? holder.issue.getContext().getString(R.string.org_reports_issue_unknown) : item.primary_issue);
            holder.lastSession.setText(item.last_session_at == null ? holder.lastSession.getContext().getString(R.string.org_reports_no_sessions) : item.last_session_at);
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            final TextView name;
            final TextView risk;
            final TextView issue;
            final TextView lastSession;
            VH(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.tvName);
                risk = itemView.findViewById(R.id.tvRisk);
                issue = itemView.findViewById(R.id.tvIssue);
                lastSession = itemView.findViewById(R.id.tvLastSession);
            }
        }
    }
}
