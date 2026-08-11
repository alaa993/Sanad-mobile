package com.brightpath.sanad.feature.specialist;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.brightpath.sanad.R;
import com.brightpath.sanad.feature.home.AppNavigator;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class SpecialistPatientsFragment extends Fragment {

    private final List<SpecialistModels.PatientMini> patients = new ArrayList<>();
    private ProgressBar progress;
    private TextView tvError, tvEmpty, tvPatientCount;
    private SwipeRefreshLayout swipeRefresh;
    private TextInputEditText etSearch;
    private SpecialistRepository repo;
    private Adapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_specialist_patients, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        progress = v.findViewById(R.id.progress);
        tvError = v.findViewById(R.id.tvError);
        tvEmpty = v.findViewById(R.id.tvEmpty);
        tvPatientCount = v.findViewById(R.id.tvPatientCount);
        swipeRefresh = v.findViewById(R.id.swipeRefresh);
        etSearch = v.findViewById(R.id.etSearch);
        RecyclerView rv = v.findViewById(R.id.rvPatients);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new Adapter();
        rv.setAdapter(adapter);
        repo = new SpecialistRepository(requireContext());

        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(() -> load(adapter, false));
        }
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {
                    filter(s != null ? s.toString() : null);
                }
            });
        }

        load(adapter, true);
    }

    private void load(Adapter adapter, boolean showProgress) {
        if (showProgress && progress != null) progress.setVisibility(View.VISIBLE);
        if (tvError != null) tvError.setVisibility(View.GONE);
        repo.patients(new SpecialistRepository.Cb<SpecialistModels.Patients>() {
            @Override
            public void ok(SpecialistModels.Patients t) {
                if (!isAdded()) return;
                if (progress != null) progress.setVisibility(View.GONE);
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                patients.clear();
                if (t != null && t.data != null) patients.addAll(t.data);
                filter(etSearch != null && etSearch.getText() != null ? etSearch.getText().toString() : null);
            }

            @Override
            public void err(Throwable e) {
                if (!isAdded()) return;
                if (progress != null) progress.setVisibility(View.GONE);
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                if (tvError != null) {
                    tvError.setVisibility(View.VISIBLE);
                    tvError.setText(R.string.error_load_failed);
                }
                updateSummary(0);
                if (tvEmpty != null) tvEmpty.setVisibility(View.GONE);
                adapter.submit(new ArrayList<>());
            }
        });
    }

    private void filter(@Nullable String query) {
        List<SpecialistModels.PatientMini> filtered = new ArrayList<>();
        if (TextUtils.isEmpty(query)) {
            filtered.addAll(patients);
        } else {
            String q = query.trim().toLowerCase();
            for (SpecialistModels.PatientMini p : patients) {
                if (p == null) continue;
                String name = p.name != null ? p.name.toLowerCase() : "";
                if (name.contains(q) || String.valueOf(p.id).contains(q)) {
                    filtered.add(p);
                }
            }
        }
        if (adapter != null) adapter.submit(filtered);
        updateSummary(filtered.size());
        if (tvEmpty != null) {
            boolean showEmpty = filtered.isEmpty() && (tvError == null || tvError.getVisibility() != View.VISIBLE);
            tvEmpty.setVisibility(showEmpty ? View.VISIBLE : View.GONE);
            if (showEmpty && !TextUtils.isEmpty(query)) {
                tvEmpty.setText(R.string.specialist_patients_search_empty);
            } else {
                tvEmpty.setText(R.string.specialist_patients_empty);
            }
        }
    }

    private void updateSummary(int count) {
        if (tvPatientCount != null) {
            tvPatientCount.setText(getString(R.string.specialist_patients_count_format, count));
        }
    }

    private void openPatient(int patientId) {
        Bundle args = new Bundle();
        args.putInt("patientId", patientId);
        args.putInt("sessionId", -1);
        AppNavigator.go(this, R.id.specialistPatientFileFragment, args);
    }

    private class Adapter extends RecyclerView.Adapter<Adapter.VH> {
        private final List<SpecialistModels.PatientMini> data = new ArrayList<>();

        void submit(List<SpecialistModels.PatientMini> list) {
            data.clear();
            if (list != null) data.addAll(list);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View row = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_specialist_patient, parent, false);
            return new VH(row);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            SpecialistModels.PatientMini p = data.get(position);
            holder.name.setText(p.name != null ? p.name : ("#" + p.id));
            if (!TextUtils.isEmpty(p.avatar)) {
                Glide.with(holder.avatar)
                        .load(p.avatar)
                        .placeholder(R.drawable.ic_specialists)
                        .error(R.drawable.ic_specialists)
                        .circleCrop()
                        .into(holder.avatar);
            } else {
                holder.avatar.setImageResource(R.drawable.ic_specialists);
            }
            holder.itemView.setOnClickListener(v -> openPatient(p.id));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        class VH extends RecyclerView.ViewHolder {
            final TextView name;
            final ImageView avatar;

            VH(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.tvPatientName);
                avatar = itemView.findViewById(R.id.imgAvatar);
            }
        }
    }
}
