package com.brightpath.sanad.feature.groups;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.brightpath.sanad.R;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class GroupDetailFragment extends Fragment {
    private GroupDetailViewModel vm;
    private NestedScrollView content;
    private View loading, error;
    private TextView tvTitle, tvTopic, tvTime, tvMeta, tvError;
    private Chip chipType;
    private MaterialButton btnOpenChat, btnOpenVoice, btnOpenCall, btnLeave, btnJoin, btnRetry;
    private int groupId = -1;
    private GroupModels.GroupSession current;
    private boolean openChatAfterJoin = false;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_group_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        groupId = getArguments()!=null ? getArguments().getInt("groupId", -1) : -1;

        content = v.findViewById(R.id.groupDetailContent);
        loading = v.findViewById(R.id.groupDetailLoading);
        error = v.findViewById(R.id.groupDetailError);
        tvError = v.findViewById(R.id.tvGroupDetailError);

        tvTitle = v.findViewById(R.id.tvGroupDetailTitle);
        tvTopic = v.findViewById(R.id.tvGroupDetailTopic);
        tvTime = v.findViewById(R.id.tvGroupDetailTime);
        tvMeta = v.findViewById(R.id.tvGroupDetailMeta);
        chipType = v.findViewById(R.id.chipGroupType);
        btnOpenChat = v.findViewById(R.id.btnGroupOpenChat);
        btnOpenVoice = v.findViewById(R.id.btnGroupOpenVoice);
        btnOpenCall = v.findViewById(R.id.btnGroupOpenCall);
        btnLeave = v.findViewById(R.id.btnGroupLeave);
        btnJoin = v.findViewById(R.id.btnGroupJoin);
        btnRetry = v.findViewById(R.id.btnGroupDetailRetry);

        btnRetry.setOnClickListener(x -> reload());
        btnOpenChat.setOnClickListener(x -> openChat());
        btnOpenVoice.setOnClickListener(x -> openCall(false));
        btnOpenCall.setOnClickListener(x -> openCall(true));
        btnLeave.setOnClickListener(x -> leaveGroup());
        btnJoin.setOnClickListener(x -> joinGroup());

        vm = new ViewModelProvider(this).get(GroupDetailViewModel.class);
        vm.setGroupId(groupId);
        vm.getState().observe(getViewLifecycleOwner(), st -> {
            if (st == null || st.loading) { show(loading); return; }
            if (st.error != null) {
                tvError.setText(st.error);
                show(error);
                return;
            }
            current = st.data;
            bindGroup(st.data);
            if (openChatAfterJoin && st.data != null && st.data.joined) {
                openChatAfterJoin = false;
                openChat();
            }
            show(content);
        });
        reload();
    }

    private void reload(){
        if (groupId < 0) {
            tvError.setText(R.string.group_sessions_error);
            show(error);
            return;
        }
        vm.load();
    }

    private void show(View t){
        loading.setVisibility(t == loading ? View.VISIBLE : View.GONE);
        error.setVisibility(t == error ? View.VISIBLE : View.GONE);
        content.setVisibility(t == content ? View.VISIBLE : View.GONE);
    }

    private void bindGroup(GroupModels.GroupSession g){
        if (g == null) return;
        tvTitle.setText(g.title);
        tvTopic.setText(g.topic != null ? g.topic : "");
        tvTime.setText(formatSchedule(g.startAt, g.endAt));
        tvMeta.setText(getString(R.string.group_sessions_meta, g.participantsCount, g.specialistName != null ? g.specialistName : getString(R.string.group_sessions_specialist_unknown)));
        chipType.setText(mapType(g.type));

        btnLeave.setVisibility(g.joined ? View.VISIBLE : View.GONE);
        btnJoin.setVisibility(g.joined ? View.GONE : View.VISIBLE);
        btnOpenChat.setEnabled(g.joined && g.chatId != null && g.chatId > 0);
        btnOpenChat.setAlpha(btnOpenChat.isEnabled() ? 1f : 0.5f);
        btnOpenVoice.setEnabled(g.joined);
        btnOpenVoice.setAlpha(btnOpenVoice.isEnabled() ? 1f : 0.5f);
        btnOpenCall.setEnabled(g.joined);
        btnOpenCall.setAlpha(btnOpenCall.isEnabled() ? 1f : 0.5f);
    }

    private void openChat(){
        if (current == null) {
            return;
        }
        if (!current.joined) {
            openChatAfterJoin = true;
            vm.join();
            Toast.makeText(requireContext(), R.string.group_sessions_joined, Toast.LENGTH_SHORT).show();
            return;
        }
        if (current.chatId == null || current.chatId <= 0) {
            Toast.makeText(requireContext(), R.string.session_join_no_chat, Toast.LENGTH_SHORT).show();
            return;
        }
        Bundle b = new Bundle();
        b.putInt("chatId", current.chatId);
        b.putString("chatTitle", current.title);
        b.putString("sessionEndsAt", current.endAt);
        NavHostFragment.findNavController(this).navigate(R.id.chatRoomFragment, b);
    }

    private void openCall(boolean videoEnabled){
        if (current == null) return;
        Bundle args = new Bundle();
        args.putInt("groupId", current.id);
        args.putString("title", current.title);
        args.putBoolean("videoEnabled", videoEnabled);
        args.putString("sessionEndsAt", current.endAt);
        NavHostFragment.findNavController(this).navigate(R.id.liveKitCallFragment, args);
    }

    private void leaveGroup(){
        vm.leave();
        Toast.makeText(requireContext(), R.string.group_sessions_left, Toast.LENGTH_SHORT).show();
    }

    private void joinGroup(){
        vm.join();
        Toast.makeText(requireContext(), R.string.group_sessions_joined, Toast.LENGTH_SHORT).show();
    }

    private String mapType(String raw){
        if (raw == null) return getString(R.string.next_session_type_placeholder);
        String value = raw.toLowerCase();
        if (value.contains("video")) return getString(R.string.session_type_video);
        if (value.contains("voice") || value.contains("audio")) return getString(R.string.session_type_voice);
        if (value.contains("chat")) return getString(R.string.session_type_chat);
        return raw;
    }

    private String formatSchedule(String start, String end){
        long startMs = parseMillis(start);
        if (startMs <= 0) return start != null ? start : "";
        String endText = "";
        long endMs = parseMillis(end);
        if (endMs > 0) {
            endText = " - " + formatInstant(endMs, "hh:mm a");
        }
        return formatInstant(startMs, "yyyy-MM-dd - hh:mm a") + endText;
    }

    private String formatInstant(long ms, String pattern){
        try {
            ZonedDateTime dt = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault());
            return dt.format(DateTimeFormatter.ofPattern(pattern));
        } catch (Exception e){
            return "";
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
}
