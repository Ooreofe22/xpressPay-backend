package com.payxpress.controller;

import com.payxpress.model.Biller;
import com.payxpress.repository.BillerRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/billers")
public class BillerController {

    private final BillerRepository billerRepository;

    public BillerController(BillerRepository billerRepository) {
        this.billerRepository = billerRepository;
    }

    @GetMapping
    public List<BillerSummary> listAll() {
        return billerRepository.findAll().stream().map(BillerSummary::from).toList();
    }

    @GetMapping("/{categoryPath}")
    public List<BillerSummary> listByCategory(@PathVariable String categoryPath) {
        return billerRepository.findByCategoryPath(categoryPath).stream().map(BillerSummary::from).toList();
    }

    public record BillerSummary(String uniqueCode, String name, String categoryPath, boolean requiresValidate,
                                 boolean fixedAmount, BigDecimal amount, BigDecimal minimumAmount, String imageUrl) {
        static BillerSummary from(Biller b) {
            return new BillerSummary(b.getUniqueCode(), b.getName(), b.getCategoryPath(), b.isRequiresLookup(),
                    b.isFixedAmount(), b.getAmount(), b.getMinimumAmount(), b.getImageUrl());
        }
    }
}
