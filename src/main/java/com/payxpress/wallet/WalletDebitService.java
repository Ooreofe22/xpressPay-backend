package com.payxpress.wallet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payxpress.biller.BillerFulfilDispatcher;
import com.payxpress.biller.FulfilOutcome;
import com.payxpress.model.*;
import com.payxpress.repository.BillerRepository;
import com.payxpress.repository.PaymentTransactionRepository;
import com.payxpress.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Handles the "Pay with Balance" path: atomic debit against the user's wallet,
 * then immediate biller fulfilment (single attempt - unlike the gateway path,
 * there's no webhook race to wait out, so we resolve synchronously and refund
 * on a clean failure).
 */
@Service
public class WalletDebitService {

    private final UserRepository userRepository;
    private final BillerRepository billerRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final BillerFulfilDispatcher fulfilDispatcher;
    private final ObjectMapper objectMapper;

    public WalletDebitService(UserRepository userRepository, BillerRepository billerRepository,
                               PaymentTransactionRepository transactionRepository,
                               BillerFulfilDispatcher fulfilDispatcher, ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.billerRepository = billerRepository;
        this.transactionRepository = transactionRepository;
        this.fulfilDispatcher = fulfilDispatcher;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PaymentTransaction payBillFromBalance(String userId, String uniqueCode, String categoryPath,
                                                  BigDecimal amount, Map<String, Object> details) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        if (user.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient balance");
        }

        Biller biller = billerRepository.findByUniqueCode(uniqueCode)
                .orElseThrow(() -> new IllegalArgumentException("Unknown biller: " + uniqueCode));

        String detailsJson;
        try {
            detailsJson = objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid details payload");
        }

        PaymentTransaction txn = new PaymentTransaction();
        txn.setUser(user);
        txn.setBiller(biller);
        txn.setInternalRequestId(UUID.randomUUID().toString());
        txn.setBillerUniqueCode(biller.getUniqueCode());
        txn.setBillerCategoryPath(categoryPath);
        txn.setAmount(amount);
        txn.setBillerDetailsJson(detailsJson);
        txn.setPaymentMethod(PaymentMethod.BALANCE);
        txn.setStatus(TransactionStatus.FULFILLING);

        // Debit up front - this transaction only commits if fulfilment resolves below,
        // and we explicitly refund on a clean decline.
        user.setBalance(user.getBalance().subtract(amount));
        userRepository.save(user);
        transactionRepository.save(txn);

        try {
            FulfilOutcome outcome = fulfilDispatcher.dispatch(txn, details);
            txn.setXpressReferenceId(outcome.referenceId());
            txn.setBillerResponseCode(outcome.responseCode());
            txn.setBillerResponseMessage(outcome.responseMessage());
            txn.setFulfilAttempts(1);

            if (outcome.successful()) {
                txn.setStatus(TransactionStatus.FULFILLED);
            } else {
                // clean decline - refund immediately, no ambiguity to reconcile
                user.setBalance(user.getBalance().add(amount));
                userRepository.save(user);
                txn.setStatus(TransactionStatus.FAILED);
            }
        } catch (Exception e) {
            user.setBalance(user.getBalance().add(amount));
            userRepository.save(user);
            txn.setStatus(TransactionStatus.NEEDS_RECONCILIATION);
            txn.setBillerResponseMessage("Fulfilment error: " + e.getMessage());
        }

        transactionRepository.save(txn);
        return txn;
    }
}
