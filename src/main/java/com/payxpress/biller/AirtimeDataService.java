package com.payxpress.biller;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class AirtimeDataService {

    private final BillerApiClient client;

    public AirtimeDataService(BillerApiClient client) {
        this.client = client;
    }

    public FulfilResponse fulfil(String categoryPath, String internalRequestId, String uniqueCode,
                                  String phoneNumber, BigDecimal amount) {
        Map<String, Object> body = Map.of(
                "requestId", internalRequestId,
                "uniqueCode", uniqueCode,
                "details", Map.of("phoneNumber", phoneNumber, "amount", amount.intValue())
        );
        return client.fulfil("/" + categoryPath + "/fulfil", body, FulfilResponse.class);
    }
}
