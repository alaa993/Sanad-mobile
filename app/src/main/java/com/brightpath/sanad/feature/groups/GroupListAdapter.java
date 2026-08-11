package com.brightpath.sanad.feature.groups;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.brightpath.sanad.R;
import java.util.List;

public class GroupListAdapter extends RecyclerView.Adapter<GroupListAdapter.VH> {
    public interface Listener { void onItemClick(GroupModels.GroupSession g); }
    private List<GroupModels.GroupSession> items;
    private final Listener listener;

    public GroupListAdapter(List<GroupModels.GroupSession> items, Listener l){ this.items = items; this.listener = l; }

    public void update(List<GroupModels.GroupSession> list){
        this.items = list;
        notifyDataSetChanged();
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_group_session, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int position) {
        GroupModels.GroupSession g = items.get(position);
        h.title.setText(g.title);
        h.subtitle.setText(g.topic == null ? "" : g.topic);
        h.date.setText(g.startAt == null ? "" : g.startAt);
        h.participants.setText(buildMeta(g));
        h.status.setText(mapStatus(g.status));
        h.card.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(g);
        });
    }

    @Override public int getItemCount() { return items == null ? 0 : items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        MaterialCardView card;
        TextView title, subtitle, date, participants, status;
        VH(View v){
            super(v);
            card = (MaterialCardView) v;
            title = v.findViewById(R.id.tvGroupTitle);
            subtitle = v.findViewById(R.id.tvGroupTopic);
            date = v.findViewById(R.id.tvGroupTime);
            participants = v.findViewById(R.id.tvGroupMeta);
            status = v.findViewById(R.id.tvGroupStatus);
        }
    }

    private String mapType(String t){
        if (t == null) return "";
        switch (t){
            case "video": return "Video";
            case "voice": return "Voice";
            case "chat": default: return "Chat";
        }
    }

    private String mapStatus(String s){
        if (s == null) return "";
        switch (s){
            case "ongoing": return "Ongoing";
            case "finished": return "Finished";
            case "canceled": return "Canceled";
            case "scheduled":
            default: return "Scheduled";
        }
    }

    private String buildMeta(GroupModels.GroupSession g){
        String type = mapType(g.type);
        String count = String.format("%d", g.participantsCount);
        return type.isEmpty() ? count : type + " • " + count;
    }
}
