package com.brightpath.sanad.feature.home;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.brightpath.sanad.R;
import com.brightpath.sanad.data.DashboardResponse;

/** تنقّل آمن من أي شاشة — يجرّب مسار الاختصارات ثم المسار العام ثم الوجهة مباشرة. */
public final class AppNavigator {

    private AppNavigator() {}

    public static void go(Fragment fragment, @IdRes int destination) {
        go(fragment, null, destination, null);
    }

    public static void go(Fragment fragment, @IdRes int destination, @Nullable Bundle args) {
        go(fragment, null, destination, args);
    }

    public static void go(Fragment fragment, @Nullable String role, @IdRes int destination, @Nullable Bundle args) {
        if (fragment == null || !fragment.isAdded()) return;
        NavController nav;
        try {
            nav = NavHostFragment.findNavController(fragment);
        } catch (IllegalStateException e) {
            return;
        }
        int currentDest = ShortcutNavigation.resolveSourceDestination(fragment, nav);
        int[] attempts = ShortcutNavigation.routeAttempts(currentDest, role, destination);

        for (int route : attempts) {
            if (route == 0) continue;
            if (navigate(nav, route, args, null)) return;
        }

        NavOptions options = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .build();
        for (int route : attempts) {
            if (route == 0) continue;
            if (navigate(nav, route, args, options)) return;
        }

        Toast.makeText(fragment.requireContext(), R.string.shortcut_not_supported, Toast.LENGTH_SHORT).show();
    }

    private static boolean navigate(NavController nav, @IdRes int route, @Nullable Bundle args, @Nullable NavOptions options) {
        try {
            if (options != null) {
                if (args != null) nav.navigate(route, args, options);
                else nav.navigate(route, options);
            } else if (args != null) {
                nav.navigate(route, args);
            } else {
                nav.navigate(route);
            }
            return true;
        } catch (IllegalArgumentException | IllegalStateException ignored) {
            return false;
        }
    }

    public static void goShortcut(Fragment fragment, @Nullable String role, DashboardResponse.Shortcut shortcut) {
        if (fragment == null || shortcut == null) return;
        Integer dest = ShortcutNavigation.destinationFor(role, shortcut);
        if (dest == null) {
            Toast.makeText(fragment.requireContext(), R.string.shortcut_not_supported, Toast.LENGTH_SHORT).show();
            return;
        }
        go(fragment, role, dest, null);
    }
}
