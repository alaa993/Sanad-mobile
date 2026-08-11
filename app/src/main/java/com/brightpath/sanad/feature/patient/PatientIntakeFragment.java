package com.brightpath.sanad.feature.patient;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.brightpath.sanad.R;
import java.util.ArrayList;
import java.util.List;

public class PatientIntakeFragment extends Fragment {
    private PatientIntakeViewModel vm;
    private TextInputEditText etAge, etOccupation, etPrimaryIssue, etConsultNotes, etNotes;
    private TextInputLayout consultDetailsContainer;
    private SwitchMaterial switchConsult;
    private ChipGroup groupDuration, groupSymptoms, groupTriage;
    private MaterialButton btnSave;
    private AlertDialog progressDialog;
    private TextView progressDialogMessage;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_patient_intake, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        etAge = v.findViewById(R.id.etAge);
        etOccupation = v.findViewById(R.id.etOccupation);
        etPrimaryIssue = v.findViewById(R.id.etPrimaryIssue);
        etConsultNotes = v.findViewById(R.id.etConsultNotes);
        etNotes = v.findViewById(R.id.etNotes);
        consultDetailsContainer = v.findViewById(R.id.consultDetailsContainer);
        switchConsult = v.findViewById(R.id.switchConsult);
        groupDuration = v.findViewById(R.id.groupDuration);
        groupSymptoms = v.findViewById(R.id.groupSymptoms);
        groupTriage = v.findViewById(R.id.groupTriage);
        switchConsult.setOnCheckedChangeListener((buttonView, isChecked) -> {
            consultDetailsContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        btnSave = v.findViewById(R.id.btnSave);
        btnSave.setOnClickListener(x -> saveForm());

        vm = new ViewModelProvider(this).get(PatientIntakeViewModel.class);
        vm.getForm().observe(getViewLifecycleOwner(), this::bindForm);
        vm.getStatus().observe(getViewLifecycleOwner(), this::handleStatus);
    }

    private void bindForm(@Nullable PatientIntakeForm form){
        if (form == null) return;
        if (etAge != null) etAge.setText(form.age);
        if (etOccupation != null) etOccupation.setText(form.occupation);
        if (etPrimaryIssue != null) etPrimaryIssue.setText(form.primaryIssue);
        if (etNotes != null) etNotes.setText(form.notes);
        if (switchConsult != null){
            switchConsult.setChecked(form.hadConsultation);
        }
        if (consultDetailsContainer != null){
            consultDetailsContainer.setVisibility(form.hadConsultation ? View.VISIBLE : View.GONE);
        }
        if (etConsultNotes != null) etConsultNotes.setText(form.consultationNotes);
        checkChips(groupDuration, form.duration);
        checkChips(groupSymptoms, form.symptoms);
        checkChips(groupTriage, form.triageTags);
    }

    private void checkChips(@Nullable ChipGroup group, @Nullable List<String> values){
        if (group == null) return;
        for (int i = 0; i < group.getChildCount(); i++){
            View child = group.getChildAt(i);
            if (!(child instanceof Chip)) continue;
            Chip chip = (Chip) child;
            String tag = String.valueOf(chip.getTag());
            boolean shouldCheck = values != null && values.contains(tag);
            chip.setChecked(shouldCheck);
        }
    }

    private void checkChips(@Nullable ChipGroup group, @Nullable String value){
        if (group == null) return;
        for (int i = 0; i < group.getChildCount(); i++){
            View child = group.getChildAt(i);
            if (!(child instanceof Chip)) continue;
            Chip chip = (Chip) child;
            String tag = String.valueOf(chip.getTag());
            chip.setChecked(value != null && value.equals(tag));
        }
    }

    private void saveForm(){
        if (!validateForm()) return;
        PatientIntakeForm form = new PatientIntakeForm();
        form.age = textOf(etAge);
        form.occupation = textOf(etOccupation);
        form.primaryIssue = textOf(etPrimaryIssue);
        form.notes = textOf(etNotes);
        form.consultationNotes = textOf(etConsultNotes);
        form.hadConsultation = switchConsult != null && switchConsult.isChecked();
        form.duration = selectedTag(groupDuration);
        form.symptoms = selectedTags(groupSymptoms);
        form.triageTags = selectedTags(groupTriage);
        vm.save(form);
    }

    private void handleStatus(@Nullable PatientIntakeStatus status){
        if (status == null) return;
        if (status.type == PatientIntakeStatus.Type.LOADING) {
            setSaveButtonEnabled(false);
            showLoading(status.message != null ? status.message : getString(R.string.patient_intake_saving_message));
            return;
        }
        hideLoading();
        setSaveButtonEnabled(true);
        if (status.type == PatientIntakeStatus.Type.SUCCESS) {
            vm.resetStatus();
            String message = status.message != null ? status.message : getString(R.string.patient_intake_saved);
            showAlert(R.string.patient_intake_alert_title_success, message, () ->
                    androidx.navigation.fragment.NavHostFragment.findNavController(this).navigate(R.id.homeFragment));
        } else if (status.type == PatientIntakeStatus.Type.ERROR) {
            vm.resetStatus();
            String message = status.message != null ? status.message : getString(R.string.patient_intake_submit_failed);
            showAlert(R.string.patient_intake_alert_title_error, message, null);
        }
    }

    private boolean validateForm(){
        if (TextUtils.isEmpty(textOf(etPrimaryIssue))) {
            showAlert(R.string.patient_intake_alert_title_warning,
                    getString(R.string.patient_intake_primary_issue_required),
                    null);
            return false;
        }
        return true;
    }

    private void showLoading(String message) {
        if (progressDialog == null) {
            View dialogContent = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_progress, null);
            progressDialogMessage = dialogContent.findViewById(R.id.progressMessage);
            progressDialog = new MaterialAlertDialogBuilder(requireContext())
                    .setView(dialogContent)
                    .setCancelable(false)
                    .create();
        }
        if (progressDialogMessage != null) {
            progressDialogMessage.setText(message);
        }
        if (progressDialog != null && !progressDialog.isShowing()) {
            progressDialog.show();
        }
    }

