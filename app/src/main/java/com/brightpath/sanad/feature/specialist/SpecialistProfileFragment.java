package com.brightpath.sanad.feature.specialist;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.brightpath.sanad.R;
import com.brightpath.sanad.data.AppConfig;
import com.brightpath.sanad.data.LanguageUiHelper;
import com.brightpath.sanad.data.ThemeStore;
import com.brightpath.sanad.data.auth.AuthRepository;
import com.brightpath.sanad.ui.ChangePasswordDialogHelper;
import com.brightpath.sanad.push.PushPreferencesBinder;
import com.brightpath.sanad.push.PushRegistrar;
import com.brightpath.sanad.ui.LoginActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SpecialistProfileFragment extends Fragment {
  private SpecialistViewModels.ProfileVM vm;
  private TextView tvName, tvEmail, tvRole, tvStatus, tvSpecialty, tvYears, tvRate, tvAccepting, tvLanguages, tvNotes;
  private TextView tvHeaderName, tvHeaderRole, tvHeaderStatus, tvInitial, tvAccountStatus;
  private ImageView imgAvatar;
  private View rowRole;
  private View content;
  private View progress;

  @Nullable @Override public View onCreateView(@NonNull LayoutInflater i,@Nullable ViewGroup c,@Nullable Bundle s){ return i.inflate(R.layout.fragment_specialist_profile, c, false); }
  @Override public void onViewCreated(@NonNull View v,@Nullable Bundle s){
    super.onViewCreated(v,s);
    content = v.findViewById(R.id.content);
    progress = v.findViewById(R.id.progress);
    if (progress != null) progress.setVisibility(View.VISIBLE);
    if (content != null) content.setVisibility(View.INVISIBLE);
    tvStatus = v.findViewById(R.id.tvHeaderStatus);
    tvAccountStatus = v.findViewById(R.id.tvAccountStatus);
    tvHeaderName = v.findViewById(R.id.tvHeaderName);
    tvHeaderRole = v.findViewById(R.id.tvHeaderRole);
    tvHeaderStatus = v.findViewById(R.id.tvHeaderStatus);
    tvInitial = v.findViewById(R.id.tvInitial);
    imgAvatar = v.findViewById(R.id.imgAvatar);
    rowRole = v.findViewById(R.id.rowRole);

    View rowEditInfo = v.findViewById(R.id.rowEditInfo);
    View rowSpecialistInfo = v.findViewById(R.id.rowSpecialistInfo);
    View rowChangePassword = v.findViewById(R.id.rowChangePassword);
    View rowWallet = v.findViewById(R.id.rowWallet);
    View rowContact = v.findViewById(R.id.rowContact);
    View rowPrivacy = v.findViewById(R.id.rowPrivacy);
    View btnLogout = v.findViewById(R.id.btnLogout);

    MaterialButtonToggleGroup groupLanguage = v.findViewById(R.id.groupLanguage);
    MaterialButtonToggleGroup groupTheme = v.findViewById(R.id.groupTheme);
    View btnLangArabic = v.findViewById(R.id.btnLangArabic);
    View btnLangEnglish = v.findViewById(R.id.btnLangEnglish);
    View btnLangTurkish = v.findViewById(R.id.btnLangTurkish);
    View btnThemeSanad = v.findViewById(R.id.btnThemeSanad);
    View btnThemeWardi = v.findViewById(R.id.btnThemeWardi);
    View btnThemeGraphite = v.findViewById(R.id.btnThemeGraphite);

    tvName = v.findViewById(R.id.tvName);
    tvEmail = v.findViewById(R.id.tvEmail);
    tvRole = v.findViewById(R.id.tvRole);
    if (rowRole != null) rowRole.setVisibility(View.GONE);
    tvSpecialty = v.findViewById(R.id.tvSpecialty);
    tvYears = v.findViewById(R.id.tvYears);
    tvRate = v.findViewById(R.id.tvRate);
    tvAccepting = v.findViewById(R.id.tvAccepting);
    tvLanguages = v.findViewById(R.id.tvLanguages);
    tvNotes = v.findViewById(R.id.tvNotes);

    vm=new ViewModelProvider(this).get(SpecialistViewModels.ProfileVM.class);

    vm.state.observe(getViewLifecycleOwner(), p->{ if(p!=null){
      if (progress != null) progress.setVisibility(View.GONE);
      if (content != null) content.setVisibility(View.VISIBLE);
      if (tvRole != null) {
        tvRole.setText(p.specialty!=null && !p.specialty.isEmpty()? p.specialty : getString(R.string.profile_role_placeholder));
        tvRole.setVisibility(View.GONE);
      }
      if (tvStatus != null) {
        tvStatus.setText(statusLabel(p.status));
      }
      if (tvAccountStatus != null) {
        tvAccountStatus.setText(statusLabel(p.status));
      }
      if (tvSpecialty != null) {
        tvSpecialty.setText(!TextUtils.isEmpty(p.specialty) ? p.specialty : getString(R.string.specialist_not_set));
      }
      if (tvYears != null) {
        tvYears.setText(getString(R.string.specialist_years_format, p.years_exp != null ? p.years_exp : 0));
      }
      if (tvRate != null) {
        int rateCents = p.rate_cents != null ? p.rate_cents : 0;
        if (rateCents > 0) {
          String curr = !TextUtils.isEmpty(p.currency) ? p.currency.toUpperCase(Locale.ROOT) : "USD";
          tvRate.setText(getString(R.string.specialist_rate_format, curr, (rateCents / 100f)));
        } else {
          tvRate.setText(getString(R.string.specialist_not_set));
        }
      }
      if (tvAccepting != null) {
        tvAccepting.setText(SpecialistModels.isAccepting(p.accepting_new)
                ? getString(R.string.specialist_accepting_yes)
                : getString(R.string.specialist_accepting_no));
      }
      if (tvLanguages != null) {
        if (p.languages != null && !p.languages.isEmpty()) {
          tvLanguages.setText(TextUtils.join(" · ", p.languages));
        } else {
          tvLanguages.setText(getString(R.string.specialist_not_set));
        }
      }
      if (tvNotes != null) {
        tvNotes.setText(!TextUtils.isEmpty(p.verification_notes) ? p.verification_notes : getString(R.string.specialist_not_set));
      }
      String name = p.name != null ? p.name : getString(R.string.profile_name_placeholder);
      if (tvName != null) tvName.setText(name);
      if (tvEmail != null) tvEmail.setText(!TextUtils.isEmpty(p.email) ? p.email : getString(R.string.fragment_profile_text_1));
      if (tvHeaderName != null) tvHeaderName.setText(name);
      if (tvHeaderRole != null) {
        String roleLabel = p.specialty != null && !p.specialty.isEmpty()
                ? p.specialty
                : getString(R.string.profile_role_placeholder);
        tvHeaderRole.setText(roleLabel);
        tvHeaderRole.setVisibility(View.GONE);
      }
      if (tvHeaderStatus != null) tvHeaderStatus.setText(statusLabel(p.status));
      if (tvInitial != null) {
        String initial = name.trim().isEmpty() ? "-" : name.trim().substring(0, 1).toUpperCase(Locale.ROOT);
        tvInitial.setText(initial);
      }
      bindAvatar(p.avatar);
    } });
    vm.toast.observe(getViewLifecycleOwner(), msg -> {
      if (!TextUtils.isEmpty(msg)) Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    });

    if (btnLogout != null) btnLogout.setOnClickListener(x -> confirmLogout(btnLogout));
    PushPreferencesBinder.bind(this, v);
    if (rowSpecialistInfo != null) rowSpecialistInfo.setOnClickListener(x -> safeNavigate(R.id.specialistInfoFragment));
    if (rowWallet != null) rowWallet.setOnClickListener(x -> safeNavigate(R.id.walletFragment));
    if (rowEditInfo != null) rowEditInfo.setOnClickListener(x -> safeNavigate(R.id.specialistEditFragment));
    if (rowChangePassword != null) rowChangePassword.setOnClickListener(x -> showChangePasswordDialog());
    if (rowContact != null) rowContact.setOnClickListener(x -> safeNavigate(R.id.contactUsFragment));
    if (rowPrivacy != null) rowPrivacy.setOnClickListener(x -> safeNavigate(R.id.privacyPolicyFragment));

    LanguageUiHelper.bindToggleGroup(
            this,
            groupLanguage,
            R.id.btnLangArabic,
            R.id.btnLangEnglish,
            R.id.btnLangTurkish
    );
    ThemeStore themeStore = new ThemeStore(requireContext());
    String savedTheme = themeStore.getSavedTheme();
    if (ThemeStore.THEME_PINK.equalsIgnoreCase(savedTheme) && btnThemeWardi != null) {
      if (groupTheme != null) groupTheme.check(btnThemeWardi.getId());
    } else if (ThemeStore.THEME_GRAY.equalsIgnoreCase(savedTheme) && btnThemeGraphite != null) {
      if (groupTheme != null) groupTheme.check(btnThemeGraphite.getId());
    } else if (btnThemeSanad != null) {
      if (groupTheme != null) groupTheme.check(btnThemeSanad.getId());
    }

    if (btnThemeSanad != null) {
      btnThemeSanad.setOnClickListener(v1 -> {
        if (groupTheme != null) groupTheme.check(btnThemeSanad.getId());
        applyTheme(themeStore, ThemeStore.THEME_BLUE);
      });
    }
    if (btnThemeWardi != null) {
      btnThemeWardi.setOnClickListener(v1 -> {
        if (groupTheme != null) groupTheme.check(btnThemeWardi.getId());
        applyTheme(themeStore, ThemeStore.THEME_PINK);
      });
    }
    if (btnThemeGraphite != null) {
      btnThemeGraphite.setOnClickListener(v1 -> {
        if (groupTheme != null) groupTheme.check(btnThemeGraphite.getId());
        applyTheme(themeStore, ThemeStore.THEME_GRAY);
      });
    }

    vm.load();
    paintUserMetaFromCache();
  }

  private void paintUserMetaFromCache(){
    com.brightpath.sanad.data.auth.TokenStore tokens =
            new com.brightpath.sanad.data.auth.TokenStore(requireContext());
    String name = tokens.getUserName();
    String email = tokens.getUserEmail();
    String role = tokens.getRole();
    if (name == null || name.isEmpty()) name = getString(R.string.profile_name_placeholder);
    if (tvName != null) tvName.setText(name);
    if (tvEmail != null) tvEmail.setText(email != null && !email.isEmpty() ? email : "-");
    if (tvHeaderName != null) tvHeaderName.setText(name);
    if (tvInitial != null) {
      String initial = name.trim().isEmpty() ? "-" : name.trim().substring(0, 1).toUpperCase(Locale.ROOT);
      tvInitial.setText(initial);
    }
    if (tvHeaderRole != null && role != null) tvHeaderRole.setText(role);
    if (tvRole != null && role != null && TextUtils.isEmpty(tvRole.getText())) tvRole.setText(role);
    // Show shell immediately; specialist fields fill when profile API returns.
    if (content != null) content.setVisibility(View.VISIBLE);
    if (progress != null) progress.setVisibility(View.GONE);
  }


  private void safeNavigate(int destId) {
    try {
      if (!isAdded()) return;
      com.brightpath.sanad.ui.tour.CoachMarkManager.dismissActive();
      NavHostFragment.findNavController(this).navigate(destId);
    } catch (Throwable ignored) {}
  }

  private void applyTheme(ThemeStore themeStore, String theme) {
    try {
      com.brightpath.sanad.ui.tour.CoachMarkManager.dismissActive();
      themeStore.saveTheme(theme);
      if (!isAdded()) return;
      android.app.Activity activity = getActivity();
      if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
      activity.recreate();
    } catch (Throwable ignored) {}
  }

  @Override
  public void onDestroyView() {
    try {
      com.brightpath.sanad.ui.tour.CoachMarkManager.dismissActive();
    } catch (Throwable ignored) {}
    super.onDestroyView();
  }

  private void showChangePasswordDialog(){
    ChangePasswordDialogHelper.show(requireContext(), (current, pass, confirm) -> new Thread(() -> {
      try {
        new AuthRepository(requireContext(), AppConfig.BASE_URL).updatePassword(current, pass, confirm);
        if (isAdded()) {
          requireActivity().runOnUiThread(() ->
                  Toast.makeText(requireContext(), R.string.password_updated, Toast.LENGTH_SHORT).show());
        }
      } catch (Exception e) {
        if (isAdded()) {
          requireActivity().runOnUiThread(() ->
                  Toast.makeText(requireContext(), R.string.password_update_failed, Toast.LENGTH_SHORT).show());
        }
      }
    }).start());
  }

  private void confirmLogout(View button) {
    new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.logout_confirm_title)
            .setMessage(R.string.logout_confirm_message)
            .setPositiveButton(R.string.logout, (d, w) -> performLogout(button instanceof Button ? (Button) button : null))
            .setNegativeButton(android.R.string.cancel, null)
            .show();
  }

  private void performLogout(Button button){
    if (button != null) button.setEnabled(false);
    final android.content.Context appContext = requireContext().getApplicationContext();
    new com.brightpath.sanad.data.auth.TokenStore(appContext).clear();
    Intent intent = new Intent(requireContext(), LoginActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startActivity(intent);
    new Thread(() -> {
      try {
        PushRegistrar.unregisterBeforeLogout(appContext, false);
        new AuthRepository(appContext, AppConfig.BASE_URL).logoutRemoteOnly();
      } catch (Exception ignored) {}
    }).start();
  }

  private void bindAvatar(@Nullable String avatar) {
    if (imgAvatar == null) return;
    String url = AppConfig.storageUrl(avatar);
    if (TextUtils.isEmpty(url)) {
      imgAvatar.setVisibility(View.GONE);
      Glide.with(imgAvatar.getContext()).clear(imgAvatar);
      if (tvInitial != null) tvInitial.setVisibility(View.VISIBLE);
      return;
    }
    imgAvatar.setVisibility(View.VISIBLE);
    if (tvInitial != null) tvInitial.setVisibility(View.INVISIBLE);
    Glide.with(imgAvatar.getContext())
            .load(url)
            .placeholder(R.drawable.ic_specialists)
            .error(R.drawable.ic_specialists)
            .circleCrop()
            .into(imgAvatar);
  }

  private String statusLabel(@Nullable String status){
    if ("approved".equalsIgnoreCase(status)) return getString(R.string.specialist_verification_approved);
    if ("rejected".equalsIgnoreCase(status)) return getString(R.string.specialist_verification_rejected);
    if ("under_review".equalsIgnoreCase(status)) return getString(R.string.specialist_verification_under_review);
    return getString(R.string.specialist_verification_pending);
  }

  static class DocumentsAdapter extends RecyclerView.Adapter<DocumentsAdapter.VH>{
    interface Listener{ void onRemove(int id); }
    private final List<SpecialistModels.Document> data = new ArrayList<>();
    private final Listener ls;
    DocumentsAdapter(Listener l){ this.ls = l; }
    void submit(@Nullable List<SpecialistModels.Document> docs){
      data.clear();
      if (docs != null) data.addAll(docs);
      notifyDataSetChanged();
    }
    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent,int viewType){
      View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_specialist_document, parent, false);
      return new VH(view);
    }
    @Override public void onBindViewHolder(@NonNull VH holder,int position){
      SpecialistModels.Document doc = data.get(position);
      holder.title.setText(doc.title != null ? doc.title : (doc.meta!=null?doc.meta.original_name:"Document"));
      String meta = doc.type != null ? doc.type : "";
      if (doc.verified_at != null) {
        meta += " · " + holder.title.getContext().getString(R.string.specialist_document_verified);
      }
      holder.meta.setText(meta);
      holder.remove.setOnClickListener(v -> ls.onRemove(doc.id));
    }
    @Override public int getItemCount(){ return data.size(); }
    static class VH extends RecyclerView.ViewHolder{
      TextView title, meta; Button remove;
      VH(@NonNull View itemView){
        super(itemView);
        title = itemView.findViewById(R.id.tvDocTitle);
        meta = itemView.findViewById(R.id.tvDocMeta);
        remove = itemView.findViewById(R.id.btnRemoveDoc);
      }
    }
  }
}
