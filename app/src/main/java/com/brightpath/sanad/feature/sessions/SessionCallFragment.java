package com.brightpath.sanad.feature.sessions;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.brightpath.sanad.R;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * غرفة انتظار بسيطة / Placeholder للانضمام للفيديو/الصوت.
 * تبقي داخل التطبيق وتسمح بنسخ الرابط أو فتحه.
 */
public class SessionCallFragment extends Fragment {
    private String joinUrl;
    private int chatId = -1;
    private int sessionId = -1;
    private String sessionType;
    private String scheduledAt;
    private String endsAt;
    private boolean canExtend = false;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_session_call, container, false);
    }

    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        Bundle args = getArguments();
        if (args != null){
            joinUrl = args.getString("joinUrl");
            chatId = args.getInt("chatId", -1);
            sessionId = args.getInt("sessionId", -1);
            sessionType = args.getString("type");
            scheduledAt = args.getString("scheduledAt");
            endsAt = args.getString("sessionEndsAt", scheduledAt);
            canExtend = args.getBoolean("canExtend", false);
        }
        TextView tvTitle = v.findViewById(R.id.tvCallTitle);
        TextView tvTime = v.findViewById(R.id.tvCallTime);
        TextView tvStatus = v.findViewById(R.id.tvCallStatus);
        MaterialButton btnStart = v.findViewById(R.id.btnStartCall);
        MaterialButton btnStartVoice = v.findViewById(R.id.btnStartVoice);
        MaterialButton btnOpenChat = v.findViewById(R.id.btnOpenChat);
        MaterialButton btnCopy = v.findViewById(R.id.btnCopyCall);
        Chip chipType = v.findViewById(R.id.chipCallType);

        chipType.setText(mapType(sessionType));
        tvTitle.setText(getString(R.string.session_call_header, mapType(sessionType)));
        tvTime.setText(formatDate(scheduledAt));

        boolean hasLink = !TextUtils.isEmpty(joinUrl);
        boolean hasChat = chatId > 0;
        tvStatus.setText(sessionId > 0 ? R.string.session_join_ready : (hasLink ? R.string.session_join_ready : R.string.session_join_waiting));
        boolean canStart = sessionId > 0 || hasLink || hasChat;
        btnStart.setEnabled(canStart);
        btnStart.setAlpha(canStart ? 1f : 0.5f);
        btnStartVoice.setEnabled(canStart);
        btnStartVoice.setAlpha(canStart ? 1f : 0.5f);
        btnCopy.setEnabled(hasLink);
        btnOpenChat.setEnabled(hasChat);
        btnOpenChat.setAlpha(hasChat ? 1f : 0.5f);

        btnStart.setOnClickListener(x -> startCall(true));
        btnStartVoice.setOnClickListener(x -> startCall(false));
        btnOpenChat.setOnClickListener(x -> {
            if (hasChat) openChat(chatId);
            else Toast.makeText(requireContext(), R.string.session_join_no_chat, Toast.LENGTH_SHORT).show();
        });
        btnCopy.setOnClickListener(x -> copyLink());
    }

    private void startCall(boolean videoEnabled){
        boolean hasLink = !TextUtils.isEmpty(joinUrl);
        boolean hasChat = chatId > 0;
        if (sessionId > 0) {
            openLiveCall(videoEnabled);
        } else if (hasLink) {
            openLink(joinUrl);
        } else if (hasChat) {
            openChat(chatId);
        } else {
            Toast.makeText(requireContext(), R.string.session_join_no_link, Toast.LENGTH_SHORT).show();
        }
    }

    private void openLink(String url){
        try {
            startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)));
        } catch (Exception e){
            Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openChat(int chatId){
        Bundle args = new Bundle();
        args.putInt("chatId", chatId);
        args.putString("chatTitle", mapType(sessionType));
        args.putInt("sessionId", sessionId);
        args.putBoolean("canExtend", canExtend);
        args.putString("sessionEndsAt", endsAt);
        androidx.navigation.fragment.NavHostFragment.findNavController(this)
                .navigate(R.id.chatRoomFragment, args);
    }

    private void openLiveCall(boolean videoEnabled){
        Bundle args = new Bundle();
        args.putInt("sessionId", sessionId);
        args.putString("title", mapType(sessionType));
        args.putBoolean("videoEnabled", videoEnabled);
        args.putString("sessionEndsAt", endsAt);
        androidx.navigation.fragment.NavHostFragment.findNavController(this)
                .navigate(R.id.liveKitCallFragment, args);
    }

    private void copyLink(){
        if (TextUtils.isEmpty(joinUrl)){
            Toast.makeText(requireContext(), R.string.session_join_no_link, Toast.LENGTH_SHORT).show();
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("session_link", joinUrl);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(requireContext(), R.string.session_join_copy_success, Toast.LENGTH_SHORT).show();
    }

    private String mapType(String raw){
        if (raw == null) return getString(R.string.next_session_type_placeholder);
        String value = raw.toLowerCase();
        if (value.contains("video")) return getString(R.string.session_type_video);
        if (value.contains("voice") || value.contains("audio")) return getString(R.string.session_type_voice);
        if (value.contains("chat")) return getString(R.string.session_type_chat);
        return raw;
    }

    private String formatDate(@Nullable String iso){
        if (TextUtils.isEmpty(iso)) return getString(R.string.session_unknown_schedule);
        String[] patterns = {
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd HH:mm:ss"
        };
        for (String p : patterns){
            try {
                SimpleDateFormat parser = new SimpleDateFormat(p, Locale.US);
                parser.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                Date d = parser.parse(iso);
                if (d != null){
                    SimpleDateFormat out = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
                    return out.format(d);
                }
            } catch (Exception ignored){}
        }
        return iso;
    }
}
