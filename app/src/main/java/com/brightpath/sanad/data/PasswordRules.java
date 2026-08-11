package com.brightpath.sanad.data;

import androidx.annotation.Nullable;

/**
 * قواعد كلمة المرور — مطابقة للخادم (min:6، أي أحرف صغيرة/كبيرة/أرقام/رموز).
 */
public final class PasswordRules {
    public static final int MIN_LENGTH = 6;

    private PasswordRules() {}

    public enum Issue {
        TOO_SHORT,
        MISMATCH
    }

    public static boolean isValidLength(@Nullable String password) {
        return password != null && password.length() >= MIN_LENGTH;
    }

    @Nullable
    public static Issue validateRegistration(@Nullable String password, @Nullable String confirm) {
        if (!isValidLength(password)) {
            return Issue.TOO_SHORT;
        }
        if (password == null || confirm == null || !password.equals(confirm)) {
            return Issue.MISMATCH;
        }
        return null;
    }
}
