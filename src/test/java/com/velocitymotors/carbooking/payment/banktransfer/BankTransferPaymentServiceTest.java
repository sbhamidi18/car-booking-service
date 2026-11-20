package com.velocitymotors.carbooking.payment.banktransfer;

import com.velocitymotors.carbooking.booking.domain.*;
import com.velocitymotors.carbooking.booking.persistence.BookingRepository;
import java.math.BigDecimal;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BankTransferPaymentServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-18T12:00:00Z");
    private final BookingRepository bookings = mock(BookingRepository.class);
    private final BankTransferPaymentRepository payments = mock(BankTransferPaymentRepository.class);
    private final BankTransferPaymentService service = new BankTransferPaymentService(
            bookings, payments, Clock.fixed(NOW, ZoneOffset.UTC));
    private Booking booking;

    @BeforeEach void setUp() {
        booking = Booking.pendingBankTransfer("BKG0012345", "Customer", "VEH1", VehicleCategory.SEDAN,
                LocalDate.of(2026, 11, 23), LocalDate.of(2026, 11, 25), "BT-1",
                new BigDecimal("100.00"), NOW.plusSeconds(3600), NOW.minusSeconds(3600));
        when(bookings.findByIdForUpdate("BKG0012345")).thenReturn(Optional.of(booking));
    }
    private BankTransferEvent event(String id, String amount) {
        return new BankTransferEvent(id, "NL01TEST", new BigDecimal(amount), "TXN987654321 BKG0012345");
    }
    @Test void aggregatesDistinctPartialPaymentsAndConfirmsOnFullAmount() {
        service.process(event("a", "40.00"));
        assertEquals(BookingStatus.PENDING_PAYMENT, booking.getStatus());
        service.process(event("b", "60.00"));
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        assertEquals(0, booking.getAmountReceived().compareTo(new BigDecimal("100.00")));
        verify(payments, times(2)).saveAndFlush(any(BankTransferPayment.class));
    }
    @Test void duplicatePaymentIsIgnored() {
        when(payments.existsById("a")).thenReturn(true);
        service.process(event("a", "40.00"));
        assertEquals(0, booking.getAmountReceived().signum());
        verify(bookings, never()).findByIdForUpdate(any());
    }
    @Test void rejectsPaymentAfterDeadline() {
        var expired = Booking.pendingBankTransfer("BKG0012345", "Customer", "VEH1", VehicleCategory.SEDAN,
                LocalDate.of(2026, 11, 23), LocalDate.of(2026, 11, 25), "BT-1",
                new BigDecimal("100.00"), NOW.minusSeconds(1), NOW.minusSeconds(3600));
        when(bookings.findByIdForUpdate("BKG0012345")).thenReturn(Optional.of(expired));
        assertThrows(IllegalStateException.class, () -> service.process(event("a", "100.00")));
        verify(payments, never()).saveAndFlush(any());
    }
    @Test void unknownBookingIsReported() {
        when(bookings.findByIdForUpdate("BKG0012345")).thenReturn(Optional.empty());
        assertThrows(UnknownBookingException.class, () -> service.process(event("a", "100.00")));
    }
}
