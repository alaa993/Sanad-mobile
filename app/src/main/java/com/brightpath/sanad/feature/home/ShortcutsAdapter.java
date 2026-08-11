package com.brightpath.sanad.feature.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.brightpath.sanad.R;
import com.brightpath.sanad.data.DashboardResponse;
import java.util.ArrayList;
import java.util.List;

public class ShortcutsAdapter extends RecyclerView.Adapter<ShortcutsAdapter.VH> {
    public interface OnClick { void onShortcut(DashboardResponse.Shortcut s); }
    private final List<DashboardResponse.Shortcut> data = new ArrayList<>();
    private OnClick click;
    private String role;

    public void setOnClick(OnClick c) { this.click = c; }
    public void setRole(String role) { this.role = role; }
    public void submit(List<DashboardResponse.Shortcut> d) {
        data.clear(); if (d!=null) data.addAll(d); notifyDataSetChanged();
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int v) {
        return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_shortcut, p, false));
    }

    @Override public void onBindViewHolder(@NonNull VH h, int i) {
        DashboardResponse.Shortcut s = data.get(i);
        h.title.setText(labelFor(h.itemView.getContext(), s));
        h.icon.setImageResource(iconFor(s));
        h.action.setText(actionLabelFor(h.itemView.getContext(), s));
        View.OnClickListener listener = v -> { if (click!=null) click.onShortcut(s); };
        h.itemView.setOnClickListener(listener);
        h.card.setOnClickListener(listener);
        h.action.setOnClickListener(listener);
    }

    @Override public int getItemCount(){ return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView title;
        ImageView icon;
        MaterialButton action;
        com.google.android.material.card.MaterialCardView card;
        VH(@NonNull View v){
            super(v);
            title=v.findViewById(R.id.tvTitle);
            icon=v.findViewById(R.id.icon);
            action=v.findViewById(R.id.btnAction);
            card=v.findViewById(R.id.card);
        }
    }

    private int iconFor(DashboardResponse.Shortcut shortcut){
        if (shortcut == null) return R.drawable.ic_placeholder;
        String key = shortcut.route != null ? shortcut.route : shortcut.id;
        if (key == null) return R.drawable.ic_placeholder;
        key = key.trim().toLowerCase();
        if (key.startsWith("book") || "sessions/book".equals(key)) return R.drawable.ic_book_session;
        if ("chat".equals(key) || "messages".equals(key) || "support_chat".equals(key)) return R.drawable.ic_chat;
        if ("vent".equals(key)) return R.drawable.ic_vent;
        if ("community".equals(key) || "communities".equals(key) || "support_groups".equals(key) || "groups".equals(key)) {
            return R.drawable.ic_community;
        }
        if ("library".equals(key)) return R.drawable.ic_library;
        if ("wallet".equals(key)) return R.drawable.ic_wallet;
        if ("calendar".equals(key) || "appointments".equals(key) || "availability".equals(key)) {
            return R.drawable.ic_calendar;
        }
        if ("sessions".equals(key) || "sessions_list".equals(key) || "sessions/list".equals(key) || "group_sessions".equals(key)) {
            return R.drawable.ic_sessions;
        }
        if ("patients".equals(key)) return R.drawable.ic_patients;
        if ("specialists".equals(key) || "specialist".equals(key)) return R.drawable.ic_specialists;
        if ("tasks".equals(key) || "patient_tasks".equals(key)) return R.drawable.ic_tasks;
        if ("notifications".equals(key)) return R.drawable.ic_notifications;
        if ("coach".equals(key)) return R.drawable.ic_coach;
        if ("match".equals(key) || "anonymous_match".equals(key)) return R.drawable.ic_match;
        if ("reports".equals(key)) return R.drawable.ic_reports;
        if ("settings".equals(key)) return R.drawable.ic_settings;
        if ("billing".equals(key)) return R.drawable.ic_billing;
        if ("beneficiaries".equals(key) || "add_beneficiary".equals(key)) return R.drawable.ic_beneficiaries;
        if ("safe_place".equals(key) || "self_chat".equals(key)) return R.drawable.ic_safe_place;
        return R.drawable.ic_placeholder;
    }

    private String labelFor(android.content.Context ctx, DashboardResponse.Shortcut shortcut){
        if (shortcut == null) return "";
        String key = shortcut.route != null ? shortcut.route : shortcut.id;
        if (key == null) return shortcut.title != null ? shortcut.title : "";
        key = key.trim().toLowerCase();
        if (isPatient() && ("sessions".equals(key) || "sessions_list".equals(key) || "sessions/list".equals(key))) {
            return ctx.getString(R.string.nav_sessions);
        }
        if (isSpecialist() && "sessions".equals(key)) {
            return ctx.getString(R.string.specialist_action_sessions);
        }
        if (isSpecialist() && "patients".equals(key)) {
            return ctx.getString(R.string.nav_patients);
        }
        if (isSpecialist() && "group_sessions".equals(key)) {
            return ctx.getString(R.string.specialist_action_group_sessions);
        }
        if (isSpecialist() && "availability".equals(key)) {
            return ctx.getString(R.string.specialist_action_availability);
        }
        if (isSpecialist() && "chat".equals(key)) {
            return ctx.getString(R.string.shortcut_chat);
        }
        if (isSpecialist() && "notifications".equals(key)) {
            return ctx.getString(R.string.notifications_title);
        }
        if (isPatient() && ("specialist".equals(key) || "specialists".equals(key))) {
            return ctx.getString(R.string.nav_specialists);
        }
        if (isPatient() && "chat".equals(key)) return ctx.getString(R.string.shortcut_chat);
        switch (key){
            case "book":
            case "book_session":
            case "book-session":
            case "sessions/book":
                return ctx.getString(R.string.shortcut_book);
            case "chat":
            case "messages":
            case "support_chat":
                return ctx.getString(R.string.shortcut_chat);
            case "library":
                return ctx.getString(R.string.shortcut_library);
            case "wallet":
                return ctx.getString(R.string.nav_wallet);
            case "calendar":
            case "appointments":
                return ctx.getString(R.string.shortcut_calendar);
            case "sessions":
            case "sessions_list":
            case "sessions/list":
                return ctx.getString(R.string.nav_sessions);
            case "community":
            case "communities":
            case "support_groups":
            case "groups":
                return ctx.getString(R.string.shortcut_community);
            case "vent":
                return ctx.getString(R.string.community_vent_title);
            case "tasks":
            case "patient_tasks":
                return ctx.getString(R.string.shortcut_tasks);
            case "notifications":
                return ctx.getString(R.string.notifications_title);
            default:
                return shortcut.title != null ? shortcut.title : "";
        }
    }

    private String actionLabelFor(android.content.Context ctx, DashboardResponse.Shortcut shortcut){
        if (shortcut == null) return ctx.getString(R.string.shortcut_action_explore);
        String key = shortcut.route != null ? shortcut.route : shortcut.id;
        if (key == null) return ctx.getString(R.string.shortcut_action_explore);
        key = key.trim().toLowerCase();
        if (key.startsWith("book") || "sessions/book".equals(key) || ("sessions".equals(key) && isPatient())) {
            return ctx.getString(R.string.shortcut_action_book);
        }
        return ctx.getString(R.string.shortcut_action_explore);
    }

    private boolean isPatient() {
        return role == null || role.equalsIgnoreCase("patient");
    }

    private boolean isSpecialist() {
        return role != null && role.equalsIgnoreCase("specialist");
    }
}
