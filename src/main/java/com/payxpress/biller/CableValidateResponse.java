package com.payxpress.biller;

public record CableValidateResponse(String requestId, String referenceId, String responseCode,
                                     String responseMessage, Data data) {
    public record Data(String accountNumber, String accountType, String name, String amount, String balance, String md) {}

    public boolean isSuccessful() {
        return "00".equals(responseCode);
    }
}
