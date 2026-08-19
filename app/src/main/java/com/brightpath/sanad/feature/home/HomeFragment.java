package com.brightpath.sanad.feature.home;

import android.os.Bundle;
import android.view.*;
import android.widget.ImageView;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.brightpath.sanad.R;
import com.brightpath.sanad.data.DashboardResponse;
import com.brightpath.sanad.data.ThemeStore;
import com.brightpath.sanad.data.auth.TokenStore;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.brightpath.sanad.router.RoleRouter;
import com.brightpath.sanad.ui.tour.CoachMarkManager;
import com.brightpath.sanad.ui.tour.CoachMarkStep;
import java.util.ArrayList;
import java.util.List;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;


public class HomeFragment extends Fragment {
    private HomeViewModel vm;
    private View progress, error, content;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvRole, tvUpcoming, tvUnread, tvPoints;
    private TextView tvNextTitle, tvNextType, tvNextTime, tvNoNext;
    private View cardNext, btnJoinNow;
    private View cardTools, cardIntake, cardPhysicianReferral, cardOnboarding, btnIntakeOpen;
    private LinearLayout onboardingSteps;
    private TextView tvJournalUnlocked;
    private View hero, cardHeroStats;
    private TextView tvIntakeStatus, tvIntakeSeverity, tvIntakeImpact, tvIntakePreferred, tvIntakeRisk, tvIntakeSpecialist;
    private ShortcutsAdapter adapter;
    private boolean redirected = false;
    private DashboardResponse.SessionSummary currentNext;
    private String currentRole;
    private String mode = "dashboard";
    private boolean allowRedirect = false;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        if (getArguments() != null) {
            String argMode = getArguments().getString("mode", "dashboard");
            mode = argMode != null ? argMode : "dashboard";
            allowRedirect = getArguments().getBoolean("allowRedirect", false);
        }
        if ("shortcuts".equalsIgnoreCase(mode)) {
            return i.inflate(R.layout.fragment_shortcuts, c, false);
        }
        return i.inflate(R.layout.fragment_home_dashboard, c, false);
    }

    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        progress = v.findViewById(R.id.progress);
        error    = v.findViewById(R.id.errorContainer);
        content  = v.findViewById(R.id.content);
        swipeRefresh = v.findViewById(R.id.swipeRefresh);
        hero = v.findViewById(R.id.hero);
        cardHeroStats = v.findViewById(R.id.cardHeroStats);
        tvRole   = v.findViewById(R.id.tvRole);
        tvUpcoming = v.findViewById(R.id.tvUpcoming);
        tvUnread   = v.findViewById(R.id.tvUnread);
        tvPoints   = v.findViewById(R.id.tvPoints);
        cardNext = v.findViewById(R.id.cardNextSession);
        tvNextTitle = v.findViewById(R.id.tvNextTitle);
        tvNextType  = v.findViewById(R.id.tvNextType);
        tvNextTime  = v.findViewById(R.id.tvNextTime);
        btnJoinNow  = v.findViewById(R.id.btnJoin);
        tvNoNext    = v.findViewById(R.id.tvNoNext);
        cardTools = v.findViewById(R.id.cardTools);
        cardIntake = v.findViewById(R.id.cardIntake);
        cardOnboarding = v.findViewById(R.id.cardOnboarding);
        onboardingSteps = v.findViewById(R.id.onboardingSteps);
        tvJournalUnlocked = v.findViewById(R.id.tvJournalUnlocked);
        cardPhysicianReferral = v.findViewById(R.id.cardPhysicianReferral);
        btnIntakeOpen = v.findViewById(R.id.btnIntakeOpen);
        tvIntakeStatus = v.findViewById(R.id.tvIntakeStatus);
        tvIntakeSeverity = v.findViewById(R.id.tvIntakeSeverity);
        tvIntakeImpact = v.findViewById(R.id.tvIntakeImpact);
        tvIntakePreferred = v.findViewById(R.id.tvIntakePreferred);
        tvIntakeRisk = v.findViewById(R.id.tvIntakeRisk);
        tvIntakeSpecialist = v.findViewById(R.id.tvIntakeSpecialist);
        ImageView imgLogo = v.findViewById(R.id.imgLogo);
        if (imgLogo != null) {
            try {
                ThemeStore themeStore = new ThemeStore(requireContext());
                imgLogo.setImageResource(themeStore.getLogoRes(false));
            } catch (OutOfMemoryError | Exception e) {
                try {
                    imgLogo.setImageResource(R.drawable.sanad_logo);
                } catch (Throwable ignored) {}
            }
        }

        RecyclerView rv = v.findViewById(R.id.rvShortcuts);
        if (rv != null) {
            if ("shortcuts".equalsIgnoreCase(mode)) {
                rv.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(requireContext()));
            } else {
                rv.setLayoutManager(new GridLayoutManager(requireContext(), 2));
            }
            adapter = new ShortcutsAdapter();
            rv.setAdapter(adapter);
            adapter.setOnClick(this::navigate);
        }
        View primary = v.findViewById(R.id.btnPrimaryAction);
        if (primary != null) {
            String role = new TokenStore(requireContext()).getRole();
            currentRole = role;
            boolean canBook = role == null || role.isEmpty() || "patient".equalsIgnoreCase(role);
            if (canBook) {
                primary.setVisibility(View.VISIBLE);
                primary.setOnClickListener(x -> navigateToDestination(R.id.bookSessionFragment));
            } else {
                // Specialists/org/admin must not see the patient "new session" CTA.
                primary.setVisibility(View.GONE);
            }
        }

        View btnGuide = v.findViewById(R.id.btnToolGuide);
        View btnAccess = v.findViewById(R.id.btnToolAccessibility);
        View btnSupport = v.findViewById(R.id.btnToolSupport);
        if (btnGuide != null) btnGuide.setOnClickListener(x -> navigateToDestination(R.id.libraryFragment));
        if (btnAccess != null) btnAccess.setOnClickListener(x -> navigateToDestination(R.id.aboutUsFragment));
        if (btnSupport != null) btnSupport.setOnClickListener(x -> navigateToDestination(R.id.contactUsFragment));

        View cardSafePlace = v.findViewById(R.id.cardSafePlace);
        if (cardSafePlace != null) {
            cardSafePlace.setOnClickListener(x -> navigateToDestination(R.id.selfChatFragment));
        }

        View btnBell = v.findViewById(R.id.btnBell);
        if (btnBell != null) {
            btnBell.setOnClickListener(x -> navigateToDestination(R.id.notificationsFragment));
        }

        View btnRetry = v.findViewById(R.id.btnRetry);
        if (btnRetry != null) {
            btnRetry.setOnClickListener(x -> vm.load());
        }
        if (btnIntakeOpen != null) {
            btnIntakeOpen.setOnClickListener(x -> navigateToDestination(R.id.patientIntakeFragment));
        }

        View statUpcoming = v.findViewById(R.id.statUpcoming);
        if (statUpcoming != null) {
            statUpcoming.setOnClickListener(x -> openUpcomingSessions());
        }

        vm = new ViewModelProvider(this).get(HomeViewModel.class);
        if ("shortcuts".equalsIgnoreCase(mode)) {
            showStaticShortcuts();
            vm.getState().observe(getViewLifecycleOwner(), this::render);
            if (swipeRefresh != null) {
                swipeRefresh.setOnRefreshListener(() -> showStaticShortcuts());
            }
        } else {
            vm.getState().observe(getViewLifecycleOwner(), this::render);
            if (swipeRefresh != null) {
                swipeRefresh.setOnRefreshListener(() -> vm.load());
            }
            vm.load();
        }

        v.post(() -> {
            java.util.List<CoachMarkStep> steps = new java.util.ArrayList<>();
            if (primary != null) steps.add(CoachMarkManager.step(primary, R.string.tour_home_primary_title, R.string.tour_home_primary_desc));
            if (rv != null) steps.add(CoachMarkManager.step(rv, R.string.tour_home_shortcuts_title, R.string.tour_home_shortcuts_desc));
            if (cardSafePlace != null) steps.add(CoachMarkManager.step(cardSafePlace, R.string.tour_home_safe_title, R.string.tour_home_safe_desc));
            if (btnJoinNow != null) steps.add(CoachMarkManager.step(btnJoinNow, R.string.tour_home_join_title, R.string.tour_home_join_desc));
            if (btnIntakeOpen != null) steps.add(CoachMarkManager.step(btnIntakeOpen, R.string.tour_home_intake_title, R.string.tour_home_intake_desc));
            CoachMarkManager.showIfNeeded(HomeFragment.this, "tour_home", steps);
        });
    }

    @Override public void onResume() {
        super.onResume();
        redirected = false;
        if (vm != null) {
            // Soft refresh: no double loading flash when returning to Home.
            vm.load(true);
        }
    }

    private void showStaticShortcuts() {
        if (swipeRefresh != null) {
            swipeRefresh.setRefreshing(false);
        }
        String role = new TokenStore(requireContext()).getRole();
        currentRole = role;
        if (progress != null) {
            progress.setVisibility(View.GONE);
        }
        if (error != null) {
            error.setVisibility(View.GONE);
        }
        if (content != null) {
            content.setVisibility(View.VISIBLE);
        }
        if (adapter != null) {
            adapter.setRole(role);
            adapter.submit(essentialShortcutsForRole(role));
        }
        applyMode();
    }

    private void render(UIState s) {
        if ("shortcuts".equalsIgnoreCase(mode)) {
            if (swipeRefresh != null && s != null && !s.loading) {
                swipeRefresh.setRefreshing(false);
            }
            if (s != null && s.error == null && !s.loading) {
                currentRole = s.role;
                if (adapter != null) {
                    adapter.setRole(s.role);
                    adapter.submit(essentialShortcutsForRole(s.role));
                }
            }
            applyMode();
            return;
        }
        if (swipeRefresh != null && s != null && !s.loading) {
            swipeRefresh.setRefreshing(false);
        }

        if (s == null) {
            if (progress != null) {
                show(progress);
            }
            return;
        }
        if (s.loading) {
            if (progress != null) {
                show(progress);
            }
            return;
        }
        if (s.error != null) {
            if (error != null) {
                show(error);
            }
            return;
        }
        if (content != null) {
            show(content);
        }

        if (allowRedirect && !redirected) {
            redirected = RoleRouter.redirect(this, s.role);
            if (redirected) return;
        }

        currentRole = s.role;
        if (tvRole != null) {
            tvRole.setText(arRole(s.role));
        }
        if (tvUpcoming != null) {
            tvUpcoming.setText(String.valueOf(s.upcoming));
        }
        if (tvUnread != null) {
            tvUnread.setText(String.valueOf(s.unread));
        }
        if (tvPoints != null) {
            tvPoints.setText(String.valueOf(s.points));
        }
        if (adapter != null) {
            adapter.setRole(s.role);
            java.util.List<DashboardResponse.Shortcut> shortcuts;
            if ("shortcuts".equalsIgnoreCase(mode)) {
                shortcuts = essentialShortcutsForRole(s.role);
            } else {
                shortcuts = new ArrayList<>();
            }
            adapter.submit(shortcuts);
        }
        updateToolsVisibility(s.role);
        bindIntake(s.role, s.intake, s.onboarding);
        bindOnboarding(s.role, s.onboarding);
        bindPhysicianReferral(s.role, s.intake);
        bindNextSession(s.nextSession, s.canJoinNext);
        applyMode();
    }

    private void applyMode() {
        boolean shortcutsOnly = "shortcuts".equalsIgnoreCase(mode);
        if (hero != null) hero.setVisibility(shortcutsOnly ? View.GONE : View.VISIBLE);
        if (cardHeroStats != null) cardHeroStats.setVisibility(shortcutsOnly ? View.GONE : View.VISIBLE);
        if (cardNext != null) cardNext.setVisibility(shortcutsOnly ? View.GONE : cardNext.getVisibility());
        if (tvNoNext != null) tvNoNext.setVisibility(shortcutsOnly ? View.GONE : tvNoNext.getVisibility());
        if (cardIntake != null) cardIntake.setVisibility(shortcutsOnly ? View.GONE : cardIntake.getVisibility());
        if (cardOnboarding != null) cardOnboarding.setVisibility(shortcutsOnly ? View.GONE : cardOnboarding.getVisibility());
        if (cardTools != null) cardTools.setVisibility(shortcutsOnly ? View.GONE : cardTools.getVisibility());
    }

    private void show(View t){
        if (progress != null) {
            progress.setVisibility(t==progress?View.VISIBLE:View.GONE);
        }
        if (error != null) {
            error.setVisibility(t==error?View.VISIBLE:View.GONE);
        }
        if (content != null) {
            content.setVisibility(t==content?View.VISIBLE:View.GONE);
        }
    }

    private void bindNextSession(DashboardResponse.SessionSummary next, boolean canJoin) {
        if (cardNext == null || tvNoNext == null) return;
        currentNext = next;
        if (next == null) {
            cardNext.setVisibility(View.GONE);
            tvNoNext.setVisibility(View.VISIBLE);
            cardNext.setOnClickListener(null);
            return;
        }
        cardNext.setVisibility(View.VISIBLE);
        tvNoNext.setVisibility(View.GONE);
        if (tvNextTitle != null) {
            tvNextTitle.setText(next.specialist_name != null && !next.specialist_name.isEmpty()
                    ? next.specialist_name : getString(R.string.next_session_title));
        }
        if (tvNextType != null) {
            tvNextType.setText(labelForType(next.type));
        }
        if (tvNextTime != null) {
            tvNextTime.setText(next.scheduled_at != null ? formatSchedule(next.scheduled_at)
                    : getString(R.string.next_session_time_placeholder));
        }
        if (btnJoinNow != null) {
            btnJoinNow.setVisibility(canJoin ? View.VISIBLE : View.GONE);
            btnJoinNow.setOnClickListener(x -> openJoinSession(next.id));
        }
        cardNext.setOnClickListener(x -> openSessionDetail(next.id));
    }

    private void updateToolsVisibility(@Nullable String role) {
        if (cardTools == null) return;
        boolean isPatient = role == null || role.equalsIgnoreCase("patient");
        boolean isSpecialist = role != null && role.equalsIgnoreCase("specialist");
        // Specialist home should not show guide / accessibility / support shortcuts.
        cardTools.setVisibility((isPatient || isSpecialist) ? View.GONE : View.VISIBLE);
    }

    private void bindOnboarding(@Nullable String role, @Nullable DashboardResponse.Onboarding onboarding) {
        if (cardOnboarding == null || onboardingSteps == null) return;
        boolean isPatient = role == null || role.equalsIgnoreCase("patient");
        boolean show = isPatient && onboarding != null
                && (onboarding.needs_intake || onboarding.needs_pre_session || onboarding.needs_vent);
        cardOnboarding.setVisibility(show ? View.VISIBLE : View.GONE);
        if (!show) return;
        onboardingSteps.removeAllViews();
        if (onboarding.needs_intake) {
            onboardingSteps.addView(buildOnboardingStep(
                    getString(R.string.onboarding_step_intake),
                    getString(R.string.onboarding_step_action),
                    () -> navigateToDestination(R.id.patientIntakeFragment)));
        }
        if (onboarding.needs_pre_session) {
            onboardingSteps.addView(buildOnboardingStep(
                    getString(R.string.onboarding_step_pre_session),
                    getString(R.string.onboarding_step_action),
                    () -> navigateToDestination(R.id.preSessionFragment)));
        }
        if (onboarding.needs_vent) {
            onboardingSteps.addView(buildOnboardingStep(
                    getString(R.string.onboarding_step_vent),
                    getString(R.string.onboarding_step_vent_action),
                    () -> navigateToDestination(R.id.ventFragment)));
        }
        if (tvJournalUnlocked != null) {
            tvJournalUnlocked.setVisibility(onboarding.journal_unlocked ? View.VISIBLE : View.GONE);
        }
    }

    private View buildOnboardingStep(String title, String actionLabel, Runnable action) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 0, 0, (int) (12 * getResources().getDisplayMetrics().density));
        TextView tv = new TextView(requireContext());
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        tv.setText(title);
        tv.setTextColor(getResources().getColor(R.color.sanad_on_bg, null));
        tv.setTextSize(14f);
        com.google.android.material.button.MaterialButton btn = new com.google.android.material.button.MaterialButton(
                requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btn.setText(actionLabel);
        btn.setOnClickListener(v -> action.run());
        row.addView(tv);
        row.addView(btn);
        return row;
    }

    private void bindPhysicianReferral(@Nullable String role, @Nullable DashboardResponse.Intake intake) {
        if (cardPhysicianReferral == null) return;
        boolean isPatient = role == null || role.equalsIgnoreCase("patient");
        boolean show = isPatient && intake != null && intake.external_physician_recommended;
        cardPhysicianReferral.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show && cardPhysicianReferral != null) {
            TextView tv = cardPhysicianReferral.findViewById(R.id.tvPhysicianReferral);
            if (tv != null) tv.setText(getString(R.string.external_physician_banner));
        }
    }

    private void bindIntake(@Nullable String role, @Nullable DashboardResponse.Intake intake, @Nullable DashboardResponse.Onboarding onboarding) {
        if (cardIntake == null) return;
        boolean isPatient = role == null || role.equalsIgnoreCase("patient");
        boolean completed = intake != null && intake.completed;
        boolean onboardingShowsIntake = onboarding != null && onboarding.needs_intake;
        cardIntake.setVisibility(isPatient && !completed && !onboardingShowsIntake ? View.VISIBLE : View.GONE);
        if (!isPatient) return;
        if (completed) return;
        if (intake == null) {
            setIntakeTexts(
                    getString(R.string.home_intake_incomplete),
                    getString(R.string.home_intake_severity_fmt, getString(R.string.not_available)),
                    getString(R.string.home_intake_impact_fmt, getString(R.string.not_available)),
                    getString(R.string.home_intake_preferred_fmt, getString(R.string.not_available)),
                    getString(R.string.home_intake_risk_fmt, getString(R.string.home_intake_no_risk)),
                    getString(R.string.home_intake_specialist)
            );
            return;
        }
        String updated = intake.updated_at != null ? formatSchedule(intake.updated_at) : getString(R.string.not_available);
        String severity = intake.severity_level != null ? intake.severity_level : getString(R.string.not_available);
        String impact = intake.impact_level != null ? intake.impact_level : getString(R.string.not_available);
        String preferred = formatPreferred(intake.preferred_session_mode);
        String risks = (intake.risk_flags != null && !intake.risk_flags.isEmpty())
                ? android.text.TextUtils.join(" - ", intake.risk_flags)
                : getString(R.string.home_intake_no_risk);
        String specialist = intake.recommended_specialist != null && intake.recommended_specialist.name != null
                ? intake.recommended_specialist.name
                : getString(R.string.home_intake_specialist);
        setIntakeTexts(
                intake.completed
                        ? getString(R.string.home_intake_updated_fmt, updated)
                        : getString(R.string.home_intake_incomplete),
                getString(R.string.home_intake_severity_fmt, severity),
                getString(R.string.home_intake_impact_fmt, impact),
                getString(R.string.home_intake_preferred_fmt, preferred),
                getString(R.string.home_intake_risk_fmt, risks),
                specialist
        );
    }

    private void setIntakeTexts(String status, String severity, String impact, String preferred, String risk, String specialist) {
        if (tvIntakeStatus != null) tvIntakeStatus.setText(status);
        if (tvIntakeSeverity != null) tvIntakeSeverity.setText(severity);
        if (tvIntakeImpact != null) tvIntakeImpact.setText(impact);
        if (tvIntakePreferred != null) tvIntakePreferred.setText(preferred);
        if (tvIntakeRisk != null) tvIntakeRisk.setText(risk);
        if (tvIntakeSpecialist != null) tvIntakeSpecialist.setText(specialist);
    }

    private String formatPreferred(@Nullable String mode) {
        if (mode == null || mode.isEmpty()) return getString(R.string.not_available);
        String v = mode.trim().toLowerCase();
        if (v.contains("video")) return getString(R.string.session_type_video);
        if (v.contains("voice") || v.contains("audio")) return getString(R.string.session_type_voice);
        if (v.contains("chat")) return getString(R.string.session_type_chat);
        return mode;
    }

    private String labelForType(@Nullable String raw) {
        if (raw == null) return getString(R.string.next_session_type_placeholder);
        switch (raw.toLowerCase()) {
            case "video":
                return getString(R.string.session_type_video);
            case "voice":
                return getString(R.string.session_type_voice);
            case "chat":
                return getString(R.string.session_type_chat);
            default:
                return raw;
        }
    }

    private String arRole(String role){
        if ("admin".equals(role)) return "الإدمن";
        if ("organization".equals(role)) return "المؤسسة";
        if ("specialist".equals(role)) return "الأخصائي";
        return "المريض";
    }

    private String formatSchedule(String raw){
        long ms = parseMillis(raw);
        if (ms <= 0) return raw;
        try {
            ZonedDateTime dt = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault());
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd - hh:mm a");
            return dt.format(fmt);
        } catch (Exception e){
            return raw;
        }
    }

    private long parseMillis(String raw){
        if (raw == null || raw.isEmpty()) return -1;
        try {
            return Instant.parse(raw).toEpochMilli();
        } catch (DateTimeParseException ignored){}
        try {
            return OffsetDateTime.parse(raw, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant().toEpochMilli();
        } catch (DateTimeParseException ignored){}
        return -1;
    }

    // ✅ تنقّل جاهز


    private void navigate(DashboardResponse.Shortcut s) {
        AppNavigator.goShortcut(this, currentRole, s);
    }

    private Integer destinationForShortcut(DashboardResponse.Shortcut shortcut) {
        return ShortcutNavigation.destinationFor(currentRole, shortcut);
    }

    private boolean isPatientRole() {
        return currentRole == null || currentRole.equalsIgnoreCase("patient");
    }

    private void openUpcomingSessions() {
        Bundle args = new Bundle();
        args.putString("filterStatus", "accepted");
        navigateToDestination(R.id.sessionsFragment, args);
    }

    private void navigateToDestination(int destination){
        navigateToDestination(destination, null);
    }

    private void navigateToDestination(int destination, @Nullable Bundle args){
        AppNavigator.go(this, currentRole, destination, args);
    }

    private void openSessionDetail(int sessionId){
        if (sessionId <= 0) return;
        Bundle args = new Bundle();
        args.putInt("sessionId", sessionId);
        navigateToDestination(R.id.sessionDetailFragment, args);
    }

    private void openJoinSession(int sessionId){
        // Hub path: detail owns join/accept rules and enters call/chat in-app.
        openSessionDetail(sessionId);
    }

    private List<DashboardResponse.Shortcut> essentialShortcutsForRole(@Nullable String role) {
        if (role != null && role.equalsIgnoreCase("specialist")) {
            return ShortcutNavigation.specialistEssentials();
        }
        return ShortcutNavigation.patientEssentials();
    }

    private List<DashboardResponse.Shortcut> patientDefaultShortcuts() {
        return ShortcutNavigation.patientEssentials();
    }

    private DashboardResponse.Shortcut makeShortcut(String id, String route) {
        DashboardResponse.Shortcut s = new DashboardResponse.Shortcut();
        s.id = id;
        s.route = route;
        return s;
    }

    @Override
    public void onDestroyView() {
        try { CoachMarkManager.dismissActive(); } catch (Throwable ignored) {}
        super.onDestroyView();
    }

}
