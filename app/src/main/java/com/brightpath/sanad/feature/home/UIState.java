package com.brightpath.sanad.feature.home;

import java.util.ArrayList;
import java.util.List;
import com.brightpath.sanad.data.DashboardResponse;

public class UIState {
    public boolean loading;
    public Throwable error;
    public String role;
    public int upcoming, unread, points;
    public List<DashboardResponse.Shortcut> shortcuts = new ArrayList<>();
    public DashboardResponse.Intake intake;
    public DashboardResponse.SessionSummary nextSession;
    public boolean canJoinNext;
    public DashboardResponse.Onboarding onboarding;

    public static UIState loading() { UIState s = new UIState(); s.loading = true; return s; }
    public static UIState error(Throwable e) { UIState s = new UIState(); s.error = e; return s; }
    public static UIState data(String role, int up, int un, int pt,
                               List<DashboardResponse.Shortcut> sc,
                               DashboardResponse.Intake intake,
                               DashboardResponse.SessionSummary next, boolean canJoin,
                               DashboardResponse.Onboarding onboarding) {
        UIState s = new UIState();
        s.role = role; s.upcoming = up; s.unread = un; s.points = pt;
        if (sc != null) s.shortcuts = sc;
        s.intake = intake;
        s.nextSession = next;
        s.canJoinNext = canJoin;
        s.onboarding = onboarding;
        return s;
    }
}
