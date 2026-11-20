package com.velocitymotors.carbooking.cancellation;

import com.velocitymotors.carbooking.booking.domain.Booking;
import com.velocitymotors.carbooking.booking.domain.BookingStatus;
import com.velocitymotors.carbooking.booking.domain.VehicleCategory;
import com.velocitymotors.carbooking.booking.persistence.BookingRepository;
import com.velocitymotors.carbooking.payment.banktransfer.BankTransferEvent;
import com.velocitymotors.carbooking.payment.banktransfer.BankTransferPaymentRepository;
import com.velocitymotors.carbooking.payment.banktransfer.BankTransferPaymentService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PaymentCancellationRaceIntegrationTest {
    private static final String EXPIRED_BOOKING = "BKG9002001";
    private static final String TIMELY_BOOKING = "BKG9002002";
    private static final String EXPIRED_PAYMENT = "RACE-PAY-EXPIRED";
    private static final String TIMELY_PAYMENT = "RACE-PAY-TIMELY";

    @Autowired BookingRepository bookings;
    @Autowired BankTransferPaymentRepository payments;
    @Autowired BankTransferPaymentService paymentService;
    @Autowired BookingCancellationService cancellationService;

    @AfterEach
    void cleanUp() {
        payments.deleteById(EXPIRED_PAYMENT);
        payments.deleteById(TIMELY_PAYMENT);
        bookings.deleteById(EXPIRED_BOOKING);
        bookings.deleteById(TIMELY_BOOKING);
    }

    @Test
    void overdueBookingCannotBeConfirmedWhenPaymentAndCancellationRace() throws Exception {
        createBooking(EXPIRED_BOOKING, Instant.now().minusSeconds(3600));
        Result result = race(EXPIRED_BOOKING, EXPIRED_PAYMENT);

        assertEquals(BookingStatus.CANCELLED, bookings.findById(EXPIRED_BOOKING).orElseThrow().getStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(
                bookings.findById(EXPIRED_BOOKING).orElseThrow().getAmountReceived()));
        assertFalse(payments.existsById(EXPIRED_PAYMENT));
        assertNull(result.cancellationFailure());
        // The payment service may reject the overdue event regardless of which worker locks first.
        assertTrue(result.paymentFailure() == null
                        || result.paymentFailure() instanceof IllegalStateException,
                "Unexpected payment failure: " + result.paymentFailure());
    }

    @Test
    void timelyFullPaymentConfirmsWhenCancellationCheckRunsConcurrently() throws Exception {
        createBooking(TIMELY_BOOKING, Instant.now().plusSeconds(3600));
        Result result = race(TIMELY_BOOKING, TIMELY_PAYMENT);

        assertNull(result.paymentFailure());
        assertNull(result.cancellationFailure());
        Booking stored = bookings.findById(TIMELY_BOOKING).orElseThrow();
        assertEquals(BookingStatus.CONFIRMED, stored.getStatus());
        assertEquals(0, new BigDecimal("100.00").compareTo(stored.getAmountReceived()));
        assertTrue(payments.existsById(TIMELY_PAYMENT));
    }

    private void createBooking(String id, Instant deadline) {
        Instant now = Instant.now();
        bookings.saveAndFlush(Booking.pendingBankTransfer(
                id, "Race test", "VEH001", VehicleCategory.SEDAN,
                LocalDate.now().plusDays(5), LocalDate.now().plusDays(7), "RACE-REF",
                new BigDecimal("100.00"), deadline, now));
    }

    private Result race(String bookingId, String paymentId) throws Exception {
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            Future<Throwable> payment = pool.submit(() -> {
                ready.countDown();
                await(start);
                try {
                    paymentService.process(new BankTransferEvent(paymentId,
                            "NL91ABNA0417164300", new BigDecimal("100.00"),
                            "TXN987654321 " + bookingId));
                    return null;
                } catch (RuntimeException ex) {
                    return ex;
                }
            });
            Future<Throwable> cancellation = pool.submit(() -> {
                ready.countDown();
                await(start);
                try {
                    cancellationService.cancelIfOverdue(bookingId);
                    return null;
                } catch (RuntimeException ex) {
                    return ex;
                }
            });
            assertTrue(ready.await(10, TimeUnit.SECONDS), "Workers failed to become ready");
            start.countDown();
            return new Result(payment.get(15, TimeUnit.SECONDS), cancellation.get(15, TimeUnit.SECONDS));
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }

    private record Result(Throwable paymentFailure, Throwable cancellationFailure) { }
}
