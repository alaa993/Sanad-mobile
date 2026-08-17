package com.brightpath.sanad.ui;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.graphics.Color;
import android.text.TextUtils;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomnavigation.LabelVisibilityMode;
import com.google.android.material.navigation.NavigationBarMenuView;
import com.google.android.material.navigation.NavigationBarItemView;
import com.google.android.material.navigation.NavigationBarView;
import com.brightpath.sanad.R;
import com.brightpath.sanad.data.ThemeStore;
import com.brightpath.sanad.data.AppConfig;
import com.brightpath.sanad.data.auth.AuthRepository;
import com.brightpath.sanad.data.auth.SessionGuard;
import com.brightpath.sanad.data.auth.TokenStore;
import com.brightpath.sanad.router.RoleRouter;
import com.brightpath.sanad.ui.tour.CoachMarkManager;
import com.brightpath.sanad.ui.tour.CoachMarkStep;
import com.brightpath.sanad.push.PushRegistrar;
import com.brightpath.sanad.feature.sessions.BookSessionFragment;

public class MainActivity extends AppCompatActivity {
  private NavController navController;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Register before any network so deleted-account clears redirect immediately.
        TokenStore.setSessionListener(() -> runOnUiThread(this::goToLoginClearedSession));
        if (!new TokenStore(this).hasToken()) {
            goToLoginClearedSession();
            return;
        }
        // Soft re-check only if Splash did not just validate (avoids double /me).
        final android.content.Context appCtx = getApplicationContext();
        new Thread(() -> {
            try {
                SessionGuard.validateBlocking(appCtx, 2500L);
            } catch (Throwable ignored) {
            }
            if (!new TokenStore(appCtx).hasToken()) {
                runOnUiThread(this::goToLoginClearedSession);
            }
        }, "main-session-gate").start();
        try {
            new com.brightpath.sanad.data.ThemeStore(this).applySavedTheme(this);
        } catch (Throwable ignored) {}
        try {
            applyStatusBarColor();
        } catch (Throwable ignored) {}
        try {
            setContentView(R.layout.activity_main);
        } catch (Throwable t) {
            try {
                Intent intent = new Intent(this, SplashActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            } catch (Throwable ignored) {}
            finish();
            return;
        }

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment == null) {
            // Inflate failure / OEM quirk — send user to login instead of crashing.
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        navController = navHostFragment.getNavController();
        String cachedRole = new TokenStore(this).getRole();
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        bottomNav.getMenu().clear();

        RoleUiConfig uiConfig = RoleUiConfig.from(cachedRole);
        try {
            bottomNav.inflateMenu(uiConfig.menuRes);
        } catch (Exception e) {
            try {
                bottomNav.inflateMenu(RoleUiConfig.patientFallback().menuRes);
            } catch (Exception e2) {
                bottomNav.setVisibility(android.view.View.GONE);
            }
        }
        try {
            bottomNav.setBackgroundColor(ThemeStore.chromeNavigationBarColor(this));
        } catch (Throwable ignored) {}
        ColorStateList tint = null;
        try {
            tint = ContextCompat.getColorStateList(this, R.color.bottom_nav_light_selector);
        } catch (Throwable ignored) {
            try {
                tint = ContextCompat.getColorStateList(this, uiConfig.tintRes);
            } catch (Throwable ignored2) {}
        }
        if (tint != null) {
            bottomNav.setItemTextColor(tint);
        }
        // We will tint non-center icons manually to keep the Sanad logo untouched.
        bottomNav.setItemIconTintList(null);

        String roleSafe = cachedRole != null ? cachedRole : "";
        boolean isAdmin = "admin".equalsIgnoreCase(roleSafe) || roleSafe.contains("admin");

        try {
            // Specialist: icons only + tighter padding/width
            if ("specialist".equalsIgnoreCase(roleSafe)) {
                bottomNav.setLabelVisibilityMode(NavigationBarView.LABEL_VISIBILITY_UNLABELED);
                bottomNav.setPadding(dp(6), dp(4), dp(6), dp(6));
                bottomNav.setItemIconSize(dp(26));
                android.view.ViewGroup.LayoutParams rawLp = bottomNav.getLayoutParams();
                if (rawLp instanceof ConstraintLayout.LayoutParams) {
                    ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) rawLp;
                    lp.width = ConstraintLayout.LayoutParams.WRAP_CONTENT;
                    lp.height = ConstraintLayout.LayoutParams.WRAP_CONTENT;
                    bottomNav.setLayoutParams(lp);
                }
            } else if ("patient".equalsIgnoreCase(roleSafe) || roleSafe.isEmpty() || isAdmin) {
                bottomNav.setLabelVisibilityMode(NavigationBarView.LABEL_VISIBILITY_UNLABELED);
                bottomNav.setItemIconSize(dp(30));
            } else {
                bottomNav.setLabelVisibilityMode(NavigationBarView.LABEL_VISIBILITY_UNLABELED);
                bottomNav.setItemIconSize(dp(26));
            }
            bottomNav.setItemActiveIndicatorEnabled(true);
        } catch (Throwable ignored) {
            // Material API / OEM layout quirks — keep defaults.
        }

