package com.brightpath.sanad.ui.tour;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.brightpath.sanad.R;

/**
 * Full-screen coach tip. Uses only framework views (no MaterialButton) so
 * theme-attr inflation cannot crash on MIUI/HyperOS when Skip/Next is tapped.
 */
public class CoachMarkOverlay extends FrameLayout {
    public interface Listener {
        void onNext();
        void onSkip();
    }

    private final CoachMarkHoleView holeView;
    private final TextView titleView;
    private final TextView descView;
    private final TextView skipView;
    private final TextView nextButton;
    private Listener listener;
    private boolean finishing;

    public CoachMarkOverlay(Context context) {
        super(context);
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        setClickable(true);
        setFocusable(true);
        // Block touches from reaching language/theme controls under the tip.
        setFocusableInTouchMode(true);

        holeView = new CoachMarkHoleView(context);
        addView(holeView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        card.setPadding(pad, pad, pad, pad);
        try {
            card.setBackgroundResource(R.drawable.bg_message_bubble);
        } catch (Throwable t) {
            card.setBackgroundColor(0xFFFFFFFF);
        }
        card.setClickable(true);

        titleView = new TextView(context);
        titleView.setTextColor(0xFF1A1A1A);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        titleView.setTypeface(titleView.getTypeface(), android.graphics.Typeface.BOLD);

        descView = new TextView(context);
        descView.setTextColor(0xFF4A4A4A);
        descView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        descView.setPadding(0, dp(6), 0, dp(12));

        LinearLayout actions = new LinearLayout(context);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);

        skipView = new TextView(context);
        skipView.setTextColor(0xFF2F55A5);
        skipView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        skipView.setPadding(dp(12), dp(10), dp(12), dp(10));
        skipView.setClickable(true);
        skipView.setFocusable(true);

        nextButton = new TextView(context);
        nextButton.setTextColor(0xFF2F55A5);
        nextButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        nextButton.setTypeface(nextButton.getTypeface(), android.graphics.Typeface.BOLD);
        nextButton.setPadding(dp(12), dp(10), dp(12), dp(10));
        nextButton.setClickable(true);
        nextButton.setFocusable(true);

        actions.addView(skipView);
        actions.addView(nextButton);

        card.addView(titleView);
        card.addView(descView);
        card.addView(actions);

        LayoutParams cardLp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        cardLp.gravity = Gravity.BOTTOM;
        cardLp.leftMargin = dp(16);
        cardLp.rightMargin = dp(16);
        cardLp.bottomMargin = dp(24);
        addView(card, cardLp);

        skipView.setOnClickListener(v -> fireSkip());
        nextButton.setOnClickListener(v -> fireNext());
        setOnClickListener(v -> { /* swallow background taps */ });
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void bindStep(CoachMarkStep step, boolean isLast) {
        if (finishing) return;
        if (step == null || step.target == null) return;
        setTextSafe(titleView, step.titleRes, "");
        setTextSafe(descView, step.descRes, "");
        setTextSafe(nextButton, isLast ? R.string.onboarding_start : R.string.onboarding_next, isLast ? "OK" : "Next");
        setTextSafe(skipView, R.string.onboarding_skip, "Skip");

        try {
            if (!step.target.isAttachedToWindow()) {
                holeView.setTarget(null);
                return;
            }
            Rect rect = new Rect();
            boolean visible = step.target.getGlobalVisibleRect(rect);
            if (!visible || rect.isEmpty()) {
                holeView.setTarget(null);
            } else {
                int[] loc = new int[2];
                holeView.getLocationOnScreen(loc);
                RectF local = new RectF(
                        rect.left - loc[0],
                        rect.top - loc[1],
                        rect.right - loc[0],
                        rect.bottom - loc[1]
                );
                holeView.setTarget(local);
            }
        } catch (Throwable t) {
            holeView.setTarget(null);
        }
    }

    /** Call before removing from the window hierarchy. */
    public void prepareForDetach() {
        finishing = true;
        listener = null;
        try {
            skipView.setOnClickListener(null);
            nextButton.setOnClickListener(null);
            setOnClickListener(null);
        } catch (Throwable ignored) {}
        try {
            holeView.prepareForDetach();
        } catch (Throwable ignored) {}
        try {
            setVisibility(GONE);
        } catch (Throwable ignored) {}
    }

    private void setTextSafe(TextView view, int resId, String fallback) {
        try {
            view.setText(resId);
        } catch (Throwable t) {
            try {
                view.setText(fallback);
            } catch (Throwable ignored) {}
        }
    }

    private void fireSkip() {
        if (finishing) return;
        finishing = true;
        Listener l = listener;
        listener = null;
        try {
            skipView.setOnClickListener(null);
            nextButton.setOnClickListener(null);
            setOnClickListener(null);
        } catch (Throwable ignored) {}
        try {
            holeView.prepareForDetach();
            setVisibility(GONE);
        } catch (Throwable ignored) {}
        if (l != null) {
            try {
                l.onSkip();
            } catch (Throwable ignored) {}
        }
    }

    private void fireNext() {
        if (finishing) return;
        Listener l = listener;
        if (l != null) {
            try {
                l.onNext();
            } catch (Throwable ignored) {
                finishing = true;
                listener = null;
            }
        }
    }

    /** Allow advancing to another step after Next (not Skip). */
    void resetFinishingForNextStep() {
        if (listener != null) {
            finishing = false;
        }
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }
}
