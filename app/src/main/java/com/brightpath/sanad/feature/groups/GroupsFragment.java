package com.brightpath.sanad.feature.groups;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.brightpath.sanad.R;
import com.brightpath.sanad.data.CatalogModels;
import com.brightpath.sanad.data.CatalogRepository;
import com.brightpath.sanad.data.auth.TokenStore;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.util.Locale;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class GroupsFragment extends Fragment {
    private GroupsViewModel vm;
    private View loading, error, content;
    private RecyclerView rv;
    private View empty;
    private ChipGroup chipAgeFilters, chipDisorderFilters;
    private String ageFilter;
    private String disorderFilter;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_groups, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        loading = v.findViewById(R.id.groupsLoading);
        error = v.findViewById(R.id.groupsError);
        content = v.findViewById(R.id.groupsContent);
        rv = v.findViewById(R.id.rvGroupSessions);
        empty = v.findViewById(R.id.emptyGroupSessions);
        chipAgeFilters = v.findViewById(R.id.chipAgeFilters);
        chipDisorderFilters = v.findViewById(R.id.chipDisorderFilters);
        View btnCreate = v.findViewById(R.id.btnGroupCreate);

        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        GroupAdapter adapter = new GroupAdapter(this);
        rv.setAdapter(adapter);

        vm = new ViewModelProvider(this).get(GroupsViewModel.class);
        vm.getState().observe(getViewLifecycleOwner(), st -> {
            if (st == null || st.loading) { show(loading); return; }
            if (st.error != null) { show(error); return; }
            show(content);
            List<GroupModels.GroupSession> list = st.data != null ? st.data.data : null;
            adapter.submit(list);
            empty.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
        });

        v.findViewById(R.id.btnGroupRetry).setOnClickListener(x -> vm.load(ageFilter, disorderFilter));
        loadFilterChips();
        if (btnCreate != null) {
            String role = new TokenStore(requireContext()).getRole();
            boolean specialist = role != null && role.equalsIgnoreCase("specialist");
            btnCreate.setVisibility(specialist ? View.VISIBLE : View.GONE);
            btnCreate.setOnClickListener(x -> NavHostFragment.findNavController(this).navigate(R.id.groupCreateFragment));
        }
        vm.load(ageFilter, disorderFilter);
    }

    private void loadFilterChips() {
        new CatalogRepository(requireContext()).load(new CatalogRepository.Cb() {
            @Override public void ok(CatalogModels.Catalog catalog) {
                if (!isAdded() || catalog == null) return;
                boolean ar = Locale.getDefault().getLanguage().startsWith("ar");
                populateChipGroup(chipAgeFilters, catalog.group_age_categories, ar, true);
                populateChipGroup(chipDisorderFilters, catalog.group_disorder_tags, ar, false);
            }
            @Override public void err(Throwable t) { }
        });
    }

    private void populateChipGroup(ChipGroup group, java.util.List<CatalogModels.CommunityCategory> items, boolean ar, boolean age) {
        if (group == null) return;
        group.removeAllViews();
        Chip all = new Chip(requireContext());
        all.setText(getString(R.string.community_filter_all));
        all.setCheckable(true);
        all.setChecked(age ? ageFilter == null : disorderFilter == null);
        all.setOnClickListener(v -> {
            if (age) ageFilter = null; else disorderFilter = null;
            vm.load(ageFilter, disorderFilter);
        });
        group.addView(all);
        if (items == null) return;
        for (CatalogModels.CommunityCategory item : items) {
            if (item == null || item.id == null) continue;
            Chip chip = new Chip(requireContext());
            chip.setText(ar && item.label_ar != null ? item.label_ar : (item.label_en != null ? item.label_en : item.id));
            chip.setCheckable(true);
            chip.setChecked(item.id.equals(age ? ageFilter : disorderFilter));
            String id = item.id;
            chip.setOnClickListener(v -> {
                if (age) ageFilter = id; else disorderFilter = id;
                vm.load(ageFilter, disorderFilter);
            });
            group.addView(chip);
        }
    }

    private void show(View t){
        loading.setVisibility(t == loading ? View.VISIBLE : View.GONE);
        error.setVisibility(t == error ? View.VISIBLE : View.GONE);
        content.setVisibility(t == content ? View.VISIBLE : View.GONE);
    }

    static class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.VH> {
        private final List<GroupModels.GroupSession> data = new ArrayList<>();
        private final Fragment host;
        GroupAdapter(Fragment host){ this.host = host; }
        void submit(List<GroupModels.GroupSession> list){
            data.clear();
            if (list != null) data.addAll(list);
            notifyDataSetChanged();
        }

        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int v) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_group_session, p, false));
        }

        @Override public void onBindViewHolder(@NonNull VH h, int i) {
            GroupModels.GroupSession g = data.get(i);
            h.title.setText(g.title);
            h.topic.setText(g.topic != null ? g.topic : "");
            h.status.setText(mapStatus(g.status));
            h.time.setText(formatSchedule(g.startAt, g.endAt));
            h.meta.setText(buildMeta(g));
            h.btnJoin.setText(g.joined ? host.getString(R.string.group_sessions_joined) : host.getString(R.string.group_sessions_join));
            h.btnJoin.setOnClickListener(v -> openDetail(g.id));
            h.itemView.setOnClickListener(v -> openDetail(g.id));
        }

        @Override public int getItemCount(){ return data.size(); }

        private void openDetail(int groupId){
            Bundle b = new Bundle();
            b.putInt("groupId", groupId);
            NavHostFragment.findNavController(host).navigate(R.id.groupDetailFragment, b);
        }

        private String buildMeta(GroupModels.GroupSession g){
            int count = g.participantsCount;
            String specialist = g.specialistName != null ? g.specialistName : host.getString(R.string.group_sessions_specialist_unknown);
            return host.getString(R.string.group_sessions_meta, count, specialist)
                    + (g.spotsLeft > 0 ? " · " + host.getString(R.string.group_sessions_spots_left, g.spotsLeft) : "");
        }

        private String mapStatus(String raw){
            if (raw == null) return host.getString(R.string.session_status_unknown);
            switch (raw.toLowerCase()) {
                case "scheduled":
                    return host.getString(R.string.session_status_upcoming);
                case "ongoing":
                case "in_progress":
                    return host.getString(R.string.session_status_live);
                case "finished":
                case "completed":
                    return host.getString(R.string.session_status_completed);
                case "canceled":
                case "cancelled":
                    return host.getString(R.string.session_status_cancelled);
                default:
                    return raw;
            }
        }

        private String formatSchedule(String start, String end){
            long startMs = parseMillis(start);
            if (startMs <= 0) return start != null ? start : "";
            String endText = "";
            long endMs = parseMillis(end);
            if (endMs > 0) {
                endText = " - " + formatInstant(endMs, "hh:mm a");
            }
            return formatInstant(startMs, "yyyy-MM-dd - hh:mm a") + endText;
        }

        private String formatInstant(long ms, String pattern){
            try {
                ZonedDateTime dt = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault());
                return dt.format(DateTimeFormatter.ofPattern(pattern));
            } catch (Exception e){
                return "";
            }
        }

        private long parseMillis(String raw){
            if (raw == null || raw.isEmpty()) return -1;
            try {
                return Instant.parse(raw).toEpochMilli();
            } catch (DateTimeParseException ignored){}
            try {
                return OffsetDateTime.parse(raw, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant().toEpochMilli();
            } catch (DateTimeParseException ignored){}
            return -1;
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView title, topic, time, status, meta;
            com.google.android.material.button.MaterialButton btnJoin;
            VH(@NonNull View v){
                super(v);
                title = v.findViewById(R.id.tvGroupTitle);
                topic = v.findViewById(R.id.tvGroupTopic);
                time = v.findViewById(R.id.tvGroupTime);
                status = v.findViewById(R.id.tvGroupStatus);
                meta = v.findViewById(R.id.tvGroupMeta);
                btnJoin = v.findViewById(R.id.btnGroupJoin);
            }
        }
    }
}
