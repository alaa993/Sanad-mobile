package com.brightpath.sanad.feature.patient;

import android.os.Bundle;
import android.text.Editable;
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
import com.brightpath.sanad.R;
import com.brightpath.sanad.data.ThemeStore;
import com.brightpath.sanad.feature.sessions.DirectoryModels;
import com.brightpath.sanad.feature.sessions.DirectoryRepository;

import java.util.ArrayList;
import java.util.List;

public class PatientSpecialistsFragment extends Fragment {
    private DirectoryRepository repo;
    private ProgressBar progress;
    private TextView empty;
    private SpecialistsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_patient_specialists, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        repo = new DirectoryRepository(requireContext());
        progress = v.findViewById(R.id.progress);
        empty = v.findViewById(R.id.tvEmpty);
        ImageView logo = v.findViewById(R.id.imgLogo);
        if (logo != null) {
            logo.setImageResource(new ThemeStore(requireContext()).getLogoRes(true));
        }

        v.findViewById(R.id.btnBack).setOnClickListener(x -> NavHostFragment.findNavController(this).popBackStack());

        RecyclerView rv = v.findViewById(R.id.recycler);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new SpecialistsAdapter(item -> openDetail(item));
        rv.setAdapter(adapter);

        EditText search = v.findViewById(R.id.etSearch);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { load(s.toString()); }
        });

        load(null);
    }

    private void load(@Nullable String query) {
        if (progress != null) progress.setVisibility(View.VISIBLE);
        repo.load(true, query, 1, new DirectoryRepository.Listener() {
            @Override
            public void onSuccess(DirectoryModels.Paged d) {
                if (!isAdded()) return;
                if (progress != null) progress.setVisibility(View.GONE);
                List<DirectoryModels.Item> data = d != null ? d.data : null;
                adapter.submit(data);
                boolean isEmpty = data == null || data.isEmpty();
                if (empty != null) empty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(Throwable t) {
                if (!isAdded()) return;
                if (progress != null) progress.setVisibility(View.GONE);
                adapter.submit(null);
                if (empty != null) empty.setVisibility(View.VISIBLE);
            }
        });
    }

    private void openDetail(DirectoryModels.Item item) {
        if (item == null) return;
        Bundle args = new Bundle();
        args.putInt("specialistId", item.id);
        args.putString("specialistName", item.name != null ? item.name : "");
        String meta = item.specialty != null ? item.specialty : (item.category != null ? item.category : "");
        args.putString("specialistMeta", meta);
        try {
            NavHostFragment.findNavController(this).navigate(R.id.patientSpecialistDetailFragment, args);
        } catch (IllegalArgumentException | IllegalStateException e) {
            com.brightpath.sanad.feature.home.AppNavigator.go(this, R.id.patientSpecialistDetailFragment, args);
        }
    }

    static class SpecialistsAdapter extends RecyclerView.Adapter<SpecialistsAdapter.VH> {
        interface Click { void onClick(DirectoryModels.Item item); }
        private final Click click;
        private final List<DirectoryModels.Item> data = new ArrayList<>();

        SpecialistsAdapter(Click click) {
            this.click = click;
        }

        void submit(List<DirectoryModels.Item> list) {
            data.clear();
            if (list != null) data.addAll(list);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_patient_specialist, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            DirectoryModels.Item it = data.get(position);
            h.name.setText(it.name);
            if (it.specialty != null && !it.specialty.isEmpty()) {
                h.specialty.setVisibility(View.VISIBLE);
                h.specialty.setText(it.specialty);
            } else {
                h.specialty.setVisibility(View.GONE);
            }
            if (it.years_exp != null || it.rating != null) {
                h.meta.setVisibility(View.VISIBLE);
                StringBuilder sb = new StringBuilder();
                if (it.years_exp != null) sb.append(h.itemView.getContext().getString(R.string.specialist_years_format, it.years_exp));
                if (it.rating != null) {
                    if (sb.length() > 0) sb.append(" • ");
                    sb.append(h.itemView.getContext().getString(R.string.specialist_rating_label, it.rating));
                }
                h.meta.setText(sb.toString());
            } else {
                h.meta.setVisibility(View.GONE);
            }
            if (it.languages != null && !it.languages.isEmpty()) {
                h.languages.setVisibility(View.VISIBLE);
                h.languages.setText(h.itemView.getContext().getString(R.string.specialist_languages_list, android.text.TextUtils.join(" • ", it.languages)));
            } else {
                h.languages.setVisibility(View.GONE);
            }
            if (h.avatar != null) {
                if (it.avatar != null && !it.avatar.isEmpty()) {
                    Glide.with(h.avatar.getContext())
                            .load(it.avatar)
                            .placeholder(R.drawable.ic_specialists)
                            .circleCrop()
                            .into(h.avatar);
                } else {
                    h.avatar.setImageResource(R.drawable.ic_specialists);
                }
            }
            h.itemView.setOnClickListener(v -> {
                if (click != null) click.onClick(it);
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            final TextView name, specialty, meta, languages;
            final ImageView avatar;

            VH(@NonNull View v) {
                super(v);
                avatar = v.findViewById(R.id.imgAvatar);
                name = v.findViewById(R.id.tvName);
                specialty = v.findViewById(R.id.tvSpecialty);
                meta = v.findViewById(R.id.tvMeta);
                languages = v.findViewById(R.id.tvLanguages);
            }
        }
    }
}
