package com.payxpress.repository;

import com.payxpress.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, String> {

    Optional<UserSession> findByRefreshTokenHash(String refreshTokenHash);

    @Modifying
    @Query("UPDATE UserSession s SET s.revoked = true WHERE s.user.id = :userId AND s.revoked = false")
    void revokeAllForUser(@Param("userId") String userId);
}
