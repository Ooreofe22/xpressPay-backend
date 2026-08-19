package com.payxpress.repository;

import com.payxpress.model.PaymentTransaction;
import com.payxpress.model.TransactionStatus;
import com.payxpress.model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM PaymentTransaction t WHERE t.paystackReference = :reference")
    Optional<PaymentTransaction> findByPaystackReferenceForUpdate(@Param("reference") String reference);

    Optional<PaymentTransaction> findByPaystackReference(String reference);

    List<PaymentTransaction> findByUserOrderByCreatedAtDesc(User user);

    List<PaymentTransaction> findByAmountGreaterThanOrderByCreatedAtDesc(BigDecimal amount);

    List<PaymentTransaction> findByUserAndAmountGreaterThanOrderByCreatedAtDesc(User user, BigDecimal amount);

    List<PaymentTransaction> findAllByOrderByCreatedAtDesc();

    List<PaymentTransaction> findByStatusOrderByCreatedAtDesc(TransactionStatus status);

    long countByStatus(TransactionStatus status);
}
