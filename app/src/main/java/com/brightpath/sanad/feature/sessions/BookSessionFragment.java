package com.brightpath.sanad.feature.sessions;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputLayout;
import com.brightpath.sanad.R;
import com.brightpath.sanad.feature.home.AppNavigator;
import com.brightpath.sanad.feature.calendar.CalendarModels;
import com.brightpath.sanad.feature.calendar.CalendarViewModels;
import com.brightpath.sanad.feature.patient.PatientIntakeForm;
import com.brightpath.sanad.feature.patient.PatientIntakeRepository;
import com.brightpath.sanad.feature.patient.PreSessionModels;
import com.brightpath.sanad.feature.patient.PreSessionRepository;
import com.brightpath.sanad.feature.patient.TriageRecommendation;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.ArrayList;
import java.util.List;

/**
 * BookSessionFragment
 * - يفتح Pickers لاختيار الأخصائي/المنظمة
 * - يختار التاريخ والوقت
 * - يرسل POST /api/v1/sessions إلى Laravel (therapy_sessions)
 */
public class BookSessionFragment extends Fragment {

    // أسماء المفاتيح القادمة من SelectListFragment
    public static final String FRAGMENT_RESULT_KEY = "picker";
    public static final String ARG_SPECIALIST_ID = "specialistId";
    public static final String ARG_SPECIALIST_NAME = "specialistName";

    private EditText etSpecialistId, etOrganizationId, etDate, etTime, etDateTime, etNotes;
    private ProgressBar progress;
    private ChipGroup typeChips;
    private Button btnPickSlot;
    private View dateTimeCard;
    private String sessionType = "video";

    // وقت الحجز المختار
    private final Calendar cal = Calendar.getInstance();

