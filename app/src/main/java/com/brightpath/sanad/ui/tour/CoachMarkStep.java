package com.brightpath.sanad.ui.tour;

import android.view.View;

public class CoachMarkStep {
    public final View target;
    public final int titleRes;
    public final int descRes;

    public CoachMarkStep(View target, int titleRes, int descRes) {
        this.target = target;
        this.titleRes = titleRes;
        this.descRes = descRes;
    }
}
