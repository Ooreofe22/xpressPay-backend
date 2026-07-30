package com.payxpress.repository;

import com.payxpress.model.User;
import com.payxpress.model.VirtualAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VirtualAccountRepository extends JpaRepository<VirtualAccount, String> {
    List<VirtualAccount> findByUser(User user);
}
