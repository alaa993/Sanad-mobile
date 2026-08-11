package com.brightpath.sanad.feature.home;

import android.os.Bundle;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.brightpath.sanad.data.DashboardResponse;

/** @deprecated استخدم {@link AppNavigator} — محفوظ للتوافق. */
public final class ShortcutNavigator {

    private ShortcutNavigator() {}

    public static void navigate(Fragment fragment, @Nullable String role, DashboardResponse.Shortcut shortcut) {
        AppNavigator.goShortcut(fragment, role, shortcut);
    }

    public static void navigate(Fragment fragment, @Nullable String role, @IdRes int destination, @Nullable Bundle args) {
        AppNavigator.go(fragment, role, destination, args);
    }
}
