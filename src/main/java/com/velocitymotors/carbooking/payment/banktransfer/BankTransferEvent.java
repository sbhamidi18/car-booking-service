package com.velocitymotors.carbooking.payment.banktransfer;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record BankTransferEvent(String paymentId, String senderAccountNumber,
                                BigDecimal paymentAmount, String transactionDetails) {
    private static final Pattern DETAILS = Pattern.compile("^([A-Za-z0-9]{12}) (BKG[0-9]{7})$");

    public ParsedDetails validate() {
        if (paymentId == null || paymentId.isBlank() || paymentId.length() > 120
                || senderAccountNumber == null || senderAccountNumber.isBlank()
                || senderAccountNumber.length() > 100
                || paymentAmount == null || paymentAmount.signum() <= 0
                || paymentAmount.scale() > 2) {
            throw new IllegalArgumentException("Invalid bank-transfer payment event");
        }
        Matcher matcher = DETAILS.matcher(transactionDetails == null ? "" : transactionDetails);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("transactionDetails must be '<12-char TxnRef> <10-char BookingId>'");
        }
        return new ParsedDetails(matcher.group(1), matcher.group(2));
    }
    public record ParsedDetails(String transactionReference, String bookingId) { }
}