    private void hideLoading() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void showAlert(@StringRes int titleRes, String message, @Nullable Runnable onOk){
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(titleRes)
                .setMessage(message)
                .setPositiveButton(R.string.patient_intake_alert_action_ok, (dialog, which) -> {
                    dialog.dismiss();
                    if (onOk != null) {
                        onOk.run();
                    }
                })
                .show();
    }

    private void setSaveButtonEnabled(boolean enabled){
        if (btnSave != null) {
            btnSave.setEnabled(enabled);
        }
    }

    @Override
    public void onDestroyView() {
        hideLoading();
        progressDialog = null;
        progressDialogMessage = null;
        btnSave = null;
        super.onDestroyView();
    }

    private String textOf(@Nullable TextInputEditText editText){
        if (editText == null) return null;
        CharSequence cs = editText.getText();
        return cs != null && cs.length() > 0 ? cs.toString().trim() : null;
    }

    private String selectedTag(@Nullable ChipGroup group){
        if (group == null) return null;
        int id = group.getCheckedChipId();
        if (id == View.NO_ID) return null;
        Chip chip = group.findViewById(id);
        return chip != null ? String.valueOf(chip.getTag()) : null;
    }

    private List<String> selectedTags(@Nullable ChipGroup group){
        List<String> result = new ArrayList<>();
        if (group == null) return result;
        for (int id : group.getCheckedChipIds()){
            Chip chip = group.findViewById(id);
            if (chip != null){
                String tag = String.valueOf(chip.getTag());
                if (!TextUtils.isEmpty(tag)) result.add(tag);
            }
        }
        return result;
    }
}
