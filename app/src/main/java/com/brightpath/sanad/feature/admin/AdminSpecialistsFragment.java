package com.brightpath.sanad.feature.admin;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.button.MaterialButton;
import androidx.navigation.fragment.NavHostFragment;

import com.brightpath.sanad.R;
import com.brightpath.sanad.data.AppConfig;

import java.util.ArrayList;
import java.util.List;

public class AdminSpecialistsFragment extends Fragment {
  private AdminViewModels.SpecListVM vm;
  private ProgressBar progress;
  private TextView tvError,tvEmpty;
  private SwipeRefreshLayout swipe;
  private SpecAdapter adapter;

  @Nullable @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_admin_specialists, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
    super.onViewCreated(v, s);
    progress = v.findViewById(R.id.progress);
    tvError = v.findViewById(R.id.tvError);
    tvEmpty = v.findViewById(R.id.tvEmpty);
    swipe = v.findViewById(R.id.swipe);

    RecyclerView rv = v.findViewById(R.id.rv);
    rv.setLayoutManager(new LinearLayoutManager(requireContext()));
    adapter = new SpecAdapter(new SpecAdapter.Listener() {
      @Override public void onApprove(int id) { vm.approve(id); }
      @Override public void onReject(int id) {
        AdminRejectDialog.show(requireContext(), reason -> vm.reject(id, reason));
      }
      @Override public void onReview(AdminModels.Specialists.Specialist specialist) { fetchDocuments(specialist); }
      @Override public void onSessions(AdminModels.Specialists.Specialist specialist) { openSessions(specialist); }
    });
    rv.setAdapter(adapter);
    vm = new ViewModelProvider(this).get(AdminViewModels.SpecListVM.class);
    vm.state().observe(getViewLifecycleOwner(), this::renderState);
    swipe.setOnRefreshListener(() -> vm.load());
    MaterialButton btnCreate = v.findViewById(R.id.btnCreateSpecialist);
    if (btnCreate != null) btnCreate.setOnClickListener(x -> showCreateSpecialistDialog());
    vm.load();
  }

  private void showCreateSpecialistDialog() {
    View form = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_specialist, null, false);
    EditText etName = form.findViewById(R.id.etName);
    EditText etEmail = form.findViewById(R.id.etEmail);
    EditText etPassword = form.findViewById(R.id.etPassword);
    EditText etPhone = form.findViewById(R.id.etPhone);
    EditText etSpecialty = form.findViewById(R.id.etSpecialty);
    new AlertDialog.Builder(requireContext())
        .setTitle(R.string.admin_create_specialist)
        .setView(form)
        .setPositiveButton(R.string.save, (d, w) -> {
          String name = etName.getText() != null ? etName.getText().toString().trim() : "";
          String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
          String password = etPassword.getText() != null ? etPassword.getText().toString() : "";
          if (name.isEmpty() || email.isEmpty() || password.length() < 6) {
            Toast.makeText(requireContext(), R.string.error_required_fields, Toast.LENGTH_SHORT).show();
            return;
          }
          java.util.Map<String, Object> body = new java.util.HashMap<>();
          body.put("name", name);
          body.put("email", email);
          body.put("password", password);
          String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
          if (!phone.isEmpty()) body.put("phone", phone);
          String specialty = etSpecialty.getText() != null ? etSpecialty.getText().toString().trim() : "";
          if (!specialty.isEmpty()) body.put("specialty", specialty);
          vm.createSpecialist(body);
        })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void openSessions(AdminModels.Specialists.Specialist specialist){
    Bundle args = new Bundle();
    args.putInt("specialistId", specialist.id);
    args.putString("specialistName", specialist.name);
    androidx.navigation.fragment.NavHostFragment.findNavController(this)
        .navigate(R.id.adminSessionsFragment, args);
  }

  private void renderState(AdminViewModels.ListState<AdminModels.Specialists.Specialist> state){
    if(state==null) return;
    progress.setVisibility(state.loading ? View.VISIBLE : View.GONE);
    swipe.setRefreshing(false);
    if(state.error!=null){
      tvError.setText(state.error);
      tvError.setVisibility(View.VISIBLE);
    } else tvError.setVisibility(View.GONE);
    adapter.submit(state.data);
    boolean isEmpty = !state.loading && (state.data==null || state.data.isEmpty());
    tvEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
  }

  private void fetchDocuments(AdminModels.Specialists.Specialist specialist) {
    Toast.makeText(requireContext(), R.string.admin_review_loading, Toast.LENGTH_SHORT).show();
    vm.documents(specialist.id, new AdminRepository.Cb<AdminModels.SpecialistDocuments>() {
      @Override public void ok(AdminModels.SpecialistDocuments t) {
        requireActivity().runOnUiThread(() -> showReviewDialog(specialist, t));
      }

      @Override public void err(Throwable e) {
        requireActivity().runOnUiThread(() ->
            Toast.makeText(requireContext(), R.string.admin_review_fetch_error, Toast.LENGTH_LONG).show()
        );
      }
    });
  }

  private void showReviewDialog(AdminModels.Specialists.Specialist specialist, AdminModels.SpecialistDocuments payload) {
    View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_admin_specialist_review, null);
    TextView tvStatus = dialogView.findViewById(R.id.tvCurrentStatus);
    TextView tvExistingNotes = dialogView.findViewById(R.id.tvExistingNotes);
    TextView tvDocsLabel = dialogView.findViewById(R.id.tvReviewDocsLabel);
    android.widget.EditText etNotes = dialogView.findViewById(R.id.etReviewNotes);
    LinearLayout container = dialogView.findViewById(R.id.docContainer);

    tvStatus.setText(getString(R.string.specialist_status_heading) + ": " + statusLabel(payload.status));
    if (TextUtils.isEmpty(payload.verification_notes)) {
      tvExistingNotes.setVisibility(View.GONE);
    } else {
      tvExistingNotes.setVisibility(View.VISIBLE);
      tvExistingNotes.setText(payload.verification_notes);
    }

    List<CheckBox> checkBoxes = new ArrayList<>();
    if (payload.documents != null && !payload.documents.isEmpty()) {
      for (AdminModels.Document doc : payload.documents) {
        View row = LayoutInflater.from(requireContext()).inflate(R.layout.item_admin_document_row, container, false);
        CheckBox cb = row.findViewById(R.id.cbVerify);
        TextView tvPath = row.findViewById(R.id.tvDocPath);
        Button btnOpen = row.findViewById(R.id.btnOpenDoc);
        cb.setText((doc.type != null ? doc.type : "document") + (doc.verified_at != null ? " · ✓" : ""));
        tvPath.setText(doc.meta != null ? doc.meta.original_name : doc.file_path);
        cb.setTag(doc.id);
        btnOpen.setOnClickListener(x -> openDocument(doc.file_path));
        container.addView(row);
        checkBoxes.add(cb);
      }
    } else {
      tvDocsLabel.setText(R.string.specialist_doc_list_empty);
    }

    AlertDialog dialog = new AlertDialog.Builder(requireContext())
        .setTitle(specialist.name)
        .setView(dialogView)
        .setPositiveButton(R.string.admin_review_approve, null)
        .setNegativeButton(R.string.admin_review_reject, null)
        .setNeutralButton(android.R.string.cancel, null)
        .create();

    dialog.setOnShowListener(d -> {
      Button approveBtn = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
      Button rejectBtn = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
      approveBtn.setOnClickListener(x -> submitReview(dialog, specialist.id, "approved", etNotes.getText().toString().trim(), checkBoxes));
      rejectBtn.setOnClickListener(x -> submitReview(dialog, specialist.id, "rejected", etNotes.getText().toString().trim(), checkBoxes));
    });

    dialog.show();
  }

  private void submitReview(AlertDialog dialog, int specialistId, String status, String notes, List<CheckBox> checkBoxes) {
    AdminModels.ReviewRequest body = new AdminModels.ReviewRequest();
    body.status = status;
    body.notes = notes;
    List<Integer> verified = new ArrayList<>();
    for (CheckBox cb : checkBoxes) {
      if (cb.isChecked() && cb.getTag() instanceof Integer) {
        verified.add((Integer) cb.getTag());
      }
    }
    body.verified_documents = verified;
    vm.review(specialistId, body, new AdminRepository.Cb<AdminModels.Toggle>() {
      @Override public void ok(AdminModels.Toggle t) {
        dialog.dismiss();
        Toast.makeText(requireContext(), R.string.admin_review_complete, Toast.LENGTH_SHORT).show();
      }

      @Override public void err(Throwable e) {
        Toast.makeText(requireContext(), R.string.admin_review_fetch_error, Toast.LENGTH_LONG).show();
      }
    });
  }

  private void openDocument(@Nullable String path) {
    if (TextUtils.isEmpty(path)) return;
    String url = AppConfig.storageUrl(path);
    if (url == null) return;
    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
    try {
      startActivity(intent);
    } catch (Exception ignored) { }
  }

  private String statusLabel(@Nullable String status) {
    if ("approved".equalsIgnoreCase(status)) return getString(R.string.specialist_verification_approved);
    if ("rejected".equalsIgnoreCase(status)) return getString(R.string.specialist_verification_rejected);
    if ("under_review".equalsIgnoreCase(status)) return getString(R.string.specialist_verification_under_review);
    return getString(R.string.specialist_verification_pending);
  }

  static class SpecAdapter extends RecyclerView.Adapter<SpecAdapter.VH> {
    interface Listener {
      void onApprove(int id);
      void onReject(int id);
      void onReview(AdminModels.Specialists.Specialist specialist);
      void onSessions(AdminModels.Specialists.Specialist specialist);
    }

    private final List<AdminModels.Specialists.Specialist> data = new ArrayList<>();
    private final Listener listener;
    SpecAdapter(Listener listener) { this.listener = listener; }
    void submit(List<AdminModels.Specialists.Specialist> entries) {
      data.clear();
      if (entries != null) data.addAll(entries);
      notifyDataSetChanged();
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_specialist, parent, false);
      return new VH(view);
    }

    @Override public void onBindViewHolder(@NonNull VH holder, int position) {
      AdminModels.Specialists.Specialist spec = data.get(position);
      holder.title.setText(spec.name + " · " + (spec.specialty != null ? spec.specialty : "-"));
      holder.meta.setText("status: " + (spec.status != null ? spec.status : "-"));
      holder.btnApprove.setOnClickListener(x -> listener.onApprove(spec.id));
      holder.btnReject.setOnClickListener(x -> listener.onReject(spec.id));
      holder.btnReview.setOnClickListener(x -> listener.onReview(spec));
      holder.btnSessions.setOnClickListener(x -> listener.onSessions(spec));
    }

    @Override public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
      TextView title, meta;
      Button btnApprove, btnReject, btnReview, btnSessions;
      VH(@NonNull View itemView) {
        super(itemView);
        title = itemView.findViewById(R.id.tvTitle);
        meta = itemView.findViewById(R.id.tvMeta);
        btnApprove = itemView.findViewById(R.id.btnApprove);
        btnReject = itemView.findViewById(R.id.btnReject);
        btnReview = itemView.findViewById(R.id.btnReview);
        btnSessions = itemView.findViewById(R.id.btnSessions);
      }
    }
  }
}
