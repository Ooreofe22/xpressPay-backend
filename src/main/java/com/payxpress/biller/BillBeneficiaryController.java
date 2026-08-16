package com.payxpress.biller;

import com.payxpress.model.BillBeneficiary;
import com.payxpress.model.User;
import com.payxpress.repository.BillBeneficiaryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/billers/beneficiaries")
public class BillBeneficiaryController {

    private final BillBeneficiaryRepository beneficiaryRepository;

    public BillBeneficiaryController(BillBeneficiaryRepository beneficiaryRepository) {
        this.beneficiaryRepository = beneficiaryRepository;
    }

    @GetMapping
    public List<BeneficiaryView> list(Authentication auth) {
        User user = (User) auth.getPrincipal();
        return beneficiaryRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(BeneficiaryView::from).toList();
    }

    @PostMapping
    public ResponseEntity<BeneficiaryView> create(@RequestBody CreateRequest request, Authentication auth) {
        User user = (User) auth.getPrincipal();

        BillBeneficiary b = new BillBeneficiary();
        b.setUser(user);
        b.setBillerUniqueCode(request.billerUniqueCode());
        b.setBillerCategoryPath(request.billerCategoryPath());
        b.setLabel(request.label());
        b.setAccountNumber(request.accountNumber());
        b.setPhoneNumber(request.phoneNumber());
        b.setAccountType(request.accountType());
        beneficiaryRepository.save(b);

        return ResponseEntity.ok(BeneficiaryView.from(b));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id, Authentication auth) {
        User user = (User) auth.getPrincipal();
        return beneficiaryRepository.findByIdAndUser(id, user)
                .map(b -> {
                    beneficiaryRepository.delete(b);
                    return ResponseEntity.ok(Map.of("deleted", true));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    public record CreateRequest(String billerUniqueCode, String billerCategoryPath, String label,
                                String accountNumber, String phoneNumber, String accountType) {}

    public record BeneficiaryView(String id, String billerUniqueCode, String billerCategoryPath, String label,
                                  String accountNumber, String phoneNumber, String accountType, Instant createdAt) {
        static BeneficiaryView from(BillBeneficiary b) {
            return new BeneficiaryView(b.getId(), b.getBillerUniqueCode(), b.getBillerCategoryPath(), b.getLabel(),
                    b.getAccountNumber(), b.getPhoneNumber(), b.getAccountType(), b.getCreatedAt());
        }
    }
}