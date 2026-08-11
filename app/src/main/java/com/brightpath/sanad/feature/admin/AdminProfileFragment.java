package com.brightpath.sanad.feature.admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.brightpath.sanad.R;
import com.brightpath.sanad.data.AppConfig;
import com.brightpath.sanad.data.ThemeStore;
import com.brightpath.sanad.data.auth.AuthRepository;
import com.brightpath.sanad.ui.ChangePasswordDialogHelper;
import com.brightpath.sanad.push.PushPreferencesBinder;
import com.brightpath.sanad.push.PushRegistrar;
import com.brightpath.sanad.ui.LoginActivity;

import java.util.HashMap;
import java.util.Map;

public class AdminProfileFragment extends Fragment {
    private AdminViewModels.ProfileVM vm;
    private ImageView imgAvatar;
    private EditText etName, etLocale, etPhone, etPrivacy, etContact, etPlatformFee;
    private TextView tvEmail, tvStats;
    private ProgressBar progress;
    private ActivityResultLauncher<String> avatarPicker;
    private android.net.Uri pendingAvatar;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_profile, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        avatarPicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                pendingAvatar = uri;
                if (imgAvatar != null) {
                    Glide.with(this).load(uri).circleCrop().placeholder(R.drawable.ic_profile).into(imgAvatar);
                }
            }
        });
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        imgAvatar = v.findViewById(R.id.imgAdminAvatar);
        etName = v.findViewById(R.id.etAdminName);
        etLocale = v.findViewById(R.id.etAdminLocale);
        etPhone = v.findViewById(R.id.etAdminPhone);
        etPrivacy = v.findViewById(R.id.etPrivacy);
        etContact = v.findViewById(R.id.etContact);
        etPlatformFee = v.findViewById(R.id.etPlatformFee);
        tvEmail = v.findViewById(R.id.tvAdminEmail);
        tvStats = v.findViewById(R.id.tvAdminStats);
        progress = v.findViewById(R.id.profileProgress);
        ImageView logo = v.findViewById(R.id.imgProfileLogo);
        if (logo != null) {
            logo.setImageResource(new ThemeStore(requireContext()).getLogoRes(false));
        }
        MaterialButton btnSave = v.findViewById(R.id.btnSaveProfile);
        MaterialButton btnSaveSettings = v.findViewById(R.id.btnSaveSettings);
        MaterialButton btnChangePassword = v.findViewById(R.id.btnChangePassword);
        MaterialButton btnPickAvatar = v.findViewById(R.id.btnPickAvatar);
        MaterialButton btnWallet = v.findViewById(R.id.btnAdminWallet);
        MaterialButton btnLogout = v.findViewById(R.id.btnLogout);

        vm = new ViewModelProvider(this).get(AdminViewModels.ProfileVM.class);
        vm.state.observe(getViewLifecycleOwner(), profile -> {
            if (profile == null) return;
            progress.setVisibility(View.GONE);
            etName.setText(profile.name != null ? profile.name : "");
            etLocale.setText(profile.locale != null ? profile.locale : "");
            etPhone.setText(profile.phone != null ? profile.phone : "");
            tvEmail.setText(profile.email != null ? profile.email : "-");
            if (!TextUtils.isEmpty(profile.privacy_policy)) etPrivacy.setText(profile.privacy_policy);
            if (!TextUtils.isEmpty(profile.contact_info)) etContact.setText(profile.contact_info);
            if (profile.platform_fee_percent != null && etPlatformFee != null) {
                etPlatformFee.setText(String.valueOf(profile.platform_fee_percent));
            }
            if (profile.stats != null) {
                tvStats.setText(getString(R.string.admin_profile_stats_fmt,
                        profile.stats.pending_specialists,
                        profile.stats.pending_organizations,
                        profile.stats.total_users,
                        profile.stats.total_sessions));
            }
            Glide.with(this)
                    .load(profile.avatar)
                    .placeholder(R.drawable.ic_profile)
                    .circleCrop()
                    .into(imgAvatar);
        });
        vm.toast.observe(getViewLifecycleOwner(), msg -> {
            if (!TextUtils.isEmpty(msg)) Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
        });

        btnPickAvatar.setOnClickListener(x -> { pendingAvatar = null; avatarPicker.launch("image/*"); });
        btnSave.setOnClickListener(x -> saveProfile());
        if (btnChangePassword != null) {
            btnChangePassword.setOnClickListener(x -> showChangePasswordDialog());
        }
        btnSaveSettings.setOnClickListener(x -> saveSettings());
        btnWallet.setOnClickListener(x -> NavHostFragment.findNavController(this).navigate(R.id.adminWalletFragment));
        btnLogout.setOnClickListener(x -> confirmLogout(btnLogout));
        wireQuickLink(v, R.id.btnQuickSpecialists, R.id.adminSpecialistsFragment);
        wireQuickLink(v, R.id.btnQuickOrgs, R.id.adminOrganizationsFragment);
        wireQuickLink(v, R.id.btnQuickSessions, R.id.adminSessionsFragment);
        wireQuickLink(v, R.id.btnQuickUsers, R.id.adminUsersFragment);
        PushPreferencesBinder.bind(this, v);

        progress.setVisibility(View.VISIBLE);
        vm.load();
    }

    private void saveProfile(){
        Map<String,Object> body = new HashMap<>();
        put(body, "name", etName);
        put(body, "locale", etLocale);
        put(body, "phone", etPhone);
        if (pendingAvatar != null) {
            vm.uploadAvatar(pendingAvatar);
            pendingAvatar = null;
        }
        vm.save(body);
    }

    private void showChangePasswordDialog() {
        ChangePasswordDialogHelper.show(requireContext(),
                (current, pass, confirm) -> vm.changePassword(current, pass, confirm));
    }

    private void saveSettings(){
        vm.saveSettings(text(etPrivacy), text(etContact), text(etPlatformFee));
    }

    private void wireQuickLink(View root, int buttonId, int destination) {
        MaterialButton btn = root.findViewById(buttonId);
        if (btn != null) {
            btn.setOnClickListener(x -> NavHostFragment.findNavController(this).navigate(destination));
        }
    }

    private void put(Map<String,Object> map, String key, EditText et){
        String val = text(et);
        if (!TextUtils.isEmpty(val)) map.put(key, val);
    }

    private String text(EditText et){
        return et.getText()!=null ? et.getText().toString().trim() : "";
    }


    @Override
    public void onDestroyView() {
        try {
            com.brightpath.sanad.ui.tour.CoachMarkManager.dismissActive();
        } catch (Throwable ignored) {}
        super.onDestroyView();
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
        final android.content.Context appContext = requireContext().getApplicationContext();
        new com.brightpath.sanad.data.auth.TokenStore(appContext).clear();
        android.content.Intent intent = new android.content.Intent(requireContext(), LoginActivity.class);
        intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        new Thread(() -> {
            try {
                PushRegistrar.unregisterBeforeLogout(appContext, false);
                new AuthRepository(appContext, AppConfig.BASE_URL).logoutRemoteOnly();
            } catch (Exception ignored) {}
        }).start();
    }
}
