package com.brightpath.sanad.feature.groups;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.brightpath.sanad.R;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Locale;

public class GroupCreateFragment extends Fragment {
    public static final String PATIENTS_RESULT_KEY = "groupPatientsPicker";
    public static final String PATIENTS_RESULT_IDS = "patientIds";
    private TextInputEditText etTitle, etTopic, etDate, etTime, etDuration;
    private TextInputEditText etPatients;
    private ChipGroup typeGroup;
    private String sessionType = "video";
    private final Calendar cal = Calendar.getInstance();
    private GroupRepository repo;
    private final java.util.List<Integer> selectedPatientIds = new java.util.ArrayList<>();

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_group_create, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        etTitle = v.findViewById(R.id.etGroupTitle);
        etTopic = v.findViewById(R.id.etGroupTopic);
        etPatients = v.findViewById(R.id.etGroupPatients);
        etDate = v.findViewById(R.id.etGroupDate);
        etTime = v.findViewById(R.id.etGroupTime);
        etDuration = v.findViewById(R.id.etGroupDuration);
        typeGroup = v.findViewById(R.id.chipGroupType);
        repo = new GroupRepository(requireContext());

        if (etDate != null) etDate.setOnClickListener(x -> pickDate());
        if (etTime != null) etTime.setOnClickListener(x -> pickTime());
        if (etPatients != null) {
            etPatients.setOnClickListener(x -> NavHostFragment.findNavController(this).navigate(R.id.groupSelectPatientsFragment));
        }

        getParentFragmentManager().setFragmentResultListener(PATIENTS_RESULT_KEY, this, (key, bundle) -> {
            java.util.ArrayList<Integer> ids = bundle.getIntegerArrayList(PATIENTS_RESULT_IDS);
            selectedPatientIds.clear();
            if (ids != null) selectedPatientIds.addAll(ids);
            updateSelectedPatientsLabel();
        });

        if (typeGroup != null) {
            typeGroup.setSelectionRequired(true);
            Chip defaultChip = v.findViewById(R.id.chipGroupVideo);
            typeGroup.setOnCheckedChangeListener((group, checkedId) -> {
                applySessionType(checkedId);
            });
            if (defaultChip != null) defaultChip.setChecked(true);
            applySessionType(typeGroup.getCheckedChipId());
        }

        v.findViewById(R.id.btnGroupCreateSubmit).setOnClickListener(x -> submit());
        updateSelectedPatientsLabel();
    }

    private void pickDate(){
        DatePickerDialog dlg = new DatePickerDialog(requireContext(), (picker, y, m, d) -> {
            cal.set(Calendar.YEAR, y);
            cal.set(Calendar.MONTH, m);
            cal.set(Calendar.DAY_OF_MONTH, d);
            LocalDate date = LocalDate.of(y, m + 1, d);
            if (etDate != null) etDate.setText(date.format(DateTimeFormatter.ISO_LOCAL_DATE));
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dlg.show();
    }

    private void pickTime(){
        TimePickerDialog dlg = new TimePickerDialog(requireContext(), (picker, h, min) -> {
            cal.set(Calendar.HOUR_OF_DAY, h);
            cal.set(Calendar.MINUTE, min);
            LocalTime time = LocalTime.of(h, min);
            if (etTime != null) etTime.setText(time.format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())));
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true);
        dlg.show();
    }

    private void submit(){
        if (typeGroup != null) {
            applySessionType(typeGroup.getCheckedChipId());
        }
        String title = textOf(etTitle);
        String topic = textOf(etTopic);
        String date = textOf(etDate);
        String time = textOf(etTime);
        String durationRaw = textOf(etDuration);
        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(date) || TextUtils.isEmpty(time)) {
            Toast.makeText(requireContext(), R.string.error_required_fields, Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedPatientIds.isEmpty()) {
            Toast.makeText(requireContext(), R.string.group_sessions_patients_required, Toast.LENGTH_SHORT).show();
            return;
        }
        int duration = 60;
        if (!TextUtils.isEmpty(durationRaw)) {
            try { duration = Integer.parseInt(durationRaw); } catch (Exception ignored) {}
        }
        if (duration < 15) duration = 15;

        LocalDate d = LocalDate.parse(date);
        LocalTime t = LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()));
        ZonedDateTime start = ZonedDateTime.of(d, t, ZoneId.systemDefault());
        ZonedDateTime end = start.plusMinutes(duration);

        repo.create(title, topic, sessionType, start.toOffsetDateTime().toString(), end.toOffsetDateTime().toString(),
                selectedPatientIds,
                ZoneId.systemDefault().getId(), new GroupRepository.DetailCb() {
                    @Override public void ok(GroupModels.GroupSession g) {
                        Toast.makeText(requireContext(), R.string.group_sessions_create_success, Toast.LENGTH_SHORT).show();
                        NavHostFragment.findNavController(GroupCreateFragment.this).popBackStack();
                    }
                    @Override public void err(Throwable t) {
                        Toast.makeText(requireContext(), R.string.group_sessions_create_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String textOf(TextInputEditText et){
        return et != null && et.getText() != null ? et.getText().toString().trim() : "";
    }

    private void applySessionType(int checkedId) {
        if (checkedId == R.id.chipGroupVoice) {
            sessionType = "voice";
        } else if (checkedId == R.id.chipGroupChat) {
            sessionType = "chat";
        } else if (checkedId == R.id.chipGroupVideo) {
            sessionType = "video";
        }
    }

    private void updateSelectedPatientsLabel() {
        if (etPatients == null) return;
        if (selectedPatientIds.isEmpty()) {
            etPatients.setText("");
        } else {
            etPatients.setText(getString(R.string.group_sessions_patients_count_format, selectedPatientIds.size()));
        }
    }
}
