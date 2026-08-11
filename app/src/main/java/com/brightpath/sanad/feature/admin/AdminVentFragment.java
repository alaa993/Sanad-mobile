package com.brightpath.sanad.feature.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.brightpath.sanad.R;

import java.util.ArrayList;
import java.util.List;

public class AdminVentFragment extends Fragment {
    private AdminRepository repo;
    private ProgressBar progress;
    private TextView tvEmpty;
    private ReportsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_vent, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repo = new AdminRepository(requireContext());
        progress = view.findViewById(R.id.progress);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        RecyclerView rv = view.findViewById(R.id.rvReports);
        adapter = new ReportsAdapter(this::hidePost);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);
        load();
    }

    private void load() {
        if (progress != null) progress.setVisibility(View.VISIBLE);
        repo.ventReports(new AdminRepository.Cb<AdminModels.VentReports>() {
            @Override public void ok(AdminModels.VentReports data) {
                if (!isAdded()) return;
                if (progress != null) progress.setVisibility(View.GONE);
                List<AdminModels.VentReport> rows = data != null && data.data != null ? data.data : new ArrayList<>();
                adapter.submit(rows);
                if (tvEmpty != null) tvEmpty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
            }
            @Override public void err(Throwable e) {
                if (!isAdded()) return;
                if (progress != null) progress.setVisibility(View.GONE);
                Toast.makeText(requireContext(), R.string.login_failure_message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void hidePost(AdminModels.VentReport report) {
        if (report == null || report.post == null) return;
        repo.hideVentPost(report.post.id, new AdminRepository.Cb<AdminModels.Toggle>() {
            @Override public void ok(AdminModels.Toggle data) { load(); }
            @Override public void err(Throwable e) {
                if (isAdded()) Toast.makeText(requireContext(), R.string.paywall_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static class ReportsAdapter extends RecyclerView.Adapter<ReportsAdapter.VH> {
        interface Listener { void onHide(AdminModels.VentReport report); }
        private final List<AdminModels.VentReport> data = new ArrayList<>();
        private final Listener listener;
        ReportsAdapter(Listener l) { listener = l; }
        void submit(List<AdminModels.VentReport> rows) {
            data.clear();
            if (rows != null) data.addAll(rows);
            notifyDataSetChanged();
        }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_vent_report, parent, false);
            return new VH(v);
        }
        @Override public void onBindViewHolder(@NonNull VH holder, int position) {
            AdminModels.VentReport r = data.get(position);
            String alias = r.post != null && r.post.alias != null ? r.post.alias : "#" + (r.post != null ? r.post.id : r.id);
            holder.alias.setText(alias);
            holder.body.setText(r.post != null && r.post.body != null ? r.post.body : "");
            holder.reason.setText(r.reason != null ? r.reason : "");
            holder.hide.setOnClickListener(v -> { if (listener != null) listener.onHide(r); });
        }
        @Override public int getItemCount() { return data.size(); }
        static class VH extends RecyclerView.ViewHolder {
            final TextView alias, body, reason;
            final View hide;
            VH(@NonNull View itemView) {
                super(itemView);
                alias = itemView.findViewById(R.id.tvAlias);
                body = itemView.findViewById(R.id.tvBody);
                reason = itemView.findViewById(R.id.tvReason);
                hide = itemView.findViewById(R.id.btnHide);
            }
        }
    }
}
