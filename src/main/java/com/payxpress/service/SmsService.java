package com.payxpress.service;

public interface SmsService {
    void sendOtp(String phone, String code);
}
