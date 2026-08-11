package com.brightpath.sanad.feature.library;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.brightpath.sanad.R;
import com.brightpath.sanad.data.AppConfig;
import com.brightpath.sanad.data.LibraryModels;
import com.brightpath.sanad.data.LibraryRepository;
import com.brightpath.sanad.data.auth.TokenStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LibraryFragment extends Fragment {

    private LibraryViewModel vm;
    private View progress, error, content;
    private RecyclerView rv;
    private CategoriesAdapter adapter;
    private MaterialButton btnAddArticle;
    private LibraryRepository repo;
    private List<LibraryModels.Category> lastCategories = new ArrayList<>();
    private View cardDailyTip;
    private View cardSyriaEurope;
    private TextView tvDailyTipTitle, tvDailyTipBody;
    private View tagScroll;
    private ChipGroup chipGroupTags;
    private String selectedTag;
    private boolean showingCurated = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_library, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);

        // Views
        progress = v.findViewById(R.id.progress);
        error = v.findViewById(R.id.errorContainer);
        content = v.findViewById(R.id.content);
        rv = v.findViewById(R.id.rvArticles);
        btnAddArticle = v.findViewById(R.id.btnAddArticle);
        cardDailyTip = v.findViewById(R.id.cardDailyTip);
        cardSyriaEurope = v.findViewById(R.id.cardSyriaEurope);
        tvDailyTipTitle = v.findViewById(R.id.tvDailyTipTitle);
        tvDailyTipBody = v.findViewById(R.id.tvDailyTipBody);
        tagScroll = v.findViewById(R.id.tagScroll);
        chipGroupTags = v.findViewById(R.id.chipGroupTags);
        repo = new LibraryRepository(requireContext());

        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CategoriesAdapter(this);
        rv.setAdapter(adapter);

        if (btnAddArticle != null) {
            boolean canPublish = canPublishArticles();
            btnAddArticle.setVisibility(canPublish ? View.VISIBLE : View.GONE);
            btnAddArticle.setOnClickListener(x -> openPublishDialog());
        }
        if (cardSyriaEurope != null) {
            cardSyriaEurope.setOnClickListener(x -> loadCuratedSyriaEurope());
        }

        // ViewModel
        vm = new ViewModelProvider(this).get(LibraryViewModel.class);

        // Observe state
        vm.getState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) return;
            // While browsing curated Syria/Europe, do not let main-library LiveData wipe the list.
            if (showingCurated) {
                if (state.loading) return;
                if (state.error == null && state.categories != null) {
                    lastCategories = state.categories;
                }
                return;
            }
            if (state.loading) {
                show(progress);
                return;
            }
            if (state.error != null) {
                show(error);
                return;
            }
            show(content);
            adapter.submit(state.categories);
            lastCategories = state.categories != null ? state.categories : new ArrayList<>();
        });

        // Load data
        vm.load(selectedTag);
        loadDailyTip();
        loadTags();
    }

    private void loadCuratedSyriaEurope() {
        show(progress);
        showingCurated = true;
        repo.fetchCuratedSyriaEurope(new LibraryRepository.CuratedListener() {
            @Override public void onSuccess(LibraryModels.CuratedResponse response) {
                if (!isAdded()) return;
                showingCurated = true;
                show(content);
                java.util.List<LibraryModels.ArticleListItem> articles = response != null ? response.data : null;
                if (articles == null || articles.isEmpty()) {
                    android.widget.Toast.makeText(requireContext(), R.string.library_syria_europe_empty, android.widget.Toast.LENGTH_SHORT).show();
                    showingCurated = false;
                    vm.load(selectedTag);
                    return;
                }
                LibraryModels.Category cat = new LibraryModels.Category();
                cat.id = -1;
                java.util.Map<String, String> title = new java.util.HashMap<>();
                title.put("ar", getString(R.string.library_syria_europe_title));
                title.put("en", getString(R.string.library_syria_europe_title));
                cat.title = title;
                cat.articles = articles;
                java.util.List<LibraryModels.Category> cats = new java.util.ArrayList<>();
                cats.add(cat);
                adapter.submit(cats);
                if (cardSyriaEurope != null) cardSyriaEurope.setVisibility(View.GONE);
                if (content instanceof androidx.core.widget.NestedScrollView) {
                    content.post(() -> {
                        if (rv != null) {
                            ((androidx.core.widget.NestedScrollView) content).smoothScrollTo(0, rv.getTop());
                        }
                    });
                }
            }
            @Override public void onError(Throwable t) {
                if (!isAdded()) return;
                showingCurated = false;
                android.widget.Toast.makeText(requireContext(), R.string.library_syria_europe_failed, android.widget.Toast.LENGTH_SHORT).show();
                show(content);
                if (cardSyriaEurope != null) cardSyriaEurope.setVisibility(View.VISIBLE);
                vm.load(selectedTag);
            }
        });
    }

    private void loadTags() {
        if (chipGroupTags == null) return;
        repo.fetchTags(new LibraryRepository.TagsListener() {
            @Override public void onSuccess(List<String> tags) {
                if (!isAdded() || tags == null || tags.isEmpty()) return;
                chipGroupTags.removeAllViews();
                if (tagScroll != null) tagScroll.setVisibility(View.VISIBLE);
                Chip allChip = styledChip(getString(R.string.library_all_tags), selectedTag == null);
                allChip.setOnClickListener(x -> {
                    selectedTag = null;
                    showingCurated = false;
                    if (cardSyriaEurope != null) cardSyriaEurope.setVisibility(View.VISIBLE);
                    refreshChipSelection(null);
                    vm.load(null);
                });
                chipGroupTags.addView(allChip);
                for (String tag : tags) {
                    Chip chip = styledChip(prettyTag(tag), tag.equals(selectedTag));
                    chip.setOnClickListener(x -> {
                        selectedTag = tag;
                        showingCurated = false;
                        if (cardSyriaEurope != null) cardSyriaEurope.setVisibility(View.VISIBLE);
                        refreshChipSelection(tag);
                        vm.load(tag);
                    });
                    chip.setTag(tag);
                    chipGroupTags.addView(chip);
                }
            }
            @Override public void onError(Throwable t) {
                if (tagScroll != null) tagScroll.setVisibility(View.GONE);
            }
        });
    }

    private Chip styledChip(String label, boolean checked) {
        Chip chip = new Chip(requireContext());
        chip.setText(label);
        chip.setCheckable(true);
        chip.setChecked(checked);
        chip.setCheckedIconVisible(false);
        chip.setEnsureMinTouchTargetSize(false);
        chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        chip.setChipCornerRadius(dp(16));
        chip.setChipStrokeWidth(dp(1));
        int primary = resolvePrimaryColor();
        int stroke = Color.parseColor("#D8DEEF");
        int soft = Color.parseColor("#E8ECFF");
        chip.setChipBackgroundColor(new ColorStateList(
                new int[][]{ new int[]{android.R.attr.state_checked}, new int[]{} },
                new int[]{ soft, Color.WHITE }
        ));
        chip.setChipStrokeColor(new ColorStateList(
                new int[][]{ new int[]{android.R.attr.state_checked}, new int[]{} },
                new int[]{ primary, stroke }
        ));
        chip.setTextColor(new ColorStateList(
                new int[][]{ new int[]{android.R.attr.state_checked}, new int[]{} },
                new int[]{ primary, Color.parseColor("#5E5F5F") }
        ));
        return chip;
    }

    private void refreshChipSelection(String tag) {
        if (chipGroupTags == null) return;
        for (int i = 0; i < chipGroupTags.getChildCount(); i++) {
            View child = chipGroupTags.getChildAt(i);
            if (!(child instanceof Chip)) continue;
            Chip chip = (Chip) child;
            Object chipTag = chip.getTag();
            boolean match = (tag == null && chipTag == null) || (tag != null && tag.equals(chipTag));
            if (chipTag == null && tag == null && i == 0) match = true;
            chip.setChecked(match);
        }
    }

    private String prettyTag(String tag) {
        if (tag == null || tag.isEmpty()) return "";
        if (tag.length() == 1) return tag.toUpperCase(Locale.getDefault());
        return tag.substring(0, 1).toUpperCase(Locale.getDefault()) + tag.substring(1);
    }

    private float dp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    private int resolvePrimaryColor() {
        TypedValue tv = new TypedValue();
        requireContext().getTheme().resolveAttribute(R.attr.sanadPrimary, tv, true);
        return tv.data;
    }

    private void loadDailyTip() {
        if (cardDailyTip == null) return;
        repo.fetchDailyTip(new LibraryRepository.DailyTipListener() {
            @Override public void onSuccess(LibraryModels.DailyTip tip) {
                if (!isAdded() || tip == null) return;
                String title = tip.title != null ? tip.title.trim() : "";
                String body = tip.body != null ? tip.body.trim() : "";
                if (!isMeaningfulTip(title, body)) {
                    cardDailyTip.setVisibility(View.GONE);
                    return;
                }
                cardDailyTip.setVisibility(View.VISIBLE);
                if (tvDailyTipTitle != null) {
                    tvDailyTipTitle.setVisibility(title.isEmpty() ? View.GONE : View.VISIBLE);
                    tvDailyTipTitle.setText(title);
                }
                if (tvDailyTipBody != null) tvDailyTipBody.setText(body);
            }
            @Override public void onError(Throwable t) {
                if (cardDailyTip != null) cardDailyTip.setVisibility(View.GONE);
            }
        });
    }

    private boolean isMeaningfulTip(String title, String body) {
        String combined = ((title == null ? "" : title) + " " + (body == null ? "" : body)).trim();
        if (combined.length() < 4) return false;
        String normalized = combined.replaceAll("\\s+", "").toLowerCase(Locale.US);
        return !(normalized.equals("aa") || normalized.equals("a") || normalized.equals("-") || normalized.equals("test"));
    }

    private void show(View t) {
        progress.setVisibility(t == progress ? View.VISIBLE : View.GONE);
        error.setVisibility(t == error ? View.VISIBLE : View.GONE);
        content.setVisibility(t == content ? View.VISIBLE : View.GONE);
    }

    // Helper to localize titles
    private static String pick(Map<String, String> map) {
        if (map == null) return "";
        String lang = Locale.getDefault().getLanguage();
        if (map.containsKey(lang)) return map.get(lang);
        if (map.containsKey("ar")) return map.get("ar");
        if (map.containsKey("en")) return map.get("en");
        return map.values().stream().findFirst().orElse("");
    }

    private boolean canPublishArticles() {
        String role = new TokenStore(requireContext()).getRole();
        return "specialist".equalsIgnoreCase(role)
                || "admin".equalsIgnoreCase(role)
                || "organization".equalsIgnoreCase(role);
    }

    private void openPublishDialog() {
        View dialog = LayoutInflater.from(requireContext()).inflate(R.layout.view_article_editor, null, false);
        TextInputEditText etTitle = dialog.findViewById(R.id.etArticleTitle);
        MaterialAutoCompleteTextView etCategory = dialog.findViewById(R.id.etArticleCategory);
        TextInputEditText etBody = dialog.findViewById(R.id.etArticleBody);
        MaterialSwitch switchPublish = dialog.findViewById(R.id.switchPublish);
        if (switchPublish != null) {
            switchPublish.setChecked(true);
        }
        final List<LibraryModels.Category> cats = lastCategories != null ? lastCategories : new ArrayList<>();
        final List<String> labels = new ArrayList<>();
        for (LibraryModels.Category c : cats) {
            String label = pick(c.title);
            if (label == null || label.trim().isEmpty()) {
                label = "Category " + c.id;
            }
            labels.add(label);
        }
        if (etCategory != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, labels);
            etCategory.setAdapter(adapter);
            if (!labels.isEmpty()) {
                etCategory.setText(labels.get(0), false);
            }
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.action_publish_article)
                .setView(dialog)
                .setPositiveButton(R.string.publish, (d, w) -> {
                    String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
                    String body = etBody.getText() != null ? etBody.getText().toString().trim() : "";
                    boolean publish = switchPublish != null && switchPublish.isChecked();
                    int categoryId = 0;
                    if (etCategory != null && !labels.isEmpty()) {
                        String selected = etCategory.getText() != null ? etCategory.getText().toString().trim() : "";
                        int idx = labels.indexOf(selected);
                        if (idx < 0) idx = 0;
                        if (idx < cats.size()) {
                            categoryId = cats.get(idx).id;
                        }
                    }
                    if (title.isEmpty() || body.isEmpty()) {
                        android.widget.Toast.makeText(requireContext(), R.string.error_required_fields, android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }
                    repo.createArticle(title, body, publish, categoryId, new LibraryRepository.CreateListener() {
                        @Override public void onSuccess() {
                            int msg = publish ? R.string.article_publish_success : R.string.article_draft_saved;
                            android.widget.Toast.makeText(requireContext(), msg, android.widget.Toast.LENGTH_SHORT).show();
                            showingCurated = false;
                            vm.load(selectedTag);
                        }
                        @Override public void onError(Throwable t) {
                            String detail = t != null && t.getMessage() != null ? t.getMessage() : "";
                            android.widget.Toast.makeText(requireContext(),
                                    getString(R.string.article_publish_error) + (detail.isEmpty() ? "" : " (" + detail + ")"),
                                    android.widget.Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    // ==============================
    // Adapter for displaying articles
    // ==============================
    static class CategoriesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_HEADER = 0;
        private static final int TYPE_ARTICLE = 1;
        private final List<Object> rows = new ArrayList<>();
        private final Fragment host;

        CategoriesAdapter(Fragment host) {
            this.host = host;
        }

        void submit(List<LibraryModels.Category> cats) {
            rows.clear();
            if (cats != null) {
                boolean singleCategory = cats.size() == 1;
                for (LibraryModels.Category c : cats) {
                    String title = pick(c.title);
                    // Show section header only for curated/special lists or multi-category feeds.
                    if (!singleCategory && title != null && !title.trim().isEmpty()) {
                        rows.add(title);
                    } else if (singleCategory && c.id == -1 && title != null && !title.trim().isEmpty()) {
                        rows.add(title);
                    }
                    if (c.articles != null) {
                        rows.addAll(c.articles);
                    }
                }
            }
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            return rows.get(position) instanceof String ? TYPE_HEADER : TYPE_ARTICLE;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_HEADER) {
                TextView tv = new TextView(parent.getContext());
                int padH = (int) (4 * parent.getResources().getDisplayMetrics().density);
                int padV = (int) (14 * parent.getResources().getDisplayMetrics().density);
                tv.setPadding(padH, padV, padH, padH);
                tv.setTextSize(13f);
                tv.setLetterSpacing(0.02f);
                tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
                tv.setTextColor(Color.parseColor("#5E5F5F"));
                tv.setLayoutParams(new RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
                return new HeaderVH(tv);
            }
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_article, parent, false);
            return new ArticleVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int i) {
            if (holder instanceof HeaderVH) {
                ((HeaderVH) holder).title.setText(String.valueOf(rows.get(i)));
                return;
            }
            ArticleVH h = (ArticleVH) holder;
            LibraryModels.ArticleListItem a = (LibraryModels.ArticleListItem) rows.get(i);
            h.title.setText(pick(a.title));
            h.author.setText(buildAuthorLine(h.itemView, a.author_name, a.author_title));
            h.meta.setText(a.duration != null ? a.duration : ("video".equalsIgnoreCase(a.type)
                    ? h.itemView.getContext().getString(R.string.library_type_video)
                    : (a.type != null ? a.type : "")));
            bindCover(h, a);

            h.itemView.setOnClickListener(v -> {
                if (host == null || !host.isAdded() || a == null || a.id <= 0) return;
                Bundle b = new Bundle();
                b.putInt("articleId", a.id);
                try {
                    NavHostFragment.findNavController(host).navigate(R.id.articleFragment, b);
                } catch (IllegalArgumentException | IllegalStateException e) {
                    com.brightpath.sanad.feature.home.AppNavigator.go(host, R.id.articleFragment, b);
                }
            });
        }

        private void bindCover(ArticleVH h, LibraryModels.ArticleListItem a) {
            String raw = a.thumbnail != null && !a.thumbnail.isEmpty() ? a.thumbnail : a.image;
            String url = AppConfig.storageUrl(raw);
            if (url == null || url.isEmpty()) {
                h.coverFallback.setVisibility(View.VISIBLE);
                h.cover.setImageDrawable(null);
                return;
            }
            h.coverFallback.setVisibility(View.GONE);
            Glide.with(h.cover.getContext())
                    .load(url)
                    .centerCrop()
                    .placeholder(R.drawable.bg_library_cover)
                    .error(R.drawable.bg_library_cover)
                    .into(h.cover);
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }

        static class HeaderVH extends RecyclerView.ViewHolder {
            final TextView title;
            HeaderVH(@NonNull TextView v) {
                super(v);
                title = v;
            }
        }

        static class ArticleVH extends RecyclerView.ViewHolder {
            TextView title, author, meta;
            android.widget.ImageView cover, coverFallback;

            ArticleVH(@NonNull View v) {
                super(v);
                title = v.findViewById(R.id.tvTitle);
                author = v.findViewById(R.id.tvAuthor);
                meta = v.findViewById(R.id.tvMeta);
                cover = v.findViewById(R.id.imgCover);
                coverFallback = v.findViewById(R.id.imgCoverFallback);
            }
        }

        private String buildAuthorLine(View v, String name, String title) {
            String safeName = name != null ? name.trim() : "";
            String safeTitle = title != null ? title.trim() : "";
            if (safeName.isEmpty()) {
                return v.getContext().getString(R.string.library_author_placeholder);
            }
            if (safeTitle.isEmpty()) {
                return v.getContext().getString(R.string.library_author_format, safeName);
            }
            return v.getContext().getString(R.string.library_author_with_title, safeName, safeTitle);
        }
    }
}
