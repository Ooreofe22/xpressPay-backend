package com.payxpress.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    private String id = UUID.randomUUID().toString();

    @Column(unique = true, nullable = false)
    private String phone; // E.164, e.g. "+2348012345678"

    private String bvn; // 11 digits, format-checked only

    @Enumerated(EnumType.STRING)
    private UserStatus status = UserStatus.PENDING_VERIFICATION;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
}
