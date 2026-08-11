package com.brightpath.sanad.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.brightpath.sanad.R;
import com.brightpath.sanad.data.AppConfig;
import com.brightpath.sanad.data.LanguageUiHelper;
import com.brightpath.sanad.data.LocaleHelper;
import com.brightpath.sanad.data.PasswordRules;
import com.brightpath.sanad.data.auth.AuthException;
import com.brightpath.sanad.data.auth.AuthRepository;
import com.brightpath.sanad.models.RegisterRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class RegisterActivity extends AppCompatActivity {

    private static final String STATE_ROLE = "register_selected_role";
    private static final String[] ROLE_VALUES = new String[]{"patient", "specialist", "organization"};

    private EditText inputName, inputPassword, inputConfirm, inputSecurityAnswer;
    private TextInputLayout tilPassword, tilConfirm;
    private EditText inputEmail, inputPhone;
    private MaterialButton btnRegister;
    private View btnLoginLink;
    private TextInputLayout tilEmail, tilPhone;
    private AutoCompleteTextView roleDropdown;
    private View contactLabel;
    private View registerProgressOverlay;
    private String selectedRole = "patient";
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LocaleHelper.applySavedLocale(this);
        new com.brightpath.sanad.data.ThemeStore(this).applySavedTheme(this);
        applyStatusBarColor();
        setContentView(R.layout.activity_register);

        if (savedInstanceState != null) {
            String restored = savedInstanceState.getString(STATE_ROLE);
            if (!TextUtils.isEmpty(restored)) {
                selectedRole = normalizeRole(restored);
            }
        }

        inputName = findViewById(R.id.inputName);
        inputEmail = findViewById(R.id.inputEmail);
        inputPhone = findViewById(R.id.inputPhone);
        inputPassword = findViewById(R.id.inputPassword);
        inputConfirm = findViewById(R.id.inputConfirm);
        inputSecurityAnswer = findViewById(R.id.inputSecurityAnswer);
        btnRegister = findViewById(R.id.btnRegisterAccount);
        btnLoginLink = findViewById(R.id.linkLogin);
        tilEmail = findViewById(R.id.tilEmail);
        tilPhone = findViewById(R.id.tilPhone);
        roleDropdown = findViewById(R.id.roleDropdown);
        contactLabel = findViewById(R.id.contactLabel);
        registerProgressOverlay = findViewById(R.id.registerProgressOverlay);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirm = findViewById(R.id.tilConfirm);

        configurePasswordField(inputPassword);
        configurePasswordField(inputConfirm);

        LanguageUiHelper.bindAuthToggleGroup(
                this,
                findViewById(R.id.groupAuthLanguage),
                R.id.btnAuthLangArabic,
                R.id.btnAuthLangEnglish,
                R.id.btnAuthLangTurkish
        );

        authRepository = new AuthRepository(this, AppConfig.BASE_URL);

        setupRoleDropdown();
        updateRoleFieldsVisibility();

        btnRegister.setOnClickListener(v -> attemptRegister());
        btnLoginLink.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_ROLE, selectedRole);
    }

    private void setupRoleDropdown() {
        if (roleDropdown == null) return;
        String[] labels = new String[]{
                getString(R.string.register_role_patient),
                getString(R.string.register_role_specialist),
                getString(R.string.register_role_organization)
        };
        NonFilterArrayAdapter adapter = new NonFilterArrayAdapter(this, labels);
        roleDropdown.setAdapter(adapter);
        roleDropdown.setThreshold(Integer.MAX_VALUE);
        roleDropdown.setKeyListener(null);
        roleDropdown.setFocusable(false);
        roleDropdown.setCursorVisible(false);
        roleDropdown.setSaveEnabled(false);

        int index = roleIndex(selectedRole);
        selectedRole = ROLE_VALUES[index];
        roleDropdown.setText(labels[index], false);

        roleDropdown.setOnClickListener(v -> roleDropdown.showDropDown());
        roleDropdown.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> {
            if (position < 0 || position >= ROLE_VALUES.length) return;
            selectedRole = ROLE_VALUES[position];
            roleDropdown.setText(labels[position], false);
            updateRoleFieldsVisibility();
        });
    }

    private void updateRoleFieldsVisibility() {
        boolean showContactFields = !"patient".equalsIgnoreCase(selectedRole);
        if (tilEmail != null) tilEmail.setVisibility(showContactFields ? View.VISIBLE : View.GONE);
        if (tilPhone != null) tilPhone.setVisibility(showContactFields ? View.VISIBLE : View.GONE);
        if (contactLabel != null) contactLabel.setVisibility(showContactFields ? View.VISIBLE : View.GONE);
        if (!showContactFields) {
            if (inputEmail != null) inputEmail.setText("");
            if (inputPhone != null) inputPhone.setText("");
        }
    }

    private void attemptRegister() {
        String name = inputName.getText().toString().trim();
        String password = inputPassword.getText().toString();
        String confirm = inputConfirm.getText().toString();

        boolean needsContactFields = !"patient".equalsIgnoreCase(selectedRole);
        String email = needsContactFields && inputEmail != null
                ? blankToNull(inputEmail.getText().toString())
                : null;
        String phone = needsContactFields && inputPhone != null
                ? blankToNull(inputPhone.getText().toString())
                : null;

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, R.string.error_required_fields, Toast.LENGTH_SHORT).show();
            return;
        }
        if (name.length() < 2) {
            Toast.makeText(this, R.string.register_error_name_short, Toast.LENGTH_SHORT).show();
            return;
        }
        if (inputSecurityAnswer == null || TextUtils.isEmpty(inputSecurityAnswer.getText())) {
            Toast.makeText(this, R.string.security_answer_required, Toast.LENGTH_SHORT).show();
            return;
        }
        if (needsContactFields && TextUtils.isEmpty(email)) {
            Toast.makeText(this, R.string.error_required_fields, Toast.LENGTH_SHORT).show();
            return;
        }
        clearPasswordErrors();
        PasswordRules.Issue passwordIssue = PasswordRules.validateRegistration(password, confirm);
        if (passwordIssue == PasswordRules.Issue.TOO_SHORT) {
            String msg = getString(R.string.register_error_password_short, PasswordRules.MIN_LENGTH);
            if (tilPassword != null) {
                tilPassword.setError(msg);
            }
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            return;
        }
        if (passwordIssue == PasswordRules.Issue.MISMATCH) {
            String msg = getString(R.string.error_password_mismatch);
            if (tilConfirm != null) {
                tilConfirm.setError(msg);
            }
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            return;
        }

        String locale = LocaleHelper.resolveSavedTag(this);
        if (locale.length() > 5) locale = locale.substring(0, 5);
        String timezone = TimeZone.getDefault().getID();
        if (timezone != null && timezone.length() > 64) {
            timezone = timezone.substring(0, 64);
        }
        RegisterRequest request = new RegisterRequest(name, email, password, phone, locale, timezone, selectedRole);
        String securityAnswer = inputSecurityAnswer.getText().toString().trim();

        toggleRegisterLoading(true);
        new Thread(() -> {
            try {
                authRepository.register(request);
                try {
                    authRepository.saveSecurityAnswer(name, securityAnswer);
                } catch (Exception ignored) {
                    // التسجيل نجح — لا نمنع المستخدم إذا فشل حفظ سؤال الأمان
                }
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.register_success, Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, OnboardingActivity.class));
                    finishAffinity();
                });
            } catch (AuthException authEx) {
                String message = resolveRegisterError(authEx);
                runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                String toast = getString(R.string.register_error_general);
                String msg = e.getMessage() != null ? e.getMessage() : "";
                if (e instanceof java.net.UnknownHostException
                        || msg.contains("Unable to resolve host")
                        || msg.contains("No address associated")) {
                    toast = getString(R.string.login_dns_error);
                } else if (e instanceof java.net.SocketTimeoutException
                        || e instanceof java.io.InterruptedIOException
                        || msg.contains("timed out")
                        || msg.contains("Timeout")) {
                    toast = getString(R.string.login_timeout_error);
                } else if (e instanceof java.io.IOException) {
                    toast = getString(R.string.login_network_error);
                }
                final String shown = toast;
                runOnUiThread(() ->
                        Toast.makeText(this, shown, Toast.LENGTH_LONG).show()
                );
            } finally {
                runOnUiThread(() -> toggleRegisterLoading(false));
            }
        }).start();
    }

    private String resolveRegisterError(AuthException e) {
        if (e.fieldErrors != null && !e.fieldErrors.isEmpty()) {
            String mapped = mapFieldErrors(e.fieldErrors);
            if (mapped != null) return mapped;
        }
        if (e.serverMessage != null && !e.serverMessage.trim().isEmpty()) {
            return e.serverMessage.trim();
        }
        String body = e.errorBody != null ? e.errorBody.toLowerCase(Locale.US) : "";
        if (body.contains("exists") || body.contains("already") || body.contains("taken") || e.code == 409) {
            return getString(R.string.register_error_name_exists);
        }
        if (e.code == 422) {
            return getString(R.string.register_error_validation);
        }
        return getString(R.string.register_error_general);
    }

    @Nullable
    private String mapFieldErrors(Map<String, List<String>> errors) {
        String passwordMsg = firstFieldError(errors, "password");
        if (passwordMsg != null) {
            if (passwordMsg.toLowerCase(Locale.US).contains("at least") || passwordMsg.contains("6")) {
                return getString(R.string.register_error_password_short, PasswordRules.MIN_LENGTH);
            }
            return passwordMsg;
        }
        String nameMsg = firstFieldError(errors, "name");
        if (nameMsg != null) {
            if (nameMsg.toLowerCase(Locale.US).contains("at least") || nameMsg.contains("2")) {
                return getString(R.string.register_error_name_short);
            }
            return nameMsg;
        }
        String emailMsg = firstFieldError(errors, "email");
        if (emailMsg != null) {
            String lower = emailMsg.toLowerCase(Locale.US);
            if (lower.contains("taken") || lower.contains("unique") || lower.contains("already")) {
                return getString(R.string.register_error_email_taken);
            }
            return getString(R.string.register_error_email_invalid);
        }
        String phoneMsg = firstFieldError(errors, "phone");
        if (phoneMsg != null) {
            String lower = phoneMsg.toLowerCase(Locale.US);
            if (lower.contains("taken") || lower.contains("unique") || lower.contains("already")) {
                return getString(R.string.register_error_phone_taken);
            }
            return getString(R.string.register_error_phone_invalid);
        }
        // أي حقل آخر: أظهر أول رسالة من السيرفر
        for (List<String> messages : errors.values()) {
            if (messages != null && !messages.isEmpty() && !TextUtils.isEmpty(messages.get(0))) {
                return messages.get(0);
            }
        }
        return null;
    }

    private String firstFieldError(Map<String, List<String>> errors, String field) {
        List<String> messages = errors.get(field);
        if (messages == null || messages.isEmpty()) return null;
        return TextUtils.join("\n", messages);
    }

    @Nullable
    private static String blankToNull(@Nullable String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static int roleIndex(@Nullable String role) {
        String normalized = normalizeRole(role);
        for (int i = 0; i < ROLE_VALUES.length; i++) {
            if (ROLE_VALUES[i].equals(normalized)) return i;
        }
        return 0;
    }

    @NonNull
    private static String normalizeRole(@Nullable String role) {
        if (role == null) return "patient";
        String value = role.trim().toLowerCase(Locale.US);
        if ("specialist".equals(value) || "organization".equals(value) || "patient".equals(value)) {
            return value;
        }
        return "patient";
    }

    private void configurePasswordField(@Nullable EditText field) {
        if (field == null) return;
        field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
    }

    private void clearPasswordErrors() {
        if (tilPassword != null) tilPassword.setError(null);
        if (tilConfirm != null) tilConfirm.setError(null);
    }

    private void toggleRegisterLoading(boolean loading) {
        btnRegister.setEnabled(!loading);
        if (registerProgressOverlay != null) {
            registerProgressOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }

    private void applyStatusBarColor() {
        com.brightpath.sanad.data.ThemeStore.applyLightSystemBars(this);
    }

    /** يعرض كل الأدوار دائماً دون فلترة نص الحقل (مشكلة شائعة مع AutoComplete بعد recreate). */
    private static final class NonFilterArrayAdapter extends ArrayAdapter<String> {
        private final List<String> items;

        NonFilterArrayAdapter(android.content.Context context, String[] labels) {
            super(context, android.R.layout.simple_list_item_1, new ArrayList<>(Arrays.asList(labels)));
            this.items = new ArrayList<>(Arrays.asList(labels));
        }

        @NonNull
        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();
                    results.values = items;
                    results.count = items.size();
                    return results;
                }

                @Override
                @SuppressWarnings("unchecked")
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    clear();
                    if (results != null && results.values instanceof List) {
                        addAll((List<String>) results.values);
                    }
                    notifyDataSetChanged();
                }
            };
        }
    }
}
