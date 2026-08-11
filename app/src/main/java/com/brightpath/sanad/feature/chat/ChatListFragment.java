package com.brightpath.sanad.feature.chat;

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
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.brightpath.sanad.R;
import com.brightpath.sanad.ui.tour.CoachMarkManager;
import com.brightpath.sanad.ui.tour.CoachMarkStep;
import com.brightpath.sanad.feature.sessions.DirectoryModels;
import com.brightpath.sanad.feature.sessions.DirectoryRepository;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import java.util.List;

public class ChatListFragment extends Fragment {
    private ChatViewModel vm;
    private View progress, error, content, empty;
    private TextView tvError, tvSubtitle;
    private ChatAdapter adapter;
    private List<ChatModels.Chat> currentList = new ArrayList<>();
    private boolean isLoading = false;
    private String errorMsg = null;
    private ChatRepository chatRepo;
    private DirectoryRepository directoryRepo;
    private boolean creatingChat = false;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        progress = v.findViewById(R.id.progress);
        error    = v.findViewById(R.id.errorContainer);
        content  = v.findViewById(R.id.content);
        empty    = v.findViewById(R.id.emptyState);
        tvError  = v.findViewById(R.id.tvError);
        tvSubtitle = v.findViewById(R.id.tvSubtitle);

        RecyclerView rv = v.findViewById(R.id.rvChats);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        int userId = new com.brightpath.sanad.data.auth.TokenStore(requireContext()).getUserId();
        adapter = new ChatAdapter(userId, chat -> {
            Bundle args = new Bundle();
            args.putInt("chatId", chat.id);
            args.putString("chatTitle", chat.subject);
            NavHostFragment.findNavController(this).navigate(R.id.chatRoomFragment, args);
        });
        rv.setAdapter(adapter);

        v.findViewById(R.id.btnRetry).setOnClickListener(x -> vm.loadChats());
        chatRepo = new ChatRepository(requireContext());
        directoryRepo = new DirectoryRepository(requireContext());
        View btnStartChat = v.findViewById(R.id.btnStartChat);
        if (btnStartChat != null) btnStartChat.setOnClickListener(x -> showSpecialistPicker());

        vm = new ViewModelProvider(this).get(ChatViewModel.class);
        vm.getChats().observe(getViewLifecycleOwner(), list -> {
            currentList = list != null ? list : new ArrayList<>();
            updateSubtitle();
            render();
        });
        vm.getChatsLoading().observe(getViewLifecycleOwner(), loading -> {
            isLoading = loading != null && loading;
            render();
        });
        vm.getChatsError().observe(getViewLifecycleOwner(), err -> {
            errorMsg = err;
            render();
        });

        vm.loadChats();

