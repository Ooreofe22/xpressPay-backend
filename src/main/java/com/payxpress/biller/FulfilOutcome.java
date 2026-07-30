package com.payxpress.biller;

public record FulfilOutcome(boolean successful, String referenceId, String responseCode, String responseMessage) {

    public static FulfilOutcome from(ElectricityFulfilResponse r) {
        return new FulfilOutcome(r.isSuccessful(), r.referenceId(), r.responseCode(), r.responseMessage());
    }

    public static FulfilOutcome from(CableFulfilResponse r) {
        return new FulfilOutcome(r.isSuccessful(), r.referenceId(), r.responseCode(), r.responseMessage());
    }

    public static FulfilOutcome from(FulfilResponse r) {
        return new FulfilOutcome(r.isSuccessful(), r.referenceId(), r.responseCode(), r.responseMessage());
    }
}
