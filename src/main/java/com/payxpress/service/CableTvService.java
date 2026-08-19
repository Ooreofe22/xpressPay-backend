package com.payxpress.service;

import com.payxpress.biller.BillerApiClient;
import com.payxpress.dto.CableFulfilResponse;
import com.payxpress.dto.CableValidateResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class CableTvService {

    private final BillerApiClient client;

    public CableTvService(BillerApiClient client) {
        this.client = client;
    }

    public CableValidateResponse validate(String uniqueCode, String accountNumber, int noOfMonth) {
        Map<String, Object> body = Map.of(
                "requestId", UUID.randomUUID().toString(),
                "uniqueCode", uniqueCode,
                "details", Map.of("accountNumber", accountNumber, "noOfMonth", noOfMonth)
        );
        return client.validate("/cable/validate", body, CableValidateResponse.class);
    }

    // name/accountType/amount all come from validate() and must be carried through checkout
    public CableFulfilResponse fulfil(String internalRequestId, String uniqueCode, String accountNumber,
                                      String accountType, int noOfMonth, BigDecimal amount,
                                      String name, String phoneNumber) {
        Map<String, Object> details = new HashMap<>();
        details.put("accountNumber", accountNumber);
        details.put("accountType", accountType);
        details.put("noOfMonth", noOfMonth);
        details.put("amount", amount.intValue());
        details.put("name", name);
        details.put("phoneNumber", phoneNumber);

        Map<String, Object> body = Map.of("requestId", internalRequestId, "uniqueCode", uniqueCode, "details", details);
        return client.fulfil("/cable/fulfil", body, CableFulfilResponse.class);
    }
}
