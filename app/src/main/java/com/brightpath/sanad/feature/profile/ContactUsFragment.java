package com.brightpath.sanad.feature.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.brightpath.sanad.R;

public class ContactUsFragment extends Fragment {
    private SettingsRepository repo;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_simple_page, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.btnBack).setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
        TextView title = view.findViewById(R.id.tvTitle);
        TextView body = view.findViewById(R.id.tvBody);
        title.setText(R.string.menu_contact_us);
        body.setText(R.string.loading);
        repo = new SettingsRepository(requireContext());
        repo.fetch(new SettingsRepository.Listener() {
            @Override public void onSuccess(SettingsResponse settings) {
                if (!isAdded()) return;
                String content = settings != null ? settings.contact_info : null;
                requireActivity().runOnUiThread(() ->
                        body.setText(content != null && !content.isEmpty()
                                ? content
                                : getString(R.string.contact_us_body)));
            }

            @Override public void onError(Throwable t) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> body.setText(R.string.contact_us_body));
            }
        });
    }
}
