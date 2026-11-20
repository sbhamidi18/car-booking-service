package com.velocitymotors.carbooking.payment.banktransfer;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BankTransferEventTest {
    @Test void parsesTransactionReferenceAndBookingId() {
        var event = new BankTransferEvent("pay-1", "NL01TEST", new BigDecimal("25.00"),
                "TXN987654321 BKG0012345");
        var parsed = event.validate();
        assertEquals("TXN987654321", parsed.transactionReference());
        assertEquals("BKG0012345", parsed.bookingId());
    }
    @Test void rejectsMalformedReference() {
        assertThrows(IllegalArgumentException.class, () ->
                new BankTransferEvent("p1", "NL01TEST", BigDecimal.ONE, "BKG0012345").validate());
    }
    @Test void rejectsNegativeAndExcessivelyPreciseAmounts() {
        assertThrows(IllegalArgumentException.class, () ->
                new BankTransferEvent("p1", "NL01TEST", new BigDecimal("-1.00"), "TXN987654321 BKG0012345").validate());
        assertThrows(IllegalArgumentException.class, () ->
                new BankTransferEvent("p1", "NL01TEST", new BigDecimal("1.001"), "TXN987654321 BKG0012345").validate());
    }
}
