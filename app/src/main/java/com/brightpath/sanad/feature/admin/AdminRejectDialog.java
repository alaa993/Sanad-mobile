package com.brightpath.sanad.feature.admin;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.brightpath.sanad.R;

public final class AdminRejectDialog {
    public interface Callback {
        void onConfirm(@Nullable String reason);
    }

    private AdminRejectDialog() {}

    public static void show(@NonNull Context context, @NonNull Callback callback) {
        EditText input = new EditText(context);
        input.setHint(R.string.admin_reject_reason_hint);
        int pad = (int) (16 * context.getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad / 2, pad, pad / 2);
        new AlertDialog.Builder(context)
                .setTitle(R.string.admin_reject_title)
                .setView(input)
                .setPositiveButton(R.string.admin_review_reject, (d, w) -> {
                    String reason = input.getText() != null ? input.getText().toString().trim() : "";
                    callback.onConfirm(TextUtils.isEmpty(reason) ? null : reason);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
