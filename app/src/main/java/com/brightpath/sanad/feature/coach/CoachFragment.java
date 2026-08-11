package com.brightpath.sanad.feature.coach;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.*;
import com.brightpath.sanad.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.*;

public class CoachFragment extends Fragment {
    private static final String[] CATEGORY_KEYS = {"vitamins", "weight", "general"};
    private static final int[] CATEGORY_LABELS = {
            R.string.coach_cat_vitamins,
            R.string.coach_cat_weight,
            R.string.coach_cat_general
    };

    private CoachRepository repo;
    private Adapter adapter;
    private View progress, empty;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_coach, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        if (!com.brightpath.sanad.ui.PatientOnlyGuard.allowOrLeave(this)) return;
        repo = new CoachRepository(requireContext());
        progress = v.findViewById(R.id.progress);
        empty = v.findViewById(R.id.tvEmpty);
        RecyclerView rv = v.findViewById(R.id.rvPrograms);
        MaterialToolbar toolbar = v.findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitle(R.string.coach_title);
            toolbar.setNavigationOnClickListener(x -> NavHostFragment.findNavController(this).popBackStack());
        }
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new Adapter(program -> {
            Bundle args = new Bundle();
            args.putInt("programId", program.id);
            NavHostFragment.findNavController(this).navigate(R.id.coachDetailFragment, args);
        });
        rv.setAdapter(adapter);
        v.findViewById(R.id.btnCreate).setOnClickListener(x -> showCreateDialog());
        load();
    }

    private void load() {
        progress.setVisibility(View.VISIBLE);
        repo.list(new CoachRepository.Cb<CoachModels.ProgramListResponse>() {
            @Override public void ok(CoachModels.ProgramListResponse res) {
                if (!isAdded()) return;
                progress.setVisibility(View.GONE);
                List<CoachModels.ProgramSummary> data = res != null && res.data != null ? res.data : new ArrayList<>();
                adapter.submit(data);
                empty.setVisibility(data.isEmpty() ? View.VISIBLE : View.GONE);
            }
            @Override public void err(Throwable e) {
                if (!isAdded()) return;
                progress.setVisibility(View.GONE);
                Toast.makeText(requireContext(), R.string.coach_load_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCreateDialog() {
        String[] labels = new String[CATEGORY_LABELS.length];
        for (int i = 0; i < CATEGORY_LABELS.length; i++) {
            labels[i] = getString(CATEGORY_LABELS[i]);
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.coach_create)
                .setItems(labels, (d, which) -> {
                    String key = CATEGORY_KEYS[which];
                    String title = labels[which];
                    repo.create(key, title, new CoachRepository.Cb<CoachModels.ProgramDetail>() {
                        @Override public void ok(CoachModels.ProgramDetail detail) {
                            if (!isAdded()) return;
                            Toast.makeText(requireContext(), R.string.coach_created, Toast.LENGTH_SHORT).show();
                            load();
                        }
                        @Override public void err(Throwable e) {
                            if (isAdded()) {
                                Toast.makeText(requireContext(), R.string.coach_error, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                })
                .show();
    }

    private static class Adapter extends RecyclerView.Adapter<Adapter.VH> {
        interface OnProgramClick { void onClick(CoachModels.ProgramSummary program); }
        private final List<CoachModels.ProgramSummary> data = new ArrayList<>();
        private final OnProgramClick listener;
        Adapter(OnProgramClick listener) { this.listener = listener; }
        void submit(List<CoachModels.ProgramSummary> list) {
            data.clear();
            if (list != null) data.addAll(list);
            notifyDataSetChanged();
        }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int v) {
            View view = LayoutInflater.from(p.getContext()).inflate(R.layout.item_coach_program, p, false);
            return new VH(view);
        }
        @Override public void onBindViewHolder(@NonNull VH h, int i) {
            CoachModels.ProgramSummary p = data.get(i);
            h.title.setText(p.title != null ? p.title : h.itemView.getContext().getString(R.string.coach_title));
            h.meta.setText(h.itemView.getContext().getString(R.string.coach_meta_fmt, p.items_count, p.checkins_count));
            h.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onClick(p);
            });
        }
        @Override public int getItemCount() { return data.size(); }
        static class VH extends RecyclerView.ViewHolder {
            final TextView title;
            final TextView meta;
            VH(View v) {
                super(v);
                title = v.findViewById(R.id.txtTitle);
                meta = v.findViewById(R.id.txtMeta);
            }
        }
    }
}
