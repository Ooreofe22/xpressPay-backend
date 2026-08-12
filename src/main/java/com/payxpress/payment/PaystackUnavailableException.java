package com.payxpress.payment;

public class PaystackUnavailableException extends RuntimeException {
    public PaystackUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
