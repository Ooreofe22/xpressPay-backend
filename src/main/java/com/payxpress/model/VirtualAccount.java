package com.payxpress.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "virtual_accounts")
@Getter
@Setter
@NoArgsConstructor
public class VirtualAccount {

    @Id
    private String id = UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String bankName;
    private String accountNumber;
    private String accountName;
    private Instant createdAt = Instant.now();
}
