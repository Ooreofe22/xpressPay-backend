package com.payxpress.service;

import com.payxpress.model.User;
import com.payxpress.model.UserSession;
import com.payxpress.repository.UserSessionRepository;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
public class SessionService {

    private final UserSessionRepository sessionRepository;
    private static final int REFRESH_TOKEN_DAYS = 30;

    public SessionService(UserSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Transactional
    public String issueSession(User user, String deviceInfo) {
        // revoke every existing session for this user — enforces single active session
        sessionRepository.revokeAllForUser(user.getId());

        String refreshToken = generateOpaqueToken();

        UserSession session = new UserSession();
        session.setUser(user);
        session.setRefreshTokenHash(DigestUtils.sha256Hex(refreshToken));
        session.setDeviceInfo(deviceInfo);
        session.setExpiresAt(Instant.now().plus(REFRESH_TOKEN_DAYS, ChronoUnit.DAYS));
        sessionRepository.save(session);

        return refreshToken;
    }

    public UserSession validateRefreshToken(String refreshToken) {
        String hash = DigestUtils.sha256Hex(refreshToken);
        UserSession session = sessionRepository.findByRefreshTokenHash(hash)
                .orElseThrow(() -> new IllegalStateException("Invalid session"));

        if (session.isRevoked() || Instant.now().isAfter(session.getExpiresAt())) {
            throw new IllegalStateException("Session expired or revoked");
        }
        return session;
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
