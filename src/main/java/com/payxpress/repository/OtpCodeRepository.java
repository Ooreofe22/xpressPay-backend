package com.payxpress.repository;

import com.payxpress.model.OtpCode;
import com.payxpress.model.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpCodeRepository extends JpaRepository<OtpCode, String> {
    Optional<OtpCode> findTopByPhoneAndPurposeOrderByCreatedAtDesc(String phone, OtpPurpose purpose);
    Optional<OtpCode> findTopByPhoneAndPurposeAndConsumedFalseOrderByCreatedAtDesc(String phone, OtpPurpose purpose);
}