        int logoRes = R.drawable.sanad_logo;
        try {
            logoRes = new ThemeStore(this).getLogoRes(true);
        } catch (Throwable ignored) {}
        try {
            if (bottomNav.getMenu().findItem(R.id.patientShortcutsFragment) != null) {
                bottomNav.getMenu().findItem(R.id.patientShortcutsFragment).setIcon(logoRes).setIconTintList(null);
            }
            if (bottomNav.getMenu().findItem(R.id.specialistShortcutsFragment) != null) {
                bottomNav.getMenu().findItem(R.id.specialistShortcutsFragment).setIcon(logoRes).setIconTintList(null);
            }
            if (bottomNav.getMenu().findItem(R.id.adminShortcutsFragment) != null) {
                bottomNav.getMenu().findItem(R.id.adminShortcutsFragment).setIcon(logoRes).setIconTintList(null);
            }
            if (bottomNav.getMenu().findItem(R.id.orgShortcutsFragment) != null) {
                bottomNav.getMenu().findItem(R.id.orgShortcutsFragment).setIcon(logoRes).setIconTintList(null);
            }
        } catch (OutOfMemoryError | Exception e) {
            // Oversized logo bitmaps on low-RAM devices — leave default menu icons.
        }
        if (tint != null) {
            if (bottomNav.getMenu().findItem(R.id.patientDashboardFragment) != null) {
                bottomNav.getMenu().findItem(R.id.patientDashboardFragment).setIconTintList(tint);
            }
            if (bottomNav.getMenu().findItem(R.id.profileFragment) != null) {
                bottomNav.getMenu().findItem(R.id.profileFragment).setIconTintList(tint);
            }
            if (bottomNav.getMenu().findItem(R.id.specialistDashboardFragment) != null) {
                bottomNav.getMenu().findItem(R.id.specialistDashboardFragment).setIconTintList(tint);
            }
            if (bottomNav.getMenu().findItem(R.id.specialistProfileFragment) != null) {
                bottomNav.getMenu().findItem(R.id.specialistProfileFragment).setIconTintList(tint);
            }
            if (bottomNav.getMenu().findItem(R.id.adminDashboardFragment) != null) {
                bottomNav.getMenu().findItem(R.id.adminDashboardFragment).setIconTintList(tint);
            }
            if (bottomNav.getMenu().findItem(R.id.adminProfileFragment) != null) {
                bottomNav.getMenu().findItem(R.id.adminProfileFragment).setIconTintList(tint);
            }
            if (bottomNav.getMenu().findItem(R.id.orgDashboardFragment) != null) {
                bottomNav.getMenu().findItem(R.id.orgDashboardFragment).setIconTintList(tint);
            }
        }

