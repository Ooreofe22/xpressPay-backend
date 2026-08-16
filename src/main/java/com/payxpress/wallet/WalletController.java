package com.payxpress.wallet;

import com.payxpress.model.PaymentTransaction;
import com.payxpress.model.User;
import com.payxpress.model.VirtualAccount;
import com.payxpress.repository.VirtualAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/wallet")
public class WalletController {

    private final VirtualAccountRepository virtualAccountRepository;
    private final WalletDebitService walletDebitService;

    public WalletController(VirtualAccountRepository virtualAccountRepository, WalletDebitService walletDebitService) {
        this.virtualAccountRepository = virtualAccountRepository;
        this.walletDebitService = walletDebitService;
    }

    @GetMapping("/balance")
    public Map<String, Object> balance(Authentication auth) {
        User user = (User) auth.getPrincipal();
        return Map.of("balance", user.getBalance(), "currency", "NGN");
    }

    @GetMapping("/accounts")
    public List<AccountView> accounts(Authentication auth) {
        User user = (User) auth.getPrincipal();
        return virtualAccountRepository.findByUser(user).stream().map(AccountView::from).toList();
    }

    // "Pay with Balance" - atomic debit, single-attempt fulfilment, refund on clean decline
    @PostMapping("/debits/bill-payment")
    public ResponseEntity<?> payBillFromWallet(@RequestBody WalletDebitRequest request, Authentication auth) {
        User user = (User) auth.getPrincipal();

        PaymentTransaction txn = walletDebitService.payBillFromBalance(
                user.getId(), request.uniqueCode(), request.categoryPath(), request.amount(), request.details());

        boolean success = txn.getStatus().name().equals("FULFILLED");
        HttpStatus status = success ? HttpStatus.OK : HttpStatus.UNPROCESSABLE_ENTITY;

        return ResponseEntity.status(status).body(Map.of(
                "transactionId", txn.getId(),
                "status", txn.getStatus().name(),
                "xpressReferenceId", txn.getXpressReferenceId() == null ? "" : txn.getXpressReferenceId(),
                "billerResponseMessage", txn.getBillerResponseMessage() == null ? "" : txn.getBillerResponseMessage(),
                "amount", txn.getAmount(),
                "newBalance", user.getBalance()
        ));
    }

    public record WalletDebitRequest(String uniqueCode, String categoryPath, BigDecimal amount,
                                      Map<String, Object> details) {}

    public record AccountView(String bankName, String accountNumber, String accountName) {
        static AccountView from(VirtualAccount va) {
            return new AccountView(va.getBankName(), va.getAccountNumber(), va.getAccountName());
        }
    }
}
