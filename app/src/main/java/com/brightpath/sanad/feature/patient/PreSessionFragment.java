package com.brightpath.sanad.feature.patient;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.brightpath.sanad.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PreSessionFragment extends Fragment {
    private PreSessionRepository repo;
    private View content, error;
    private ProgressBar loading;
    private LinearLayout questionsContainer;
    private MaterialButton btnSubmit;
    private final Map<String, View> answerViews = new HashMap<>();

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pre_session, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        repo = new PreSessionRepository(requireContext());
        content = v.findViewById(R.id.preSessionContent);
        loading = v.findViewById(R.id.preSessionLoading);
        error = v.findViewById(R.id.preSessionError);
        questionsContainer = v.findViewById(R.id.questionsContainer);
        btnSubmit = v.findViewById(R.id.btnSubmitPreSession);
        btnSubmit.setOnClickListener(x -> submit());
        load();
    }

    private void load() {
        show(loading);
        repo.fetchStatus(new PreSessionRepository.StatusCb() {
            @Override public void ok(PreSessionModels.Status status) {
                if (!isAdded()) return;
                if (status != null && status.completed) {
                    Toast.makeText(requireContext(), R.string.pre_session_survey_done, Toast.LENGTH_SHORT).show();
                    requireActivity().onBackPressed();
                    return;
                }
                buildQuestions(status != null ? status.questions : null);
                show(content);
            }
            @Override public void err(Throwable t) { if (isAdded()) show(error); }
        });
    }

    private void buildQuestions(@Nullable List<PreSessionModels.Question> questions) {
        questionsContainer.removeAllViews();
        answerViews.clear();
        if (questions == null || questions.isEmpty()) return;
        boolean ar = Locale.getDefault().getLanguage().startsWith("ar");
        for (PreSessionModels.Question q : questions) {
            if (q == null || q.id == null) continue;
            TextView label = new TextView(requireContext());
            label.setText(ar && q.label_ar != null ? q.label_ar : (q.label_en != null ? q.label_en : q.id));
            label.setTextColor(getResources().getColor(R.color.sanad_on_bg, null));
            label.setPadding(0, 0, 0, 8);
            questionsContainer.addView(label);
            View input;
            if ("scale".equalsIgnoreCase(q.type)) {
                Slider slider = new Slider(requireContext());
                slider.setValueFrom(1f);
                slider.setValueTo(10f);
                slider.setStepSize(1f);
                slider.setValue(5f);
                input = slider;
            } else if ("boolean".equalsIgnoreCase(q.type)) {
                input = new SwitchMaterial(requireContext());
            } else {
                EditText et = new EditText(requireContext());
                et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                et.setMinLines(2);
                input = et;
            }
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = (int) (16 * getResources().getDisplayMetrics().density);
            input.setLayoutParams(lp);
            questionsContainer.addView(input);
            answerViews.put(q.id, input);
        }
    }

    private void submit() {
        Map<String, Object> answers = new HashMap<>();
        for (Map.Entry<String, View> e : answerViews.entrySet()) {
            View v = e.getValue();
            if (v instanceof Slider) answers.put(e.getKey(), String.valueOf((int) ((Slider) v).getValue()));
            else if (v instanceof SwitchMaterial) answers.put(e.getKey(), ((SwitchMaterial) v).isChecked() ? "yes" : "no");
            else if (v instanceof EditText) answers.put(e.getKey(), ((EditText) v).getText().toString().trim());
        }
        btnSubmit.setEnabled(false);
        repo.submit(answers, new PreSessionRepository.SubmitCb() {
            @Override public void ok() {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), R.string.pre_session_survey_done, Toast.LENGTH_SHORT).show();
                requireActivity().onBackPressed();
            }
            @Override public void err(Throwable t) {
                if (!isAdded()) return;
                btnSubmit.setEnabled(true);
                Toast.makeText(requireContext(), R.string.error_load_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void show(View target) {
        loading.setVisibility(target == loading ? View.VISIBLE : View.GONE);
        content.setVisibility(target == content ? View.VISIBLE : View.GONE);
        error.setVisibility(target == error ? View.VISIBLE : View.GONE);
    }
}
