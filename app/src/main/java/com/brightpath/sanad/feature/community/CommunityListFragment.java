package com.brightpath.sanad.feature.community;
import android.os.Bundle; import android.text.Editable; import android.text.TextWatcher; import android.view.*; import android.widget.EditText; import android.widget.ImageView; import android.widget.ProgressBar; import android.widget.Spinner; import android.widget.TextView; import android.widget.Toast;
import androidx.core.content.ContextCompat;
import android.widget.ArrayAdapter;
import androidx.annotation.*; import androidx.fragment.app.Fragment; import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager; import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.brightpath.sanad.data.CatalogModels;
import com.brightpath.sanad.data.CatalogRepository;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.brightpath.sanad.R; import java.util.*; import java.util.Locale;
import com.brightpath.sanad.ui.tour.CoachMarkManager;
import com.brightpath.sanad.ui.tour.CoachMarkStep;
public class CommunityListFragment extends Fragment {
  private CommunityViewModels.CommunityListVM vm; private RecyclerView rv;
  private final List<CommunityModels.Community> latest = new ArrayList<>();
  private Adapter ad;
  private ProgressBar progress;
  private TextView empty;
  private EditText search;
  private SwipeRefreshLayout swipeRefresh;
  private ChipGroup chipFilters, chipCategoryFilters;
  private int filterMode = 0;
  private String categoryFilter = null;
  private CommunityRolePolicy policy;
  @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState){ return inflater.inflate(R.layout.fragment_community_list, container, false); }
  @Override public void onViewCreated(@NonNull View v, @Nullable Bundle s){
    super.onViewCreated(v,s);
    policy = new CommunityRolePolicy(new com.brightpath.sanad.data.auth.TokenStore(requireContext()).getRole());
    TextView tvTitle = v.findViewById(R.id.tvTitle);
    TextView tvSubtitle = v.findViewById(R.id.tvSubtitle);
    if (tvTitle != null) tvTitle.setText(policy.screenTitleRes());
    if (tvSubtitle != null) tvSubtitle.setText(policy.screenSubtitleRes());
    TextView tvCtaTitle = v.findViewById(R.id.tvPublicHeading);
    TextView tvCtaSubtitle = v.findViewById(R.id.tvPublicSubtitle);
    if (tvCtaTitle != null) tvCtaTitle.setText(policy.ctaTitleRes());
    if (tvCtaSubtitle != null) tvCtaSubtitle.setText(policy.ctaSubtitleRes());
    vm = new ViewModelProvider(this).get(CommunityViewModels.CommunityListVM.class);
    rv = v.findViewById(R.id.rvCommunities); rv.setLayoutManager(new LinearLayoutManager(requireContext()));
    ad = new Adapter(this, vm, policy);
    rv.setAdapter(ad);
    progress = v.findViewById(R.id.progressBar);
    empty = v.findViewById(R.id.tvEmpty);
    search = v.findViewById(R.id.etSearch);
    swipeRefresh = v.findViewById(R.id.swipeRefresh);
    chipFilters = v.findViewById(R.id.chipFilters);
    chipCategoryFilters = v.findViewById(R.id.chipCategoryFilters);
    vm.list.observe(getViewLifecycleOwner(), d -> {
      latest.clear();
      if (d != null) latest.addAll(d);
      applyFilters();
      if (progress != null) progress.setVisibility(View.GONE);
      if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
    });
    vm.error.observe(getViewLifecycleOwner(), err -> {
      if (err != null && !err.isEmpty()) {
        Toast.makeText(requireContext(), R.string.error_load_failed, Toast.LENGTH_SHORT).show();
      }
    });
    vm.createError.observe(getViewLifecycleOwner(), err -> {
      if (err != null && !err.isEmpty()) {
        if (progress != null) progress.setVisibility(View.GONE);
        Toast.makeText(requireContext(), R.string.community_create_failed, Toast.LENGTH_SHORT).show();
      }
    });
    vm.createSuccess.observe(getViewLifecycleOwner(), ok -> {
      if (ok != null && ok) {
        if (progress != null) progress.setVisibility(View.GONE);
        Toast.makeText(requireContext(), R.string.community_create_success, Toast.LENGTH_SHORT).show();
        vm.createSuccess.setValue(null);
      }
    });
    if (progress != null) progress.setVisibility(View.VISIBLE);
    vm.load();

    if (search != null) {
      View searchLayout = v.findViewById(R.id.searchLayout);
      int searchVisibility = policy.showsSearch() ? View.VISIBLE : View.GONE;
      search.setVisibility(searchVisibility);
      if (searchLayout != null) searchLayout.setVisibility(searchVisibility);
      search.addTextChangedListener(new TextWatcher() {
        @Override public void beforeTextChanged(CharSequence s1, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s12, int start, int before, int count) { applyFilters(); }
        @Override public void afterTextChanged(Editable s13) {}
      });
    }

    if (chipFilters != null) {
      chipFilters.setVisibility(policy.showsFilters() ? View.VISIBLE : View.GONE);
      chipFilters.setOnCheckedChangeListener((group, checkedId) -> {
        if (checkedId == R.id.chipFilterJoined) filterMode = 1;
        else if (checkedId == R.id.chipFilterDiscover) filterMode = 2;
        else filterMode = 0;
        applyFilters();
      });
    }

    if (chipCategoryFilters != null) {
      chipCategoryFilters.setVisibility(policy.showsFilters() ? View.VISIBLE : View.GONE);
      loadCategoryFilters();
    }

    if (swipeRefresh != null) {
      swipeRefresh.setOnRefreshListener(() -> {
        if (progress != null) progress.setVisibility(View.VISIBLE);
        vm.load();
      });
    }

    MaterialButton btnCreateCommunity = v.findViewById(R.id.btnCreateCommunity);
    if (btnCreateCommunity != null) {
      btnCreateCommunity.setVisibility(policy.canCreateCommunity() ? View.VISIBLE : View.GONE);
      btnCreateCommunity.setOnClickListener(x -> showCreateCommunityDialog());
    }

    View rowPublicFeed = v.findViewById(R.id.rowOpenPublicFeed);
    if (rowPublicFeed != null) {
      bindActionRow(rowPublicFeed, R.drawable.ic_community,
          R.string.community_open_feed, R.string.community_open_feed_subtitle);
      rowPublicFeed.setVisibility(policy.showsPublicFeedCta() ? View.VISIBLE : View.GONE);
      rowPublicFeed.setOnClickListener(x -> openFirstCommunity());
    }

    View rowOpenVent = v.findViewById(R.id.rowOpenVent);
    if (rowOpenVent != null) {
      bindActionRow(rowOpenVent, R.drawable.ic_vent,
          R.string.community_vent_title, R.string.community_vent_feed_subtitle);
      rowOpenVent.setVisibility(policy.showsVent() ? View.VISIBLE : View.GONE);
      rowOpenVent.setOnClickListener(x -> NavHostFragment.findNavController(this).navigate(R.id.ventFragment));
    }

    View rowOpenAnonymous = v.findViewById(R.id.rowOpenAnonymous);
    if (rowOpenAnonymous != null) {
      bindActionRow(rowOpenAnonymous, R.drawable.ic_match,
          R.string.anonymous_match_title, R.string.anonymous_match_subtitle);
      rowOpenAnonymous.setVisibility(policy.showsAnonymousMatch() ? View.VISIBLE : View.GONE);
      rowOpenAnonymous.setOnClickListener(x -> NavHostFragment.findNavController(this).navigate(R.id.anonymousMatchFragment));
    }

    View rowOpenCoach = v.findViewById(R.id.rowOpenCoach);
    if (rowOpenCoach != null) {
      bindActionRow(rowOpenCoach, R.drawable.ic_coach,
          R.string.coach_title, R.string.coach_subtitle);
      rowOpenCoach.setVisibility(policy.showsCoach() ? View.VISIBLE : View.GONE);
      rowOpenCoach.setOnClickListener(x -> NavHostFragment.findNavController(this).navigate(R.id.coachFragment));
    }

    View ctaCard = v.findViewById(R.id.cardCommunityCta);
    if (ctaCard != null) {
      boolean showCta = policy.showsPublicFeedCta() || policy.showsVent()
          || policy.showsAnonymousMatch() || policy.showsCoach();
      ctaCard.setVisibility(showCta ? View.VISIBLE : View.GONE);
    }

    v.post(() -> {
      java.util.List<CoachMarkStep> steps = new java.util.ArrayList<>();
      if (search != null) steps.add(CoachMarkManager.step(search, R.string.tour_community_search_title, R.string.tour_community_search_desc));
      if (chipFilters != null) steps.add(CoachMarkManager.step(chipFilters, R.string.tour_community_filter_title, R.string.tour_community_filter_desc));
      if (rv != null) steps.add(CoachMarkManager.step(rv, R.string.tour_community_list_title, R.string.tour_community_list_desc));
      if (rowPublicFeed != null && rowPublicFeed.getVisibility() == View.VISIBLE) {
        steps.add(CoachMarkManager.step(rowPublicFeed, R.string.tour_community_public_title, R.string.tour_community_public_desc));
      }
      CoachMarkManager.showIfNeeded(CommunityListFragment.this, "tour_community", steps);
    });
  }

  private void bindActionRow(View row, int iconRes, int titleRes, int subtitleRes) {
    ImageView icon = row.findViewById(R.id.imgActionIcon);
    TextView title = row.findViewById(R.id.tvActionTitle);
    TextView subtitle = row.findViewById(R.id.tvActionSubtitle);
    if (icon != null) icon.setImageResource(iconRes);
    if (title != null) title.setText(titleRes);
    if (subtitle != null) subtitle.setText(subtitleRes);
  }

  private void loadCategoryFilters() {
    if (chipCategoryFilters == null) return;
    new CatalogRepository(requireContext()).load(new CatalogRepository.Cb() {
      @Override public void ok(CatalogModels.Catalog catalog) {
        if (!isAdded() || catalog == null || catalog.community_categories == null) return;
        chipCategoryFilters.removeAllViews();
        Chip all = new Chip(requireContext());
        all.setText(getString(R.string.community_filter_all));
        all.setCheckable(true);
        all.setChecked(categoryFilter == null);
        all.setOnClickListener(v -> {
          categoryFilter = null;
          if (progress != null) progress.setVisibility(View.VISIBLE);
          vm.load(null);
        });
        chipCategoryFilters.addView(all);
        boolean ar = Locale.getDefault().getLanguage().startsWith("ar");
        for (CatalogModels.CommunityCategory cat : catalog.community_categories) {
          if (cat == null || cat.id == null) continue;
          Chip chip = new Chip(requireContext());
          chip.setText(ar && cat.label_ar != null ? cat.label_ar : (cat.label_en != null ? cat.label_en : cat.id));
          chip.setCheckable(true);
          chip.setChecked(cat.id.equals(categoryFilter));
          String id = cat.id;
          chip.setOnClickListener(v -> {
            categoryFilter = id;
            if (progress != null) progress.setVisibility(View.VISIBLE);
            vm.load(id);
          });
          chipCategoryFilters.addView(chip);
        }
      }
      @Override public void err(Throwable t) { }
    });
  }

  private void showCreateCommunityDialog() {
    View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_community_create, null, false);
    EditText etSlug = dialogView.findViewById(R.id.etSlug);
    EditText etName = dialogView.findViewById(R.id.etName);
    EditText etAbout = dialogView.findViewById(R.id.etAbout);
    Spinner spinnerVisibility = dialogView.findViewById(R.id.spinnerVisibility);
    Spinner spinnerKind = dialogView.findViewById(R.id.spinnerKind);
    String[] visibilityValues = {"public", "private"};
    String[] visibilityLabels = {
        getString(R.string.community_visibility_public),
        getString(R.string.community_visibility_private)
    };
    String[] kindValues = {"discussion", "qa"};
    String[] kindLabels = {
        getString(R.string.community_kind_discussion),
        getString(R.string.community_kind_qa)
    };
    spinnerVisibility.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, visibilityLabels));
    spinnerKind.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, kindLabels));
    new MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.community_create)
        .setView(dialogView)
        .setPositiveButton(R.string.save, (d, w) -> {
          String slug = etSlug.getText() != null ? etSlug.getText().toString().trim() : "";
          String name = etName.getText() != null ? etName.getText().toString().trim() : "";
          if (slug.isEmpty() || name.isEmpty()) {
            Toast.makeText(requireContext(), R.string.login_error_required_fields, Toast.LENGTH_SHORT).show();
            return;
          }
          String about = etAbout.getText() != null ? etAbout.getText().toString().trim() : "";
          String visibility = visibilityValues[spinnerVisibility.getSelectedItemPosition()];
          String kind = kindValues[spinnerKind.getSelectedItemPosition()];
          if (progress != null) progress.setVisibility(View.VISIBLE);
          vm.create(slug, name, about, visibility, kind);
        })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void openFirstCommunity(){
    CommunityModels.Community c = findFirstPublic();
    if (c == null) {
      Toast.makeText(requireContext(), R.string.community_list_title, Toast.LENGTH_SHORT).show();
      return;
    }
    Bundle b = buildBundle(c);
    NavHostFragment.findNavController(this).navigate(R.id.communityFeedFragment, b);
  }

  private CommunityModels.Community findFirstPublic(){
    List<CommunityModels.Community> base = policy != null ? policy.filter(latest) : latest;
    for (CommunityModels.Community candidate : base){
      if (candidate==null || candidate.slug==null) continue;
      return candidate;
    }
    return !base.isEmpty()? base.get(0) : null;
  }

  private Bundle buildBundle(CommunityModels.Community selected){
    Bundle b = new Bundle();
    String title = preferLocalized(selected.name, selected.slug);
    b.putInt("communityId", selected.id);
    b.putString("communityTitle", title);
    b.putString("communitySlug", selected.slug);
    CommunityModels.Community general = findFirstPublic();
    if (general != null){
      b.putInt("publicCommunityId", general.id);
      b.putString("publicCommunityTitle", preferLocalized(general.name, general.slug));
      b.putString("publicCommunitySlug", general.slug);
    }
    return b;
  }

  private void applyFilters(){
    if (ad == null) return;
    List<CommunityModels.Community> filtered = new ArrayList<>();
    List<CommunityModels.Community> base = policy != null ? policy.filter(latest) : latest;
    String query = search != null && search.getText() != null ? search.getText().toString().trim().toLowerCase(Locale.US) : "";
    for (CommunityModels.Community c : base) {
      if (c == null) continue;
      boolean matchJoined = filterMode == 0 || (filterMode == 1 && c.joined) || (filterMode == 2 && !c.joined);
      if (!matchJoined) continue;
      if (!query.isEmpty()) {
        String title = preferLocalized(c.name, c.slug != null ? c.slug : "").toLowerCase(Locale.US);
        String about = preferLocalized(c.about, "").toLowerCase(Locale.US);
        if (!title.contains(query) && !about.contains(query)) continue;
      }
      filtered.add(c);
    }
    ad.submit(filtered);
    if (empty != null) empty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
  }
  static class Adapter extends RecyclerView.Adapter<Adapter.VH> {
    private final Fragment host;
    private final CommunityViewModels.CommunityListVM vm;
    private final CommunityRolePolicy policy;
    Adapter(Fragment host, CommunityViewModels.CommunityListVM vm, CommunityRolePolicy policy){
      this.host = host; this.vm = vm; this.policy = policy;
    }
    private final List<CommunityModels.Community> data = new ArrayList<>();
    void submit(List<CommunityModels.Community> d){ data.clear(); if(d!=null) data.addAll(d); notifyDataSetChanged(); }
    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int v){ return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_community, p, false)); }
    @Override public void onBindViewHolder(@NonNull VH h, int i){
      CommunityModels.Community c = data.get(i);
      String title = preferLocalized(c.name, c.slug);
      String about = preferLocalized(c.about, "");
      h.title.setText(title);
      boolean showAbout = about != null && about.trim().length() > 1 && !about.trim().equalsIgnoreCase(title.trim());
      if (showAbout) {
        h.meta.setText(about);
        h.meta.setVisibility(View.VISIBLE);
      } else {
        h.meta.setVisibility(View.GONE);
      }
      h.members.setText(h.itemView.getContext().getString(R.string.community_members_count, c.members_count));
      boolean joined = c.joined;
      h.status.setText(joined ? h.itemView.getContext().getString(R.string.community_status_joined) : h.itemView.getContext().getString(R.string.community_status_discover));
      h.action.setText(joined ? R.string.community_leave : R.string.community_join);
      if (joined) {
        h.action.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
            ContextCompat.getColor(h.itemView.getContext(), R.color.sanad_surface)));
        h.action.setTextColor(ContextCompat.getColor(h.itemView.getContext(), R.color.sanad_error));
        h.action.setStrokeColor(android.content.res.ColorStateList.valueOf(
            ContextCompat.getColor(h.itemView.getContext(), R.color.sanad_error)));
        h.action.setStrokeWidth((int) (1 * h.itemView.getResources().getDisplayMetrics().density));
      } else {
        h.action.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
            ContextCompat.getColor(h.itemView.getContext(), R.color.sanad_primary)));
        h.action.setTextColor(ContextCompat.getColor(h.itemView.getContext(), R.color.sanad_on_primary));
        h.action.setStrokeWidth(0);
      }
      boolean showJoin = policy == null || policy.canJoinFreely();
      h.action.setVisibility(showJoin ? View.VISIBLE : View.GONE);
      h.action.setOnClickListener(v -> vm.toggle(c));
      h.itemView.setOnClickListener(v -> {
        CommunityListFragment parent = (CommunityListFragment) host;
        Bundle b = parent.buildBundle(c);
        NavHostFragment.findNavController(host).navigate(R.id.action_communityListFragment_to_communityFeedFragment, b);
      });
    }
    @Override public int getItemCount(){ return data.size(); }
    static class VH extends RecyclerView.ViewHolder {
      TextView title, meta, members, status;
      MaterialButton action;
      VH(@NonNull View v){
        super(v);
        title=v.findViewById(R.id.tvTitle);
        meta=v.findViewById(R.id.tvMeta);
        members=v.findViewById(R.id.tvMembers);
        status=v.findViewById(R.id.tvStatus);
        action=v.findViewById(R.id.btnAction);
      }
    }
  }

  static String preferLocalized(Map<String,String> map, String fallback){
    if(map==null || map.isEmpty()) return fallback!=null? fallback : "";
    String lang = Locale.getDefault().getLanguage();
    if(map.containsKey(lang)) return map.get(lang);
    if(lang != null && lang.startsWith("ar") && map.containsKey("ar")) return map.get("ar");
    if(map.containsKey("en")) return map.get("en");
    if(map.containsKey("ar")) return map.get("ar");
    return map.values().iterator().hasNext()? map.values().iterator().next() : fallback;
  }

    @Override
    public void onDestroyView() {
        try { CoachMarkManager.dismissActive(); } catch (Throwable ignored) {}
        super.onDestroyView();
    }

}
