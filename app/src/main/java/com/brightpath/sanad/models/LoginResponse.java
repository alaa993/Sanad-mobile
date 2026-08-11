package com.brightpath.sanad.models;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {
    public boolean ok;
    public String status;
    public String message;

    @SerializedName("token")
    public String token;

    @SerializedName("access_token")
    public String accessToken;

    public User user;

    @SerializedName("data")
    public Data data;

    public String resolveToken() {
        if (token != null && !token.isEmpty()) { return token; }
        if (accessToken != null && !accessToken.isEmpty()) { return accessToken; }
        if (data != null) {
            if (data.token != null && !data.token.isEmpty()) { return data.token; }
            if (data.accessToken != null && !data.accessToken.isEmpty()) { return data.accessToken; }
        }
        return null;
    }

    public User resolveUser() {
        if (user != null) { return user; }
        return data != null ? data.user : null;
    }

    public static class Data {
        @SerializedName("token")
        public String token;

        @SerializedName("access_token")
        public String accessToken;

        public User user;
    }
}
