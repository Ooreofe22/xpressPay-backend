package com.payxpress.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payxpress.biller.BillerFulfilDispatcher;
import com.payxpress.biller.FulfilOutcome;
import com.payxpress.model.PaymentTransaction;
import com.payxpress.model.TransactionStatus;
import com.payxpress.repository.PaymentTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class PaymentConfirmationService {

    private final PaymentTransactionRepository transactionRepository;
    private final PaystackService paystackService;
    private final BillerFulfilDispatcher fulfilDispatcher;
    private final ObjectMapper objectMapper;

    private static final int MAX_FULFIL_ATTEMPTS = 3;

    public PaymentConfirmationService(PaymentTransactionRepository transactionRepository,
                                       PaystackService paystackService,
                                       BillerFulfilDispatcher fulfilDispatcher,
                                       ObjectMapper objectMapper) {
        this.transactionRepository = transactionRepository;
        this.paystackService = paystackService;
        this.fulfilDispatcher = fulfilDispatcher;
        this.objectMapper = objectMapper;
    }

    /**
     * Called by BOTH the webhook handler and the verify-on-redirect handler.
     * Whichever arrives first does the work; the second is a safe no-op —
     * enforced by SELECT ... FOR UPDATE in the repository, not application logic.
     */
    @Transactional
    public void confirmPayment(String paystackReference) {
        PaymentTransaction txn = transactionRepository.findByPaystackReferenceForUpdate(paystackReference)
                .orElseThrow(() -> new IllegalStateException("No transaction found for reference " + paystackReference));

        if (txn.getStatus() != TransactionStatus.PENDING_PAYMENT) {
            return; // already handled by the other race participant
        }

        Map<String, Object> verification = paystackService.verifyTransaction(paystackReference);
        Map<String, Object> data = (Map<String, Object>) verification.get("data");
        String gatewayStatus = data != null ? (String) data.get("status") : null;

        if (!"success".equals(gatewayStatus)) {
            txn.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(txn);
            return;
        }

        BigDecimal verifiedAmount = new BigDecimal(String.valueOf(data.get("amount")))
                .divide(BigDecimal.valueOf(100)); // kobo -> naira

        if (verifiedAmount.compareTo(txn.getAmount()) != 0) {
            txn.setStatus(TransactionStatus.NEEDS_RECONCILIATION);
            transactionRepository.save(txn);
            return;
        }

        txn.setStatus(TransactionStatus.PAYSTACK_CONFIRMED);
        transactionRepository.save(txn);

        attemptFulfil(txn);
    }

    private void attemptFulfil(PaymentTransaction txn) {
        txn.setStatus(TransactionStatus.FULFILLING);
        transactionRepository.save(txn);

        Map<String, Object> details = parseDetails(txn.getBillerDetailsJson());
        int attempt = 0;
        long backoffMillis = 1000;

        while (attempt < MAX_FULFIL_ATTEMPTS) {
            attempt++;
            txn.setFulfilAttempts(attempt);
            try {
                FulfilOutcome outcome = fulfilDispatcher.dispatch(txn, details);

                txn.setXpressReferenceId(outcome.referenceId());
                txn.setBillerResponseCode(outcome.responseCode());
                txn.setBillerResponseMessage(outcome.responseMessage());

                if (outcome.successful()) {
                    txn.setStatus(TransactionStatus.FULFILLED);
                } else {
                    // a real decline (e.g. bad code, insufficient balance) — don't retry blindly
                    txn.setStatus(TransactionStatus.NEEDS_RECONCILIATION);
                }
                transactionRepository.save(txn);
                return;

            } catch (Exception e) {
                if (attempt >= MAX_FULFIL_ATTEMPTS) {
                    txn.setStatus(TransactionStatus.NEEDS_RECONCILIATION);
                    txn.setBillerResponseMessage("Exhausted retries: " + e.getMessage());
                    transactionRepository.save(txn);
                    return;
                }
                sleep(backoffMillis);
                backoffMillis *= 2;
            }
        }
    }

    private Map<String, Object> parseDetails(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Corrupted billerDetailsJson", e);
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
