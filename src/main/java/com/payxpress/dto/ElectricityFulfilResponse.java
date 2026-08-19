package com.payxpress.dto;

public record ElectricityFulfilResponse(String requestId, String referenceId, String responseCode,
                                         String responseMessage, Data data) {
    public record Data(String amount, String unit, String address, String arrears, String tariffName,
                        String rate, String vat, String accountNumber, String token) {}

    public boolean isSuccessful() {
        return "00".equals(responseCode);
    }
}
