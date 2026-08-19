package com.payxpress.service;

import com.payxpress.biller.BillerApiClient;
import com.payxpress.dto.ElectricityFulfilResponse;
import com.payxpress.dto.ElectricityValidateResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class ElectricityService {

    private final BillerApiClient client;

    public ElectricityService(BillerApiClient client) {
        this.client = client;
    }

    public ElectricityValidateResponse validateMeter(String uniqueCode, String accountNumber, String accountType) {
        Map<String, Object> body = Map.of(
                "requestId", UUID.randomUUID().toString(),
                "uniqueCode", uniqueCode,
                "details", Map.of("accountNumber", accountNumber, "accountType", accountType)
        );
        return client.validate("/electricity/validate", body, ElectricityValidateResponse.class);
    }

    // name/address come from validateMeter() and must be carried through checkout
    public ElectricityFulfilResponse fulfil(String internalRequestId, String uniqueCode,
                                            String accountNumber, String accountType, BigDecimal amount,
                                            String name, String address) {
        Map<String, Object> details = new HashMap<>();
        details.put("accountNumber", accountNumber);
        details.put("accountType", accountType);
        details.put("amount", amount.intValue());
        details.put("name", name);
        details.put("address", address);
        details.put("phoneNumber", "");

        Map<String, Object> body = Map.of("requestId", internalRequestId, "uniqueCode", uniqueCode, "details", details);
        return client.fulfil("/electricity/fulfil", body, ElectricityFulfilResponse.class);
    }
}
