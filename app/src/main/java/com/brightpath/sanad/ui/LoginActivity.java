package com.brightpath.sanad.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.brightpath.sanad.R;
import com.brightpath.sanad.data.AppConfig;
import com.brightpath.sanad.data.LanguageUiHelper;
import com.brightpath.sanad.data.ThemeStore;
import com.brightpath.sanad.data.auth.AuthException;
import com.brightpath.sanad.data.auth.AuthRepository;
import com.brightpath.sanad.models.LoginResponse;

public class LoginActivity extends AppCompatActivity {

    private EditText usernameInput, passwordInput;
    private MaterialButton btnLogin;
    private View btnRegisterLink;
    private View loginProgressOverlay;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean recovery = getIntent() != null
                && getIntent().getBooleanExtra("recovery_after_crash", false);
        // After a crash, skip theme overlays that may have caused InflateException loops on MIUI.
        if (!recovery) {
            try {
                new ThemeStore(this).applySavedTheme(this);
            } catch (Throwable ignored) {}
            try {
                applyStatusBarColor();
            } catch (Throwable ignored) {}
        }
        try {
            setContentView(R.layout.activity_login);
        } catch (Throwable t) {
            // Absolute last resort: empty shell so process stays alive.
            try {
                setContentView(new android.widget.FrameLayout(this));
            } catch (Throwable ignored) {}
            return;
        }

        LanguageUiHelper.bindAuthToggleGroup(
                this,
                findViewById(R.id.groupAuthLanguage),
                R.id.btnAuthLangArabic,
                R.id.btnAuthLangEnglish,
                R.id.btnAuthLangTurkish
        );

        usernameInput = findViewById(R.id.username);
        passwordInput = findViewById(R.id.password);
        btnLogin = findViewById(R.id.btnLogin);
        loginProgressOverlay = findViewById(R.id.loginProgressOverlay);
        View badge = findViewById(R.id.badge);
        if (badge instanceof android.widget.ImageView) {
            try {
                ((android.widget.ImageView) badge).setImageResource(new ThemeStore(this).getLogoRes(true));
            } catch (OutOfMemoryError | Exception e) {
                try {
                    ((android.widget.ImageView) badge).setImageResource(R.drawable.sanad_logo);
                } catch (Throwable ignored) {}
            }
        }

        btnRegisterLink = findViewById(R.id.linkRegister);
        View btnForgot = findViewById(R.id.forgot);

        authRepository = new AuthRepository(this, AppConfig.BASE_URL);

        btnLogin.setOnClickListener(view -> {
            if (btnLogin != null && !btnLogin.isEnabled()) return;
            String username = usernameInput.getText().toString().trim();
            String password = passwordInput.getText().toString();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, R.string.login_error_required_fields, Toast.LENGTH_SHORT).show();
                return;
            }

