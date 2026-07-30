package com.payxpress.biller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class BillerApiClient {

    private final RestClient restClient;
    private final BillerHashService hashService;
    private final ObjectMapper objectMapper;

    @Value("${xpresspay.public-key}")
    private String publicKey;

    public BillerApiClient(@Value("${xpresspay.base-url}") String baseUrl,
                            BillerHashService hashService,
                            ObjectMapper objectMapper) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.hashService = hashService;
        this.objectMapper = objectMapper;
    }

    /** Read-only calls: /products, /categories, /billers */
    public <T> T get(String path, Object requestBody, Class<T> responseType) {
        return restClient.post()
                .uri(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + publicKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(responseType);
    }

    /** Validate/lookup calls — no wallet debit, but PaymentHash sent anyway (harmless; some categories require it) */
    public <T> T validate(String path, Object requestBody, Class<T> responseType) {
        return postWithHash(path, requestBody, responseType);
    }

    /** /fulfil calls — always debits the XpressPay merchant wallet */
    public <T> T fulfil(String path, Object requestBody, Class<T> responseType) {
        return postWithHash(path, requestBody, responseType);
    }

    private <T> T postWithHash(String path, Object requestBody, Class<T> responseType) {
        try {
            String rawJsonBody = objectMapper.writeValueAsString(requestBody);
            String paymentHash = hashService.generatePaymentHash(rawJsonBody);

            return restClient.post()
                    .uri(path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + publicKey)
                    .header("PaymentHash", paymentHash)
                    .header("channel", "api")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(rawJsonBody)
                    .retrieve()
                    .body(responseType);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize request for " + path, e);
        }
    }
}
