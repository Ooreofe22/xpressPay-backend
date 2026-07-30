package com.payxpress.wallet;

import com.payxpress.model.User;
import com.payxpress.model.VirtualAccount;
import com.payxpress.repository.VirtualAccountRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/wallet")
public class WalletController {

    private final VirtualAccountRepository virtualAccountRepository;

    public WalletController(VirtualAccountRepository virtualAccountRepository) {
        this.virtualAccountRepository = virtualAccountRepository;
    }

    @GetMapping("/balance")
    public Map<String, Object> balance() {
        // intentionally always zero — no internal ledger exists;
        // all payments route through Paystack, never debit anything here
        return Map.of("balance", BigDecimal.ZERO, "currency", "NGN");
    }

    @GetMapping("/accounts")
    public List<AccountView> accounts(Authentication auth) {
        User user = (User) auth.getPrincipal();
        return virtualAccountRepository.findByUser(user).stream().map(AccountView::from).toList();
    }

    public record AccountView(String bankName, String accountNumber, String accountName) {
        static AccountView from(VirtualAccount va) {
            return new AccountView(va.getBankName(), va.getAccountNumber(), va.getAccountName());
        }
    }
}
