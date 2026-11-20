package com.velocitymotors.carbooking.payment.creditcard;

public class PaymentValidationUnavailableException extends RuntimeException {
    public PaymentValidationUnavailableException(String message) { super(message); }
    public PaymentValidationUnavailableException(String message, Throwable cause) { super(message, cause); }
}
