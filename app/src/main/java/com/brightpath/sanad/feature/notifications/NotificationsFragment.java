package com.brightpath.sanad.feature.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.brightpath.sanad.R;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class NotificationsFragment extends Fragment {
    private NotificationsRepository repo;
    private View content, emptyState;
    private ProgressBar progress;
    private Adapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        repo = new NotificationsRepository(requireContext());
        content = v.findViewById(R.id.content);
        emptyState = v.findViewById(R.id.emptyState);
        progress = v.findViewById(R.id.progress);
        MaterialToolbar toolbar = v.findViewById(R.id.toolbar);
        RecyclerView rv = v.findViewById(R.id.rvNotifications);

        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(x -> NavHostFragment.findNavController(this).popBackStack());
        }

        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new Adapter();
        rv.setAdapter(adapter);

        load();
    }

    private void load() {
        if (progress != null) progress.setVisibility(View.VISIBLE);
        if (content != null) content.setVisibility(View.GONE);
        if (emptyState != null) emptyState.setVisibility(View.GONE);

        repo.list(new NotificationsRepository.Cb<NotificationsApi.NotificationList>() {
            @Override
            public void ok(NotificationsApi.NotificationList list) {
                if (!isAdded()) return;
                List<NotificationsApi.NotificationItem> items = list != null && list.data != null ? list.data : new ArrayList<>();
                adapter.submit(items);
                if (progress != null) progress.setVisibility(View.GONE);
                if (items.isEmpty()) {
                    if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
                } else if (content != null) {
                    content.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void err(Throwable e) {
                if (!isAdded()) return;
                if (progress != null) progress.setVisibility(View.GONE);
                if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
            }
        });
    }

    static class Adapter extends RecyclerView.Adapter<Adapter.VH> {
        private final List<NotificationsApi.NotificationItem> data = new ArrayList<>();

        void submit(List<NotificationsApi.NotificationItem> items) {
            data.clear();
            if (items != null) data.addAll(items);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
            return new VH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            NotificationsApi.NotificationItem item = data.get(position);
            holder.title.setText(item.title != null ? item.title : "");
            holder.body.setText(item.body != null ? item.body : "");
            holder.time.setText(item.created_at != null ? item.created_at : "");
            float alpha = item.read ? 0.65f : 1f;
            holder.title.setAlpha(alpha);
            holder.body.setAlpha(alpha);
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            final TextView title, body, time;

            VH(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.tvTitle);
                body = itemView.findViewById(R.id.tvBody);
                time = itemView.findViewById(R.id.tvTime);
            }
        }
    }
}
