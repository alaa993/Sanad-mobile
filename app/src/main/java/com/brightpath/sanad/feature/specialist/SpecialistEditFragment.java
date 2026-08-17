package com.brightpath.sanad.feature.specialist;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.brightpath.sanad.R;
import java.util.HashMap;
import java.util.Map;

public class SpecialistEditFragment extends Fragment {
  private SpecialistViewModels.ProfileVM vm;
  private SpecialistViewModels.DocumentsVM docsVm;
  private ActivityResultLauncher<String> docPicker;
  private String pendingDocType;
  private ActivityResultLauncher<String> avatarPicker;
  private android.net.Uri pendingAvatar;
  private String currentAvatarUrl;
  private SpecialistProfileFragment.DocumentsAdapter docAdapter;
  private TextView tvStatus, tvNotes, tvDocsEmpty;
  private EditText etSpec, etYears, etRate, etCurrency;
  private EditText etLanguages;
  private ImageView imgAvatar;
  private TextView tvAvatarHint;
  private Switch swAccepting;

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    docPicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
      if (uri != null && pendingDocType != null) {
        docsVm.upload(pendingDocType, uri, new SpecialistRepository.Cb<SpecialistModels.Document>() {
          @Override public void ok(SpecialistModels.Document t) { pendingDocType = null; Toast.makeText(requireContext(), R.string.specialist_doc_upload_success, Toast.LENGTH_SHORT).show(); }
          @Override public void err(Throwable e) { pendingDocType = null; Toast.makeText(requireContext(), R.string.specialist_doc_upload_error, Toast.LENGTH_LONG).show(); }
        });
      } else {
        pendingDocType = null;
      }
    });
    avatarPicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
      if (uri != null) {
        pendingAvatar = uri;
        renderAvatarPreview(uri);
      }
    });
  }

  @Nullable @Override public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s){
    return i.inflate(R.layout.fragment_specialist_edit, c, false);
  }

  @Override public void onViewCreated(@NonNull View v, @Nullable Bundle s){
    super.onViewCreated(v, s);
    etSpec = v.findViewById(R.id.etSpecialty);
    etYears = v.findViewById(R.id.etYears);
    etRate = v.findViewById(R.id.etRate);
    etCurrency = v.findViewById(R.id.etCurrency);
    etLanguages = v.findViewById(R.id.etLanguages);
    imgAvatar = v.findViewById(R.id.imgAvatar);
    tvAvatarHint = v.findViewById(R.id.tvAvatarHint);
    swAccepting = v.findViewById(R.id.swAccepting);
    tvStatus = v.findViewById(R.id.tvVerificationStatus);
    tvNotes = v.findViewById(R.id.tvVerificationNotes);
    tvDocsEmpty = v.findViewById(R.id.tvDocsEmpty);
    View btnUpload = v.findViewById(R.id.btnUploadDocument);
    View btnSave = v.findViewById(R.id.btnSave);
    View btnPickAvatar = v.findViewById(R.id.btnPickAvatar);

    RecyclerView rvDocs = v.findViewById(R.id.rvDocuments);
    rvDocs.setLayoutManager(new LinearLayoutManager(requireContext()));
    docAdapter = new SpecialistProfileFragment.DocumentsAdapter(id -> docsVm.delete(id));
    rvDocs.setAdapter(docAdapter);

    vm = new ViewModelProvider(this).get(SpecialistViewModels.ProfileVM.class);
    docsVm = new ViewModelProvider(this).get(SpecialistViewModels.DocumentsVM.class);

    vm.state.observe(getViewLifecycleOwner(), p -> {
      if (p == null) return;
      etSpec.setText(p.specialty==null?"":p.specialty);
      etYears.setText(p.years_exp != null ? String.valueOf(p.years_exp) : "");
      etRate.setText(p.rate_cents != null ? String.valueOf(p.rate_cents) : "");
      etCurrency.setText(p.currency);
      swAccepting.setChecked(SpecialistModels.isAccepting(p.accepting_new));
      currentAvatarUrl = p.avatar;
      if (p.languages != null && !p.languages.isEmpty()) {
        etLanguages.setText(TextUtils.join(", ", p.languages));
      }
      if (tvAvatarHint != null && p.requires_avatar) {
        tvAvatarHint.setText(getString(R.string.specialist_avatar_required));
        tvAvatarHint.setTextColor(getResources().getColor(R.color.sanad_error));
      }
      renderAvatarPreview(null);
      if (tvStatus != null) tvStatus.setText(statusLabel(p.status));
    });
    vm.toast.observe(getViewLifecycleOwner(), msg -> {
      if (!TextUtils.isEmpty(msg)) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
        if (msg.contains("تم حفظ")) {
          androidx.navigation.fragment.NavHostFragment.findNavController(this)
              .navigate(R.id.specialistHomeFragment);
        }
      }
    });

    docsVm.state.observe(getViewLifecycleOwner(), docs -> {
      if (docs != null) {
        if (!TextUtils.isEmpty(docs.verification_notes)) {
          tvNotes.setVisibility(View.VISIBLE);
          tvNotes.setText(docs.verification_notes);
        } else {
          tvNotes.setVisibility(View.GONE);
        }
        docAdapter.submit(docs.documents);
        tvDocsEmpty.setVisibility(docs.documents == null || docs.documents.isEmpty() ? View.VISIBLE : View.GONE);
      }
    });

    btnSave.setOnClickListener(x -> {
      Map<String,Object> body = new HashMap<>();
      putIfNotEmpty(body, "specialty", etSpec.getText().toString().trim());
      body.put("years_exp", safeInt(etYears.getText().toString()));
      body.put("rate_cents", safeInt(etRate.getText().toString()));
      String currency = etCurrency.getText().toString().trim();
      if (!TextUtils.isEmpty(currency)) {
        currency = currency.toUpperCase();
        if (currency.length() != 3) {
          Toast.makeText(requireContext(), R.string.currency_format_hint, Toast.LENGTH_SHORT).show();
          return;
        }
        body.put("currency", currency);
      }
      if (etLanguages != null) {
        String langsRaw = etLanguages.getText() != null ? etLanguages.getText().toString().trim() : "";
        if (!langsRaw.isEmpty()) {
          java.util.List<String> langs = new java.util.ArrayList<>();
          for (String part : langsRaw.split(",")){
            if (!TextUtils.isEmpty(part.trim())) langs.add(part.trim());
          }
          body.put("languages", langs);
        }
      }
      body.put("accepting_new", swAccepting.isChecked());
      if (pendingAvatar != null) {
        vm.uploadAvatar(pendingAvatar, new SpecialistRepository.Cb<String>() {
          @Override public void ok(String url) {
            currentAvatarUrl = url;
            pendingAvatar = null;
            vm.update(body);
            Toast.makeText(requireContext(), R.string.save, Toast.LENGTH_SHORT).show();
          }
          @Override public void err(Throwable e) {
            Toast.makeText(requireContext(), R.string.specialist_avatar_required, Toast.LENGTH_SHORT).show();
          }
        });
      } else if (TextUtils.isEmpty(currentAvatarUrl)) {
        Toast.makeText(requireContext(), R.string.specialist_avatar_required, Toast.LENGTH_SHORT).show();
      } else {
        vm.update(body);
        Toast.makeText(requireContext(), R.string.save, Toast.LENGTH_SHORT).show();
      }
    });

    if (btnPickAvatar != null) btnPickAvatar.setOnClickListener(x -> {
      pendingAvatar = null;
      if (avatarPicker != null) avatarPicker.launch("image/*");
    });
    btnUpload.setOnClickListener(x -> showTypeDialog());

    vm.load();
    docsVm.load();
  }

  private void showTypeDialog(){
    final EditText input = new EditText(requireContext());
    input.setHint(R.string.specialist_doc_type_hint);
    new android.app.AlertDialog.Builder(requireContext())
        .setTitle(R.string.specialist_upload_button)
        .setView(input)
        .setPositiveButton(android.R.string.ok, (d, w) -> {
          String type = input.getText().toString().trim();
          if (TextUtils.isEmpty(type)) type = "document";
          pendingDocType = type;
          docPicker.launch("*/*");
        })
        .setNegativeButton(android.R.string.cancel, (d,w)-> pendingDocType=null)
        .show();
  }

  private static int safeInt(String value){
    try { return Integer.parseInt(value.trim()); } catch (Exception e){ return 0; }
  }

  private String statusLabel(@Nullable String status){
    if ("approved".equalsIgnoreCase(status)) return getString(R.string.specialist_verification_approved);
    if ("rejected".equalsIgnoreCase(status)) return getString(R.string.specialist_verification_rejected);
    if ("under_review".equalsIgnoreCase(status)) return getString(R.string.specialist_verification_under_review);
    return getString(R.string.specialist_verification_pending);
  }

  private static void putIfNotEmpty(Map<String,Object> body, String key, String value){
    if (!TextUtils.isEmpty(value)) {
      body.put(key, value);
    }
  }

  private void renderAvatarPreview(@Nullable android.net.Uri local){
    if (imgAvatar == null) return;
    imgAvatar.setVisibility(View.VISIBLE);
    if (local != null) {
      Glide.with(imgAvatar.getContext())
          .load(local)
          .placeholder(R.drawable.ic_specialists)
          .circleCrop()
          .into(imgAvatar);
      return;
    }
    if (!TextUtils.isEmpty(currentAvatarUrl)) {
      Glide.with(imgAvatar.getContext())
          .load(currentAvatarUrl)
          .placeholder(R.drawable.ic_specialists)
          .circleCrop()
          .into(imgAvatar);
      return;
    }
    imgAvatar.setImageResource(R.drawable.ic_specialists);
  }
}
