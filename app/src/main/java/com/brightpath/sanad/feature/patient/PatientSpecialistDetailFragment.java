package com.brightpath.sanad.feature.patient;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.brightpath.sanad.R;
import com.brightpath.sanad.data.ThemeStore;
import com.brightpath.sanad.feature.sessions.BookSessionFragment;
import com.brightpath.sanad.feature.sessions.DirectoryModels;
import com.brightpath.sanad.feature.sessions.DirectoryRepository;

public class PatientSpecialistDetailFragment extends Fragment {
    private DirectoryRepository repo;
    private ProgressBar progress;
    private View content;
    private ImageView imgAvatar;
    private TextView tvName, tvMeta, tvLanguages, tvRating, tvYears, tvSessions, tvAccepting, tvBio;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_patient_specialist_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.btnBack).setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
        ImageView logo = view.findViewById(R.id.imgLogo);
        if (logo != null) {
            logo.setImageResource(new ThemeStore(requireContext()).getLogoRes(true));
        }
        repo = new DirectoryRepository(requireContext());
        progress = view.findViewById(R.id.progress);
        content = view.findViewById(R.id.contentCard);
        imgAvatar = view.findViewById(R.id.imgAvatar);
        tvName = view.findViewById(R.id.tvName);
        tvMeta = view.findViewById(R.id.tvMeta);
        tvLanguages = view.findViewById(R.id.tvLanguages);
        tvRating = view.findViewById(R.id.tvRating);
        tvYears = view.findViewById(R.id.tvYears);
        tvSessions = view.findViewById(R.id.tvSessions);
        tvAccepting = view.findViewById(R.id.tvAccepting);
        tvBio = view.findViewById(R.id.tvBio);

        int specialistId = requireArguments().getInt("specialistId", -1);
        String name = requireArguments().getString("specialistName", "");
        String meta = requireArguments().getString("specialistMeta", "");

        tvName.setText(name == null || name.isEmpty() ? getString(R.string.nav_specialists) : name);
        tvMeta.setText(meta == null || meta.isEmpty() ? "—" : meta);
        if (content != null) content.setVisibility(View.VISIBLE);

        MaterialButton btnBook = view.findViewById(R.id.btnBook);
        btnBook.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putInt(BookSessionFragment.ARG_SPECIALIST_ID, specialistId);
            args.putString(BookSessionFragment.ARG_SPECIALIST_NAME, name);
            try {
                NavHostFragment.findNavController(this).navigate(R.id.bookSessionFragment, args);
            } catch (IllegalArgumentException | IllegalStateException e) {
                com.brightpath.sanad.feature.home.AppNavigator.go(this, R.id.bookSessionFragment, args);
            }
        });

        loadDetail(specialistId);
    }

    private void loadDetail(int id){
        if (progress != null) progress.setVisibility(View.VISIBLE);
        repo.detail(id, new DirectoryRepository.DetailListener() {
            @Override public void onSuccess(DirectoryModels.Detail d) {
                if (!isAdded()) return;
                bindDetail(d.data);
                if (progress != null) progress.setVisibility(View.GONE);
                if (content != null) content.setVisibility(View.VISIBLE);
            }

            @Override public void onError(Throwable t) {
                if (!isAdded()) return;
                if (progress != null) progress.setVisibility(View.GONE);
                if (content != null) content.setVisibility(View.VISIBLE);
                android.widget.Toast.makeText(requireContext(), R.string.error_fetch_data, android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindDetail(@Nullable DirectoryModels.Item item){
        if (item == null) return;
        tvName.setText(item.name != null ? item.name : getString(R.string.nav_specialists));
        if (item.specialty != null && !item.specialty.isEmpty()) {
            tvMeta.setText(item.specialty);
        }
        if (imgAvatar != null) {
            if (item.avatar != null && !item.avatar.isEmpty()) {
                imgAvatar.setVisibility(View.VISIBLE);
                Glide.with(imgAvatar.getContext())
                        .load(item.avatar)
                        .placeholder(R.drawable.ic_specialists)
                        .circleCrop()
                        .into(imgAvatar);
            } else {
                imgAvatar.setVisibility(View.GONE);
            }
        }
        if (item.languages != null && !item.languages.isEmpty()) {
            tvLanguages.setText(getString(R.string.specialist_languages_list, android.text.TextUtils.join(" • ", item.languages)));
            tvLanguages.setVisibility(View.VISIBLE);
        } else {
            tvLanguages.setVisibility(View.GONE);
        }
        if (item.rating != null) {
            tvRating.setText(getString(R.string.specialist_rating_label, item.rating));
            tvRating.setVisibility(View.VISIBLE);
        } else {
            tvRating.setVisibility(View.GONE);
        }
        if (item.years_exp != null) {
            tvYears.setText(getString(R.string.specialist_years_format, item.years_exp));
            tvYears.setVisibility(View.VISIBLE);
        }
        if (item.session_types != null && !item.session_types.isEmpty()) {
            tvSessions.setText(getString(R.string.specialist_sessions_list, android.text.TextUtils.join(" • ", item.session_types)));
            tvSessions.setVisibility(View.VISIBLE);
        } else {
            tvSessions.setVisibility(View.GONE);
        }
        if (item.accepting_new != null) {
            boolean accepting = DirectoryModels.isAccepting(item.accepting_new);
            tvAccepting.setText(getString(R.string.specialist_accepting_label, accepting ? getString(R.string.specialist_accepting_yes) : getString(R.string.specialist_accepting_no)));
            tvAccepting.setVisibility(View.VISIBLE);
        } else {
            tvAccepting.setVisibility(View.GONE);
        }
        if (item.bio != null && !item.bio.isEmpty()) {
            String bio = item.bio.get("ar");
            if (bio == null && !item.bio.isEmpty()) bio = item.bio.values().iterator().next();
            if (bio != null) {
                tvBio.setVisibility(View.VISIBLE);
                tvBio.setText(bio);
            }
        } else {
            tvBio.setVisibility(View.GONE);
        }
    }
}
