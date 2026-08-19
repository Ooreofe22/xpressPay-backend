package com.payxpress.dto;

public record ElectricityValidateResponse(String requestId, String referenceId, String responseCode,
                                           String responseMessage, Data data) {
    public record Data(String accountNumber, String accountType, String name, String address, String minimumVending) {}

    public boolean isSuccessful() {
        return "00".equals(responseCode);
    }
}
