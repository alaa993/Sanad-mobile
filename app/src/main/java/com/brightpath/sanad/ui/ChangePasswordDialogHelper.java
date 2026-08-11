package com.brightpath.sanad.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.brightpath.sanad.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

public final class ChangePasswordDialogHelper {

    public interface Callback {
        void onSave(@NonNull String current, @NonNull String password, @NonNull String confirm);
    }

    private ChangePasswordDialogHelper() {}

    public static void show(@NonNull Context context, @NonNull Callback callback) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_change_password, null);
        TextInputEditText etCurrent = dialogView.findViewById(R.id.etCurrentPassword);
        TextInputEditText etNew = dialogView.findViewById(R.id.etNewPassword);
        TextInputEditText etConfirm = dialogView.findViewById(R.id.etConfirmPassword);

        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.admin_profile_change_password)
                .setView(dialogView)
                .setPositiveButton(R.string.admin_profile_save_password, (d, w) -> {
                    String current = etCurrent.getText() != null ? etCurrent.getText().toString().trim() : "";
                    String pass = etNew.getText() != null ? etNew.getText().toString().trim() : "";
                    String confirm = etConfirm.getText() != null ? etConfirm.getText().toString().trim() : "";
                    if (pass.isEmpty()) {
                        Toast.makeText(context, R.string.admin_profile_password_required, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!pass.equals(confirm)) {
                        Toast.makeText(context, R.string.error_password_mismatch, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    callback.onSave(current, pass, confirm);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
