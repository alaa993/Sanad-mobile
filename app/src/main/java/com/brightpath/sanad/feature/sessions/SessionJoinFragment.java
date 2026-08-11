package com.brightpath.sanad.feature.sessions;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
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
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class SessionJoinFragment extends Fragment {
    private static final long EARLY_JOIN_MS = 5 * 60 * 1000L; // allow 5 minutes early

    private SessionDetailViewModel vm;
    private View loading, error;
    private NestedScrollView content;
    private TextView tvError, tvSchedule, tvStatus, tvHeader;
    private Chip chipType;
    private MaterialButton btnOpenChat, btnOpenVoice, btnOpenCall, btnCopy, btnRetry;
    private SessionModels.Session currentSession;
    private int sessionId = -1;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable unlockRunnable;
    private long scheduledAtMs = -1;
    private SessionRealtimeClient sessionRealtime;
    private final SessionRealtimeClient.Listener sessionRealtimeListener = (id, status) -> {
        if (id == sessionId && currentSession != null) {
            currentSession.status = status;
            bindSession(currentSession);
        }
    };

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_session_join, container, false);
    }

    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        sessionId = getArguments()!=null ? getArguments().getInt("sessionId", -1) : -1;

        loading = v.findViewById(R.id.joinLoading);
        error   = v.findViewById(R.id.joinError);
        content = v.findViewById(R.id.contentJoin);
        tvError = v.findViewById(R.id.tvJoinError);
        tvSchedule = v.findViewById(R.id.tvJoinSchedule);
        tvStatus   = v.findViewById(R.id.tvJoinStatus);
        tvHeader   = v.findViewById(R.id.tvJoinHeader);
        chipType   = v.findViewById(R.id.chipJoinType);
        btnOpenChat = v.findViewById(R.id.btnOpenChat);
        btnOpenVoice = v.findViewById(R.id.btnOpenVoice);
        btnOpenCall = v.findViewById(R.id.btnOpenCall);
        btnCopy    = v.findViewById(R.id.btnCopyLink);
        btnRetry   = v.findViewById(R.id.btnJoinRetry);

        btnRetry.setOnClickListener(x -> { if (sessionId > 0) vm.load(sessionId); });
        btnOpenVoice.setOnClickListener(x -> attemptStart(false, "voice"));
        btnOpenCall.setOnClickListener(x -> attemptStart(false, "video"));
        btnOpenChat.setOnClickListener(x -> openChat());
        btnCopy.setOnClickListener(x -> copyLink());

        vm = new ViewModelProvider(this).get(SessionDetailViewModel.class);
        vm.getState().observe(getViewLifecycleOwner(), st -> {
            if (st==null || st.loading) { show(loading); return; }
            if (st.error!=null){ tvError.setText(st.error); show(error); return; }
            currentSession = st.data;
            bindSession(st.data);
            show(content);
        });

        if (sessionId > 0) {
            vm.load(sessionId);
            sessionRealtime = SessionRealtimeClient.get(requireContext());
            sessionRealtime.addListener(sessionRealtimeListener);
            sessionRealtime.joinSession(sessionId);
        } else {
            tvError.setText(R.string.error_fetch_data);
            show(error);
        }
    }

    private void bindSession(SessionModels.Session session){
        chipType.setText(mapType(session.type));
        if (tvHeader != null) {
            tvHeader.setText(getString(R.string.session_join_title_format, mapType(session.type)));
        }
        tvSchedule.setText(!TextUtils.isEmpty(session.scheduled_at)
                ? formatSchedule(session.scheduled_at)
                : getString(R.string.session_unknown_schedule));

        boolean hasLink = !TextUtils.isEmpty(session.join_url);
        boolean hasChat = session.chat_id != null && session.chat_id > 0;
        scheduledAtMs = parseUtcMillis(session.scheduled_at);
        SessionActionGate gate = SessionActionGate.evaluate(session.status, scheduledAtMs, false, System.currentTimeMillis());
        boolean enableJoin = gate.canJoin;
        String status = enableJoin
                ? getString(R.string.session_join_ready_details, mapType(session.type))
                : getString(
                        "session_join_wait_accept".equals(gate.joinHintKey)
                                ? R.string.session_join_wait_accept
                                : R.string.session_join_waiting
                );
        if (session.status!=null && session.status.toLowerCase().contains("rejected")) {
            status = getString(R.string.session_status_rejected);
        } else if (session.status!=null && session.status.toLowerCase().contains("cancel")) {
            status = getString(R.string.session_status_cancelled);
        }
        String closesNote = formatEndsAt(session.ends_at);
        if (!TextUtils.isEmpty(closesNote)) {
            status = status + "\n" + getString(R.string.session_closes_at, closesNote);
        } else {
            status = status + "\n" + getString(R.string.session_closes_note);
        }
        tvStatus.setText(status);

        btnOpenVoice.setEnabled(enableJoin);
        btnOpenVoice.setAlpha(btnOpenVoice.isEnabled() ? 1f : 0.5f);
        btnOpenCall.setEnabled(enableJoin);
        btnOpenCall.setAlpha(btnOpenCall.isEnabled() ? 1f : 0.5f);
        btnOpenChat.setEnabled(hasChat && enableJoin);
        btnOpenChat.setAlpha(btnOpenChat.isEnabled() ? 1f : 0.5f);
        btnCopy.setEnabled(hasLink);

        // جدولة تفعيل الزر تلقائياً عند حلول وقت الجلسة
        scheduleAutoUnlock(hasLink, hasChat);
    }

    private void show(View t){
        loading.setVisibility(t==loading?View.VISIBLE:View.GONE);
        error.setVisibility(t==error?View.VISIBLE:View.GONE);
        content.setVisibility(t==content?View.VISIBLE:View.GONE);
    }

    @Override
    public void onDestroyView() {
        if (sessionRealtime != null && sessionId > 0) {
            sessionRealtime.leaveSession(sessionId);
            sessionRealtime.removeListener(sessionRealtimeListener);
        }
        if (unlockRunnable != null) {
            handler.removeCallbacks(unlockRunnable);
        }
        super.onDestroyView();
    }

    private void scheduleAutoUnlock(boolean hasLink, boolean hasChat){
        if (unlockRunnable != null) {
            handler.removeCallbacks(unlockRunnable);
        }
        if (scheduledAtMs < 0) return;
        long target = scheduledAtMs - EARLY_JOIN_MS;
        long delay = target - System.currentTimeMillis();
        if (delay <= 0) return; // already within window
        unlockRunnable = () -> {
            // أعد الربط لتفعيل الأزرار وحاول فتح الجلسة تلقائياً إذا كانت متاحة
            if (currentSession != null) {
                bindSession(currentSession);
                // افتح تلقائياً إذا أصبح لدينا رابط أو شات
                if ((hasLink || hasChat) && btnOpenCall.isEnabled()) {
                    attemptStart(true, "video");
                }
            }
        };
        handler.postDelayed(unlockRunnable, delay);
    }

    private void attemptStart(boolean fromTimer, String mode){
        if (currentSession == null) return;
        if (sessionId > 0) {
            openCallLobby(currentSession.join_url, mode);
            return;
        }
        if (!fromTimer) {
            Toast.makeText(requireContext(), R.string.session_join_waiting, Toast.LENGTH_SHORT).show();
        }
    }

    private void openCallLobby(String joinUrl, String mode){
        Bundle args = new Bundle();
        args.putString("joinUrl", joinUrl);
        args.putInt("chatId", currentSession != null && currentSession.chat_id != null ? currentSession.chat_id : -1);
        args.putInt("sessionId", sessionId);
        args.putString("type", currentSession != null ? currentSession.type : null);
        args.putString("scheduledAt", currentSession != null ? currentSession.scheduled_at : null);
        args.putString("sessionEndsAt", currentSession != null ? currentSession.ends_at : null);
        args.putString("callMode", mode);
        NavHostFragment.findNavController(this).navigate(R.id.sessionCallFragment, args);
    }

    private void openChat(){
        if (currentSession == null || currentSession.chat_id == null || currentSession.chat_id <= 0) {
            Toast.makeText(requireContext(), R.string.session_join_no_chat, Toast.LENGTH_SHORT).show();
            return;
        }
        Bundle b = new Bundle();
        b.putInt("chatId", currentSession.chat_id);
        b.putString("chatTitle", mapType(currentSession.type));
        b.putInt("sessionId", sessionId);
        b.putString("sessionEndsAt", currentSession.ends_at);
        NavHostFragment.findNavController(this).navigate(R.id.chatRoomFragment, b);
    }

    private void copyLink(){
        if (currentSession == null || TextUtils.isEmpty(currentSession.join_url)){
            Toast.makeText(requireContext(), R.string.session_join_no_link, Toast.LENGTH_SHORT).show();
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("session_link", currentSession.join_url);
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

    private long parseUtcMillis(String value){
        if (TextUtils.isEmpty(value)) return -1;
        try {
            return Instant.parse(value).toEpochMilli();
        } catch (DateTimeParseException ignored){}
        try {
            return OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant().toEpochMilli();
        } catch (DateTimeParseException ignored){}
        return -1;
    }

    private String formatSchedule(String raw){
        long ms = parseUtcMillis(raw);
        if (ms <= 0) return raw;
        try {
            java.time.ZoneId zone = java.time.ZoneId.systemDefault();
            java.time.ZonedDateTime dt = java.time.Instant.ofEpochMilli(ms).atZone(zone);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd - hh:mm a");
            return dt.format(fmt);
        } catch (Exception e){
            return raw;
        }
    }

    private String formatEndsAt(String raw){
        long ms = parseUtcMillis(raw);
        if (ms <= 0) return "";
        try {
            java.time.ZoneId zone = java.time.ZoneId.systemDefault();
            java.time.ZonedDateTime dt = java.time.Instant.ofEpochMilli(ms).atZone(zone);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd - hh:mm a");
            return dt.format(fmt);
        } catch (Exception e){
            return raw;
        }
    }
}
