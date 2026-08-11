package com.brightpath.sanad.router;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.brightpath.sanad.R;

/**
 * Central place to map backend roles to the correct NavGraph destination.
 */
public final class RoleRouter {

    private RoleRouter(){}

    /**
     * Navigate away from the patient dashboard when the role requires a custom home.
     *
     * @return true if a navigation action was performed.
     */
    public static boolean redirect(Fragment host, @Nullable String role) {
        if (host == null) return false;
        NavController nav = NavHostFragment.findNavController(host);
        return redirect(nav, role);
    }

    public static boolean redirect(NavController nav, @Nullable String role) {
        if (nav == null || role == null) return false;

        int destination = resolveDest(role.trim().toLowerCase());
        if (destination == 0) return false;

        if (nav.getCurrentDestination() != null
                && nav.getCurrentDestination().getId() == destination) {
            return true;
        }

        try {
            NavOptions options = new NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setPopUpTo(nav.getGraph().getStartDestinationId(), true)
                    .build();
            nav.navigate(destination, null, options);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    /** وجهة البداية حسب الدور — تتجنب تحميل شاشة المريض ثم إعادة التوجيه. */
    public static int startDestinationFor(@Nullable String role) {
        if (role == null || role.trim().isEmpty()) {
            return R.id.patientShortcutsFragment;
        }
        String r = role.trim().toLowerCase();
        switch (r) {
            case "specialist":
                return R.id.specialistShortcutsFragment;
            case "organization":
                return R.id.orgShortcutsFragment;
            case "admin":
                return R.id.adminShortcutsFragment;
            default:
                if (r.contains("admin")) {
                    return R.id.adminShortcutsFragment;
                }
                return R.id.patientShortcutsFragment;
        }
    }

    private static int resolveDest(String role) {
        switch (role) {
            case "specialist":
                return R.id.specialistShortcutsFragment;
            case "organization":
                return R.id.orgShortcutsFragment;
            case "admin":
                return R.id.adminShortcutsFragment;
            default:
                if (role.contains("admin")) {
                    return R.id.adminShortcutsFragment;
                }
                return 0;
        }
    }
}
