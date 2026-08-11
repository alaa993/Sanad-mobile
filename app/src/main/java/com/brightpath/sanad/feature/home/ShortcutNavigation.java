package com.brightpath.sanad.feature.home;

import android.os.Bundle;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;

import com.brightpath.sanad.R;
import com.brightpath.sanad.data.DashboardResponse;
import com.brightpath.sanad.feature.admin.AdminHomeFragment;
import com.brightpath.sanad.feature.org.OrgHomeFragment;
import com.brightpath.sanad.feature.specialist.SpecialistShortcutsFragment;

import java.util.ArrayList;
import java.util.List;

/** تعريف الاختصارات والتنقّل الموحّد لكل الأدوار. */
public final class ShortcutNavigation {

    private ShortcutNavigation() {}

    public static List<DashboardResponse.Shortcut> patientEssentials() {
        List<DashboardResponse.Shortcut> list = new ArrayList<>();
        list.add(make("community", "community"));
        list.add(make("sessions", "sessions"));
        list.add(make("specialists", "specialists"));
        list.add(make("library", "library"));
        return list;
    }

    public static List<DashboardResponse.Shortcut> specialistEssentials() {
        List<DashboardResponse.Shortcut> list = new ArrayList<>();
        list.add(make("sessions", "sessions"));
        list.add(make("patients", "patients"));
        list.add(make("community", "community"));
        list.add(make("library", "library"));
        list.add(make("group_sessions", "group_sessions"));
        return list;
    }

    @Nullable
    public static Integer destinationFor(@Nullable String role, DashboardResponse.Shortcut shortcut) {
        if (shortcut == null) return null;
        String key = shortcut.route != null ? shortcut.route : shortcut.id;
        if (key == null) return null;
        key = key.trim().toLowerCase();
        boolean patient = role == null || role.equalsIgnoreCase("patient");
        boolean specialist = role != null && role.equalsIgnoreCase("specialist");

        if (patient) {
            if ("sessions".equals(key)) return R.id.sessionsFragment;
            if ("community".equals(key) || "groups".equals(key)) return R.id.communityListFragment;
            if ("wallet".equals(key)) return R.id.walletFragment;
            if ("library".equals(key)) return R.id.libraryFragment;
            if ("chat".equals(key)) return R.id.chatListFragment;
            if ("vent".equals(key)) return R.id.ventFragment;
            if ("tasks".equals(key) || "patient_tasks".equals(key)) return R.id.patientTasksFragment;
            if ("notifications".equals(key)) return R.id.notificationsFragment;
            if ("specialists".equals(key) || "calendar".equals(key)) return R.id.patientSpecialistsFragment;
            if (key.startsWith("book")) return R.id.bookSessionFragment;
        }

        if (specialist) {
            if ("sessions".equals(key)) return R.id.specialistSessionsFragment;
            if ("patients".equals(key)) return R.id.specialistPatientsFragment;
            if ("community".equals(key) || "groups".equals(key)) return R.id.communityListFragment;
            if ("library".equals(key)) return R.id.libraryFragment;
            if ("group_sessions".equals(key) || "group_session".equals(key)) return R.id.groupsFragment;
            if ("chat".equals(key)) return R.id.chatListFragment;
        }

        if ("approve_specialists".equals(key)) return R.id.adminSpecialistsFragment;
        if ("approve_orgs".equals(key)) return R.id.adminOrganizationsFragment;
        if ("users".equals(key)) return R.id.adminUsersFragment;
        if ("reports".equals(key)) return R.id.reportsFragment;
        if ("vent".equals(key)) return R.id.adminVentFragment;
        if ("daily_tips".equals(key)) return R.id.adminDailyTipsFragment;
        if ("settings".equals(key)) return R.id.adminProfileFragment;
        if ("community".equals(key) || "groups".equals(key)) return R.id.communityListFragment;
        if ("beneficiaries".equals(key) || "add_beneficiary".equals(key)) return R.id.orgBeneficiariesFragment;
        if ("group_session".equals(key)) return R.id.orgSessionsFragment;
        if ("billing".equals(key)) return R.id.orgBillingFragment;
        // community_room requires API lookup — handled in OrgHomeFragment.handleQuickAction

        switch (key) {
            case "library": return R.id.libraryFragment;
            case "chat": return R.id.chatListFragment;
            case "community": return R.id.communityListFragment;
            case "wallet": return R.id.walletFragment;
            case "notifications": return R.id.notificationsFragment;
            case "sessions": return R.id.sessionsFragment;
            default: return null;
        }
    }

