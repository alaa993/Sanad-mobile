package com.brightpath.sanad.feature.groups;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.brightpath.sanad.R;
import com.brightpath.sanad.feature.specialist.SpecialistModels;
import com.brightpath.sanad.feature.specialist.SpecialistRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GroupSelectPatientsFragment extends Fragment {
    public static final String RESULT_KEY = "groupPatientsPicker";
    public static final String RESULT_IDS = "patientIds";

    private SpecialistRepository repo;
    private ProgressBar progress;
    private TextView empty;
    private PatientsAdapter adapter;
    private final List<SpecialistModels.PatientMini> all = new ArrayList<>();
    private final Set<Integer> selected = new HashSet<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_group_select_patients, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        repo = new SpecialistRepository(requireContext());
        progress = v.findViewById(R.id.progress);
        empty = v.findViewById(R.id.tvEmpty);

        RecyclerView rv = v.findViewById(R.id.recycler);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new PatientsAdapter(selected, this::toggleSelection);
        rv.setAdapter(adapter);

        EditText search = v.findViewById(R.id.etSearch);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { filter(s.toString()); }
        });

        MaterialButton btnConfirm = v.findViewById(R.id.btnConfirm);
        btnConfirm.setOnClickListener(x -> submit());

        load();
    }

    private void load() {
        if (progress != null) progress.setVisibility(View.VISIBLE);
        repo.patients(new SpecialistRepository.Cb<SpecialistModels.Patients>() {
            @Override public void ok(SpecialistModels.Patients t) {
                if (!isAdded()) return;
                if (progress != null) progress.setVisibility(View.GONE);
                all.clear();
                if (t != null && t.data != null) all.addAll(t.data);
                filter(null);
            }
            @Override public void err(Throwable e) {
                if (!isAdded()) return;
                if (progress != null) progress.setVisibility(View.GONE);
                all.clear();
                adapter.submit(null);
                if (empty != null) empty.setVisibility(View.VISIBLE);
            }
        });
    }

    private void filter(@Nullable String query) {
        List<SpecialistModels.PatientMini> filtered = new ArrayList<>();
        if (TextUtils.isEmpty(query)) {
            filtered.addAll(all);
        } else {
            String q = query.toLowerCase();
            for (SpecialistModels.PatientMini p : all) {
                if (p == null || TextUtils.isEmpty(p.name)) continue;
                if (p.name.toLowerCase().contains(q)) {
                    filtered.add(p);
                }
            }
        }
        adapter.submit(filtered);
        boolean isEmpty = filtered.isEmpty();
        if (empty != null) empty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    private void toggleSelection(SpecialistModels.PatientMini patient) {
        if (patient == null) return;
        if (selected.contains(patient.id)) {
            selected.remove(patient.id);
        } else {
            selected.add(patient.id);
        }
        adapter.notifyDataSetChanged();
    }

    private void submit() {
        ArrayList<Integer> ids = new ArrayList<>(selected);
        Bundle res = new Bundle();
        res.putIntegerArrayList(RESULT_IDS, ids);
        getParentFragmentManager().setFragmentResult(RESULT_KEY, res);
        NavHostFragment.findNavController(this).popBackStack();
    }

    static class PatientsAdapter extends RecyclerView.Adapter<PatientsAdapter.VH> {
        interface Toggle { void onToggle(SpecialistModels.PatientMini patient); }
        private final Set<Integer> selected;
        private final Toggle toggle;
        private List<SpecialistModels.PatientMini> data = new ArrayList<>();

        PatientsAdapter(Set<Integer> selected, Toggle toggle) {
            this.selected = selected;
            this.toggle = toggle;
        }

        void submit(List<SpecialistModels.PatientMini> list) {
            data = list != null ? list : new ArrayList<>();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_patient_select, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            SpecialistModels.PatientMini p = data.get(position);
            h.title.setText(p != null ? p.name : "");
            if (p != null && !TextUtils.isEmpty(p.avatar)) {
                Glide.with(h.avatar)
                        .load(p.avatar)
                        .placeholder(R.drawable.ic_specialists)
                        .circleCrop()
                        .into(h.avatar);
            } else {
                h.avatar.setImageResource(R.drawable.ic_specialists);
            }
            boolean isChecked = p != null && selected.contains(p.id);
            h.check.setOnCheckedChangeListener(null);
            h.check.setChecked(isChecked);
            h.check.setOnCheckedChangeListener((buttonView, checked) -> {
                if (toggle != null) toggle.onToggle(p);
            });
            h.itemView.setOnClickListener(v -> {
                if (toggle != null) toggle.onToggle(p);
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            final TextView title;
            final ImageView avatar;
            final MaterialCheckBox check;

            VH(@NonNull View v) {
                super(v);
                title = v.findViewById(R.id.tvTitle);
                avatar = v.findViewById(R.id.imgAvatar);
                check = v.findViewById(R.id.cbSelect);
            }
        }
    }
}
