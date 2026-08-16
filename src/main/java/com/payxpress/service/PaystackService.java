package com.payxpress.service;

import com.payxpress.exception.PaystackUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;

/**
 * NOTE: this is a clean reconstruction matching the shape of the real
 * PaystackService already in the PayXpress repo (initializeTransaction /
 * verifyTransaction), with verifyWebhookSignature() added since the original
 * never actually checked the incoming signature. Reconcile field/method names
 * with the real class when merging — don't just drop this in blind.
 */
@Service
public class PaystackService {

    private final WebClient webClient;

    @Value("${paystack.secret.key}")
    private String secretKey;

    @Value("${paystack.callback.url}")
    private String callbackUrl;

    public PaystackService(WebClient paystackWebClient) {
        this.webClient = paystackWebClient;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> initializeTransaction(String email, BigDecimal amount, String reference,
                                                       Map<String, String> metadata) {
        Map<String, Object> body = Map.of(
                "email", email,
                "amount", amount.multiply(BigDecimal.valueOf(100)).intValue(), // kobo
                "reference", reference,
                "callback_url", callbackUrl,
                "metadata", metadata
        );

           try {
        return webClient.post()
                .uri("/transaction/initialize")
                .header("Authorization", "Bearer " + secretKey)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    } catch (Exception e) {
               throw new PaystackUnavailableException("Paystack call failed", e);
           }
           }

    @SuppressWarnings("unchecked")
    public Map<String, Object> verifyTransaction(String reference) {
        return webClient.get()
                .uri("/transaction/verify/{reference}", reference)
                .header("Authorization", "Bearer " + secretKey)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    public boolean verifyWebhookSignature(String rawBody, String signatureHeader) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            String computed = HexFormat.of().formatHex(mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8)));
            return computed.equals(signatureHeader);
        } catch (Exception e) {
            return false;
        }
    }
}
