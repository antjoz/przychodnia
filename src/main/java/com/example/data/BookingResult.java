package com.example.data;

public class BookingResult {
    private boolean success;
    private String generatedLogin;
    private String generatedPassword; // Hasło jawne (do jednorazowego wyświetlenia)

    public BookingResult(boolean success, String generatedLogin, String generatedPassword) {
        this.success = success;
        this.generatedLogin = generatedLogin;
        this.generatedPassword = generatedPassword;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getGeneratedLogin() {
        return generatedLogin;
    }

    public String getGeneratedPassword() {
        return generatedPassword;
    }
}