package com.brightpath.sanad.feature.specialist;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.brightpath.sanad.R;
import com.brightpath.sanad.data.DashboardResponse;
import com.brightpath.sanad.data.ThemeStore;
import com.brightpath.sanad.feature.home.AppNavigator;
import com.brightpath.sanad.feature.home.ShortcutNavigation;
import com.brightpath.sanad.feature.home.ShortcutsAdapter;

public class SpecialistShortcutsFragment extends Fragment {

    private ShortcutsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_shortcuts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);

        View safePlace = v.findViewById(R.id.cardSafePlace);
        if (safePlace != null) safePlace.setVisibility(View.GONE);

        // Specialists do not book sessions — hide patient "new session" CTA from shared layout.
        View primary = v.findViewById(R.id.btnPrimaryAction);
        if (primary != null) {
            primary.setVisibility(View.GONE);
        }

        ImageView logo = v.findViewById(R.id.imgLogo);
        if (logo != null) {
            logo.setImageResource(new ThemeStore(requireContext()).getLogoRes(false));
        }

        TextView label = v.findViewById(R.id.shortcutsLabel);
        if (label != null) {
            label.setText(R.string.specialist_shortcuts_title);
        }

        RecyclerView rv = v.findViewById(R.id.rvShortcuts);
        if (rv != null) {
            rv.setLayoutManager(new GridLayoutManager(requireContext(), 2));
            rv.setNestedScrollingEnabled(false);
            adapter = new ShortcutsAdapter();
            adapter.setRole("specialist");
            adapter.setOnClick(this::navigate);
            adapter.submit(ShortcutNavigation.specialistEssentials());
            rv.setAdapter(adapter);
        }
    }

    private void navigate(DashboardResponse.Shortcut shortcut) {
        AppNavigator.goShortcut(this, "specialist", shortcut);
    }
}