        try {
            // Do not use setupWithNavController alone — it fights RoleBoot's start destination.
            // Custom listener below owns tab switches.
            NavigationUI.setupWithNavController(bottomNav, navController);
        } catch (Throwable ignored) {}
        bottomNav.setOnItemSelectedListener(item -> {
            // Keep the center Sanad logo un-tinted even when selected.
            if (bottomNav.getMenu().findItem(R.id.patientShortcutsFragment) != null) {
                bottomNav.getMenu().findItem(R.id.patientShortcutsFragment).setIconTintList(null);
            }
            if (bottomNav.getMenu().findItem(R.id.specialistShortcutsFragment) != null) {
                bottomNav.getMenu().findItem(R.id.specialistShortcutsFragment).setIconTintList(null);
            }
            if (bottomNav.getMenu().findItem(R.id.adminShortcutsFragment) != null) {
                bottomNav.getMenu().findItem(R.id.adminShortcutsFragment).setIconTintList(null);
            }
            if (bottomNav.getMenu().findItem(R.id.orgShortcutsFragment) != null) {
                bottomNav.getMenu().findItem(R.id.orgShortcutsFragment).setIconTintList(null);
            }
            int destId = item.getItemId();
            try {
                if (navController.getCurrentDestination() != null
                        && navController.getCurrentDestination().getId() == destId) {
                    return true;
                }
                // Prefer navigate; pop only when that destination is already under us.
                if (isOnBackStack(navController, destId)
                        && navController.popBackStack(destId, false)) {
                    return true;
                }
                return NavigationUI.onNavDestinationSelected(item, navController);
            } catch (Throwable t) {
                return false;
            }
        });
        bottomNav.setOnItemReselectedListener(item -> {
            try {
                int destId = item.getItemId();
                if (isOnBackStack(navController, destId)) {
                    navController.popBackStack(destId, false);
                }
            } catch (Throwable ignored) {}
        });

        getWindow().getDecorView().post(() -> {
            try {
                android.view.View child0 = bottomNav.getChildAt(0);
                if (!(child0 instanceof NavigationBarMenuView)) return;
                NavigationBarMenuView menuView = (NavigationBarMenuView) child0;
                if (menuView.getChildCount() < 2) return;
                int centerIndex = 1;
                for (int i = 0; i < menuView.getChildCount(); i++) {
                    android.view.View rawChild = menuView.getChildAt(i);
                    if (!(rawChild instanceof NavigationBarItemView)) continue;
                    NavigationBarItemView item = (NavigationBarItemView) rawChild;
                    android.view.View icon = item.findViewById(com.google.android.material.R.id.navigation_bar_item_icon_view);
                    if (icon instanceof android.widget.ImageView) {
                        android.widget.ImageView img = (android.widget.ImageView) icon;
                        android.view.ViewGroup.LayoutParams lp = img.getLayoutParams();
                        int size = i == centerIndex ? dp(48) : dp(26);
                        lp.width = size;
                        lp.height = size;
                        img.setLayoutParams(lp);
                    }
                }
            } catch (Throwable ignored) {
                // OEM BottomNav hierarchy differs — skip cosmetic resize.
            }
        });

