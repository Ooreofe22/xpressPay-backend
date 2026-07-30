package com.payxpress.biller;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class EducationService {

    private final BillerApiClient client;

    public EducationService(BillerApiClient client) {
        this.client = client;
    }

    public FulfilResponse buyWaecPin(String internalRequestId, String uniqueCode, String email, String phoneNumber) {
        Map<String, Object> body = Map.of(
                "requestId", internalRequestId,
                "uniqueCode", uniqueCode,
                "details", Map.of("email", email, "phoneNumber", phoneNumber)
        );
        return client.fulfil("/education/waec/pin/fulfil", body, FulfilResponse.class);
    }
}
