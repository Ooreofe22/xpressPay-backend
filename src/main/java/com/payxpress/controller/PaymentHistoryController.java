package com.payxpress.controller;

import com.payxpress.model.PaymentTransaction;
import com.payxpress.model.User;
import com.payxpress.service.PaymentConfirmationService;
import com.payxpress.repository.PaymentTransactionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/billers/payments")
public class PaymentHistoryController {

    private final PaymentTransactionRepository transactionRepository;
    private final PaymentConfirmationService confirmationService;

    public PaymentHistoryController(PaymentTransactionRepository transactionRepository,
                                     PaymentConfirmationService confirmationService) {
        this.transactionRepository = transactionRepository;
        this.confirmationService = confirmationService;
    }

    @GetMapping("/history")
    public List<TransactionView> history(Authentication auth) {
        User user = (User) auth.getPrincipal();
        return transactionRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(TransactionView::from).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable String id, Authentication auth) {
        User user = (User) auth.getPrincipal();
        return transactionRepository.findById(id)
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .map(txn -> ResponseEntity.ok(TransactionView.from(txn)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * This is the "verify-on-redirect" half of the race guard described throughout
     * our design — the frontend calls this right after the Paystack redirect returns.
     * It actively triggers confirmPayment() (safe even if the webhook already ran —
     * confirmPayment() no-ops via the PENDING_PAYMENT guard + row lock), then reports
     * whatever the resulting status is. Previously this only read the DB without ever
     * calling confirmPayment() — meaning if the webhook was slow or never arrived,
     * this endpoint would just report PENDING_PAYMENT forever. Fixed here.
     */
    @GetMapping("/verify/{reference}")
    public ResponseEntity<?> checkStatus(@PathVariable String reference) {
        confirmationService.confirmPayment(reference);

        return transactionRepository.findByPaystackReference(reference)
                .map(txn -> ResponseEntity.ok(TransactionView.from(txn)))
                .orElse(ResponseEntity.notFound().build());
    }

    public record TransactionView(String id, String type, String recipient, BigDecimal amount,
                                   String description, Instant timestamp, String status, String reference,
                                   String billerName, String billerCategoryPath, String paymentMethod,
                                   String xpressReferenceId, String billerResponseMessage) {
        static TransactionView from(PaymentTransaction t) {
            String frontendStatus = switch (t.getStatus()) {
                case FULFILLED -> "completed";
                case FAILED, NEEDS_RECONCILIATION -> "failed"; // internal reconciliation state hidden from customer
                default -> "pending";
            };

            // reference shown to the user — Paystack ref for gateway payments,
            // our own internal request id for balance payments (no Paystack ref exists)
            String displayReference = t.getPaymentMethod().name().equals("GATEWAY")
                    ? t.getPaystackReference()
                    : t.getInternalRequestId();

            return new TransactionView(
                    t.getId(), "payment",
                    t.getBiller().getName() + " — " + t.getBillerUniqueCode(),
                    t.getAmount(),
                    t.getBillerCategoryPath() + " — " + t.getBiller().getName(),
                    t.getCreatedAt(), frontendStatus, displayReference,
                    t.getBiller().getName(), t.getBillerCategoryPath(), t.getPaymentMethod().name(),
                    t.getXpressReferenceId(), t.getBillerResponseMessage()
            );
        }
    }
}
