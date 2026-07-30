package com.payxpress.model;

public enum TransactionStatus {
    PENDING_PAYMENT,
    PAYSTACK_CONFIRMED,
    FULFILLING,
    FULFILLED,
    FAILED,
    NEEDS_RECONCILIATION
}
