package com.brightpath.sanad.ui;

import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.brightpath.sanad.R;
import com.brightpath.sanad.data.auth.TokenStore;
import com.brightpath.sanad.feature.community.CommunityRolePolicy;

/** Blocks patient-only destinations when the signed-in role is not a patient. */
public final class PatientOnlyGuard {

    private PatientOnlyGuard() {}

    public static boolean allowOrLeave(@NonNull Fragment fragment) {
        CommunityRolePolicy policy = new CommunityRolePolicy(
                new TokenStore(fragment.requireContext()).getRole());
        if (policy.isPatient()) return true;
        Toast.makeText(fragment.requireContext(), R.string.error_unauthorized_feature, Toast.LENGTH_SHORT).show();
        try {
            NavHostFragment.findNavController(fragment).navigateUp();
        } catch (Exception ignored) {
            // Fragment may not be on a nav host (sheet / nested).
        }
        return false;
    }
}
