package com.payxpress.auth;

public interface EmailService {
    void sendOtp(String email, String code);
}
