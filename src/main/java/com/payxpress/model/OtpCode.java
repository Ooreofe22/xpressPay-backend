package com.payxpress.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "otp_codes")
@Getter
@Setter
@NoArgsConstructor
public class OtpCode {

    @Id
    private String id = UUID.randomUUID().toString();

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String codeHash;

    @Enumerated(EnumType.STRING)
    private OtpPurpose purpose;

    private Instant expiresAt;
    private boolean consumed = false;
    private int attempts = 0;
    private Instant createdAt = Instant.now();
}
