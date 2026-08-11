package com.payxpress.repository;

import com.payxpress.model.BillBeneficiary;
import com.payxpress.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BillBeneficiaryRepository extends JpaRepository<BillBeneficiary, String> {
    List<BillBeneficiary> findByUserOrderByCreatedAtDesc(User user);
    Optional<BillBeneficiary> findByIdAndUser(String id, User user); // ownership check for delete
}
