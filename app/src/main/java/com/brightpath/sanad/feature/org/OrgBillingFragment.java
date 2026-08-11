package com.brightpath.sanad.feature.org;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textview.MaterialTextView;
import com.brightpath.sanad.R;

import java.util.ArrayList;
import java.util.List;

public class OrgBillingFragment extends Fragment {
    private OrgViewModels.BillingVM vm;
    private ProgressBar progress;
    private View errorGroup;
    private TextView tvError;
    private View content;
    private TextView tvPlanName, tvPlanStatus, tvRenewsAt;
    private MaterialTextView tvSeatUsage, tvSessionUsage;
    private LinearProgressIndicator seatProgress, sessionsProgress;
    private TextView tvWalletBalance;
    private InvoicesAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_org_billing, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        progress = view.findViewById(R.id.progress);
        errorGroup = view.findViewById(R.id.errorGroup);
        tvError = view.findViewById(R.id.tvError);
        content = view.findViewById(R.id.contentGroup);
        tvPlanName = view.findViewById(R.id.tvPlanName);
        tvPlanStatus = view.findViewById(R.id.tvPlanStatus);
        tvRenewsAt = view.findViewById(R.id.tvRenewsAt);
        tvSeatUsage = view.findViewById(R.id.tvSeatUsage);
        tvSessionUsage = view.findViewById(R.id.tvSessionUsage);
        seatProgress = view.findViewById(R.id.progressSeats);
        sessionsProgress = view.findViewById(R.id.progressSessions);
        tvWalletBalance = view.findViewById(R.id.tvWalletBalance);
        RecyclerView rv = view.findViewById(R.id.rvInvoices);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new InvoicesAdapter();
        rv.setAdapter(adapter);
        view.findViewById(R.id.btnRetry).setOnClickListener(v -> vm.load());
        vm = new ViewModelProvider(this).get(OrgViewModels.BillingVM.class);
        vm.state().observe(getViewLifecycleOwner(), this::render);
        vm.load();
    }

    private void render(OrgViewModels.BillingVM.UiState state) {
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
        OrgModels.BillingOverview.Plan plan = state.data.plan;
        if (plan != null) {
            tvPlanName.setText(plan.name);
            tvPlanStatus.setText(plan.status);
            tvRenewsAt.setText(getString(R.string.org_billing_renews_at, plan.renews_at));
        }
        OrgModels.BillingOverview.SeatUsage seats = state.data.seats;
        if (seats != null) {
            tvSeatUsage.setText(getString(R.string.org_billing_usage_pattern, seats.used, seats.limit));
            seatProgress.setMax(Math.max(seats.limit, 1));
            seatProgress.setProgress(Math.min(seats.used, seats.limit));
        }
        OrgModels.BillingOverview.SessionUsage sessions = state.data.sessions;
        if (sessions != null) {
            tvSessionUsage.setText(getString(R.string.org_billing_usage_pattern, sessions.used, sessions.limit));
            sessionsProgress.setMax(Math.max(sessions.limit, 1));
            sessionsProgress.setProgress(Math.min(sessions.used, sessions.limit));
        }
        OrgModels.BillingOverview.Wallet wallet = state.data.wallet;
        if (wallet != null) {
            int pts = wallet.points > 0 ? wallet.points : wallet.balance;
            tvWalletBalance.setText(getString(R.string.org_billing_wallet_balance, pts,
                    wallet.currency == null ? "PTS" : wallet.currency));
        }
        adapter.submit(state.data.invoices);
    }

    private static class InvoicesAdapter extends RecyclerView.Adapter<InvoicesAdapter.VH> {
        private final List<OrgModels.BillingOverview.Invoice> data = new ArrayList<>();
        void submit(List<OrgModels.BillingOverview.Invoice> list) {
            data.clear();
            if (list != null) data.addAll(list);
            notifyDataSetChanged();
        }
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_org_invoice, parent, false);
            return new VH(v);
        }
        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            OrgModels.BillingOverview.Invoice invoice = data.get(position);
            holder.title.setText(holder.title.getContext().getString(R.string.org_billing_invoice_label, invoice.id));
            holder.amount.setText(holder.amount.getContext().getString(R.string.org_billing_amount_format, invoice.total, invoice.currency == null ? "USD" : invoice.currency));
            holder.status.setText(invoice.status == null ? "--" : invoice.status);
            holder.date.setText(invoice.created_at == null ? "--" : invoice.created_at);
        }
        @Override
        public int getItemCount() {
            return data.size();
        }
        static class VH extends RecyclerView.ViewHolder {
            final TextView title;
            final TextView amount;
            final TextView status;
            final TextView date;
            VH(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.tvInvoiceTitle);
                amount = itemView.findViewById(R.id.tvInvoiceAmount);
                status = itemView.findViewById(R.id.tvInvoiceStatus);
                date = itemView.findViewById(R.id.tvInvoiceDate);
            }
        }
    }
}
