package com.payxpress.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bill_beneficiaries")
@Getter
@Setter
@NoArgsConstructor
public class BillBeneficiary {

    @Id
    private String id = UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String billerUniqueCode;
    private String billerCategoryPath;

    private String label; // user-facing nickname, e.g. "Mum's DSTV"
    private String accountNumber; // electricity/cable meter or smartcard number
    private String phoneNumber;   // airtime/data
    private String accountType;   // prepaid/postpaid, where relevant

    private Instant createdAt = Instant.now();
}
