package com.brightpath.sanad.feature.sessions;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.*;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.brightpath.sanad.R;
import com.brightpath.sanad.feature.patient.PatientIntakeForm;
import com.brightpath.sanad.feature.patient.PatientIntakeRepository;
import com.brightpath.sanad.feature.patient.TriageRecommendation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SelectListFragment extends Fragment {
    public static final String ARG_IS_SPECIALISTS = "isSpecialists";
    public static final String RESULT_ID = "selectedId";
    public static final String RESULT_NAME = "selectedName";
    public static final String RESULT_IS_SPECIALISTS = "isSpecialists";
    public static final String RESULT_CLEARED = "cleared";

    private DirectoryRepository repo;
    private boolean isSpecialists;
    private RecyclerView rv;
    private ProgressBar progress;
    private EditText etSearch;
    private ListAdapter adapter;
    private TextView tvHint;
    private TextView tvEmpty;
    private View filterScroll;
    private ChipGroup chipGroupFilters;
    private String filterSpecialty;
    private String filterLanguage;
    private String filterMinRating;
    private TriageRecommendation recommendation;
    private boolean highlightRecommendation;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_select_list, container, false);
    }

    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        isSpecialists = getArguments()!=null && getArguments().getBoolean(ARG_IS_SPECIALISTS, true);
        repo = new DirectoryRepository(requireContext());
        View btnBack = v.findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(x -> NavHostFragment.findNavController(this).popBackStack());
        }
        ImageView logo = v.findViewById(R.id.imgLogo);
        if (logo != null) {
            logo.setImageResource(new com.brightpath.sanad.data.ThemeStore(requireContext()).getLogoRes(false));
        }
        tvHint = v.findViewById(R.id.tvRecommendationHint);
        tvEmpty = v.findViewById(R.id.tvEmpty);
        filterScroll = v.findViewById(R.id.filterScroll);
        chipGroupFilters = v.findViewById(R.id.chipGroupFilters);
        loadRecommendation();
        setupFilters();

        rv = v.findViewById(R.id.recycler);
        progress = v.findViewById(R.id.progress);
        etSearch = v.findViewById(R.id.etSearch);
        adapter = new ListAdapter(item -> {
            Bundle res = new Bundle();
            res.putBoolean(RESULT_IS_SPECIALISTS, isSpecialists);
            res.putInt(RESULT_ID, item.id);
            res.putString(RESULT_NAME, item.name);
            getParentFragmentManager().setFragmentResult("picker", res);
            NavController nav = NavHostFragment.findNavController(this);
            nav.popBackStack();
        }, this::isRecommended, isSpecialists);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);

        View btnClearOrg = v.findViewById(R.id.btnClearOrg);
        if (btnClearOrg != null) {
            if (isSpecialists) {
                btnClearOrg.setVisibility(View.GONE);
            } else {
                btnClearOrg.setVisibility(View.VISIBLE);
                btnClearOrg.setOnClickListener(x -> {
                    Bundle res = new Bundle();
                    res.putBoolean(RESULT_IS_SPECIALISTS, false);
                    res.putBoolean(RESULT_CLEARED, true);
                    getParentFragmentManager().setFragmentResult("picker", res);
                    NavHostFragment.findNavController(this).popBackStack();
                });
            }
        }

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { load(s.toString()); }
        });

        load(null);
    }

    private void setupFilters() {
        if (!isSpecialists || chipGroupFilters == null) return;
        if (filterScroll != null) filterScroll.setVisibility(View.VISIBLE);
        chipGroupFilters.removeAllViews();
        addFilterChip(getString(R.string.filter_all), "all", null, null, null);
        addFilterChip(getString(R.string.filter_specialty_psychiatrist), "specialty", "psychiat", null, null);
        addFilterChip(getString(R.string.filter_specialty_psychologist), "specialty", "psycholog", null, null);
        addFilterChip(getString(R.string.filter_specialty_cbt), "specialty", "cbt", null, null);
        addFilterChip(getString(R.string.filter_language_ar), "language", null, "ar", null);
        addFilterChip(getString(R.string.filter_language_en), "language", null, "en", null);
        addFilterChip(getString(R.string.filter_rating_4), "rating", null, null, "4");
        addFilterChip(getString(R.string.filter_rating_3), "rating", null, null, "3");
    }

    private void addFilterChip(String label, String kind, String specialty, String language, String minRating) {
        Chip chip = new Chip(requireContext());
        chip.setText(label);
        chip.setCheckable(true);
        chip.setTag(kind + "|" + (specialty != null ? specialty : "") + "|" + (language != null ? language : "") + "|" + (minRating != null ? minRating : ""));
        chip.setOnClickListener(v -> {
            String tag = chip.getTag() != null ? chip.getTag().toString() : "";
            String[] parts = tag.split("\\|", -1);
            if ("all".equals(parts[0])) {
                filterSpecialty = null;
                filterLanguage = null;
                filterMinRating = null;
            } else if ("specialty".equals(parts[0])) {
                filterSpecialty = parts.length > 1 && !parts[1].isEmpty() ? parts[1] : null;
                filterLanguage = null;
                filterMinRating = null;
            } else if ("language".equals(parts[0])) {
                filterLanguage = parts.length > 2 && !parts[2].isEmpty() ? parts[2] : null;
                filterSpecialty = null;
                filterMinRating = null;
            } else if ("rating".equals(parts[0])) {
                filterMinRating = parts.length > 3 && !parts[3].isEmpty() ? parts[3] : null;
                filterSpecialty = null;
                filterLanguage = null;
            }
            for (int i = 0; i < chipGroupFilters.getChildCount(); i++) {
                View child = chipGroupFilters.getChildAt(i);
                if (child instanceof Chip && child != chip) {
                    ((Chip) child).setChecked(false);
                }
            }
            chip.setChecked(true);
            load(etSearch != null ? etSearch.getText().toString() : null);
        });
        chipGroupFilters.addView(chip);
        if ("all".equals(kind)) chip.setChecked(true);
    }

    private void load(String search){
        progress.setVisibility(View.VISIBLE);
        if (highlightRecommendation && tvHint != null) {
            tvHint.setVisibility(View.VISIBLE);
        }

        repo.load(isSpecialists, search, 1, filterSpecialty, filterLanguage, filterMinRating, new DirectoryRepository.Listener() {
            @Override public void onSuccess(DirectoryModels.Paged d){
                progress.setVisibility(View.GONE);
                List<DirectoryModels.Item> data = d!=null?d.data:null;
                if (data != null && highlightRecommendation){
                    Collections.sort(data, new Comparator<DirectoryModels.Item>() {
                        @Override public int compare(DirectoryModels.Item a, DirectoryModels.Item b){
                            boolean ra = isRecommended(a);
                            boolean rb = isRecommended(b);
                            if (ra == rb) return 0;
                            return ra ? -1 : 1;
                        }
                    });
                }
                adapter.submit(data);
                boolean empty = data == null || data.isEmpty();
                if (tvEmpty != null) tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                rv.setVisibility(empty ? View.GONE : View.VISIBLE);
            }
            @Override public void onError(Throwable t){
                progress.setVisibility(View.GONE);
                if (tvEmpty != null) tvEmpty.setVisibility(View.VISIBLE);
                rv.setVisibility(View.GONE);
            }
        });
    }

    private void loadRecommendation(){
        if (!isSpecialists) { recommendation = null; highlightRecommendation = false; return; }
        PatientIntakeForm form = new PatientIntakeRepository(requireContext()).load();
        boolean hasData = form != null && (
                !TextUtils.isEmpty(form.fullName) ||
                (form.symptoms != null && !form.symptoms.isEmpty()) ||
                !TextUtils.isEmpty(form.duration));
        if (!hasData){
            recommendation = null;
            highlightRecommendation = false;
            return;
        }
        recommendation = TriageRecommendation.evaluate(form);
        highlightRecommendation = recommendation != null && recommendation.category != null;
    }

    private boolean isRecommended(@Nullable DirectoryModels.Item item){
        if (!highlightRecommendation || item == null) return false;
        String category = item.category != null ? item.category.toUpperCase() : null;
        if (category != null && recommendation.category != null &&
                category.equalsIgnoreCase(recommendation.category.name())) {
            return true;
        }
        if (item.tags != null && recommendation.category != null){
            for (String tag : item.tags){
                if (tag != null && tag.equalsIgnoreCase(recommendation.category.name())) return true;
            }
        }
        if (!TextUtils.isEmpty(item.specialty) && recommendation.suggestedSpecialist != null){
            return item.specialty.contains(recommendation.suggestedSpecialist);
        }
        return false;
    }

    static class ListAdapter extends RecyclerView.Adapter<ListAdapter.VH> {
        interface Click { void onClick(DirectoryModels.Item it); }
        interface RecommendationChecker { boolean isRecommended(DirectoryModels.Item it); }
        private List<DirectoryModels.Item> data = new ArrayList<>();
        private final Click click;
        private final RecommendationChecker checker;
        private final boolean specialistMode;
        ListAdapter(Click c, RecommendationChecker checker, boolean specialistMode){
            click=c;
            this.checker = checker;
            this.specialistMode = specialistMode;
        }
        void submit(List<DirectoryModels.Item> list){ data = list!=null?list:new ArrayList<>(); notifyDataSetChanged(); }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int v){
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_select, p, false));
        }
        @Override public void onBindViewHolder(@NonNull VH h, int i){
            DirectoryModels.Item it = data.get(i);
            if (specialistMode) {
                String avatar = it.avatar;
                if (!TextUtils.isEmpty(avatar)) {
                    Glide.with(h.avatar)
                            .load(avatar)
                            .placeholder(R.drawable.ic_specialists)
                            .circleCrop()
                            .into(h.avatar);
                } else {
                    h.avatar.setImageResource(R.drawable.ic_specialists);
                }
            } else {
                h.avatar.setImageResource(R.drawable.ic_care);
            }
            h.title.setText(it.name);
            if (!TextUtils.isEmpty(it.specialty)){
                h.subtitle.setVisibility(View.VISIBLE);
                h.subtitle.setText(it.specialty);
            } else {
                h.subtitle.setVisibility(View.GONE);
            }
            if (specialistMode) {
                if (it.years_exp != null || it.rating != null) {
                    h.category.setVisibility(View.VISIBLE);
                    StringBuilder sb = new StringBuilder();
                    if (it.years_exp != null) {
                        sb.append(h.itemView.getContext().getString(R.string.specialist_years_format, it.years_exp));
                    }
                    if (it.rating != null) {
                        if (sb.length() > 0) sb.append(" • ");
                        sb.append(h.itemView.getContext().getString(R.string.specialist_rating_label, it.rating));
                    }
                    h.category.setText(sb.toString());
                } else {
                    h.category.setVisibility(View.GONE);
                }
                if (it.languages != null && !it.languages.isEmpty()) {
                    h.tags.setVisibility(View.VISIBLE);
                    h.tags.setText(h.itemView.getContext().getString(
                            R.string.specialist_languages_list,
                            TextUtils.join(" • ", it.languages)
                    ));
                } else {
                    h.tags.setVisibility(View.GONE);
                }
            } else {
                if (!TextUtils.isEmpty(it.category)){
                    h.category.setVisibility(View.VISIBLE);
                    h.category.setText(it.category);
                } else {
                    h.category.setVisibility(View.GONE);
                }
                if (it.tags != null && !it.tags.isEmpty()){
                    String joined = TextUtils.join(" • ", it.tags);
                    h.tags.setVisibility(View.VISIBLE);
                    h.tags.setText(joined);
                } else {
                    h.tags.setVisibility(View.GONE);
                }
            }
            boolean rec = checker != null && checker.isRecommended(it);
            h.badge.setVisibility(rec ? View.VISIBLE : View.GONE);
            h.itemView.setOnClickListener(v -> click.onClick(it));
        }
        @Override public int getItemCount(){ return data.size(); }
        static class VH extends RecyclerView.ViewHolder {
            TextView title, subtitle, badge, category, tags;
            ImageView avatar;
            VH(@NonNull View v){ super(v);
                avatar=v.findViewById(R.id.imgAvatar);
                title=v.findViewById(R.id.tvTitle);
                subtitle=v.findViewById(R.id.tvSubtitle);
                badge=v.findViewById(R.id.tvBadge);
                category=v.findViewById(R.id.tvCategory);
                tags=v.findViewById(R.id.tvTags);
            }
        }
    }
}
