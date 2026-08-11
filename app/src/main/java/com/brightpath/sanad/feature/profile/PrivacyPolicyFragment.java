package com.brightpath.sanad.feature.profile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
import com.brightpath.sanad.data.AppConfig;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.brightpath.sanad.R;

public class PrivacyPolicyFragment extends Fragment {
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
        title.setText(R.string.menu_privacy_policy);
        body.setText(R.string.loading);
        repo = new SettingsRepository(requireContext());
        repo.fetch(new SettingsRepository.Listener() {
            @Override public void onSuccess(SettingsResponse settings) {
                if (!isAdded()) return;
                String content = settings != null ? settings.privacy_policy : null;
                String webUrl = settings != null && settings.privacy_policy_url != null && !settings.privacy_policy_url.isEmpty()
                        ? settings.privacy_policy_url
                        : AppConfig.privacyPolicyUrl();
                requireActivity().runOnUiThread(() -> {
                    body.setText(content != null && !content.isEmpty()
                            ? content
                            : getString(R.string.privacy_policy_body));
                    attachOpenWebButton(webUrl);
                });
            }

            @Override public void onError(Throwable t) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> body.setText(R.string.privacy_policy_body));
            }
        });
    }

    private void attachOpenWebButton(String url) {
        View root = getView();
        if (root == null) return;
        LinearLayout card = root.findViewById(R.id.tvBody).getParent() instanceof LinearLayout
                ? (LinearLayout) root.findViewById(R.id.tvBody).getParent()
                : null;
        if (card == null) return;
        if (card.findViewWithTag("btn_open_privacy_web") != null) return;
        MaterialButton btn = new MaterialButton(requireContext());
        btn.setTag("btn_open_privacy_web");
        btn.setText(R.string.open_privacy_on_web);
        btn.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.topMargin = (int) (12 * getResources().getDisplayMetrics().density);
        card.addView(btn, lp);
    }
}
