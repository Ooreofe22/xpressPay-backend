package com.payxpress.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payxpress.model.Biller;
import com.payxpress.model.PaymentTransaction;
import com.payxpress.model.User;
import com.payxpress.service.PaystackService;
import com.payxpress.repository.BillerRepository;
import com.payxpress.repository.PaymentTransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/billers/payments")
public class BillerCheckoutController {

    private final PaymentTransactionRepository transactionRepository;
    private final BillerRepository billerRepository;
    private final PaystackService paystackService;
    private final ObjectMapper objectMapper;

    public BillerCheckoutController(PaymentTransactionRepository transactionRepository, BillerRepository billerRepository,
                                     PaystackService paystackService, ObjectMapper objectMapper) {
        this.transactionRepository = transactionRepository;
        this.billerRepository = billerRepository;
        this.paystackService = paystackService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/initiate")
    @Transactional
    public ResponseEntity<?> initiate(@RequestBody CheckoutRequest request, Authentication auth) {
        User user = (User) auth.getPrincipal();

        Biller biller = billerRepository.findByUniqueCode(request.uniqueCode())
                .orElseThrow(() -> new IllegalArgumentException("Unknown biller: " + request.uniqueCode()));

        String detailsJson;
        try {
            detailsJson = objectMapper.writeValueAsString(request.details());
        } catch (JsonProcessingException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid details payload"));
        }

        PaymentTransaction txn = new PaymentTransaction();
        txn.setUser(user);
        txn.setBiller(biller);
        txn.setInternalRequestId(UUID.randomUUID().toString());
        txn.setBillerUniqueCode(biller.getUniqueCode());
        txn.setBillerCategoryPath(request.categoryPath());
        txn.setAmount(request.amount());
        txn.setBillerDetailsJson(detailsJson);
        txn.setStatus(TransactionStatus.PENDING_PAYMENT);
        transactionRepository.save(txn);

        String email = user.getEmail();
        Map<String, String> metadata = Map.of(
                "biller_unique_code", biller.getUniqueCode(),
                "internal_request_id", txn.getInternalRequestId()
        );

        Map<String, Object> paystackResponse;
        try {
            paystackResponse = paystackService.initializeTransaction(
                    email, request.amount(), txn.getInternalRequestId(), metadata);
        } catch (PaystackUnavailableException e) {
            txn.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(txn);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("message", "Could not initialize payment, unfortunately"));
        }


        if (!Boolean.TRUE.equals(paystackResponse.get("status"))) {
            txn.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(txn);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("message", "Could not initialize payment"));
        }

        Map<String, Object> data = (Map<String, Object>) paystackResponse.get("data");
        txn.setPaystackReference((String) data.get("reference"));
        transactionRepository.save(txn);

        return ResponseEntity.ok(Map.of(
                "authorizationUrl", data.get("authorization_url"),
                "reference", txn.getPaystackReference(),
                "amount", txn.getAmount()
        ));
    }

    public record CheckoutRequest(String uniqueCode, String categoryPath, BigDecimal amount,
                                   Map<String, Object> details) {}
}
