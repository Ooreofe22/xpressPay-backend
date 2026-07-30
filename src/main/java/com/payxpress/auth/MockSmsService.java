package com.payxpress.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Logs the OTP instead of sending a real SMS. Active only under dev/test/local
 * profiles — see application.properties. Swap for a real provider (e.g. Termii)
 * by implementing SmsService and giving it a prod-only @Profile.
 */
@Service
@Profile({"dev", "test", "local"})
public class MockSmsService implements SmsService {

    private static final Logger log = LoggerFactory.getLogger(MockSmsService.class);

    @Override
    public void sendOtp(String phone, String code) {
        log.info("[MOCK SMS] OTP for {} is: {}", phone, code);
    }
}