    private BookSessionViewModel vm;
    private PreSessionRepository preSessionRepo;
    private CalendarViewModels.SuggestedSlotsVM slotsVM;
    private boolean waitingSlots = false;
    private boolean bookingHandled = false;
    private View btnConfirm;
    private View btnBackStep;
    private TextView tvStepProgress;
    private TextView tvSubtitle;
    private View stepType;
    private View stepSpecialist;
    private View stepTime;
    private View stepReview;
    private View[] stepDots;
    private int currentStep = 0;
    private static final int TOTAL_STEPS = 4;
    private MaterialSwitch switchWeeklyRecurring;
    private TextInputLayout tilRecurrenceCount;
    private TextInputLayout tilOrganization;
    private EditText etRecurrenceCount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_book_session, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);

        // ربط الواجهات
        btnConfirm = v.findViewById(R.id.btnConfirm);

        etSpecialistId   = v.findViewById(R.id.etSpecialistId);
        etOrganizationId = v.findViewById(R.id.etOrganizationId);
        tilOrganization  = v.findViewById(R.id.tilOrganization);
        etDate           = v.findViewById(R.id.etDate);
        etTime           = v.findViewById(R.id.etTime);
        etDateTime       = v.findViewById(R.id.etDateTime);
        etNotes          = v.findViewById(R.id.etNotes);
        progress         = v.findViewById(R.id.progress);
        typeChips        = v.findViewById(R.id.chipSessionType);
        btnPickSlot      = v.findViewById(R.id.btnPickSlot);
        dateTimeCard     = v.findViewById(R.id.dateTimeCard);
        View cardRecommendation = v.findViewById(R.id.cardRecommendation);
        TextView tvRecSpecialist = v.findViewById(R.id.tvRecSpecialist);
        TextView tvRecReason = v.findViewById(R.id.tvRecReason);
        // Hide recommendation card (patient-side "suggestions" should not appear during booking).
        if (cardRecommendation != null) cardRecommendation.setVisibility(View.GONE);

        vm = new ViewModelProvider(this).get(BookSessionViewModel.class);
        preSessionRepo = new PreSessionRepository(requireContext());
        vm.getState().observe(getViewLifecycleOwner(), this::renderState);
        slotsVM = new ViewModelProvider(this).get(CalendarViewModels.SuggestedSlotsVM.class);
        slotsVM.data.observe(getViewLifecycleOwner(), suggestions -> {
            if (!waitingSlots) return;
            waitingSlots = false;
            if (suggestions == null || suggestions.isEmpty()) {
                toast(getString(R.string.session_slot_empty));
                return;
            }
            showSlotDialog(suggestions);
        });

        // استقبال نتيجة الـ Picker (أخصائي/منظمة) — النوع من الـ result حتى لا يضيع الاختيار بعد الرجوع
        getParentFragmentManager().setFragmentResultListener(FRAGMENT_RESULT_KEY, this, (key, b) -> {
            boolean isSpecialists = b.getBoolean(SelectListFragment.RESULT_IS_SPECIALISTS, true);
            if (!isSpecialists && b.getBoolean(SelectListFragment.RESULT_CLEARED, false)) {
                vm.clearOrganization();
                bindSelectionFields();
                return;
            }
            int id = b.getInt(SelectListFragment.RESULT_ID);
            String name = b.getString(SelectListFragment.RESULT_NAME);
            if (isSpecialists) {
                vm.setSpecialist(id, name);
            } else {
                vm.setOrganization(id, name);
            }
            bindSelectionFields();
        });

        // Make the fields themselves the only action points (no side buttons).
        if (etSpecialistId != null) {
            etSpecialistId.setOnClickListener(x -> openPicker(true));
        }
        if (etOrganizationId != null) {
            etOrganizationId.setOnClickListener(x -> openPicker(false));
        }
        if (tilOrganization != null) {
            tilOrganization.setEndIconOnClickListener(x -> {
                vm.clearOrganization();
                bindSelectionFields();
            });
        }
        if (etDate != null) {
            etDate.setOnClickListener(x -> pickDate());
        }
        if (etTime != null) {
            etTime.setOnClickListener(x -> pickTime());
        }
        if (etDateTime != null) {
            etDateTime.setOnClickListener(x -> pickDateTimeUnified());
        }
        if (dateTimeCard != null) {
            dateTimeCard.setOnClickListener(x -> pickDateTimeUnified());
        }

        // اختيار التاريخ
        // (also triggered by tapping the field)
        // no-op here: handled by pickDate()
        // اختيار الوقت handled by pickTime()

        // Pre-fill specialist when arriving from a specialist detail screen.
        if (vm.getSpecialistId() == null
                && getArguments() != null
                && getArguments().containsKey(ARG_SPECIALIST_ID)) {
            int presetId = getArguments().getInt(ARG_SPECIALIST_ID, -1);
            String presetName = getArguments().getString(ARG_SPECIALIST_NAME);
            if (presetId > 0) {
                vm.setSpecialist(presetId, presetName);
            }
        }
        bindSelectionFields();

        if (btnPickSlot != null) {
            btnPickSlot.setVisibility(View.GONE);
        }

        tvStepProgress = v.findViewById(R.id.tvStepProgress);
        tvSubtitle = v.findViewById(R.id.tvSubtitle);
        stepType = v.findViewById(R.id.stepType);
        stepSpecialist = v.findViewById(R.id.stepSpecialist);
        stepTime = v.findViewById(R.id.stepTime);
        stepReview = v.findViewById(R.id.stepReview);
        btnBackStep = v.findViewById(R.id.btnBackStep);
        stepDots = new View[] {
                v.findViewById(R.id.stepDot0),
                v.findViewById(R.id.stepDot1),
                v.findViewById(R.id.stepDot2),
                v.findViewById(R.id.stepDot3)
        };
        if (btnBackStep != null) {
            btnBackStep.setOnClickListener(x -> {
                if (currentStep > 0) {
                    currentStep--;
                    renderWizardStep();
                }
            });
        }
        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(x -> onWizardPrimary());
        }
        renderWizardStep();

        switchWeeklyRecurring = v.findViewById(R.id.switchWeeklyRecurring);
        tilRecurrenceCount = v.findViewById(R.id.tilRecurrenceCount);
        etRecurrenceCount = v.findViewById(R.id.etRecurrenceCount);
        if (switchWeeklyRecurring != null) {
            switchWeeklyRecurring.setOnCheckedChangeListener((buttonView, checked) -> {
                if (tilRecurrenceCount != null) {
                    tilRecurrenceCount.setVisibility(checked ? View.VISIBLE : View.GONE);
                }
            });
        }

        if (typeChips != null) {
            typeChips.setSelectionRequired(true);
            typeChips.setOnCheckedChangeListener((group, checkedId) -> {
                applySessionType(checkedId);
            });
            // اضبط الديفولت على محادثة لتقليل الكتابة
            Chip chat = typeChips.findViewById(R.id.chipChat);
            if (chat != null) chat.setChecked(true);
            applySessionType(typeChips.getCheckedChipId());
        }

        // تأكيد الحجز
        // (wired above)
    }

    private void openPicker(boolean specialists) {
        Bundle args = new Bundle();
        args.putBoolean(SelectListFragment.ARG_IS_SPECIALISTS, specialists);
        NavController nav = NavHostFragment.findNavController(this);
        nav.navigate(specialists ? R.id.selectSpecialistFragment : R.id.selectOrganizationFragment, args);
    }

    private void bindSelectionFields() {
        if (etSpecialistId != null) {
            Integer id = vm.getSpecialistId();
            String name = vm.getSpecialistName();
            if (id != null) {
                etSpecialistId.setTag(R.id.tag_specialist_id, id);
                etSpecialistId.setText(name != null ? name : String.valueOf(id));
            } else {
                etSpecialistId.setTag(R.id.tag_specialist_id, null);
                etSpecialistId.setText("");
            }
        }
        if (etOrganizationId != null) {
            Integer id = vm.getOrganizationId();
            String name = vm.getOrganizationName();
            if (id != null) {
                etOrganizationId.setTag(R.id.tag_organization_id, id);
                etOrganizationId.setText(name != null ? name : String.valueOf(id));
            } else {
                etOrganizationId.setTag(R.id.tag_organization_id, null);
                etOrganizationId.setText("");
            }
        }
        if (tilOrganization != null) {
            tilOrganization.setEndIconVisible(vm.getOrganizationId() != null);
        }
    }

    private void pickDate() {
        int y = cal.get(Calendar.YEAR);
        int m = cal.get(Calendar.MONTH);
        int d = cal.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog dialog = new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, month);
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            etDate.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.getTime()));
        }, y, m, d);
        dialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        dialog.show();
    }

    private void pickTime() {
        int hh = cal.get(Calendar.HOUR_OF_DAY);
        int mm = cal.get(Calendar.MINUTE);
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(hh)
                .setMinute(mm)
                .setTitleText(R.string.book_session_pick_time)
                .build();
        picker.addOnPositiveButtonClickListener(v -> {
            int hourOfDay = picker.getHour();
            int minute = picker.getMinute();
            cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
            cal.set(Calendar.MINUTE, minute);
            if (etTime != null) {
                etTime.setText(String.format(Locale.US, "%02d:%02d", hourOfDay, minute));
            }
        });
        picker.show(getParentFragmentManager(), "book_session_time_picker");
    }

    private void onWizardPrimary() {
        if (currentStep < TOTAL_STEPS - 1) {
            if (!validateWizardStep()) return;
            currentStep++;
            renderWizardStep();
            return;
        }
        submitBooking();
    }

    private boolean validateWizardStep() {
        if (currentStep == 1) {
            if (vm.getSpecialistId() == null || vm.getSpecialistId() <= 0) {
                toast(getString(R.string.book_session_error_pick_specialist));
                return false;
            }
        }
        if (currentStep == 2) {
            if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
                toast(getString(R.string.book_session_error_past_datetime));
                return false;
            }
        }
        return true;
    }

    private void renderWizardStep() {
        if (stepType != null) stepType.setVisibility(currentStep == 0 ? View.VISIBLE : View.GONE);
        if (stepSpecialist != null) stepSpecialist.setVisibility(currentStep == 1 ? View.VISIBLE : View.GONE);
        if (stepTime != null) stepTime.setVisibility(currentStep == 2 ? View.VISIBLE : View.GONE);
        if (stepReview != null) stepReview.setVisibility(currentStep == 3 ? View.VISIBLE : View.GONE);
        if (btnBackStep != null) btnBackStep.setVisibility(currentStep > 0 ? View.VISIBLE : View.GONE);
        if (btnConfirm instanceof com.google.android.material.button.MaterialButton) {
            ((com.google.android.material.button.MaterialButton) btnConfirm).setText(
                    currentStep >= TOTAL_STEPS - 1 ? R.string.book_step_submit : R.string.book_step_next);
        } else if (btnConfirm instanceof Button) {
            ((Button) btnConfirm).setText(currentStep >= TOTAL_STEPS - 1
                    ? R.string.book_step_submit
                    : R.string.book_step_next);
        }
        if (tvStepProgress != null) {
            tvStepProgress.setText(getString(R.string.book_step_progress, currentStep + 1, TOTAL_STEPS));
        }
        if (tvSubtitle != null) {
            int sub = R.string.book_step_type;
            if (currentStep == 1) sub = R.string.book_step_specialist;
            else if (currentStep == 2) sub = R.string.book_step_time;
            else if (currentStep == 3) sub = R.string.book_step_review;
            tvSubtitle.setText(sub);
        }
        if (stepDots != null) {
            for (int i = 0; i < stepDots.length; i++) {
                if (stepDots[i] == null) continue;
                stepDots[i].setBackgroundColor(i <= currentStep
                        ? androidx.core.content.ContextCompat.getColor(requireContext(), R.color.sanad_blue_primary)
                        : androidx.core.content.ContextCompat.getColor(requireContext(), R.color.sanad_field_stroke));
            }
        }
    }

    private void submitBooking() {
        Integer specialistIdVal = vm.getSpecialistId();
        Integer orgId = vm.getOrganizationId();
        String dateStr = etDate != null && etDate.getText() != null ? etDate.getText().toString().trim() : "";
        String timeStr = etTime != null && etTime.getText() != null ? etTime.getText().toString().trim() : "";
        String notesStr = etNotes != null && etNotes.getText() != null ? etNotes.getText().toString().trim() : "";

        if (specialistIdVal == null || specialistIdVal <= 0) {
            toast(getString(R.string.book_session_error_pick_specialist));
            return;
        }
        if (TextUtils.isEmpty(dateStr) || TextUtils.isEmpty(timeStr)) {
            toast(getString(R.string.book_session_error_pick_date_time));
            return;
        }
        if (isPastDateTime(dateStr, timeStr)) {
            toast(getString(R.string.book_session_error_past_datetime));
            return;
        }
        if (TextUtils.isEmpty(sessionType)) {
            toast(getString(R.string.book_session_error_pick_type));
            return;
        }
        String isoScheduled = toIsoString(dateStr, timeStr);
        boolean weekly = switchWeeklyRecurring != null && switchWeeklyRecurring.isChecked();
        Integer recurrence = null;
        if (weekly && etRecurrenceCount != null) {
            recurrence = parseIntSafe(etRecurrenceCount.getText().toString().trim());
            if (recurrence == null || recurrence < 2) recurrence = 4;
        }

        PendingBooking booking = new PendingBooking(sessionType, isoScheduled, specialistIdVal, orgId,
                TextUtils.isEmpty(notesStr) ? null : notesStr, weekly, recurrence);
        preSessionRepo.fetchStatus(new PreSessionRepository.StatusCb() {
            @Override public void ok(PreSessionModels.Status status) {
                if (!isAdded()) return;
                if (status != null && status.completed) {
                    proceedBooking(booking);
                    return;
                }
                showPreSessionDialog(status != null ? status.questions : null, booking);
            }
            @Override public void err(Throwable t) {
                if (isAdded()) proceedBooking(booking);
            }
        });
    }

    private void proceedBooking(PendingBooking booking) {
        vm.book(booking.type, booking.isoScheduled, booking.specialistId, booking.orgId, booking.notes,
                booking.weeklyRecurring, booking.recurrenceCount);
    }

    private void showPreSessionDialog(@Nullable List<PreSessionModels.Question> questions, PendingBooking booking) {
        if (questions == null || questions.isEmpty()) {
            proceedBooking(booking);
            return;
        }
        android.widget.LinearLayout container = new android.widget.LinearLayout(requireContext());
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.setPadding(32, 16, 32, 8);
        final Map<String, android.view.View> inputs = new HashMap<>();
        String lang = Locale.getDefault().getLanguage();
        for (PreSessionModels.Question q : questions) {
            String label = "ar".equals(lang) && !TextUtils.isEmpty(q.label_ar) ? q.label_ar
                    : (!TextUtils.isEmpty(q.label_en) ? q.label_en : q.id);
            android.widget.TextView labelView = new android.widget.TextView(requireContext());
            labelView.setText(label);
            labelView.setPadding(0, 16, 0, 4);
            container.addView(labelView);
            android.view.View input;
            if ("boolean".equalsIgnoreCase(q.type)) {
                android.widget.CheckBox cb = new android.widget.CheckBox(requireContext());
                cb.setText(R.string.yes);
                input = cb;
            } else if ("scale".equalsIgnoreCase(q.type)) {
                android.widget.SeekBar seek = new android.widget.SeekBar(requireContext());
                seek.setMax(9);
                seek.setProgress(4);
                input = seek;
            } else {
                EditText et = new EditText(requireContext());
                et.setMinLines(2);
                input = et;
            }
            inputs.put(q.id, input);
            container.addView(input);
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.pre_session_survey_title)
                .setView(container)
                .setCancelable(false)
                .setPositiveButton(R.string.pre_session_survey_submit, (d, w) -> {
                    Map<String, Object> answers = new HashMap<>();
                    for (PreSessionModels.Question q : questions) {
                        android.view.View input = inputs.get(q.id);
                        if (input == null) continue;
                        if (input instanceof android.widget.CheckBox) {
                            answers.put(q.id, ((android.widget.CheckBox) input).isChecked());
                        } else if (input instanceof android.widget.SeekBar) {
                            answers.put(q.id, ((android.widget.SeekBar) input).getProgress() + 1);
                        } else if (input instanceof EditText) {
                            String text = ((EditText) input).getText().toString().trim();
                            answers.put(q.id, text);
                        }
                    }
                    preSessionRepo.submit(answers, new PreSessionRepository.SubmitCb() {
                        @Override public void ok() {
                            if (isAdded()) proceedBooking(booking);
                        }
                        @Override public void err(Throwable t) {
                            if (isAdded()) toast(getString(R.string.pre_session_survey_failed));
                        }
                    });
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private static class PendingBooking {
        final String type, isoScheduled, notes;
        final Integer specialistId, orgId, recurrenceCount;
        final boolean weeklyRecurring;
        PendingBooking(String type, String isoScheduled, Integer specialistId, Integer orgId, String notes,
                       boolean weeklyRecurring, Integer recurrenceCount) {
            this.type = type;
            this.isoScheduled = isoScheduled;
            this.specialistId = specialistId;
            this.orgId = orgId;
            this.notes = notes;
            this.weeklyRecurring = weeklyRecurring;
            this.recurrenceCount = recurrenceCount;
        }
    }

    private Integer parseIntSafe(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return null;
        }
    }

    private void pickDateTimeUnified() {
        int y = cal.get(Calendar.YEAR);
        int m = cal.get(Calendar.MONTH);
        int d = cal.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog dialog = new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, month);
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            int hh = cal.get(Calendar.HOUR_OF_DAY);
            int mm = cal.get(Calendar.MINUTE);
            MaterialTimePicker picker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setHour(hh)
                    .setMinute(mm)
                    .setTitleText(R.string.book_session_pick_time)
                    .build();
            picker.addOnPositiveButtonClickListener(v -> {
                int hourOfDay = picker.getHour();
                int minute = picker.getMinute();
                cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
                cal.set(Calendar.MINUTE, minute);
                if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
                    toast(getString(R.string.book_session_error_past_datetime));
                    return;
                }
                String dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.getTime());
                String timeStr = String.format(Locale.US, "%02d:%02d", hourOfDay, minute);
                if (etDate != null) etDate.setText(dateStr);
                if (etTime != null) etTime.setText(timeStr);
                if (etDateTime != null) etDateTime.setText(dateStr + " • " + timeStr);
            });
            picker.show(getParentFragmentManager(), "book_session_datetime_picker");
        }, y, m, d);
        dialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        dialog.show();
    }

    private void bindRecommendation(@Nullable View card, @Nullable TextView tvSpecialist, @Nullable TextView tvReason,
                                    @Nullable ChipGroup toggle, @Nullable EditText etSpecialist){
        if (card == null) return;
        PatientIntakeRepository repo = new PatientIntakeRepository(requireContext());
        PatientIntakeForm form = repo.load();
        if (form == null){
            card.setVisibility(View.GONE);
            return;
        }
        TriageRecommendation rec = TriageRecommendation.evaluate(form);
        if (rec == null || TextUtils.isEmpty(rec.suggestedSpecialist)){
            card.setVisibility(View.GONE);
            return;
        }
        card.setVisibility(View.VISIBLE);
        if (tvSpecialist != null){
            tvSpecialist.setText(getString(R.string.book_recommendation_specialist, rec.suggestedSpecialist));
        }
        if (tvReason != null){
            String reason = !TextUtils.isEmpty(rec.reasoning) ? rec.reasoning : getString(R.string.book_recommendation_missing);
            tvReason.setText(getString(R.string.book_recommendation_reason, reason));
        }
        if (etSpecialist != null && !TextUtils.isEmpty(rec.suggestedSpecialist)) {
            etSpecialist.setHint(rec.suggestedSpecialist);
        }
        String suggestedType = suggestedSessionType(rec);
        if (!TextUtils.isEmpty(suggestedType)){
            sessionType = suggestedType;
            if (toggle != null){
                if ("video".equals(suggestedType)) toggle.check(R.id.chipVideo);
                else if ("voice".equals(suggestedType)) toggle.check(R.id.chipVoice);
                else if ("chat".equals(suggestedType)) toggle.check(R.id.chipChat);
            }
        }
    }

    private String suggestedSessionType(TriageRecommendation rec){
        if (rec == null || rec.category == null) return null;
        switch (rec.category){
            case BIPOLAR:
            case SCHIZOPHRENIA:
                return "video"; // لتواصل مباشر مع طبيب
            case ANXIETY_DEPRESSION:
                return "video";
            case CHILDREN:
                return "video";
            case MILD:
                return "chat";
            case IDENTITY:
                return "voice";
            default:
                return "video";
        }
    }

    private void applySessionType(int checkedId) {
        if (checkedId == R.id.chipVideo) {
            sessionType = "video";
        } else if (checkedId == R.id.chipVoice) {
            sessionType = "voice";
        } else if (checkedId == R.id.chipChat) {
            sessionType = "chat";
        }
    }

    private void setLoading(boolean v) {
        if (progress != null) progress.setVisibility(v ? View.VISIBLE : View.GONE);
    }

    private void toast(String m) {
        Toast.makeText(requireContext(), m, Toast.LENGTH_SHORT).show();
    }

    private void renderState(BookSessionViewModel.UIState state){
        if (state == null) return;
        setLoading(state.loading);
        if (btnConfirm != null) {
            btnConfirm.setEnabled(!state.loading && !bookingHandled);
        }
        if (state.error != null) {
            showBookingAlert(
                    getString(R.string.book_session_alert_error_title),
                    state.error,
                    state.errorCode,
                    false
            );
            vm.clearResult();
            return;
        }
        if (state.data != null && !bookingHandled) {
            bookingHandled = true;
            finishBooking();
        }
    }

    private void showBookingAlert(String title, String message, @Nullable String errorCode, boolean success) {
        if (!isAdded()) return;
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(R.string.common_ok, (d, w) -> {
                    if (success) {
                        navigateAfterBooking();
                    }
                });
        if ("intake_required".equals(errorCode)) {
            builder.setNeutralButton(R.string.home_intake_open, (d, w) ->
                    navigateToDestination(R.id.patientIntakeFragment));
        } else if ("pre_session_required".equals(errorCode)) {
            builder.setNeutralButton(R.string.pre_session_survey_title, (d, w) ->
                    navigateToDestination(R.id.preSessionFragment));
        }
        builder.show();
    }

    private void showBookingAlert(String title, String message, boolean success) {
        showBookingAlert(title, message, null, success);
    }

    private void finishBooking() {
        if (!isAdded()) return;
        showBookingAlert(
                getString(R.string.book_session_alert_success_title),
                getString(R.string.booking_success),
                true
        );
        vm.clearResult();
    }

    private void navigateToDestination(int destination) {
        AppNavigator.go(this, destination);
    }

    private void navigateAfterBooking() {
        View root = getView();
        if (root == null || !isAdded()) return;
        root.post(() -> {
            if (!isAdded()) return;
            NavController nav;
            try {
                nav = NavHostFragment.findNavController(BookSessionFragment.this);
            } catch (IllegalStateException e) {
                return;
            }
            if (nav.navigateUp() || nav.popBackStack()) return;

            Bundle args = new Bundle();
            args.putString("filterStatus", "pending");
            NavOptions options = new NavOptions.Builder()
                    .setPopUpTo(R.id.bookSessionFragment, true)
                    .setLaunchSingleTop(true)
                    .build();
            try {
                nav.navigate(R.id.sessionsFragment, args, options);
            } catch (IllegalArgumentException e) {
                try {
                    nav.navigate(R.id.patientDashboardFragment, null, options);
                } catch (IllegalArgumentException ignored) {
                }
            }
        });
    }

    private void fetchSlots(){
        Integer specialistId = (Integer) etSpecialistId.getTag(R.id.tag_specialist_id);
        if (specialistId == null) {
            toast(getString(R.string.session_slot_need_specialist));
            return;
        }
        String date = etDate.getText().toString().trim();
        if (TextUtils.isEmpty(date)) {
            toast(getString(R.string.session_slot_need_date));
            return;
        }
        waitingSlots = true;
        toast(getString(R.string.session_slot_loading));
        slotsVM.load(specialistId, date);
    }

    private void showSlotDialog(List<CalendarModels.Suggestion> slots){
        List<String> labels = new ArrayList<>();
        for (CalendarModels.Suggestion s : slots) {
            labels.add(formatSlotLabel(s));
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.session_slot_title)
                .setItems(labels.toArray(new String[0]), (dialog, which) -> applySlot(slots.get(which)))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private String formatSlotLabel(CalendarModels.Suggestion suggestion){
        String start = formatDisplayTime(suggestion.starts_at);
        String end = formatDisplayTime(suggestion.ends_at);
        return start + " - " + end;
    }

    private void applySlot(CalendarModels.Suggestion selected){
        if (selected.starts_at != null) {
            String[] parts = formatDateTimeSelection(selected.starts_at);
            if (parts[0] != null) etDate.setText(parts[0]);
            if (parts[1] != null) etTime.setText(parts[1]);
        }
    }

    private boolean isPastDateTime(String date, String time) {
        try {
            LocalDate d = LocalDate.parse(date);
            LocalTime t = LocalTime.parse(time);
            return !ZonedDateTime.of(d, t, ZoneId.systemDefault()).isAfter(ZonedDateTime.now());
        } catch (Exception e) {
            return false;
        }
    }

    private String toIsoString(String date, String time){
        try {
            LocalDate d = LocalDate.parse(date);
            LocalTime t = LocalTime.parse(time);
            ZoneId zone = ZoneId.systemDefault();
            ZonedDateTime zdt = ZonedDateTime.of(d, t, zone);
            return zdt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch (Exception e){
            return date + "T" + time + ":00" + ZoneId.systemDefault().getRules().getOffset(java.time.Instant.now()).getId();
        }
    }

    private String formatDisplayTime(String iso){
        if (iso == null) return "--:--";
        try {
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            parser.setTimeZone(TimeZone.getTimeZone("UTC"));
            SimpleDateFormat fmt = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return fmt.format(parser.parse(iso));
        } catch (Exception e){
            return iso;
        }
    }

    private String[] formatDateTimeSelection(String iso){
        String[] res = new String[2];
        if (iso == null) return res;
        try {
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            parser.setTimeZone(TimeZone.getTimeZone("UTC"));
            SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());
            long ts = parser.parse(iso).getTime();
            res[0] = dateFmt.format(ts);
            res[1] = timeFmt.format(ts);
        } catch (Exception ignored) {
        }
        return res;
    }
}
