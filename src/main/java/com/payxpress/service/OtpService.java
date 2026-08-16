package com.payxpress.service;

import com.payxpress.model.OtpCode;
import com.payxpress.enums.OtpPurpose;
import com.payxpress.repository.OtpCodeRepository;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class OtpService {

    private final OtpCodeRepository otpRepository;
    private final SmsService smsService;

    private static final int EXPIRY_MINUTES = 5;
    private static final int RESEND_COOLDOWN_SECONDS = 59;
    private static final int MAX_ATTEMPTS = 5;

    public OtpService(OtpCodeRepository otpRepository, SmsService smsService) {
        this.otpRepository = otpRepository;
        this.smsService = smsService;
    }

    @Transactional
    public void generateAndSend(String phone, OtpPurpose purpose) {
        otpRepository.findTopByPhoneAndPurposeOrderByCreatedAtDesc(phone, purpose)
                .filter(last -> Duration.between(last.getCreatedAt(), Instant.now()).getSeconds() < RESEND_COOLDOWN_SECONDS)
                .ifPresent(last -> {
                    throw new IllegalStateException("Please wait before requesting another code");
                });

        String code = generateSixDigitCode();

        OtpCode otp = new OtpCode();
        otp.setPhone(phone);
        otp.setCodeHash(hash(code));
        otp.setPurpose(purpose);
        otp.setExpiresAt(Instant.now().plus(EXPIRY_MINUTES, ChronoUnit.MINUTES));
        otpRepository.save(otp);

        smsService.sendOtp(phone, code);
    }

    @Transactional
    public boolean verify(String phone, OtpPurpose purpose, String submittedCode) {
        OtpCode otp = otpRepository.findTopByPhoneAndPurposeAndConsumedFalseOrderByCreatedAtDesc(phone, purpose)
                .orElseThrow(() -> new IllegalStateException("No active code for this number"));

        if (Instant.now().isAfter(otp.getExpiresAt())) {
            throw new IllegalStateException("Code expired");
        }
        if (otp.getAttempts() >= MAX_ATTEMPTS) {
            throw new IllegalStateException("Too many attempts — request a new code");
        }

        otp.setAttempts(otp.getAttempts() + 1);

        boolean matches = hash(submittedCode).equals(otp.getCodeHash());
        if (matches) {
            otp.setConsumed(true);
        }
        otpRepository.save(otp);
        return matches;
    }

    private String generateSixDigitCode() {
        return String.valueOf(100000 + new SecureRandom().nextInt(900000));
    }

    private String hash(String code) {
        return DigestUtils.sha256Hex(code);
    }
}
