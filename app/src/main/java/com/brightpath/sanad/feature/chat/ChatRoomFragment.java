package com.brightpath.sanad.feature.chat;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.brightpath.sanad.R;
import com.brightpath.sanad.feature.sessions.SessionActionsRepository;
import com.brightpath.sanad.feature.sessions.SessionActionsRepository.ExtendResponse;
import com.brightpath.sanad.data.auth.TokenStore;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import android.view.Gravity;
import androidx.core.content.ContextCompat;

/**
 * Chat room UI: REST history via ChatViewModel, live appends via ChatRealtime on chat_{id}.
 * Supports image attach (base64), session extend CTA when linked to an appointment.
 */
public class ChatRoomFragment extends Fragment {
    private ChatViewModel vm;
    private ChatRealtime realtime;
    private ChatRealtime.RoomListener realtimeListener;
    private RecyclerView rv;
    private MessageAdapter adapter;
    private View content, progress, error, empty;
    private TextView tvError;
    private TextInputEditText etMessage;
    private ImageButton btnSend, btnAttach;
    private MaterialButton btnExtend;
    private int chatId = -1;
    private String chatTitle;
    private boolean firstLoad = true;
    private ActivityResultLauncher<String> pickImageLauncher;

    // جلسة للمحادثة
    private int sessionId = -1;
    private boolean canExtend = false;
    private Date sessionEndsAt = null;
    private boolean isLocked = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable countdownRunnable = new Runnable() {
        @Override public void run() {
            updateCountdown();
            handler.postDelayed(this, 1000);
        }
    };
    private int userId = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat_room, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                sendImage(uri);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        toggleBottomNav(false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        chatId = getArguments()!=null ? getArguments().getInt("chatId", -1) : -1;
        chatTitle = getArguments()!=null ? getArguments().getString("chatTitle") : null;
        sessionId = getArguments()!=null ? getArguments().getInt("sessionId", -1) : -1;
        canExtend = getArguments()!=null && getArguments().getBoolean("canExtend", false);
        String endsIso = getArguments()!=null ? getArguments().getString("sessionEndsAt") : null;
        sessionEndsAt = parseIso(endsIso);
        userId = new TokenStore(requireContext()).getUserId();

        content = v.findViewById(R.id.content);
        progress = v.findViewById(R.id.progress);
        error = v.findViewById(R.id.errorContainer);
        empty = v.findViewById(R.id.emptyState);
        tvError = v.findViewById(R.id.tvError);
        etMessage = v.findViewById(R.id.etMessage);
        btnSend = v.findViewById(R.id.btnSend);
        btnExtend = v.findViewById(R.id.btnExtendSession);
        btnAttach = v.findViewById(R.id.btnAttach);
        MaterialToolbar toolbar = v.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(x -> NavHostFragment.findNavController(this).popBackStack());
        if (!TextUtils.isEmpty(chatTitle)) {
            toolbar.setTitle(chatTitle);
        }

        rv = v.findViewById(R.id.rvMessages);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new MessageAdapter(userId);
        rv.setAdapter(adapter);

        btnSend.setOnClickListener(x -> sendMessage());
        if (btnAttach != null) {
            btnAttach.setOnClickListener(x -> {
                if (pickImageLauncher != null) pickImageLauncher.launch("image/*");
            });
        }
        etMessage.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });
        etMessage.addTextChangedListener(new SimpleTextWatcher(t -> btnSend.setEnabled(t.trim().length() > 0 && !isLocked)));

        v.findViewById(R.id.btnRetry).setOnClickListener(x -> reload());
        if (btnExtend != null) {
            btnExtend.setVisibility(canExtend ? View.VISIBLE : View.GONE);
            btnExtend.setOnClickListener(x -> extendSession());
        }
        View timerCard = v.findViewById(R.id.sessionTimerCard);
        if (timerCard != null) {
            timerCard.setVisibility(sessionEndsAt != null ? View.VISIBLE : View.GONE);
        }

        vm = new ViewModelProvider(this).get(ChatViewModel.class);
        vm.getMessages().observe(getViewLifecycleOwner(), msgs -> {
            if (msgs == null) msgs = new ArrayList<>();
            adapter.submit(msgs);
            empty.setVisibility(msgs.isEmpty() ? View.VISIBLE : View.GONE);
            show(content);
            if (!msgs.isEmpty()) rv.scrollToPosition(msgs.size() - 1);
            firstLoad = false;
        });

        realtime = ChatRealtime.get(requireContext());
        realtimeListener = new ChatRealtime.RoomListener() {
            @Override public void onMessage(ChatModels.Message message) {
                vm.applyRealtimeMessage(message);
            }
        };
        reload();
        startCountdownIfNeeded();
        toggleBottomNav(false);
    }

    private void reload(){
        if (chatId <= 0) {
            tvError.setText(R.string.chat_room_error);
            show(error);
            return;
        }
        show(progress);
        vm.openChat(chatId);
        if (realtime != null) {
            realtime.unregister(chatId, realtimeListener);
            realtime.register(chatId, realtimeListener);
        }
    }

    private void sendMessage(){
        String text = etMessage.getText()!=null ? etMessage.getText().toString().trim() : "";
        if (text.isEmpty()) return;
        if (isLocked) {
            Toast.makeText(requireContext(), R.string.session_time_expired, Toast.LENGTH_SHORT).show();
            return;
        }
        btnSend.setEnabled(false);
        if (realtime != null) {
            realtime.emitMessage(chatId, text);
        }
        vm.send(text);
        etMessage.setText("");
        btnSend.setEnabled(!isLocked);
    }

    private void sendImage(Uri uri){
        Context ctx = getContext();
        if (ctx == null) return;
        new Thread(() -> {
            try {
                ReadResult result = readBytes(ctx, uri, 2_000_000);
                if (result.tooLarge) {
                    handler.post(() -> Toast.makeText(ctx, R.string.file_too_large, Toast.LENGTH_SHORT).show());
                    return;
                }
                if (result.data == null || result.data.length == 0) {
                    handler.post(() -> Toast.makeText(ctx, R.string.error_fetch_data, Toast.LENGTH_SHORT).show());
                    return;
                }
                String mime = ctx.getContentResolver().getType(uri);
                String prefix = "data:" + (mime != null ? mime : "image/jpeg") + ";base64,";
                String base64 = prefix + Base64.encodeToString(result.data, Base64.NO_WRAP);
                if (realtime != null) {
                    realtime.emitMessage(chatId, base64, "image");
                }
                if (vm != null) {
                    vm.sendImage(base64);
                }
            } catch (Exception e){
                handler.post(() -> Toast.makeText(ctx, R.string.chat_room_error, Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void show(View target){
        progress.setVisibility(target==progress?View.VISIBLE:View.GONE);
        error.setVisibility(target==error?View.VISIBLE:View.GONE);
        content.setVisibility(target==content?View.VISIBLE:View.GONE);
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        if (realtime != null) realtime.unregister(chatId, realtimeListener);
        handler.removeCallbacks(countdownRunnable);
        toggleBottomNav(true);
    }

    private void startCountdownIfNeeded(){
        if (sessionEndsAt != null) {
            handler.post(countdownRunnable);
        }
    }

    private void updateCountdown(){
        View root = getView();
        if (root == null) return;
        TextView tvCountdown = root.findViewById(R.id.tvCountdown);
        if (tvCountdown == null) return;
        if (sessionEndsAt == null) {
            tvCountdown.setText("");
            return;
        }
        long remainingMs = sessionEndsAt.getTime() - System.currentTimeMillis();
        if (remainingMs <= 0) {
            tvCountdown.setText(getString(R.string.session_time_expired));
            lockInput(true);
            return;
        }
        long seconds = remainingMs / 1000;
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long sVal = seconds % 60;
        String formatted = h > 0 ? String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, sVal)
                : String.format(Locale.getDefault(), "%02d:%02d", m, sVal);
        tvCountdown.setText(getString(R.string.session_time_left, formatted));
        lockInput(false);
    }

    private void lockInput(boolean lock){
        isLocked = lock;
        etMessage.setEnabled(!lock);
        btnSend.setEnabled(!lock && etMessage.getText()!=null && etMessage.getText().toString().trim().length() > 0);
    }

    private void extendSession(){
        if (sessionId <= 0) {
            Toast.makeText(requireContext(), R.string.session_extend_error, Toast.LENGTH_SHORT).show();
            return;
        }
        SessionActionsRepository repo = new SessionActionsRepository(requireContext());
        btnExtend.setEnabled(false);
        repo.extend(sessionId, 15, new SessionActionsRepository.ExtendCb() {
            @Override public void ok(ExtendResponse resp) {
                btnExtend.setEnabled(true);
                if (resp != null && !TextUtils.isEmpty(resp.ends_at)) {
                    sessionEndsAt = parseIso(resp.ends_at);
                    Toast.makeText(requireContext(), R.string.session_extend_ok, Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void err(Throwable t) {
                btnExtend.setEnabled(true);
                Toast.makeText(requireContext(), R.string.session_extend_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Date parseIso(String iso){
        if (TextUtils.isEmpty(iso)) return null;
        try {
            // ISO-8601 مع إزاحة
            java.time.Instant inst = java.time.OffsetDateTime.parse(iso).toInstant();
            return new Date(inst.toEpochMilli());
        } catch (Exception ignored) {}
        try {
            java.time.Instant inst = java.time.Instant.parse(iso);
            return new Date(inst.toEpochMilli());
        } catch (Exception ignored) {}
        try {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
            ParsePosition pp = new ParsePosition(0);
            Date d = fmt.parse(iso, pp);
            if (d != null) return d;
            SimpleDateFormat fmt2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            fmt2.setTimeZone(TimeZone.getTimeZone("UTC"));
            return fmt2.parse(iso, new ParsePosition(0));
        } catch (Exception e) {
            return null;
        }
    }

    private ReadResult readBytes(Context ctx, Uri uri, int maxBytes) {
        try (InputStream in = ctx.getContentResolver().openInputStream(uri);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            int total = 0;
            while ((read = in.read(buffer)) != -1) {
                total += read;
                if (total > maxBytes) {
                    return new ReadResult(null, true);
                }
                out.write(buffer, 0, read);
            }
            return new ReadResult(out.toByteArray(), false);
        } catch (Exception e){
            return new ReadResult(null, false);
        }
    }

    private static class ReadResult {
        final byte[] data;
        final boolean tooLarge;
        ReadResult(byte[] data, boolean tooLarge){
            this.data = data;
            this.tooLarge = tooLarge;
        }
    }

    private void toggleBottomNav(boolean show){
        Activity act = getActivity();
        if (act == null) return;
        View nav = act.findViewById(R.id.bottomNav);
        if (nav != null) {
            nav.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.VH> {
        private final List<ChatModels.Message> data = new ArrayList<>();
        private final int userId;
        MessageAdapter(int userId){ this.userId = userId; }
        void submit(List<ChatModels.Message> list){
            data.clear();
            if (list != null) data.addAll(list);
            notifyDataSetChanged();
        }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_text, parent, false);
            return new VH(view);
        }
        @Override public void onBindViewHolder(@NonNull VH holder, int position) {
            holder.bind(data.get(position));
        }
        @Override public int getItemCount(){ return data.size(); }

        class VH extends RecyclerView.ViewHolder {
            private final TextView tvSender, tvBody, tvMeta;
            private final android.widget.ImageView imgBody;
            VH(@NonNull View itemView) {
                super(itemView);
                tvSender = itemView.findViewById(R.id.tvSender);
                tvBody = itemView.findViewById(R.id.tvBody);
                tvMeta = itemView.findViewById(R.id.tvMeta);
                imgBody = itemView.findViewById(R.id.imgBody);
            }
            void bind(ChatModels.Message msg){
                boolean mine = msg.sender != null && msg.sender.id == userId;
                String senderRole = msg.sender != null ? msg.sender.role : null;
                String senderName = msg.sender != null ? msg.sender.name : null;
                String display = mine ? itemView.getContext().getString(R.string.chat_me_label)
                        : (!TextUtils.isEmpty(senderName) ? senderName : itemView.getContext().getString(R.string.chat_room_title));
                if (!TextUtils.isEmpty(senderRole) && !mine) {
                    display += " • " + mapRole(senderRole);
                }
                tvSender.setText(display);
                boolean isImage = msg.type != null && msg.type.toLowerCase(Locale.US).contains("image");
                boolean imageShown = false;
                if (isImage && msg.body != null) {
                    String body = msg.body;
                    String model = body;
                    if (!isHttpUrl(body) && !body.startsWith("data:")) {
                        model = "data:image/jpeg;base64," + body;
                    }
                    imgBody.setVisibility(View.VISIBLE);
                    Glide.with(imgBody.getContext()).load(model).into(imgBody);
                    tvBody.setVisibility(View.GONE);
                    imageShown = true;
                }
                if (!imageShown) {
                    imgBody.setVisibility(View.GONE);
                    tvBody.setVisibility(View.VISIBLE);
                    tvBody.setText(msg.body != null ? msg.body : "");
                }
                tvMeta.setText(formatRelativeTime(msg.created_at));
                int bg = mine ? R.drawable.bg_message_bubble_primary : R.drawable.bg_message_bubble;
                tvBody.setBackgroundResource(bg);
                imgBody.setBackgroundResource(bg);
                int textColor = mine ? android.R.color.white : R.color.sanad_on_bg;
                tvBody.setTextColor(ContextCompat.getColor(itemView.getContext(), textColor));
                android.widget.LinearLayout root = (android.widget.LinearLayout) itemView;
                root.setGravity(mine ? Gravity.END : Gravity.START);
                tvMeta.setTextAlignment(mine ? View.TEXT_ALIGNMENT_TEXT_END : View.TEXT_ALIGNMENT_TEXT_START);
            }

            private String mapRole(String role){
                if ("specialist".equalsIgnoreCase(role)) return itemView.getContext().getString(R.string.role_specialist);
                if ("user".equalsIgnoreCase(role)) return itemView.getContext().getString(R.string.role_patient);
                if ("support".equalsIgnoreCase(role)) return itemView.getContext().getString(R.string.role_support);
                return role;
            }

            private boolean isHttpUrl(String value){
                if (value == null) return false;
                String v = value.toLowerCase(Locale.US);
                return v.startsWith("http://") || v.startsWith("https://");
            }
        }
    }

    private String formatRelativeTime(String iso) {
        if (TextUtils.isEmpty(iso)) return "";
        Date d = parseIso(iso);
        if (d == null) return iso;
        long diffMs = System.currentTimeMillis() - d.getTime();
        if (diffMs < 0) diffMs = 0;
        long seconds = diffMs / 1000;
        if (seconds < 60) return getString(R.string.time_now);
        long minutes = seconds / 60;
        if (minutes < 60) {
            return formatCount(minutes, R.string.time_minute_singular, R.string.time_minute_dual, R.string.time_minute_plural);
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return formatCount(hours, R.string.time_hour_singular, R.string.time_hour_dual, R.string.time_hour_plural);
        }
        long days = hours / 24;
        return formatCount(days, R.string.time_day_singular, R.string.time_day_dual, R.string.time_day_plural);
    }

    private String formatCount(long count, int singularRes, int dualRes, int pluralRes) {
        boolean isArabic = Locale.getDefault().getLanguage().startsWith("ar");
        if (count == 1) return getString(singularRes, count);
        if (count == 2 && isArabic) return getString(dualRes);
        return getString(pluralRes, count);
    }

    private static class SimpleTextWatcher implements android.text.TextWatcher {
        interface Listener { void onTextChanged(String text); }
        private final Listener listener;
        SimpleTextWatcher(Listener l){ listener = l; }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override public void afterTextChanged(android.text.Editable s){ listener.onTextChanged(s.toString()); }
    }
}
