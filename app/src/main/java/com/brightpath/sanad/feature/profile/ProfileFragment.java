package com.brightpath.sanad.feature.profile;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.button.MaterialButton;
import com.brightpath.sanad.push.PushPreferencesBinder;
import com.brightpath.sanad.push.PushRegistrar;
import com.brightpath.sanad.R;
import com.brightpath.sanad.data.AppConfig;
import com.brightpath.sanad.data.auth.AuthRepository;
import com.brightpath.sanad.data.auth.TokenStore;
import com.brightpath.sanad.data.LanguageUiHelper;
import com.brightpath.sanad.models.User;
import com.brightpath.sanad.ui.ChangePasswordDialogHelper;
import com.brightpath.sanad.ui.LoginActivity;
import com.brightpath.sanad.ui.tour.CoachMarkManager;
import com.brightpath.sanad.ui.tour.CoachMarkStep;

public class ProfileFragment extends Fragment {
    private ProfileViewModel vm;
    private View content, errorContainer;
    private ProgressBar progress;
    private TextView tvName, tvEmail, tvRole, tvHeaderName, tvHeaderRole, tvHeaderStatus, tvInitial, tvAccountStatus;
    private View rowEmail, rowPhone, rowRole;
    private TextView tvPhone, tvPatientPrivacyNote;
    private View cardOrgInfo;
    private TextView tvOrgName, tvOrgStatus, tvOrgStats, tvOrgReviewNotes;
    private View btnResubmit;
    private String currentRole;
    private boolean coachMarksScheduled;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        try {
            return ProfileScreenViews.inflate(inflater, container, R.layout.fragment_profile, this);
        } catch (Throwable ignored) {
            android.widget.FrameLayout root = new android.widget.FrameLayout(inflater.getContext());
            android.widget.TextView label = new android.widget.TextView(inflater.getContext());
            label.setText(R.string.nav_profile);
            label.setPadding(48, 48, 48, 48);
            root.addView(label);
            return root;
        }
    }

    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle s){
        super.onViewCreated(v, s);
        try {
            bindProfileUi(v);
        } catch (Throwable ignored) {
            // Never let profile setup take down MainActivity (HyperOS recover-to-home).
        }
    }

    private void bindProfileUi(@NonNull View v) {
        content = v.findViewById(R.id.content);
        errorContainer = v.findViewById(R.id.errorContainer);
        progress = v.findViewById(R.id.progress);
        tvName = v.findViewById(R.id.tvName);
        tvEmail = v.findViewById(R.id.tvEmail);
        tvRole = v.findViewById(R.id.tvRole);
        tvHeaderName = v.findViewById(R.id.tvHeaderName);
        tvHeaderRole = v.findViewById(R.id.tvHeaderRole);
        tvHeaderStatus = v.findViewById(R.id.tvHeaderStatus);
        tvInitial = v.findViewById(R.id.tvInitial);
        tvAccountStatus = v.findViewById(R.id.tvAccountStatus);
        rowEmail = v.findViewById(R.id.rowEmail);
        rowPhone = v.findViewById(R.id.rowPhone);
        tvPhone = v.findViewById(R.id.tvPhone);
        tvPatientPrivacyNote = v.findViewById(R.id.tvPatientPrivacyNote);
        rowRole = v.findViewById(R.id.rowRole);
        cardOrgInfo = v.findViewById(R.id.cardOrgInfo);
        tvOrgName = v.findViewById(R.id.tvOrgName);
        tvOrgStatus = v.findViewById(R.id.tvOrgStatus);
        tvOrgStats = v.findViewById(R.id.tvOrgStats);
        tvOrgReviewNotes = v.findViewById(R.id.tvOrgReviewNotes);
        btnResubmit = v.findViewById(R.id.btnResubmit);

        MaterialButton btnRetry = v.findViewById(R.id.btnRetry);
        MaterialButton btnLogout = v.findViewById(R.id.btnLogout);
        MaterialButton btnLogoutHeader = v.findViewById(R.id.btnLogoutHeader);
        View rowChangePassword = v.findViewById(R.id.rowChangePassword);
        View rowWallet = v.findViewById(R.id.rowWallet);
        View rowContact = v.findViewById(R.id.rowContact);
        View rowAbout = v.findViewById(R.id.rowAbout);
        View rowPrivacy = v.findViewById(R.id.rowPrivacy);
        View rowDeleteAccount = v.findViewById(R.id.rowDeleteAccount);
        MaterialButtonToggleGroup groupLanguage = v.findViewById(R.id.groupLanguage);
        MaterialButtonToggleGroup groupTheme = v.findViewById(R.id.groupTheme);
        View btnLangArabic = v.findViewById(R.id.btnLangArabic);
        View btnLangEnglish = v.findViewById(R.id.btnLangEnglish);
        View btnLangTurkish = v.findViewById(R.id.btnLangTurkish);
        View btnThemeSanad = v.findViewById(R.id.btnThemeSanad);
        View btnThemeWardi = v.findViewById(R.id.btnThemeWardi);
        View btnThemeGraphite = v.findViewById(R.id.btnThemeGraphite);
        try {
            PushPreferencesBinder.bind(this, v);
        } catch (Throwable ignored) {}

        vm = new ViewModelProvider(this).get(ProfileViewModel.class);
        vm.getState().observe(getViewLifecycleOwner(), this::render);
        vm.getToast().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                try {
                    android.widget.Toast.makeText(requireContext(), msg, android.widget.Toast.LENGTH_SHORT).show();
                } catch (Throwable ignored) {}
                // Stay on Profile. Auto-navigating Home after a toast made
                // HyperOS look like Profile immediately bounced back.
            }
        });
        vm.load();

        View rowName = v.findViewById(R.id.rowName);
        if (rowName != null) {
            rowName.setOnClickListener(x -> showEditNameDialog());
        } else if (tvName != null) {
            tvName.setOnClickListener(x -> showEditNameDialog());
        }

        if (btnRetry != null) btnRetry.setOnClickListener(x -> vm.load());
        if (btnLogout != null) btnLogout.setOnClickListener(x -> confirmLogout(btnLogout));
        if (btnLogoutHeader != null) btnLogoutHeader.setOnClickListener(x -> confirmLogout(btnLogoutHeader));
        if (rowContact != null) {
            rowContact.setOnClickListener(x -> safeNavigate(R.id.contactUsFragment));
        }
        if (rowChangePassword != null) {
            rowChangePassword.setOnClickListener(x -> showChangePasswordDialog());
        }
        if (rowWallet != null) {
            rowWallet.setOnClickListener(x -> safeNavigate(R.id.walletFragment));
        }
        if (rowAbout != null) {
            rowAbout.setOnClickListener(x -> safeNavigate(R.id.aboutUsFragment));
        }
        if (rowPrivacy != null) {
            rowPrivacy.setOnClickListener(x -> safeNavigate(R.id.privacyPolicyFragment));
        }
        if (rowDeleteAccount != null) {
            rowDeleteAccount.setOnClickListener(x -> openDeleteAccountPage());
        }
        if (btnResubmit != null) {
            btnResubmit.setOnClickListener(x -> vm.resubmit(currentRole));
        }
        try {
            LanguageUiHelper.bindToggleGroup(
                    this,
                    groupLanguage,
                    R.id.btnLangArabic,
                    R.id.btnLangEnglish,
                    R.id.btnLangTurkish
            );
            ProfileScreenViews.passVerticalScrollToParent(groupLanguage);
            ProfileScreenViews.passVerticalScrollToParent(groupTheme);
        } catch (Throwable ignored) {}
        try {
            ProfileScreenViews.bindThemeGroup(
                    this, groupTheme, btnThemeSanad, btnThemeWardi, btnThemeGraphite);
        } catch (Throwable ignored) {}

        // Tips are scheduled from render() once profile content is visible.
    }

    @Override
    public void onDestroyView() {
        coachMarksScheduled = false;
        try {
            CoachMarkManager.dismissActive();
        } catch (Throwable ignored) {}
        super.onDestroyView();
    }

    private void maybeShowCoachMarks() {
        if (coachMarksScheduled) return;
        View root = getView();
        if (root == null || !isAdded()) return;
        if (content == null || content.getVisibility() != View.VISIBLE) return;
        coachMarksScheduled = true;
        // Wait for NestedScrollView layout — tips over language/theme otherwise
        // measure empty rects and crash on some OEM GPUs when Skip is tapped.
        root.post(() -> {
            if (!isAdded() || getView() == null) {
                coachMarksScheduled = false;
                return;
            }
            if (!isResumed()) {
                coachMarksScheduled = false;
                return;
            }
            try {
                View r = getView();
                if (r == null) {
                    coachMarksScheduled = false;
                    return;
                }
                View rowContact = r.findViewById(R.id.rowContact);
                View rowPrivacy = r.findViewById(R.id.rowPrivacy);
                java.util.List<CoachMarkStep> steps = new java.util.ArrayList<>();
                // Never tip language/theme toggles — Skip over recreate controls crashes on MIUI.
                if (rowContact != null) {
                    steps.add(CoachMarkManager.step(rowContact, R.string.tour_profile_contact_title, R.string.tour_profile_contact_desc));
                }
                if (rowPrivacy != null) {
                    steps.add(CoachMarkManager.step(rowPrivacy, R.string.tour_profile_privacy_title, R.string.tour_profile_privacy_desc));
                }
                if (!steps.isEmpty()) {
                    CoachMarkManager.showIfNeeded(ProfileFragment.this, "coach_profile", steps);
                }
            } catch (Throwable ignored) {
                coachMarksScheduled = false;
            }
        });
    }

    private void safeNavigate(int destId) {
        try {
            if (!isAdded()) return;
            CoachMarkManager.dismissActive();
            NavHostFragment.findNavController(this).navigate(destId);
        } catch (Throwable ignored) {}
    }

    private void render(ProfileViewModel.UIState state){
        if (state == null || !isAdded()) return;
        try {
            if (state.loading && state.data == null){ show(progress); return; }
            if (state.error != null && state.data == null){ show(errorContainer); return; }
            if (state.data != null){
                show(content);
                String name = state.data.name != null ? state.data.name : "-";
                String role = state.data.role != null ? state.data.role : "-";
                if (tvName != null) tvName.setText(name);
                if (tvRole != null) tvRole.setText(role);
                if (tvHeaderName != null) tvHeaderName.setText(name);
                if (tvHeaderRole != null) tvHeaderRole.setText(role);
                if (tvInitial != null) {
                    tvInitial.setText(ProfileScreenViews.initialOf(name));
                }
                currentRole = state.data.role;
                boolean isPatient = isPatientRole(currentRole);
                boolean isOrganization = isOrganizationRole(currentRole);
                if (rowRole != null) rowRole.setVisibility(View.GONE);
                if (tvHeaderRole != null) tvHeaderRole.setVisibility(View.GONE);
                if (tvPatientPrivacyNote != null) {
                    tvPatientPrivacyNote.setVisibility(isPatient ? View.VISIBLE : View.GONE);
                }
                bindOrganizationInfo(state.data, isOrganization);
                bindAccountStatus(state.data, isOrganization);
                View root = getView();
                View rowWallet = root != null ? root.findViewById(R.id.rowWallet) : null;
                if (rowWallet != null) {
                    rowWallet.setVisibility(isPatient ? View.VISIBLE : View.GONE);
                }
                if (isPatient) {
                    if (rowEmail != null) rowEmail.setVisibility(View.GONE);
                    if (rowPhone != null) rowPhone.setVisibility(View.GONE);
                } else {
                    if (rowEmail != null) rowEmail.setVisibility(View.VISIBLE);
                    if (tvEmail != null) {
                        tvEmail.setText(state.data.email != null ? state.data.email : "-");
                    }
                    String phone = state.data.phone;
                    boolean hasPhone = phone != null && !phone.trim().isEmpty();
                    if (rowPhone != null) rowPhone.setVisibility(hasPhone ? View.VISIBLE : View.GONE);
                    if (tvPhone != null && hasPhone) tvPhone.setText(phone);
                }
                maybeShowCoachMarks();
            }
        } catch (Throwable ignored) {
            // Profile UI must never crash the host Activity.
        }
    }

    private boolean isPatientRole(@Nullable String role) {
        if (role == null || role.trim().isEmpty()) return true;
        return role.toLowerCase().contains("patient");
    }

    private boolean isOrganizationRole(@Nullable String role) {
        return role != null && role.toLowerCase().contains("organization");
    }

    private void bindOrganizationInfo(User user, boolean isOrganization) {
        if (cardOrgInfo == null) return;
        if (!isOrganization) {
            cardOrgInfo.setVisibility(View.GONE);
            if (btnResubmit != null) btnResubmit.setVisibility(View.GONE);
            return;
        }
        cardOrgInfo.setVisibility(View.VISIBLE);
        User.OrgProfile org = user.org_profile;
        String orgName = org != null && org.name != null ? org.name : user.name;
        if (tvOrgName != null) tvOrgName.setText(orgName != null ? orgName : "-");
        String status = user.organization_status;
        if (status == null && org != null) status = org.status;
        if (tvOrgStatus != null) tvOrgStatus.setText(getString(R.string.profile_org_status_fmt, statusLabel(status)));
        if (tvOrgStats != null && org != null) {
            int members = org.members != null ? org.members : 0;
            int specialists = org.specialists != null ? org.specialists : 0;
            int beneficiaries = org.beneficiaries != null ? org.beneficiaries : 0;
            tvOrgStats.setText(getString(R.string.profile_org_stats_fmt, members, specialists, beneficiaries));
        } else if (tvOrgStats != null) {
            tvOrgStats.setText("");
        }
        String notes = user.org_rejection_reason;
        if (notes == null && org != null) notes = org.review_notes;
        if (tvOrgReviewNotes != null) {
            if (notes != null && !notes.trim().isEmpty()) {
                tvOrgReviewNotes.setVisibility(View.VISIBLE);
                tvOrgReviewNotes.setText(getString(R.string.profile_rejected_hint) + "\n" + notes);
            } else {
                tvOrgReviewNotes.setVisibility(View.GONE);
            }
        }
        if (btnResubmit != null) {
            boolean rejected = "rejected".equalsIgnoreCase(status);
            btnResubmit.setVisibility(rejected ? View.VISIBLE : View.GONE);
        }
    }

    private void bindAccountStatus(User user, boolean isOrganization) {
        String statusText = getString(R.string.profile_status_active);
        if (isOrganization) {
            String status = user.organization_status;
            if (user.org_profile != null && user.org_profile.status != null) status = user.org_profile.status;
            statusText = statusLabel(status);
        }
        if (tvHeaderStatus != null) tvHeaderStatus.setText(statusText);
        if (tvAccountStatus != null) tvAccountStatus.setText(statusText);
    }

    private String statusLabel(@Nullable String status) {
        if ("approved".equalsIgnoreCase(status)) return getString(R.string.specialist_verification_approved);
        if ("rejected".equalsIgnoreCase(status)) return getString(R.string.specialist_verification_rejected);
        if ("under_review".equalsIgnoreCase(status)) return getString(R.string.specialist_verification_under_review);
        return getString(R.string.specialist_verification_pending);
    }

    private void show(View target){
        if (content != null) content.setVisibility(target==content?View.VISIBLE:View.GONE);
        if (errorContainer != null) errorContainer.setVisibility(target==errorContainer?View.VISIBLE:View.GONE);
        if (progress != null) progress.setVisibility(target==progress?View.VISIBLE:View.GONE);
    }

    private void openDeleteAccountPage() {
        // Open immediately; don't block the tap on a settings round-trip.
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(AppConfig.deleteAccountUrl())));
    }

    private void confirmLogout(MaterialButton button) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.logout_confirm_title)
                .setMessage(R.string.logout_confirm_message)
                .setPositiveButton(R.string.logout, (d, w) -> performLogout(button))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void performLogout(MaterialButton button){
        if (button != null) button.setEnabled(false);
        final Context appContext = requireContext().getApplicationContext();
        // Leave the session instantly; revoke push/token in background.
        new TokenStore(appContext).clear();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        new Thread(() -> {
            try {
                PushRegistrar.unregisterBeforeLogout(appContext, false);
                new AuthRepository(appContext, AppConfig.BASE_URL).logoutRemoteOnly();
            } catch (Exception ignored) {}
        }).start();
    }

    private void showMenu(){
        java.util.List<String> labels = new java.util.ArrayList<>();
        java.util.List<Runnable> actions = new java.util.ArrayList<>();

        labels.add(getString(R.string.menu_about_us));
        actions.add(() -> NavHostFragment.findNavController(this).navigate(R.id.aboutUsFragment));

        labels.add(getString(R.string.menu_privacy_policy));
        actions.add(() -> NavHostFragment.findNavController(this).navigate(R.id.privacyPolicyFragment));

        labels.add(getString(R.string.menu_contact_us));
        actions.add(() -> NavHostFragment.findNavController(this).navigate(R.id.contactUsFragment));

        labels.add(getString(R.string.menu_change_language));
        actions.add(() -> NavHostFragment.findNavController(this).navigate(R.id.languageSettingsFragment));

        labels.add(getString(R.string.menu_change_theme));
        actions.add(() -> NavHostFragment.findNavController(this).navigate(R.id.themeSettingsFragment));

        labels.add(getString(R.string.admin_profile_change_password));
        actions.add(this::showChangePasswordDialog);

        if (!isPatientRole(currentRole)) {
            labels.add(getString(R.string.nav_specialists));
            actions.add(() -> NavHostFragment.findNavController(this).navigate(R.id.patientSpecialistsFragment));
        }

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.profile_menu_button)
                .setItems(labels.toArray(new CharSequence[0]), (d, which) -> actions.get(which).run())
                .show();
    }

    private void showEditNameDialog() {
        android.widget.EditText input = new android.widget.EditText(requireContext());
        input.setHint(R.string.profile_full_name);
        CharSequence current = tvName != null ? tvName.getText() : "";
        if (current != null && !"-".contentEquals(current)) {
            input.setText(current);
            input.setSelection(current.length());
        }
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.profile_full_name)
                .setView(input)
                .setPositiveButton(R.string.save, (d, w) -> {
                    String name = input.getText() != null ? input.getText().toString().trim() : "";
                    if (name.length() < 2) {
                        android.widget.Toast.makeText(requireContext(), R.string.login_error_username_required, android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }
                    vm.updateProfile(name, null, null);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showChangePasswordDialog() {
        ChangePasswordDialogHelper.show(requireContext(),
                (current, pass, confirm) -> vm.changePassword(current, pass, confirm));
    }
}