    /**
     * Infer the shortcuts tab destination when HomeFragment is reused across graph nodes.
     */
    @IdRes
    public static int resolveSourceDestination(Fragment fragment, NavController nav) {
        if (nav != null && nav.getCurrentDestination() != null) {
            int current = nav.getCurrentDestination().getId();
            if (current == R.id.patientShortcutsFragment
                    || current == R.id.specialistShortcutsFragment
                    || current == R.id.adminShortcutsFragment
                    || current == R.id.orgShortcutsFragment) {
                return current;
            }
        }
        if (fragment instanceof SpecialistShortcutsFragment) {
            return R.id.specialistShortcutsFragment;
        }
        if (fragment instanceof AdminHomeFragment) {
            Bundle args = fragment.getArguments();
            if (args != null && "shortcuts".equalsIgnoreCase(args.getString("mode"))) {
                return R.id.adminShortcutsFragment;
            }
        }
        if (fragment instanceof OrgHomeFragment) {
            Bundle args = fragment.getArguments();
            if (args != null && "shortcuts".equalsIgnoreCase(args.getString("mode"))) {
                return R.id.orgShortcutsFragment;
            }
        }
        if (fragment instanceof HomeFragment) {
            Bundle args = fragment.getArguments();
            if (args != null && "shortcuts".equalsIgnoreCase(args.getString("mode"))) {
                return R.id.patientShortcutsFragment;
            }
        }
        return nav != null && nav.getCurrentDestination() != null
                ? nav.getCurrentDestination().getId()
                : 0;
    }

    /** ترتيب المحاولات: الوجهة مباشرة → مسار عام → مسار شاشة الاختصارات. */
    public static int[] routeAttempts(int currentDest, @Nullable String role, @IdRes int destination) {
        int scoped = scopedActionFor(currentDest, role, destination);
        int global = globalActionFor(destination);
        java.util.LinkedHashSet<Integer> ordered = new java.util.LinkedHashSet<>();
        ordered.add(destination);
        if (global != 0 && global != destination) ordered.add(global);
        if (scoped != 0 && scoped != destination) ordered.add(scoped);
        int[] attempts = new int[ordered.size()];
        int i = 0;
        for (Integer route : ordered) {
            attempts[i++] = route;
        }
        return attempts;
    }