            toggleLoading(true);
            new Thread(() -> {
                try {
                    LoginResponse res = authRepository.login(username, password);
                    if (res != null && res.resolveToken() != null) {
                        com.brightpath.sanad.data.auth.SessionGuard.markFresh();
                        runOnUiThread(() -> {
                            toggleLoading(false);
                            Toast.makeText(this, R.string.login_success_message, Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(this, MainActivity.class));
                            finish();
                        });
                    } else {
                        runOnUiThread(() -> {
                            toggleLoading(false);
                            Toast.makeText(this, R.string.login_failure_message, Toast.LENGTH_SHORT).show();
                        });
                    }
                } catch (AuthException e) {
                    runOnUiThread(() -> {
                        toggleLoading(false);
                        showLoginError(e);
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        toggleLoading(false);
                        showGenericLoginError(e);
                    });
                }
            }, "login-request").start();
        });

        btnRegisterLink.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        if (btnForgot != null) {
            btnForgot.setOnClickListener(v -> showForgotPasswordDialog());
        }
    }

    private void showLoginError(AuthException e) {
        if (e.code == 422) {
            boolean hasUser = hasFieldError(e, "username");
            boolean hasPass = hasFieldError(e, "password");
            if (hasUser && hasPass) {
                Toast.makeText(this, R.string.login_error_required_fields, Toast.LENGTH_SHORT).show();
                return;
            }
            if (hasUser) {
                Toast.makeText(this, R.string.login_error_username_required, Toast.LENGTH_SHORT).show();
                return;
            }
            if (hasPass) {
                Toast.makeText(this, R.string.login_error_password_required, Toast.LENGTH_SHORT).show();
                return;
            }
            if (hasFieldError(e, "email")) {
                Toast.makeText(this, R.string.login_invalid_credentials, Toast.LENGTH_SHORT).show();
                return;
            }
        }
        if (e.code == 401) {
            Toast.makeText(this, R.string.login_invalid_credentials, Toast.LENGTH_SHORT).show();
            return;
        }
        if (e.code == 429) {
            Toast.makeText(this, R.string.login_too_many_attempts, Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, R.string.login_failure_message, Toast.LENGTH_SHORT).show();
    }

    private boolean hasFieldError(AuthException e, String field) {
        return e.fieldErrors != null
                && e.fieldErrors.get(field) != null
                && !e.fieldErrors.get(field).isEmpty();
    }

    private void showGenericLoginError(Exception e) {
        String msg = e.getMessage();
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        if (e instanceof java.net.SocketTimeoutException
                || root instanceof java.net.SocketTimeoutException
                || e instanceof java.io.InterruptedIOException
                || (msg != null && (msg.contains("timed out") || msg.contains("Timeout")
                || msg.contains("interrupted")))) {
            Toast.makeText(this, R.string.login_timeout_error, Toast.LENGTH_LONG).show();
            return;
        }
        if (e instanceof java.net.UnknownHostException
                || e instanceof java.net.ConnectException
                || root instanceof java.net.UnknownHostException
                || root instanceof java.net.ConnectException
                || (msg != null && (msg.contains("Unable to resolve host")
                || msg.contains("Failed to connect")
                || msg.contains("Connection reset")
                || msg.contains("SSLHandshake")
                || msg.contains("No address associated")))) {
            Toast.makeText(this, R.string.login_dns_error, Toast.LENGTH_LONG).show();
            return;
        }
        if (msg != null) {
            if (msg.contains("Invalid credentials")) {
                Toast.makeText(this, R.string.login_invalid_credentials, Toast.LENGTH_SHORT).show();
                return;
            }
            String json = extractJsonFromMessage(msg);
            if (json != null) {
                ApiError err = parseApiError(json);
                if (err != null && err.errors != null) {
                    if (err.errors.containsKey("username")) {
                        Toast.makeText(this, R.string.login_error_username_required, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (err.errors.containsKey("password")) {
                        Toast.makeText(this, R.string.login_error_password_required, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (err.errors.containsKey("email")) {
                        Toast.makeText(this, R.string.login_invalid_credentials, Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
            }
            if (msg.contains("422")) {
                Toast.makeText(this, R.string.login_failure_message, Toast.LENGTH_SHORT).show();
                return;
            }
        }
        Toast.makeText(this, R.string.login_network_error, Toast.LENGTH_SHORT).show();
    }

    private String extractJsonFromMessage(String msg) {
        int idx = msg.indexOf('{');
        if (idx >= 0) {
            return msg.substring(idx);
        }
        return null;
    }

    private ApiError parseApiError(String json) {
        try {
            Gson g = new GsonBuilder().setLenient().create();
            return g.fromJson(json, ApiError.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static class ApiError {
        java.util.Map<String, java.util.List<String>> errors;
    }

    private void showForgotPasswordDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_forgot_password, null);
        EditText etUsername = dialogView.findViewById(R.id.etUsername);
        TextView tvAccountInfo = dialogView.findViewById(R.id.tvAccountInfo);
        TextView tvSecurityQuestion = dialogView.findViewById(R.id.tvSecurityQuestion);
        EditText etAnswer = dialogView.findViewById(R.id.etSecurityAnswer);
        EditText etNew = dialogView.findViewById(R.id.etNewPassword);
        EditText etConfirm = dialogView.findViewById(R.id.etConfirmPassword);

        String prefill = usernameInput.getText() != null ? usernameInput.getText().toString().trim() : "";
        if (!prefill.isEmpty() && etUsername != null) {
            etUsername.setText(prefill);
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.forgot_password_title)
                .setView(dialogView)
                .setPositiveButton(R.string.forgot_password_submit, null)
                .setNegativeButton(android.R.string.cancel, null);

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            android.widget.Button positive = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
            if (positive != null) {
                positive.setText(getString(R.string.forgot_password_submit));
                positive.setOnClickListener(v -> {
                    String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
                    String answer = etAnswer.getText() != null ? etAnswer.getText().toString().trim() : "";
                    String pass = etNew.getText() != null ? etNew.getText().toString() : "";
                    String confirm = etConfirm.getText() != null ? etConfirm.getText().toString() : "";
                    if (username.isEmpty()) {
                        Toast.makeText(this, R.string.enter_username, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (answer.isEmpty()) {
                        Toast.makeText(this, R.string.security_answer_required, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (pass.length() < 6) {
                        Toast.makeText(this, R.string.admin_profile_password_required, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!pass.equals(confirm)) {
                        Toast.makeText(this, R.string.error_password_mismatch, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    positive.setEnabled(false);
                    new Thread(() -> {
                        try {
                            authRepository.resetPasswordWithAnswer(username, answer, pass, confirm);
                            runOnUiThread(() -> {
                                Toast.makeText(this, R.string.password_updated, Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            });
                        } catch (Exception e) {
                            runOnUiThread(() -> {
                                positive.setEnabled(true);
                                Toast.makeText(this, R.string.forgot_password_failed, Toast.LENGTH_SHORT).show();
                            });
                        }
                    }).start();
                });
            }
        });
        dialog.show();

        Runnable lookup = () -> {
            String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
            if (username.isEmpty()) return;
            if (tvSecurityQuestion != null) {
                tvSecurityQuestion.setText(R.string.forgot_password_loading);
            }
            new Thread(() -> {
                try {
                    AuthRepository.ForgotLookup info = authRepository.forgotLookup(username);
                    runOnUiThread(() -> {
                        if (!dialog.isShowing()) return;
                        if (info == null || !info.exists) {
                            if (tvAccountInfo != null) {
                                tvAccountInfo.setVisibility(View.VISIBLE);
                                tvAccountInfo.setText(R.string.forgot_password_account_not_found);
                            }
                            if (tvSecurityQuestion != null) {
                                tvSecurityQuestion.setText(R.string.forgot_password_account_not_found);
                            }
                            return;
                        }
                        if (tvAccountInfo != null) {
                            tvAccountInfo.setVisibility(View.VISIBLE);
                            String hint = info.account_hint != null ? info.account_hint : "";
                            tvAccountInfo.setText(getString(R.string.forgot_password_account_found, info.name, hint));
                        }
                        if (tvSecurityQuestion != null) {
                            if (info.has_security_answer) {
                                tvSecurityQuestion.setText(info.security_question != null
                                        ? info.security_question
                                        : getString(R.string.security_question_text));
                            } else {
                                tvSecurityQuestion.setText(R.string.forgot_password_no_security);
                            }
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        if (tvAccountInfo != null) {
                            tvAccountInfo.setVisibility(View.VISIBLE);
                            tvAccountInfo.setText(R.string.forgot_password_lookup_failed);
                        }
                    });
                }
            }).start();
        };

        if (!prefill.isEmpty()) {
            lookup.run();
        }
        if (etUsername != null) {
            etUsername.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) lookup.run();
            });
        }
    }

    private void toggleLoading(boolean loading) {
        runOnUiThread(() -> {
            btnLogin.setEnabled(!loading);
            if (loginProgressOverlay != null) {
                loginProgressOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void applyStatusBarColor() {
        ThemeStore.applyLightSystemBars(this);
    }
}
