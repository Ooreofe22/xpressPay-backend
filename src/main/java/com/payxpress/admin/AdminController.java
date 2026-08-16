package com.payxpress.admin;

import com.payxpress.model.PaymentTransaction;
import com.payxpress.model.User;
import com.payxpress.model.UserStatus;
import com.payxpress.repository.PaymentTransactionRepository;
import com.payxpress.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Admin dashboard endpoints. Gated by a single shared secret header
 * (X-Admin-Key) compared against admin.api-key - this is intentionally
 * basic and NOT production-grade auth. Swap for real role-based auth
 * (a dedicated admin user + JWT role claim) before shipping this beyond
 * internal/demo use.
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    private static final BigDecimal FLAG_THRESHOLD = new BigDecimal("20000");

    private final UserRepository userRepository;
    private final PaymentTransactionRepository transactionRepository;

    @Value("${admin.api-key:changeme-admin-key}")
    private String adminApiKey;

    public AdminController(UserRepository userRepository, PaymentTransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    @GetMapping("/accounts")
    public ResponseEntity<?> listAccounts(@RequestHeader("X-Admin-Key") String key) {
        if (!checkKey(key)) return unauthorized();

        List<AccountSummary> accounts = userRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(u -> {
                    boolean flagged = !transactionRepository
                            .findByUserAndAmountGreaterThanOrderByCreatedAtDesc(u, FLAG_THRESHOLD).isEmpty();
                    return AccountSummary.from(u, flagged);
                })
                .toList();

        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/accounts/{id}")
    public ResponseEntity<?> getAccount(@PathVariable String id, @RequestHeader("X-Admin-Key") String key) {
        if (!checkKey(key)) return unauthorized();

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        List<PaymentTransaction> flagged = transactionRepository
                .findByUserAndAmountGreaterThanOrderByCreatedAtDesc(user, FLAG_THRESHOLD);

        return ResponseEntity.ok(Map.of(
                "account", AccountSummary.from(user, !flagged.isEmpty()),
                "flaggedTransactions", flagged.stream().map(FlaggedTransactionView::from).toList()
        ));
    }

    @PostMapping("/accounts/{id}/deactivate")
    public ResponseEntity<?> deactivate(@PathVariable String id, @RequestHeader("X-Admin-Key") String key) {
        if (!checkKey(key)) return unauthorized();
        User user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Account not found"));
        user.setStatus(UserStatus.SUSPENDED);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("status", "SUSPENDED"));
    }

    @PostMapping("/accounts/{id}/reactivate")
    public ResponseEntity<?> reactivate(@PathVariable String id, @RequestHeader("X-Admin-Key") String key) {
        if (!checkKey(key)) return unauthorized();
        User user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Account not found"));
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("status", "ACTIVE"));
    }

    @DeleteMapping("/accounts/{id}")
    public ResponseEntity<?> delete(@PathVariable String id, @RequestHeader("X-Admin-Key") String key) {
        if (!checkKey(key)) return unauthorized();
        if (!userRepository.existsById(id)) throw new IllegalArgumentException("Account not found");
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/transactions/flagged")
    public ResponseEntity<?> listFlagged(@RequestHeader("X-Admin-Key") String key) {
        if (!checkKey(key)) return unauthorized();
        List<PaymentTransaction> flagged = transactionRepository
                .findByAmountGreaterThanOrderByCreatedAtDesc(FLAG_THRESHOLD);
        return ResponseEntity.ok(flagged.stream().map(FlaggedTransactionView::from).toList());
    }

    private boolean checkKey(String provided) {
        return adminApiKey.equals(provided);
    }

    private ResponseEntity<?> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid admin key"));
    }

    public record AccountSummary(String id, String email, java.math.BigDecimal balance, String status,
                                  boolean flagged, java.time.Instant createdAt) {
        static AccountSummary from(User u, boolean flagged) {
            return new AccountSummary(u.getId(), u.getEmail(), u.getBalance(), u.getStatus().name(),
                    flagged, u.getCreatedAt());
        }
    }

    public record FlaggedTransactionView(String id, String userEmail, java.math.BigDecimal amount,
                                          String status, String billerUniqueCode, java.time.Instant createdAt) {
        static FlaggedTransactionView from(PaymentTransaction t) {
            return new FlaggedTransactionView(t.getId(), t.getUser().getEmail(), t.getAmount(),
                    t.getStatus().name(), t.getBillerUniqueCode(), t.getCreatedAt());
        }
    }
}
