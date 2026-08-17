package com.brightpath.sanad.feature.specialist;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.brightpath.sanad.R;

public class SpecialistInfoFragment extends Fragment {
  private SpecialistViewModels.ProfileVM vm;
  private View progress, content, error;
  private TextView tvName, tvSpecialty, tvYears, tvRate, tvCurrency, tvLanguages, tvStatus, tvNotes, tvBio, tvError;
  private android.widget.ImageView imgAvatar;

  @Nullable @Override public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s){
    return i.inflate(R.layout.fragment_specialist_info, c, false);
  }

  @Override public void onViewCreated(@NonNull View v, @Nullable Bundle s){
    super.onViewCreated(v, s);
    progress = v.findViewById(R.id.progress);
    content = v.findViewById(R.id.content);
    error = v.findViewById(R.id.errorContainer);
    tvError = v.findViewById(R.id.tvError);
    tvName = v.findViewById(R.id.tvName);
    tvSpecialty = v.findViewById(R.id.tvSpecialty);
    tvYears = v.findViewById(R.id.tvYears);
    tvRate = v.findViewById(R.id.tvRate);
    tvCurrency = v.findViewById(R.id.tvCurrency);
    tvLanguages = v.findViewById(R.id.tvLanguages);
    tvStatus = v.findViewById(R.id.tvStatus);
    tvNotes = v.findViewById(R.id.tvNotes);
    tvBio = v.findViewById(R.id.tvBio);
    imgAvatar = v.findViewById(R.id.imgAvatar);

    vm = new ViewModelProvider(this).get(SpecialistViewModels.ProfileVM.class);
    vm.state.observe(getViewLifecycleOwner(), p -> {
      if (p == null){
        show(error); return;
      }
      show(content);
      tvName.setText(!TextUtils.isEmpty(p.name) ? p.name : getString(R.string.profile_name_placeholder));
      tvSpecialty.setText(safe(p.specialty));
      tvYears.setText(p.years_exp != null ? String.valueOf(p.years_exp) : getString(R.string.specialist_not_set));
      int rateCents = p.rate_cents != null ? p.rate_cents : 0;
      if (rateCents > 0) {
        String curr = !TextUtils.isEmpty(p.currency) ? p.currency.toUpperCase() : "USD";
        tvRate.setText(getString(R.string.specialist_rate_format, curr, (rateCents / 100f)));
      } else {
        tvRate.setText(getString(R.string.specialist_not_set));
      }
      String accepting = SpecialistModels.isAccepting(p.accepting_new)
              ? getString(R.string.specialist_accepting_yes)
              : getString(R.string.specialist_accepting_no);
      tvCurrency.setText(getString(R.string.specialist_accepting_label, accepting));
      if (p.languages != null && !p.languages.isEmpty()) {
        tvLanguages.setText(TextUtils.join(" • ", p.languages));
      } else {
        tvLanguages.setText(getString(R.string.not_available));
      }
      if (tvBio != null) {
        if (p.bio != null && !p.bio.isEmpty()) {
          String bio = p.bio.get("ar");
          if (bio == null && !p.bio.isEmpty()) bio = p.bio.values().iterator().next();
          tvBio.setText(bio != null ? bio : getString(R.string.not_available));
        } else {
          tvBio.setText(getString(R.string.not_available));
        }
      }
      if (imgAvatar != null) {
        imgAvatar.setVisibility(View.GONE);
      }
      tvStatus.setText(statusLabel(p.status));
      if (!TextUtils.isEmpty(p.verification_notes)){
        tvNotes.setVisibility(View.VISIBLE);
        tvNotes.setText(p.verification_notes);
      } else {
        tvNotes.setVisibility(View.GONE);
      }
    });
    vm.error.observe(getViewLifecycleOwner(), msg -> {
      if (msg != null && !msg.isEmpty()) {
        if (tvError != null) tvError.setText(msg);
        show(error);
      }
    });
    show(progress);
    vm.load();
  }

  private void show(View t){
    if (content != null) content.setVisibility(t==content?View.VISIBLE:View.GONE);
    if (progress != null) progress.setVisibility(t==progress?View.VISIBLE:View.GONE);
    if (error != null) error.setVisibility(t==error?View.VISIBLE:View.GONE);
  }

  private String safe(String v){ return v!=null && !v.isEmpty()? v : getString(R.string.not_available); }

  private String statusLabel(@Nullable String status){
    if ("approved".equalsIgnoreCase(status)) return getString(R.string.specialist_verification_approved);
    if ("rejected".equalsIgnoreCase(status)) return getString(R.string.specialist_verification_rejected);
    if ("under_review".equalsIgnoreCase(status)) return getString(R.string.specialist_verification_under_review);
    return getString(R.string.specialist_verification_pending);
  }
}
