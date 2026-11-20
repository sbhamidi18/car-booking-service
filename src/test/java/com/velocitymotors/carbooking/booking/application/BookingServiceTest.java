package com.velocitymotors.carbooking.booking.application;

import com.velocitymotors.carbooking.booking.api.CreateBookingRequest;
import com.velocitymotors.carbooking.booking.domain.*;
import com.velocitymotors.carbooking.booking.persistence.BookingRepository;
import com.velocitymotors.carbooking.vehicle.VehicleValidationService;
import com.velocitymotors.carbooking.payment.creditcard.CreditCardValidationClient;
import com.velocitymotors.carbooking.payment.creditcard.PaymentRejectedException;
import java.time.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BookingServiceTest {
    private final BookingRepository repository = mock(BookingRepository.class);
    private final BookingIdGenerator idGenerator = mock(BookingIdGenerator.class);
    private final VehicleValidationService vehicles = new VehicleValidationService();
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-18T12:00:00Z"), ZoneOffset.UTC);
    private BookingService service;
    private final CreditCardValidationClient creditCards = mock(CreditCardValidationClient.class);

    @BeforeEach void setup() {
        service = new BookingService(repository, idGenerator, new RentalPricingService(), vehicles, clock, creditCards);
        when(repository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(idGenerator.nextId()).thenReturn("BKG0000001");
    }

    private CreateBookingRequest request(PaymentMode mode, LocalDate start) {
        return new CreateBookingRequest("Customer", "VEH001", start, start.plusDays(3),
                VehicleCategory.SEDAN, mode, "REF001");
    }

    @Test void bankTransferStartsPendingWithExactDeadline() {
        Booking booking = service.create(request(PaymentMode.BANK_TRANSFER, LocalDate.of(2026, 11, 25)));
        assertEquals(BookingStatus.PENDING_PAYMENT, booking.getStatus());
        assertEquals(Instant.parse("2026-11-22T23:00:00Z"), booking.getPaymentDeadline());
        assertEquals("BKG0000001", booking.getId());
    }

    @Test void digitalWalletConfirmsImmediately() {
        Booking booking = service.create(request(PaymentMode.DIGITAL_WALLET, LocalDate.of(2026, 11, 25)));
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        assertNull(booking.getPaymentDeadline());
    }

    @Test void approvedCreditCardConfirmsAfterValidation() {
        Booking booking = service.create(request(PaymentMode.CREDIT_CARD, LocalDate.of(2026, 11, 25)));
        verify(creditCards).requireApproved("REF001");
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        verify(repository).save(any(Booking.class));
    }

    @Test void rejectedCreditCardNeverPersistsBooking() {
        doThrow(new PaymentRejectedException("Rejected")).when(creditCards).requireApproved("REF001");
        assertThrows(PaymentRejectedException.class,
                () -> service.create(request(PaymentMode.CREDIT_CARD, LocalDate.of(2026, 11, 25))));
        verify(repository, never()).save(any());
        verify(idGenerator, never()).nextId();
    }

    @Test void bankTransferTooCloseToStartIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.create(request(PaymentMode.BANK_TRANSFER, LocalDate.of(2026, 11, 20))));
        verify(repository, never()).save(any());
    }

    @Test void invalidVehicleIsRejected() {
        var request = new CreateBookingRequest("Customer", "BAD", LocalDate.of(2026, 11, 25),
                LocalDate.of(2026, 11, 27), VehicleCategory.SEDAN, PaymentMode.DIGITAL_WALLET, "REF");
        assertThrows(IllegalArgumentException.class, () -> service.create(request));
        verify(repository, never()).save(any());
    }
}
