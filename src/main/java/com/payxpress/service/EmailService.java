package com.payxpress.service;

public interface EmailService {
    void sendOtp(String email, String code);
}
