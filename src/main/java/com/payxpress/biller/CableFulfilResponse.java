package com.payxpress.biller;

public record CableFulfilResponse(String requestId, String referenceId, String responseCode,
                                   String responseMessage, Object data) {
    public boolean isSuccessful() {
        return "00".equals(responseCode);
    }
}
