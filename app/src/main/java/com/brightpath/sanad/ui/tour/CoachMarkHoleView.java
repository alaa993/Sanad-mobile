package com.brightpath.sanad.ui.tour;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * Dim overlay with a clear "hole". Uses Even-Odd path fill — never PorterDuff.CLEAR
 * or saveLayer, which crash natively on some HyperOS / Mali GPUs (Skip/dismiss path).
 */
public class CoachMarkHoleView extends View {
    private final Paint dimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path dimPath = new Path();
    private final RectF holeScratch = new RectF();
    private RectF targetRect;
    private float cornerRadiusPx = 16f;
    private float paddingPx = 12f;
    private boolean drawingEnabled = true;

    public CoachMarkHoleView(Context context) {
        super(context);
        init();
    }

    public CoachMarkHoleView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        dimPaint.setColor(0xB3000000);
        dimPaint.setStyle(Paint.Style.FILL);
        dimPath.setFillType(Path.FillType.EVEN_ODD);
        // Avoid software layers + xfermode — both are crash-prone on MIUI when the
        // overlay is removed right after Skip.
        setLayerType(LAYER_TYPE_NONE, null);
        setClickable(true);
        setFocusable(true);
    }

    public void setTarget(RectF rect) {
        this.targetRect = rect;
        if (drawingEnabled) {
            invalidate();
        }
    }

    public void setCornerRadius(float radiusPx) {
        this.cornerRadiusPx = radiusPx;
    }

    public void setPadding(float paddingPx) {
        this.paddingPx = paddingPx;
    }

    /** Stop further draws before the overlay is detached (Skip / dismiss). */
    public void prepareForDetach() {
        drawingEnabled = false;
        targetRect = null;
        try {
            setLayerType(LAYER_TYPE_NONE, null);
        } catch (Throwable ignored) {}
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!drawingEnabled || !isAttachedToWindow()) return;
        try {
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;

            dimPath.reset();
            dimPath.setFillType(Path.FillType.EVEN_ODD);
            dimPath.addRect(0f, 0f, w, h, Path.Direction.CW);

            if (targetRect != null && !targetRect.isEmpty()) {
                holeScratch.set(targetRect);
                holeScratch.inset(-paddingPx, -paddingPx);
                // Keep hole on-screen to avoid extreme path coords on some OEMs.
                if (holeScratch.left < 0f) holeScratch.left = 0f;
                if (holeScratch.top < 0f) holeScratch.top = 0f;
                if (holeScratch.right > w) holeScratch.right = w;
                if (holeScratch.bottom > h) holeScratch.bottom = h;
                if (holeScratch.width() > 1f && holeScratch.height() > 1f) {
                    dimPath.addRoundRect(holeScratch, cornerRadiusPx, cornerRadiusPx, Path.Direction.CW);
                }
            }

            canvas.drawPath(dimPath, dimPaint);
        } catch (Throwable ignored) {
            // Never let coach-mark drawing kill the host Activity on OEM GPUs.
            try {
                canvas.drawColor(0xB3000000);
            } catch (Throwable ignored2) {}
        }
    }
}
