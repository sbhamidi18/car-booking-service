package com.velocitymotors.carbooking.payment.banktransfer;

public class UnknownBookingException extends RuntimeException {
    public UnknownBookingException(String bookingId) {
        super("Booking does not exist: " + bookingId);
    }
}
