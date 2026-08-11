package com.brightpath.sanad.feature.community;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.brightpath.sanad.R;

import java.util.ArrayList;
import java.util.List;

public class SelfChatFragment extends Fragment {
    private SelfChatStore store;
    private CommunityRepository journalRepo;
    private MessagesAdapter adapter;
    private EditText input;
    private View composer;
    private TextView tvJournalLocked;
    private boolean journalLocked = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_self_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        if (!com.brightpath.sanad.ui.PatientOnlyGuard.allowOrLeave(this)) return;
        store = new SelfChatStore(requireContext());
        journalRepo = new CommunityRepository(requireContext());

        v.findViewById(R.id.btnBack).setOnClickListener(x -> NavHostFragment.findNavController(this).popBackStack());

        RecyclerView rv = v.findViewById(R.id.rvMessages);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new MessagesAdapter();
        rv.setAdapter(adapter);

        input = v.findViewById(R.id.etMessage);
        composer = v.findViewById(R.id.composer);
        tvJournalLocked = v.findViewById(R.id.tvJournalLocked);
        ImageButton send = v.findViewById(R.id.btnSend);
        send.setOnClickListener(x -> send());

        refresh();
        loadFromServer(rv);
    }

    private void loadFromServer(RecyclerView rv) {
        journalRepo.journal(new CommunityRepository.Cb<CommunityModels.ListResponse<CommunityModels.Journal>>() {
            @Override
            public void ok(CommunityModels.ListResponse<CommunityModels.Journal> response) {
                if (!isAdded() || response == null) return;
                store.replaceFromServer(response.data);
                refresh();
                rv.post(() -> rv.scrollToPosition(Math.max(0, adapter.getItemCount() - 1)));
            }

            @Override
            public void err(Throwable e) {
                if (!isAdded()) return;
                if (e instanceof CommunityRepository.JournalLockedException) {
                    setJournalLocked(true);
                }
            }
        });
    }

    private void setJournalLocked(boolean locked) {
        journalLocked = locked;
        if (tvJournalLocked != null) {
            if (locked) {
                tvJournalLocked.setText(R.string.journal_locked_banner);
                tvJournalLocked.setVisibility(View.VISIBLE);
            } else {
                tvJournalLocked.setVisibility(View.GONE);
            }
        }
        // Always keep the composer usable — entries can stay local until sync unlocks.
        if (composer != null) {
            composer.setVisibility(View.VISIBLE);
        }
    }

    private void refresh() {
        List<SelfChatStore.Message> messages = store.load();
        adapter.submit(messages);
        if (tvJournalLocked != null && journalLocked && !messages.isEmpty()) {
            // Keep banner compact at top; don't hide chat history.
            tvJournalLocked.setVisibility(View.VISIBLE);
        }
    }

    private void send() {
        if (input == null) return;
        String text = input.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;
        store.add(text);
        input.setText("");
        refresh();

        if (journalLocked) {
            return;
        }

        journalRepo.addJournal(text, new CommunityRepository.Cb<CommunityModels.SimpleResponse>() {
            @Override
            public void ok(CommunityModels.SimpleResponse response) {
                if (!isAdded()) return;
                if (response != null && response.id != null) {
                    store.markSynced(text, response.id, response.created_at);
                    refresh();
                }
            }

            @Override
            public void err(Throwable e) {
                if (!isAdded()) return;
                if (e instanceof CommunityRepository.JournalLockedException) {
                    setJournalLocked(true);
                    Toast.makeText(requireContext(), R.string.journal_locked_banner, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(requireContext(), R.string.error_load_failed, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    static class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.VH> {
        private final List<SelfChatStore.Message> data = new ArrayList<>();

        void submit(List<SelfChatStore.Message> list) {
            data.clear();
            if (list != null) data.addAll(list);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_self_chat_message, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            SelfChatStore.Message m = data.get(position);
            holder.body.setText(m.text);
            holder.meta.setText(m.formattedTime(holder.itemView.getContext()));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            final TextView body;
            final TextView meta;

            VH(@NonNull View itemView) {
                super(itemView);
                body = itemView.findViewById(R.id.tvBody);
                meta = itemView.findViewById(R.id.tvMeta);
            }
        }
    }
}
