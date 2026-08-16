package com.payxpress.enums;

public enum TransactionStatus {
    PENDING_PAYMENT,
    PAYSTACK_CONFIRMED,
    FULFILLING,
    FULFILLED,
    FAILED,
    NEEDS_RECONCILIATION
}
