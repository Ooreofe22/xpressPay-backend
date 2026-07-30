package com.payxpress.repository;

import com.payxpress.model.Biller;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BillerRepository extends JpaRepository<Biller, String> {
    Optional<Biller> findByUniqueCode(String uniqueCode);
    List<Biller> findByCategoryPath(String categoryPath);
}
