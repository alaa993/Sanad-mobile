package com.brightpath.sanad.feature.admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.brightpath.sanad.R;
import com.google.android.material.button.MaterialButton;

public class AdminSettingsFragment extends Fragment {
    private ProgressBar progress;
    private TextView tvError, tvPlatformFee, tvContactInfo;
    private LinearLayout contentGroup;
    private AdminRepository repo;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        progress = view.findViewById(R.id.progress);
        tvError = view.findViewById(R.id.tvError);
        contentGroup = view.findViewById(R.id.contentGroup);
        tvPlatformFee = view.findViewById(R.id.tvPlatformFee);
        tvContactInfo = view.findViewById(R.id.tvContactInfo);
        MaterialButton btnPrivacy = view.findViewById(R.id.btnPrivacy);
        MaterialButton btnContact = view.findViewById(R.id.btnContact);
        MaterialButton btnLanguage = view.findViewById(R.id.btnLanguage);

        btnPrivacy.setOnClickListener(v -> NavHostFragment.findNavController(this).navigate(R.id.privacyPolicyFragment));
        btnContact.setOnClickListener(v -> NavHostFragment.findNavController(this).navigate(R.id.contactUsFragment));
        btnLanguage.setOnClickListener(v -> NavHostFragment.findNavController(this).navigate(R.id.languageSettingsFragment));

        repo = new AdminRepository(requireContext());
        loadSettings();
    }

    private void loadSettings() {
        progress.setVisibility(View.VISIBLE);
        contentGroup.setVisibility(View.GONE);
        tvError.setVisibility(View.GONE);
        repo.settings(new AdminRepository.Cb<AdminModels.AdminSettings>() {
            @Override public void ok(AdminModels.AdminSettings settings) {
                if (!isAdded()) return;
                progress.setVisibility(View.GONE);
                contentGroup.setVisibility(View.VISIBLE);
                Integer fee = settings != null ? settings.platform_fee_percent : null;
                tvPlatformFee.setText(fee != null ? fee + "%" : getString(R.string.not_available));
                String contact = settings != null ? settings.contact_info : null;
                tvContactInfo.setText(!TextUtils.isEmpty(contact) ? contact : getString(R.string.not_available));
            }
            @Override public void err(Throwable e) {
                if (!isAdded()) return;
                progress.setVisibility(View.GONE);
                tvError.setVisibility(View.VISIBLE);
                tvError.setText(R.string.error_fetch_data);
            }
        });
    }
}
