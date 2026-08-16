package com.payxpress.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

@Component
public class BillerHashService {

    @Value("${xpresspay.private-key}")
    private String privateKey;

    /**
     * Hashes the EXACT bytes about to be sent as the request body. Must run on
     * the final serialized string, not a re-serialization, or the merchant-side
     * hash won't match what XpressPay receives.
     */
    public String generatePaymentHash(String rawJsonBody) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec keySpec = new SecretKeySpec(privateKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            mac.init(keySpec);
            byte[] hashBytes = mac.doFinal(rawJsonBody.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate PaymentHash", e);
        }
    }
}