        // Defer push registration and coach marks so the first taps on bottom nav stay responsive.
        getWindow().getDecorView().postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;
            try {
                PushRegistrar.requestPermissionIfNeeded(this);
                PushRegistrar.sync(this);
            } catch (Throwable ignored) {}
            try {
                java.util.List<CoachMarkStep> steps = new java.util.ArrayList<>();
                steps.add(CoachMarkManager.step(bottomNav, R.string.coach_nav_title, R.string.coach_nav_desc));
                CoachMarkManager.showIfNeeded(this, "coach_nav", steps);
            } catch (Throwable ignored) {}
            try {
                handlePushIntent(getIntent());
            } catch (Throwable ignored) {}
        }, 250);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!new TokenStore(this).hasToken()) {
            goToLoginClearedSession();
        }
    }

    @Override
    protected void onDestroy() {
        try {
            CoachMarkManager.dismissActive();
        } catch (Throwable ignored) {}
        TokenStore.setSessionListener(null);
        super.onDestroy();
    }

    /** Deleted/expired account: leave Main without crashing feature screens. */
    private void goToLoginClearedSession() {
        try {
            if (isFinishing()) return;
            if (android.os.Build.VERSION.SDK_INT >= 17 && isDestroyed()) return;
        } catch (Throwable ignored) {
            return;
        }
        if (new TokenStore(this).hasToken()) return;
        try {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } catch (Throwable ignored) {}
    }

    private static boolean isOnBackStack(NavController nav, int destId) {
        try {
            nav.getBackStackEntry(destId);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handlePushIntent(intent);
    }

    private void handlePushIntent(Intent intent) {
        if (intent == null || navController == null) return;
        String type = intent.getStringExtra(PushRegistrar.EXTRA_PUSH_TYPE);
        if (TextUtils.isEmpty(type)) {
            type = intent.getStringExtra("type");
        }
        final String sessionIdStr = firstExtra(intent, PushRegistrar.EXTRA_SESSION_ID, "session_id");
        final String specialistIdStr = firstExtra(intent, PushRegistrar.EXTRA_SPECIALIST_ID, "specialist_id");
        intent.removeExtra(PushRegistrar.EXTRA_PUSH_TYPE);
        intent.removeExtra("type");
        intent.removeExtra(PushRegistrar.EXTRA_SESSION_ID);
        intent.removeExtra(PushRegistrar.EXTRA_SPECIALIST_ID);

        // Reminder / transfer payloads can arrive with only session_id.
        if (TextUtils.isEmpty(type) && sessionIdStr != null) {
            type = "session";
        }
        if (TextUtils.isEmpty(type)) return;

        final String resolvedType = type;
        getWindow().getDecorView().post(() -> {
            if (navController == null) return;
            if ("session".equals(resolvedType)
                    || "transfer".equals(resolvedType)
                    || "session_reminder".equals(resolvedType)
                    || "appointment_reminder".equals(resolvedType)) {
                if (sessionIdStr != null) {
                    try {
                        int sessionId = Integer.parseInt(sessionIdStr);
                        android.os.Bundle args = new android.os.Bundle();
                        args.putInt("sessionId", sessionId);
                        String role = new TokenStore(this).getRole();
                        if (role != null && role.equalsIgnoreCase("specialist")) {
                            navController.navigate(R.id.specialistSessionDetailFragment, args);
                        } else {
                            args.putBoolean("isSpecialist", false);
                            navController.navigate(R.id.sessionDetailFragment, args);
                        }
                    } catch (NumberFormatException ignored) {}
                }
            } else if ("physician_referral".equals(resolvedType)) {
                if (specialistIdStr != null) {
                    try {
                        int specialistId = Integer.parseInt(specialistIdStr);
                        android.os.Bundle args = new android.os.Bundle();
                        args.putInt(BookSessionFragment.ARG_SPECIALIST_ID, specialistId);
                        navController.navigate(R.id.bookSessionFragment, args);
                    } catch (NumberFormatException ignored) {
                        navController.navigate(R.id.patientSpecialistsFragment);
                    }
                } else {
                    navController.navigate(R.id.patientSpecialistsFragment);
                }
            }
        });
    }

    private static String firstExtra(Intent intent, String... keys) {
        for (String key : keys) {
            String value = intent.getStringExtra(key);
            if (!TextUtils.isEmpty(value)) return value;
            if (intent.hasExtra(key)) {
                int asInt = intent.getIntExtra(key, Integer.MIN_VALUE);
                if (asInt != Integer.MIN_VALUE) return String.valueOf(asInt);
            }
        }
        return null;
    }

    private int dp(int value){
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

    private void applyStatusBarColor() {
        ThemeStore.applyLightSystemBars(this);
    }

    private static class RoleUiConfig {
        final int menuRes;
        final int backgroundRes;
        final int tintRes;

        private RoleUiConfig(int menuRes, int backgroundRes, int tintRes) {
            this.menuRes = menuRes;
            this.backgroundRes = backgroundRes;
            this.tintRes = tintRes;
        }

        static RoleUiConfig from(String role) {
            if (role == null) {
                return defaultConfig();
            }
            String r = role.trim().toLowerCase();
            switch (r) {
                case "specialist":
                    return new RoleUiConfig(
                            R.menu.menu_bottom_nav_specialist,
                            R.drawable.bg_bottom_nav_primary,
                            R.color.bottom_nav_patient_selector
                    );
                case "organization":
                    return new RoleUiConfig(
                            R.menu.menu_bottom_nav_org,
                            R.drawable.bg_bottom_nav_primary,
                            R.color.bottom_nav_patient_selector
                    );
                case "admin":
                    return new RoleUiConfig(
                            R.menu.menu_bottom_nav_admin,
                            R.drawable.bg_bottom_nav_primary,
                            R.color.bottom_nav_patient_selector
                    );
                default:
                    if (r.contains("admin")) {
                        return new RoleUiConfig(
                                R.menu.menu_bottom_nav_admin,
                                R.drawable.bg_bottom_nav_primary,
                                R.color.bottom_nav_patient_selector
                        );
                    }
                    return defaultConfig();
            }
        }

        private static RoleUiConfig defaultConfig() {
            return new RoleUiConfig(
                    R.menu.menu_bottom_nav,
                    R.drawable.bg_bottom_nav_primary,
                    R.color.bottom_nav_patient_selector
            );
        }

        /** Visible to MainActivity for safe menu fallback. */
        static RoleUiConfig patientFallback() {
            return defaultConfig();
        }
    }
}
