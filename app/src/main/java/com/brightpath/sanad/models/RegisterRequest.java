package com.brightpath.sanad.models;

public class RegisterRequest {
    public String name;
    public String email;
    public String password;
    public String phone;
    public String locale;
    public String timezone;
    public String role;

    public RegisterRequest(String name, String email, String password, String phone, String locale, String timezone, String role) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.locale = locale;
        this.timezone = timezone;
        this.role = role;
    }
}
