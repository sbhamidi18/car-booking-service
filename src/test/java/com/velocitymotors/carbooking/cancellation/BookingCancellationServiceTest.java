package com.velocitymotors.carbooking.cancellation;

import com.velocitymotors.carbooking.booking.domain.Booking;
import com.velocitymotors.carbooking.booking.domain.BookingStatus;
import com.velocitymotors.carbooking.booking.domain.VehicleCategory;
import com.velocitymotors.carbooking.booking.persistence.BookingRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BookingCancellationServiceTest {
    private static final Instant DEADLINE = Instant.parse("2026-11-19T00:00:00Z");
    private final BookingRepository bookings = mock(BookingRepository.class);

    private Booking pendingBooking() {
        return Booking.pendingBankTransfer("BKG0012345", "Customer", "VEH1", VehicleCategory.SEDAN,
                LocalDate.of(2026, 11, 23), LocalDate.of(2026, 11, 25), "TRANSFER-1",
                new BigDecimal("100.00"), DEADLINE, DEADLINE.minusSeconds(3600));
    }

    private BookingCancellationService serviceAt(Instant now) {
        return new BookingCancellationService(bookings, Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void cancelsUnpaidPendingBookingAfterDeadline() {
        var booking = pendingBooking();
        when(bookings.findByIdForUpdate(booking.getId())).thenReturn(Optional.of(booking));
        serviceAt(DEADLINE.plusNanos(1)).cancelIfOverdue(booking.getId());
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
    }

    @Test
    void cancelsAtExactDeadline() {
        var booking = pendingBooking();
        when(bookings.findByIdForUpdate(booking.getId())).thenReturn(Optional.of(booking));
        serviceAt(DEADLINE).cancelIfOverdue(booking.getId());
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
    }

    @Test
    void doesNotCancelBeforeDeadline() {
        var booking = pendingBooking();
        when(bookings.findByIdForUpdate(booking.getId())).thenReturn(Optional.of(booking));
        serviceAt(DEADLINE.minusSeconds(1)).cancelIfOverdue(booking.getId());
        assertEquals(BookingStatus.PENDING_PAYMENT, booking.getStatus());
    }

    @Test
    void cannotCancelAlreadyConfirmedBooking() {
        var booking = pendingBooking();
        booking.receiveBankTransfer(new BigDecimal("100.00"), DEADLINE.minusSeconds(1));
        when(bookings.findByIdForUpdate(booking.getId())).thenReturn(Optional.of(booking));
        serviceAt(DEADLINE.plusSeconds(1)).cancelIfOverdue(booking.getId());
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
    }

    @Test
    void cancellationIsIdempotentAndLatePaymentCannotReopen() {
        var booking = pendingBooking();
        when(bookings.findByIdForUpdate(booking.getId())).thenReturn(Optional.of(booking));
        var service = serviceAt(DEADLINE.plusSeconds(1));
        service.cancelIfOverdue(booking.getId());
        service.cancelIfOverdue(booking.getId());
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        assertThrows(IllegalStateException.class,
                () -> booking.receiveBankTransfer(new BigDecimal("100.00"), DEADLINE.plusSeconds(2)));
    }

    @Test
    void missingBookingIsIgnored() {
        when(bookings.findByIdForUpdate("BKG9999999")).thenReturn(Optional.empty());
        assertDoesNotThrow(() -> serviceAt(DEADLINE.plusSeconds(1)).cancelIfOverdue("BKG9999999"));
    }

    @Test
    void rejectsPaymentAtExactDeadline() {
        var booking = pendingBooking();

        assertThrows(IllegalStateException.class,
                () -> booking.receiveBankTransfer(new BigDecimal("100.00"), DEADLINE));

        assertEquals(BookingStatus.PENDING_PAYMENT, booking.getStatus());
        assertEquals(new BigDecimal("0.00"), booking.getAmountReceived());
    }
}