    @IdRes
    private static int scopedActionFor(int currentDest, @Nullable String role, @IdRes int destination) {
        if (currentDest == R.id.patientShortcutsFragment) {
            if (destination == R.id.communityListFragment) return R.id.action_patient_shortcuts_community;
            if (destination == R.id.sessionsFragment) return R.id.action_patient_shortcuts_sessions;
            if (destination == R.id.patientSpecialistsFragment) return R.id.action_patient_shortcuts_specialists;
            if (destination == R.id.libraryFragment) return R.id.action_patient_shortcuts_library;
            if (destination == R.id.chatListFragment) return R.id.action_patient_shortcuts_chat;
            if (destination == R.id.walletFragment) return R.id.action_patient_shortcuts_wallet;
            if (destination == R.id.bookSessionFragment) return R.id.action_patient_shortcuts_book;
            if (destination == R.id.notificationsFragment) return R.id.action_patient_shortcuts_notifications;
            if (destination == R.id.selfChatFragment) return R.id.action_patient_shortcuts_self_chat;
            if (destination == R.id.patientIntakeFragment) return R.id.action_patient_shortcuts_intake;
            if (destination == R.id.ventFragment) return R.id.action_patient_shortcuts_vent;
            if (destination == R.id.patientTasksFragment) return R.id.action_patient_shortcuts_tasks;
        }
        if (currentDest == R.id.specialistPatientsFragment) {
            if (destination == R.id.specialistPatientFileFragment) return R.id.action_specialist_patients_to_file;
        }
        if (currentDest == R.id.specialistShortcutsFragment) {
            if (destination == R.id.specialistSessionsFragment) return R.id.action_specialist_shortcuts_sessions;
            if (destination == R.id.specialistPatientsFragment) return R.id.action_specialist_shortcuts_patients;
            if (destination == R.id.communityListFragment) return R.id.action_specialist_shortcuts_community;
            if (destination == R.id.libraryFragment) return R.id.action_specialist_shortcuts_library;
            if (destination == R.id.groupsFragment) return R.id.action_specialist_shortcuts_groups;
        }
        if (currentDest == R.id.adminShortcutsFragment) {
            if (destination == R.id.adminSpecialistsFragment) return R.id.action_admin_shortcuts_specialists;
            if (destination == R.id.adminOrganizationsFragment) return R.id.action_admin_shortcuts_orgs;
            if (destination == R.id.adminSessionsFragment) return R.id.action_admin_shortcuts_sessions;
            if (destination == R.id.adminUsersFragment) return R.id.action_admin_shortcuts_users;
            if (destination == R.id.adminLibraryFragment) return R.id.action_admin_shortcuts_library;
            if (destination == R.id.reportsFragment) return R.id.action_admin_shortcuts_reports;
            if (destination == R.id.adminWalletFragment) return R.id.action_admin_shortcuts_wallet;
            if (destination == R.id.adminVentFragment) return R.id.action_admin_shortcuts_vent;
            if (destination == R.id.adminDailyTipsFragment) return R.id.action_admin_shortcuts_daily_tips;
            if (destination == R.id.adminProfileFragment) return R.id.action_admin_shortcuts_settings;
            if (destination == R.id.communityListFragment) return R.id.action_admin_shortcuts_community;
        }
        if (currentDest == R.id.orgShortcutsFragment) {
            if (destination == R.id.orgBeneficiariesFragment) return R.id.action_org_shortcuts_beneficiaries;
            if (destination == R.id.orgSessionsFragment) return R.id.action_org_shortcuts_sessions;
            if (destination == R.id.orgReportsFragment) return R.id.action_org_shortcuts_reports;
            if (destination == R.id.orgBillingFragment) return R.id.action_org_shortcuts_billing;
            if (destination == R.id.communityFeedFragment) return R.id.action_org_shortcuts_community_feed;
        }
        return 0;
    }

    @IdRes
    private static int globalActionFor(@IdRes int destination) {
        if (destination == R.id.specialistSessionsFragment) return R.id.action_global_specialist_sessions;
        if (destination == R.id.specialistPatientsFragment) return R.id.action_global_specialist_patients;
        if (destination == R.id.communityListFragment) return R.id.action_global_community_list;
        if (destination == R.id.libraryFragment) return R.id.action_global_library;
        if (destination == R.id.chatListFragment) return R.id.action_global_chat_list;
        if (destination == R.id.sessionsFragment) return R.id.action_global_sessions;
        if (destination == R.id.patientSpecialistsFragment) return R.id.action_global_patient_specialists;
        if (destination == R.id.walletFragment) return R.id.action_global_wallet;
        if (destination == R.id.ventFragment) return R.id.action_global_vent;
        if (destination == R.id.patientTasksFragment) return R.id.action_global_patient_tasks;
        if (destination == R.id.notificationsFragment) return R.id.action_global_notifications;
        if (destination == R.id.groupsFragment) return R.id.action_global_groups;
        if (destination == R.id.availabilityFragment) return R.id.action_global_availability;
        return 0;
    }

    private static DashboardResponse.Shortcut make(String id, String route) {
        DashboardResponse.Shortcut s = new DashboardResponse.Shortcut();
        s.id = id;
        s.route = route;
        return s;
    }
}