        v.post(() -> {
            java.util.List<CoachMarkStep> steps = new java.util.ArrayList<>();
            if (rv != null) steps.add(CoachMarkManager.step(rv, R.string.tour_chat_list_title, R.string.tour_chat_list_desc));
            if (btnStartChat != null) steps.add(CoachMarkManager.step(btnStartChat, R.string.tour_chat_start_title, R.string.tour_chat_start_desc));
            CoachMarkManager.showIfNeeded(ChatListFragment.this, "tour_chat", steps);
        });
    }

    private void updateSubtitle(){
        if (tvSubtitle == null) return;
        if (currentList != null && !currentList.isEmpty() && !TextUtils.isEmpty(currentList.get(0).updated_at)) {
            tvSubtitle.setText(getString(R.string.chat_last_update) + " · " + currentList.get(0).updated_at);
        } else {
            tvSubtitle.setText(R.string.chat_last_update);
        }
    }

    private void render(){
        if (isLoading){
            show(progress);
            return;
        }
        if (errorMsg != null){
            tvError.setText(errorMsg);
            show(error);
            return;
        }
        if (currentList == null || currentList.isEmpty()){
            show(empty);
            return;
        }
        adapter.submit(currentList);
        show(content);
    }

    private void show(View visible){
        progress.setVisibility(visible==progress?View.VISIBLE:View.GONE);
        error.setVisibility(visible==error?View.VISIBLE:View.GONE);
        content.setVisibility(visible==content?View.VISIBLE:View.GONE);
        empty.setVisibility(visible==empty?View.VISIBLE:View.GONE);
    }

    private void showSpecialistPicker() {
        if (creatingChat) return;
        directoryRepo.load(true, null, 1, new DirectoryRepository.Listener() {
            @Override public void onSuccess(DirectoryModels.Paged data) {
                if (!isAdded()) return;
                java.util.List<DirectoryModels.Item> specialists = data != null ? data.data : null;
                if (specialists == null || specialists.isEmpty()) {
                    Toast.makeText(requireContext(), R.string.patient_specialists_empty, Toast.LENGTH_SHORT).show();
                    try {
                        NavHostFragment.findNavController(ChatListFragment.this).navigate(R.id.patientSpecialistsFragment);
                    } catch (IllegalArgumentException ignored) {}
                    return;
                }
                java.util.List<DirectoryModels.Item> list = new ArrayList<>(specialists);
                CharSequence[] labels = new CharSequence[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    DirectoryModels.Item item = list.get(i);
                    String name = item.name != null ? item.name : getString(R.string.role_specialist);
                    if (item.specialty != null && !item.specialty.isEmpty()) {
                        name += " · " + item.specialty;
                    }
                    labels[i] = name;
                }
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.nav_specialists)
                        .setItems(labels, (d, which) -> startChatWith(list.get(which)))
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
            @Override public void onError(Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), R.string.patient_specialists_load_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startChatWith(DirectoryModels.Item specialist) {
        if (specialist == null || creatingChat) return;
        creatingChat = true;
        java.util.List<Integer> ids = new ArrayList<>();
        ids.add(specialist.id);
        String subject = specialist.name != null ? specialist.name : getString(R.string.chat_title);
        chatRepo.createChat(ids, subject, new ChatRepository.CreateCb() {
            @Override public void ok(int chatId) {
                creatingChat = false;
                if (!isAdded()) return;
                vm.loadChats();
                Bundle args = new Bundle();
                args.putInt("chatId", chatId);
                args.putString("chatTitle", subject);
                NavHostFragment.findNavController(ChatListFragment.this).navigate(R.id.chatRoomFragment, args);
            }
            @Override public void err(Throwable t) {
                creatingChat = false;
                if (!isAdded()) return;
                Toast.makeText(requireContext(), R.string.chat_create_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.VH> {
        interface Listener { void onChatClicked(ChatModels.Chat chat); }
        private final List<ChatModels.Chat> data = new ArrayList<>();
        private final Listener listener;
        private final int userId;
        ChatAdapter(int userId, Listener l){ this.userId = userId; listener = l; }
        void submit(List<ChatModels.Chat> list){
            data.clear();
            if (list != null) data.addAll(list);
            notifyDataSetChanged();
        }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false);
            return new VH(view);
        }
        @Override public void onBindViewHolder(@NonNull VH holder, int position) {
            ChatModels.Chat chat = data.get(position);
            holder.bind(chat);
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onChatClicked(chat);
            });
        }
        @Override public int getItemCount(){ return data.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvSubject, tvParticipants, tvLastMessage, tvTime, badge;
            VH(@NonNull View itemView) {
                super(itemView);
                tvSubject = itemView.findViewById(R.id.tvSubject);
                tvParticipants = itemView.findViewById(R.id.tvParticipants);
                tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
                tvTime = itemView.findViewById(R.id.tvTime);
                badge = itemView.findViewById(R.id.badgeUnread);
            }
            void bind(ChatModels.Chat chat){
                String myRole = null;
                List<String> names = new ArrayList<>();
                if (chat.participants != null && !chat.participants.isEmpty()){
                    for (ChatModels.UserRef u : chat.participants){
                        if (u==null) continue;
                        if (u.id == userId) { myRole = u.role; continue; }
                        String label = !TextUtils.isEmpty(u.name) ? u.name : itemView.getContext().getString(R.string.chat_title);
                        if (!TextUtils.isEmpty(u.role)) {
                            label += " (" + mapRole(u.role) + ")";
                        }
                        names.add(label);
                    }
                }
                String target = !names.isEmpty() ? TextUtils.join(" • ", names) : itemView.getContext().getString(R.string.chat_title);
                tvSubject.setText(!TextUtils.isEmpty(chat.subject) ? chat.subject : target);
                tvParticipants.setText(target + (myRole!=null ? " • " + itemView.getContext().getString(R.string.chat_you_are, mapRole(myRole)) : ""));
                tvLastMessage.setText(!TextUtils.isEmpty(chat.last_message)
                        ? chat.last_message : itemView.getContext().getString(R.string.chat_last_message_placeholder));
                tvTime.setText(chat.updated_at != null ? chat.updated_at : "");
                if (chat.unread_count > 0){
                    badge.setVisibility(View.VISIBLE);
                    badge.setText(String.valueOf(chat.unread_count));
                } else {
                    badge.setVisibility(View.GONE);
                }
            }

            private String mapRole(String role){
                if ("specialist".equalsIgnoreCase(role)) return itemView.getContext().getString(R.string.role_specialist);
                if ("user".equalsIgnoreCase(role)) return itemView.getContext().getString(R.string.role_patient);
                if ("support".equalsIgnoreCase(role)) return itemView.getContext().getString(R.string.role_support);
                return role;
            }
        }
    }

    @Override
    public void onDestroyView() {
        try { CoachMarkManager.dismissActive(); } catch (Throwable ignored) {}
        super.onDestroyView();
    }

}
