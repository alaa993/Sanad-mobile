package com.brightpath.sanad.router;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.brightpath.sanad.R;
import com.brightpath.sanad.data.auth.TokenStore;

/** شاشة إقلاع خفيفة — توجّه مباشرة لتبويب الاختصارات حسب الدور دون تحميل واجهة المريض. */
public class RoleBootFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return new View(requireContext());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.post(() -> {
            if (!isAdded()) return;
            String role = new TokenStore(requireContext()).getRole();
            int destination = RoleRouter.startDestinationFor(role);
            NavOptions options = new NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setPopUpTo(R.id.roleBootFragment, true)
                    .build();
            try {
                NavHostFragment.findNavController(this).navigate(destination, null, options);
            } catch (IllegalArgumentException first) {
                try {
                    NavHostFragment.findNavController(this)
                            .navigate(R.id.patientShortcutsFragment, null, options);
                } catch (Exception ignored) {
                    // Last resort: leave empty boot view rather than crash.
                }
            } catch (Exception ignored) {}
        });
    }
}
