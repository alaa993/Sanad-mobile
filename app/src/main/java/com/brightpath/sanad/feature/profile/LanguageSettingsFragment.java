package com.brightpath.sanad.feature.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;
import com.brightpath.sanad.R;
import com.brightpath.sanad.data.LocaleHelper;
import com.brightpath.sanad.data.LocaleStore;

public class LanguageSettingsFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_language_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.btnBack).setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());

        MaterialButton btnSystem = view.findViewById(R.id.btnLangSystem);
        MaterialButton btnArabic = view.findViewById(R.id.btnLangArabic);
        MaterialButton btnEnglish = view.findViewById(R.id.btnLangEnglish);
        MaterialButton btnTurkish = view.findViewById(R.id.btnLangTurkish);

        String saved = new LocaleStore(requireContext()).getSavedLocale();
        highlightSelected(btnSystem, btnArabic, btnEnglish, btnTurkish, saved);

        btnSystem.setOnClickListener(v -> switchLocale(""));
        btnArabic.setOnClickListener(v -> switchLocale("ar"));
        btnEnglish.setOnClickListener(v -> switchLocale("en"));
        if (btnTurkish != null) {
            btnTurkish.setOnClickListener(v -> switchLocale("tr"));
        }
    }

    private void switchLocale(String tag) {
        if (!isAdded()) return;
        LocaleHelper.applyLocaleAndRecreate(requireActivity(), tag);
    }

    private void highlightSelected(MaterialButton system, MaterialButton ar, MaterialButton en, MaterialButton tr, String saved) {
        system.setStrokeWidth(0);
        ar.setStrokeWidth(0);
        en.setStrokeWidth(0);
        if (tr != null) tr.setStrokeWidth(0);

        MaterialButton selected = ar;
        if (saved == null || saved.isEmpty()) {
            selected = system;
        } else if ("en".equalsIgnoreCase(saved)) {
            selected = en;
        } else if ("tr".equalsIgnoreCase(saved)) {
            selected = tr != null ? tr : ar;
        }
        selected.setStrokeWidth(2);
    }
}
