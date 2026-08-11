package com.brightpath.sanad.feature.patient;

import androidx.annotation.Nullable;

public final class PatientIntakeStatus {
    public enum Type {
        IDLE,
        LOADING,
        SUCCESS,
        ERROR
    }

    public final Type type;
    @Nullable
    public final String message;

    private PatientIntakeStatus(Type type, @Nullable String message) {
        this.type = type;
        this.message = message;
    }

    public static PatientIntakeStatus idle() {
        return new PatientIntakeStatus(Type.IDLE, null);
    }

    public static PatientIntakeStatus loading(@Nullable String message) {
        return new PatientIntakeStatus(Type.LOADING, message);
    }

    public static PatientIntakeStatus success(@Nullable String message) {
        return new PatientIntakeStatus(Type.SUCCESS, message);
    }

    public static PatientIntakeStatus error(@Nullable String message) {
        return new PatientIntakeStatus(Type.ERROR, message);
    }
}
