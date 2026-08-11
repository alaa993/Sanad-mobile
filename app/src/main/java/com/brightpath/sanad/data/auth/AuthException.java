package com.brightpath.sanad.data.auth;

import java.util.List;
import java.util.Map;

public class AuthException extends Exception {
    public final int code;
    public final String errorBody;
    public final String serverMessage;
    public final Map<String, List<String>> fieldErrors;

    public AuthException(int code, String errorBody, String serverMessage, Map<String, List<String>> fieldErrors) {
        super("Auth failed: " + code);
        this.code = code;
        this.errorBody = errorBody;
        this.serverMessage = serverMessage;
        this.fieldErrors = fieldErrors;
    }
}
