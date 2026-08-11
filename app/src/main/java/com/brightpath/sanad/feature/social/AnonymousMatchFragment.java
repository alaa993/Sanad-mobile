package com.brightpath.sanad.feature.social;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.brightpath.sanad.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

public class AnonymousMatchFragment extends Fragment {
    private AnonymousMatchRepository repo;
    private TextView tvStatus;
    private MaterialButton btnFind, btnOpenChat, btnReport, btnCancel;
    private AnonymousMatchModels.MatchData current;
    private String mode = "chat";

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_anonymous_match, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        if (!com.brightpath.sanad.ui.PatientOnlyGuard.allowOrLeave(this)) return;
        repo = new AnonymousMatchRepository(requireContext());
        tvStatus = v.findViewById(R.id.tvStatus);
        btnFind = v.findViewById(R.id.btnFind);
        btnOpenChat = v.findViewById(R.id.btnOpenChat);
        btnReport = v.findViewById(R.id.btnReport);
        btnCancel = v.findViewById(R.id.btnCancel);
        MaterialToolbar toolbar = v.findViewById(R.id.toolbar);
        if (toolbar != null) toolbar.setNavigationOnClickListener(x -> NavHostFragment.findNavController(this).popBackStack());

        AutoCompleteTextView spGender = v.findViewById(R.id.spGender);
        AutoCompleteTextView spPreference = v.findViewById(R.id.spPreference);
        String[] genders = getResources().getStringArray(R.array.anonymous_genders);
        String[] prefs = getResources().getStringArray(R.array.anonymous_preferences);
        spGender.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, genders));
        spPreference.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, prefs));
        spGender.setText(genders[0], false);
        spPreference.setText(prefs[0], false);

        MaterialButtonToggleGroup toggle = v.findViewById(R.id.toggleMode);
        MaterialButton btnChat = v.findViewById(R.id.btnModeChat);
        if (btnChat != null) toggle.check(btnChat.getId());
        toggle.addOnButtonCheckedListener((g, id, checked) -> {
            if (!checked) return;
            mode = id == R.id.btnModeVoice ? "voice" : "chat";
        });

        btnFind.setOnClickListener(x -> join(spGender, spPreference));
        btnOpenChat.setOnClickListener(x -> openChat());
        btnReport.setOnClickListener(x -> report());
        btnCancel.setOnClickListener(x -> leave());
        refresh();
    }

    private void join(AutoCompleteTextView spGender, AutoCompleteTextView spPreference) {
        String gender = mapGender(spGender.getText() != null ? spGender.getText().toString() : "");
        String pref = mapPref(spPreference.getText() != null ? spPreference.getText().toString() : "");
        repo.join(gender, pref, mode, new AnonymousMatchRepository.Cb<AnonymousMatchModels.StatusResponse>() {
            @Override public void ok(AnonymousMatchModels.StatusResponse res) {
                if (!isAdded()) return;
                bind(res != null ? res.data : null);
                if (res != null && res.data != null && "matched".equals(res.data.status)) {
                    Toast.makeText(requireContext(), R.string.anonymous_match_found, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), R.string.anonymous_match_waiting, Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void err(Throwable e) {
                if (isAdded()) Toast.makeText(requireContext(), R.string.anonymous_match_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void refresh() {
        repo.status(new AnonymousMatchRepository.Cb<AnonymousMatchModels.StatusResponse>() {
            @Override public void ok(AnonymousMatchModels.StatusResponse res) {
                if (isAdded()) bind(res != null ? res.data : null);
            }
            @Override public void err(Throwable e) { }
        });
    }

    private void bind(AnonymousMatchModels.MatchData data) {
        current = data;
        if (data == null) {
            tvStatus.setText(R.string.anonymous_match_idle);
            btnOpenChat.setVisibility(View.GONE);
            btnReport.setVisibility(View.GONE);
            return;
        }
        tvStatus.setText(getString(R.string.anonymous_match_status_fmt, data.status, data.alias_partner != null ? data.alias_partner : "—"));
        boolean matched = "matched".equals(data.status);
        btnOpenChat.setVisibility(matched && data.chat_id != null && data.chat_id > 0 ? View.VISIBLE : View.GONE);
        btnReport.setVisibility(matched ? View.VISIBLE : View.GONE);
    }

    private void openChat() {
        if (current == null || current.chat_id == null || current.chat_id <= 0) return;
        Bundle b = new Bundle();
        b.putInt("chatId", current.chat_id);
        b.putString("chatTitle", getString(R.string.anonymous_match_title));
        NavHostFragment.findNavController(this).navigate(R.id.chatRoomFragment, b);
    }

    private void report() {
        if (current == null) return;
        repo.report(current.id, new AnonymousMatchRepository.Cb<java.util.Map<String, Boolean>>() {
            @Override public void ok(java.util.Map<String, Boolean> res) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), R.string.community_vent_report_sent, Toast.LENGTH_SHORT).show();
                    refresh();
                }
            }
            @Override public void err(Throwable e) { }
        });
    }

    private void leave() {
        repo.leave(new AnonymousMatchRepository.Cb<java.util.Map<String, Boolean>>() {
            @Override public void ok(java.util.Map<String, Boolean> res) {
                if (isAdded()) {
                    current = null;
                    bind(null);
                }
            }
            @Override public void err(Throwable e) { }
        });
    }

    private String mapGender(String label) {
        if (label.contains("female") || label.contains("أنث") || label.contains("Kadın")) return "female";
        if (label.contains("other") || label.contains("آخر")) return "other";
        return "male";
    }

    private String mapPref(String label) {
        if (label.contains("same") || label.contains("نفس")) return "same";
        if (label.contains("male") || label.contains("ذكر")) return "male";
        if (label.contains("female") || label.contains("أنث")) return "female";
        return "any";
    }
}
