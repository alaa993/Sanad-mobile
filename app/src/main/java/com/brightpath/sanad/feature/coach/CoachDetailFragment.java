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
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class CoachDetailFragment extends Fragment {
    private CoachRepository repo;
    private int programId = -1;
    private String category;
    private ItemsAdapter itemsAdapter;
    private CheckinsAdapter checkinsAdapter;
    private View progress;
    private TextView tvTitle;
    private TextView tvCheckinsHistory;
    private TextInputLayout tilWeight;
    private TextInputEditText etMood;
    private TextInputEditText etWeight;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_coach_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        if (!com.brightpath.sanad.ui.PatientOnlyGuard.allowOrLeave(this)) return;
        if (getArguments() != null) {
            programId = getArguments().getInt("programId", -1);
        }
        repo = new CoachRepository(requireContext());
        progress = v.findViewById(R.id.progress);
        tvTitle = v.findViewById(R.id.tvTitle);
        tvCheckinsHistory = v.findViewById(R.id.tvCheckinsHistory);
        tilWeight = v.findViewById(R.id.tilWeight);
        etMood = v.findViewById(R.id.etMood);
        etWeight = v.findViewById(R.id.etWeight);
        MaterialToolbar toolbar = v.findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitle(R.string.coach_title);
            toolbar.setNavigationOnClickListener(x -> NavHostFragment.findNavController(this).popBackStack());
        }

        RecyclerView rvItems = v.findViewById(R.id.rvItems);
        RecyclerView rvCheckins = v.findViewById(R.id.rvCheckins);
        rvItems.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCheckins.setLayoutManager(new LinearLayoutManager(requireContext()));
        itemsAdapter = new ItemsAdapter();
        checkinsAdapter = new CheckinsAdapter();
        rvItems.setAdapter(itemsAdapter);
        rvCheckins.setAdapter(checkinsAdapter);

        v.findViewById(R.id.btnCheckin).setOnClickListener(x -> submitCheckin());
        load();
    }

    private void submitCheckin() {
        String mood = etMood != null && etMood.getText() != null ? etMood.getText().toString().trim() : "";
        if (mood.isEmpty()) {
            Toast.makeText(requireContext(), R.string.coach_checkin_mood_hint, Toast.LENGTH_SHORT).show();
            return;
        }
        Double weightKg = null;
        if ("weight".equals(category) && etWeight != null && etWeight.getText() != null) {
            String w = etWeight.getText().toString().trim();
            if (!w.isEmpty()) {
                try {
                    weightKg = Double.parseDouble(w);
                } catch (NumberFormatException e) {
                    Toast.makeText(requireContext(), R.string.coach_checkin_weight_hint, Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }
        repo.checkin(programId, mood, null, weightKg, new CoachRepository.Cb<CoachModels.Checkin>() {
            @Override public void ok(CoachModels.Checkin c) {
                if (!isAdded()) return;
                if (etMood != null) etMood.setText("");
                if (etWeight != null) etWeight.setText("");
                load();
            }
            @Override public void err(Throwable e) {
                if (isAdded()) Toast.makeText(requireContext(), R.string.coach_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void load() {
        if (programId <= 0) return;
        progress.setVisibility(View.VISIBLE);
        repo.show(programId, new CoachRepository.Cb<CoachModels.ProgramDetail>() {
            @Override public void ok(CoachModels.ProgramDetail detail) {
                if (!isAdded()) return;
                progress.setVisibility(View.GONE);
                if (detail == null) return;
                category = detail.category;
                tvTitle.setText(detail.title);
                MaterialToolbar toolbar = getView() != null ? getView().findViewById(R.id.toolbar) : null;
                if (toolbar != null && detail.title != null) toolbar.setTitle(detail.title);
                boolean showWeight = "weight".equals(category);
                if (tilWeight != null) tilWeight.setVisibility(showWeight ? View.VISIBLE : View.GONE);
                itemsAdapter.submit(detail.items);
                List<CoachModels.Checkin> checkins = detail.checkins != null ? detail.checkins : new ArrayList<>();
                checkinsAdapter.submit(checkins);
                if (tvCheckinsHistory != null) {
                    tvCheckinsHistory.setVisibility(checkins.isEmpty() ? View.GONE : View.VISIBLE);
                }
            }
            @Override public void err(Throwable e) {
                if (!isAdded()) return;
                progress.setVisibility(View.GONE);
                Toast.makeText(requireContext(), R.string.coach_load_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class ItemsAdapter extends RecyclerView.Adapter<ItemsAdapter.VH> {
        private final List<CoachModels.PlanItem> data = new ArrayList<>();
        void submit(List<CoachModels.PlanItem> list) {
            data.clear();
            if (list != null) data.addAll(list);
            notifyDataSetChanged();
        }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int v) {
            View view = LayoutInflater.from(p.getContext()).inflate(R.layout.item_coach_task, p, false);
            return new VH(view);
        }
        @Override public void onBindViewHolder(@NonNull VH h, int i) {
            CoachModels.PlanItem item = data.get(i);
            h.title.setText(item.title);
            h.done.setChecked(item.is_done);
            h.itemView.setOnClickListener(v -> toggle(item));
        }
        private void toggle(CoachModels.PlanItem item) {
            repo.toggleItem(item.id, new CoachRepository.Cb<Map<String, Object>>() {
                @Override public void ok(Map<String, Object> m) { load(); }
                @Override public void err(Throwable e) { load(); }
            });
        }
        @Override public int getItemCount() { return data.size(); }
        class VH extends RecyclerView.ViewHolder {
            final CheckBox done;
            final TextView title;
            VH(View v) {
                super(v);
                done = v.findViewById(R.id.chkDone);
                title = v.findViewById(R.id.txtTitle);
            }
        }
    }

    private static class CheckinsAdapter extends RecyclerView.Adapter<CheckinsAdapter.VH> {
        private final List<CoachModels.Checkin> data = new ArrayList<>();
        private final SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        void submit(List<CoachModels.Checkin> list) {
            data.clear();
            if (list != null) data.addAll(list);
            notifyDataSetChanged();
        }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int v) {
            View view = LayoutInflater.from(p.getContext()).inflate(R.layout.item_coach_checkin, p, false);
            return new VH(view);
        }
        @Override public void onBindViewHolder(@NonNull VH h, int i) {
            CoachModels.Checkin c = data.get(i);
            h.date.setText(formatDate(h.itemView, c.logged_at));
            String mood = c.mood != null && !c.mood.isEmpty() ? c.mood : "—";
            h.mood.setText(h.itemView.getContext().getString(R.string.coach_checkin_mood_fmt, mood));
            StringBuilder extras = new StringBuilder();
            if (c.weight_kg != null) {
                extras.append(h.itemView.getContext().getString(
                        R.string.coach_checkin_weight_fmt,
                        String.valueOf(c.weight_kg)));
            }
            if (c.note != null && !c.note.isEmpty()) {
                if (extras.length() > 0) extras.append('\n');
                extras.append(c.note);
            }
            if (extras.length() > 0) {
                h.note.setVisibility(View.VISIBLE);
                h.note.setText(extras.toString());
            } else {
                h.note.setVisibility(View.GONE);
            }
        }
        private String formatDate(View host, String raw) {
            if (raw == null || raw.isEmpty()) return "—";
            try {
                String truncated = raw.length() >= 19 ? raw.substring(0, 19) : raw;
                Date parsed = iso.parse(truncated);
                if (parsed != null) {
                    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                            .format(parsed);
                }
            } catch (ParseException ignored) { }
            return raw;
        }
        @Override public int getItemCount() { return data.size(); }
        static class VH extends RecyclerView.ViewHolder {
            final TextView date;
            final TextView mood;
            final TextView note;
            VH(View v) {
                super(v);
                date = v.findViewById(R.id.txtDate);
                mood = v.findViewById(R.id.txtMood);
                note = v.findViewById(R.id.txtNote);
            }
        }
    }
}
