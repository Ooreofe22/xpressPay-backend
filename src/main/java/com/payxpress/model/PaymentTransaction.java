package com.payxpress.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "payment_transactions",
        indexes = {
                @Index(name = "idx_paystack_reference", columnList = "paystackReference", unique = true)
        }
)
@Getter
@Setter
@NoArgsConstructor
public class PaymentTransaction {

    @Id
    private String id = UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "biller_id")
    private Biller biller;

    @Column(unique = true, nullable = false)
    private String internalRequestId;

    private String xpressReferenceId;

    @Column(unique = true)
    private String paystackReference;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod = PaymentMethod.GATEWAY;

    private String billerCategoryPath;
    private String billerUniqueCode;

    @Column(columnDefinition = "TEXT")
    private String billerDetailsJson;

    private String billerResponseCode;
    private String billerResponseMessage;

    private int fulfilAttempts = 0;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    @Version
    private Long version;
}
