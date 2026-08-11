package com.brightpath.sanad.feature.admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.brightpath.sanad.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminDailyTipsFragment extends Fragment {
    private AdminRepository repo;
    private ProgressBar progress;
    private TextView tvEmpty;
    private TipsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_daily_tips, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repo = new AdminRepository(requireContext());
        progress = view.findViewById(R.id.progress);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        MaterialButton btnAdd = view.findViewById(R.id.btnAdd);
        RecyclerView rv = view.findViewById(R.id.rvTips);
        adapter = new TipsAdapter(this::editTip, this::deleteTip);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);
        btnAdd.setOnClickListener(v -> showAddDialog());
        load();
    }

    private void load() {
        if (progress != null) progress.setVisibility(View.VISIBLE);
        repo.dailyTips(new AdminRepository.Cb<AdminModels.DailyTips>() {
            @Override public void ok(AdminModels.DailyTips data) {
                if (!isAdded()) return;
                if (progress != null) progress.setVisibility(View.GONE);
                List<AdminModels.DailyTip> rows = data != null && data.data != null ? data.data : new ArrayList<>();
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

    private void editTip(AdminModels.DailyTip tip) {
        if (tip == null) return;
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_admin_daily_tip, null, false);
        EditText etDate = dialogView.findViewById(R.id.etDate);
        EditText etTitle = dialogView.findViewById(R.id.etTitle);
        EditText etBody = dialogView.findViewById(R.id.etBody);
        if (tip.tip_date != null) etDate.setText(tip.tip_date);
        if (tip.title != null && tip.title.get("ar") != null) etTitle.setText(String.valueOf(tip.title.get("ar")));
        if (tip.body != null && tip.body.get("ar") != null) etBody.setText(String.valueOf(tip.body.get("ar")));
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.admin_daily_tip_edit)
                .setView(dialogView)
                .setPositiveButton(R.string.save, (d, w) -> submitTip(tip.id, etDate, etTitle, etBody, true))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showAddDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_admin_daily_tip, null, false);
        EditText etDate = dialogView.findViewById(R.id.etDate);
        EditText etTitle = dialogView.findViewById(R.id.etTitle);
        EditText etBody = dialogView.findViewById(R.id.etBody);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.admin_daily_tip_add)
                .setView(dialogView)
                .setPositiveButton(R.string.save, (d, w) -> submitTip(0, etDate, etTitle, etBody, false))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void submitTip(int id, EditText etDate, EditText etTitle, EditText etBody, boolean editing) {
        String date = text(etDate);
        String title = text(etTitle);
        if (date.isEmpty() || title.isEmpty()) {
            Toast.makeText(requireContext(), R.string.login_error_required_fields, Toast.LENGTH_SHORT).show();
            return;
        }
        Map<String, Object> body = new HashMap<>();
        body.put("tip_date", date);
        Map<String, String> titleMap = new HashMap<>();
        titleMap.put("ar", title);
        body.put("title", titleMap);
        String bodyAr = text(etBody);
        if (!bodyAr.isEmpty()) {
            Map<String, String> bodyMap = new HashMap<>();
            bodyMap.put("ar", bodyAr);
            body.put("body", bodyMap);
        }
        body.put("active", true);
        AdminRepository.Cb<AdminModels.DailyTip> cb = new AdminRepository.Cb<AdminModels.DailyTip>() {
            @Override public void ok(AdminModels.DailyTip data) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), R.string.admin_daily_tip_saved, Toast.LENGTH_SHORT).show();
                    load();
                }
            }
            @Override public void err(Throwable e) {
                if (isAdded()) Toast.makeText(requireContext(), R.string.paywall_error, Toast.LENGTH_SHORT).show();
            }
        };
        if (editing) repo.updateDailyTip(id, body, cb);
        else repo.createDailyTip(body, cb);
    }

    private void deleteTip(AdminModels.DailyTip tip) {
        if (tip == null) return;
        repo.deleteDailyTip(tip.id, new AdminRepository.Cb<AdminModels.Toggle>() {
            @Override public void ok(AdminModels.Toggle data) { load(); }
            @Override public void err(Throwable e) {
                if (isAdded()) Toast.makeText(requireContext(), R.string.paywall_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static String text(EditText et) {
        return et != null && et.getText() != null ? et.getText().toString().trim() : "";
    }

    private static class TipsAdapter extends RecyclerView.Adapter<TipsAdapter.VH> {
        interface Listener { void onDelete(AdminModels.DailyTip tip); }
        interface EditListener { void onEdit(AdminModels.DailyTip tip); }
        private final List<AdminModels.DailyTip> data = new ArrayList<>();
        private final Listener listener;
        private final EditListener editListener;
        TipsAdapter(EditListener edit, Listener l) { editListener = edit; listener = l; }
        void submit(List<AdminModels.DailyTip> rows) {
            data.clear();
            if (rows != null) data.addAll(rows);
            notifyDataSetChanged();
        }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_daily_tip, parent, false);
            return new VH(v);
        }
        @Override public void onBindViewHolder(@NonNull VH holder, int position) {
            AdminModels.DailyTip tip = data.get(position);
            holder.date.setText(tip.tip_date != null ? tip.tip_date : "");
            Object titleObj = tip.title != null ? tip.title.get("ar") : null;
            if (titleObj == null && tip.title != null) {
                titleObj = tip.title.get(Locale.getDefault().getLanguage());
            }
            String title = titleObj != null ? String.valueOf(titleObj) : "";
            holder.title.setText(title);
            holder.itemView.setOnClickListener(v -> { if (editListener != null) editListener.onEdit(tip); });
            holder.delete.setOnClickListener(v -> { if (listener != null) listener.onDelete(tip); });
        }
        @Override public int getItemCount() { return data.size(); }
        static class VH extends RecyclerView.ViewHolder {
            final TextView date, title;
            final MaterialButton delete;
            VH(@NonNull View itemView) {
                super(itemView);
                date = itemView.findViewById(R.id.tvDate);
                title = itemView.findViewById(R.id.tvTitle);
                delete = itemView.findViewById(R.id.btnDelete);
            }
        }
    }
}
