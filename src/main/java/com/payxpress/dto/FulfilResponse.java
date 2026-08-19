package com.payxpress.dto;

public record FulfilResponse(String requestId, String referenceId, String responseCode,
                              String responseMessage, Object data) {
    public boolean isSuccessful() {
        return "00".equals(responseCode);
    }
}
