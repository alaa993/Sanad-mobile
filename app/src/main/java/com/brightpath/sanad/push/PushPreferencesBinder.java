package com.brightpath.sanad.push;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.CompoundButton;
import androidx.fragment.app.Fragment;
import com.brightpath.sanad.R;

public final class PushPreferencesBinder {
    private static final String PREF = "push_prefs_local";
    private static final String KEY_ENABLED = "push_enabled";

    private PushPreferencesBinder() {}

    public static void bind(Fragment fragment, View root) {
        CompoundButton switchPushEnabled = root.findViewById(R.id.switchPushEnabled);
        if (switchPushEnabled == null) return;

        Context ctx = fragment.requireContext().getApplicationContext();
        SharedPreferences prefs = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        PushDeviceRepository pushRepo = new PushDeviceRepository(fragment.requireContext());

        // Paint instantly from local cache; sync from network off the critical path.
        boolean cached = prefs.getBoolean(KEY_ENABLED, true);
        switchPushEnabled.setOnCheckedChangeListener(null);
        switchPushEnabled.setChecked(cached);
        attachListener(fragment, switchPushEnabled, pushRepo, prefs);

        pushRepo.loadPreferences(new PushDeviceRepository.PrefCb() {
            @Override public void ok(boolean enabled) {
                applySwitchOnUi(fragment, switchPushEnabled, pushRepo, prefs, enabled);
            }

            @Override public void err(Throwable t) {
                // Keep cached / default switch value. Never clear session on push 401.
            }
        });
    }

    private static void applySwitchOnUi(
            Fragment fragment,
            CompoundButton switchPushEnabled,
            PushDeviceRepository pushRepo,
            SharedPreferences prefs,
            boolean enabled
    ) {
        switchPushEnabled.post(() -> {
            if (!fragment.isAdded()) return;
            try {
                prefs.edit().putBoolean(KEY_ENABLED, enabled).apply();
                switchPushEnabled.setOnCheckedChangeListener(null);
                switchPushEnabled.setChecked(enabled);
                attachListener(fragment, switchPushEnabled, pushRepo, prefs);
            } catch (Throwable ignored) {}
        });
    }

    private static void attachListener(
            Fragment fragment,
            CompoundButton switchPushEnabled,
            PushDeviceRepository pushRepo,
            SharedPreferences prefs
    ) {
        switchPushEnabled.setOnCheckedChangeListener((CompoundButton button, boolean checked) -> {
                prefs.edit().putBoolean(KEY_ENABLED, checked).apply();
                pushRepo.updatePreferences(checked, new PushDeviceRepository.PrefCb() {
                    @Override public void ok(boolean enabled) {
                        applySwitchOnUi(fragment, switchPushEnabled, pushRepo, prefs, enabled);
                    }

                    @Override public void err(Throwable t) {
                        applySwitchOnUi(fragment, switchPushEnabled, pushRepo, prefs, !checked);
                    }
                });
        });
    }
}
