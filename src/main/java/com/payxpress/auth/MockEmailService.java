package com.payxpress.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Logs the OTP instead of sending a real email. Active only under dev/test/local
 * profiles - see application.properties. ResendEmailService takes over in prod.
 */
@Service
@Profile({"dev", "test", "local"})
public class MockEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(MockEmailService.class);

    @Override
    public void sendOtp(String email, String code) {
        log.info("[MOCK EMAIL] OTP for {} is: {}", email, code);
    }
}
