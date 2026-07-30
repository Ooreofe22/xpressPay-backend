package com.payxpress.auth;

public interface SmsService {
    void sendOtp(String phone, String code);
}
