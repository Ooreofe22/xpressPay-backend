package com.payxpress.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payxpress.service.PaymentConfirmationService;
import com.payxpress.service.PaystackService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks")
public class PaystackWebhookController {

    private final PaystackService paystackService;
    private final PaymentConfirmationService confirmationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PaystackWebhookController(PaystackService paystackService, PaymentConfirmationService confirmationService) {
        this.paystackService = paystackService;
        this.confirmationService = confirmationService;
    }

    // CAUTION: if signature checks keep failing once this is live, the raw body
    // may be getting re-parsed/re-serialized by Spring's message converters before
    // this method sees it. May need a raw-body-capturing filter (ContentCachingRequestWrapper)
    // instead of @RequestBody String if that turns out to be the case.
    @PostMapping("/paystack")
    public ResponseEntity<Void> handleWebhook(@RequestBody String rawBody,
                                               @RequestHeader("x-paystack-signature") String signature) {
        if (!paystackService.verifyWebhookSignature(rawBody, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            JsonNode payload = objectMapper.readTree(rawBody);
            if ("charge.success".equals(payload.path("event").asText())) {
                confirmationService.confirmPayment(payload.path("data").path("reference").asText());
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok().build();
    }
}
