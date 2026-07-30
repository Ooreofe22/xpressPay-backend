package com.payxpress.wallet;

import com.payxpress.model.User;
import com.payxpress.model.VirtualAccount;
import com.payxpress.repository.VirtualAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

/**
 * Generates decorative virtual account numbers on signup. These are NOT
 * connected to any real bank/NIBSS settlement — funds sent to them will not
 * be received. All real payments in this app route through Paystack checkout.
 */
@Service
public class VirtualAccountService {

    private final VirtualAccountRepository repository;
    private static final List<String> BANK_NAMES = List.of("Wema Bank", "GTBank", "Providus Bank");

    public VirtualAccountService(VirtualAccountRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void generateAccountsForUser(User user) {
        for (String bank : BANK_NAMES) {
            VirtualAccount account = new VirtualAccount();
            account.setUser(user);
            account.setBankName(bank);
            account.setAccountNumber(generateAccountNumber());
            account.setAccountName("PAYXPRESS-" + user.getPhone().replace("+234", "0"));
            repository.save(account);
        }
    }

    private String generateAccountNumber() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) sb.append(random.nextInt(10));
        return sb.toString();
    }
}
