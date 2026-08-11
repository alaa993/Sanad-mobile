package com.brightpath.sanad.feature.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.brightpath.sanad.R;
import com.brightpath.sanad.data.ThemeStore;

public class ThemeSettingsFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_theme_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v ->
                    NavHostFragment.findNavController(this).popBackStack());
        }

        MaterialButton btnBlue = view.findViewById(R.id.btnThemeBlue);
        MaterialButton btnGray = view.findViewById(R.id.btnThemeGray);
        MaterialButton btnPink = view.findViewById(R.id.btnThemePink);
        ThemeStore store = new ThemeStore(requireContext());

        if (btnBlue != null) btnBlue.setOnClickListener(v -> applyTheme(store, ThemeStore.THEME_BLUE));
        if (btnGray != null) btnGray.setOnClickListener(v -> applyTheme(store, ThemeStore.THEME_GRAY));
        if (btnPink != null) btnPink.setOnClickListener(v -> applyTheme(store, ThemeStore.THEME_PINK));
    }

    private void applyTheme(ThemeStore store, String theme) {
        try {
            com.brightpath.sanad.ui.tour.CoachMarkManager.dismissActive();
            store.saveTheme(theme);
            if (!isAdded()) return;
            android.app.Activity activity = getActivity();
            if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
            activity.recreate();
        } catch (Throwable ignored) {}
    }
}
