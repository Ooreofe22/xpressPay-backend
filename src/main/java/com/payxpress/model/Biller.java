package com.payxpress.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "billers")
@Getter
@Setter
@NoArgsConstructor
public class Biller {

    @Id
    private String id = UUID.randomUUID().toString();

    @Column(unique = true, nullable = false)
    private String uniqueCode;

    private String name;
    private String billerName;
    private String categoryPath;
    private String categoryName;

    private boolean requiresLookup;
    private boolean fixedAmount;
    private BigDecimal amount;
    private BigDecimal minimumAmount;
    private BigDecimal maximumAmount;
    private String imageUrl;

    private Instant lastSyncedAt;

    /** Admin toggle — disabled billers are hidden from the public catalog going forward. */
    private boolean active = true;
}
