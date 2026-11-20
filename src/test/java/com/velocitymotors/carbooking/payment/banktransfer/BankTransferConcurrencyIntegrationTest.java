package com.velocitymotors.carbooking.payment.banktransfer;

import com.velocitymotors.carbooking.booking.domain.Booking;
import com.velocitymotors.carbooking.booking.domain.BookingStatus;
import com.velocitymotors.carbooking.booking.domain.VehicleCategory;
import com.velocitymotors.carbooking.booking.persistence.BookingRepository;
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
class BankTransferConcurrencyIntegrationTest {
    private static final String BOOKING_ID = "BKG9001001";
    private static final String TXN_DETAILS = "TXN987654321 " + BOOKING_ID;

    @Autowired BookingRepository bookings;
    @Autowired BankTransferPaymentRepository payments;
    @Autowired BankTransferPaymentService service;

    @AfterEach
    void cleanUp() {
        payments.deleteById("AUDIT-PAY-A");
        payments.deleteById("AUDIT-PAY-B");
        payments.deleteById("AUDIT-PAY-DUP");
        bookings.deleteById(BOOKING_ID);
    }

    private void createPendingBooking() {
        Instant now = Instant.now();
        bookings.saveAndFlush(Booking.pendingBankTransfer(
                BOOKING_ID, "Audit", "VEH001", VehicleCategory.SEDAN,
                LocalDate.now().plusDays(5), LocalDate.now().plusDays(7), "AUDIT-REF",
                new BigDecimal("100.00"), now.plusSeconds(3600), now));
    }

    private BankTransferEvent event(String id, String amount) {
        return new BankTransferEvent(id, "NL91ABNA0417164300",
                new BigDecimal(amount), TXN_DETAILS);
    }

    private void simultaneouslyProcess(BankTransferEvent a, BankTransferEvent b) throws Exception {
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            Future<?> first = pool.submit(() -> {
                ready.countDown();
                await(start);
                service.process(a);
            });
            Future<?> second = pool.submit(() -> {
                ready.countDown();
                await(start);
                service.process(b);
            });
            assertTrue(ready.await(10, TimeUnit.SECONDS), "Workers did not become ready");
            start.countDown();
            first.get(15, TimeUnit.SECONDS);
            second.get(15, TimeUnit.SECONDS);
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

    @Test
    void concurrentDistinctPaymentsAreBothCountedAndConfirmBooking() throws Exception {
        createPendingBooking();
        simultaneouslyProcess(event("AUDIT-PAY-A", "60.00"), event("AUDIT-PAY-B", "40.00"));

        var booking = bookings.findById(BOOKING_ID).orElseThrow();
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        assertEquals(0, new BigDecimal("100.00").compareTo(booking.getAmountReceived()));
        assertTrue(payments.existsById("AUDIT-PAY-A"));
        assertTrue(payments.existsById("AUDIT-PAY-B"));
    }

    @Test
    void concurrentDuplicatePaymentIsCountedOnlyOnce() throws Exception {
        createPendingBooking();
        simultaneouslyProcess(event("AUDIT-PAY-DUP", "60.00"), event("AUDIT-PAY-DUP", "60.00"));

        var booking = bookings.findById(BOOKING_ID).orElseThrow();
        assertEquals(BookingStatus.PENDING_PAYMENT, booking.getStatus());
        assertEquals(0, new BigDecimal("60.00").compareTo(booking.getAmountReceived()));
        assertTrue(payments.existsById("AUDIT-PAY-DUP"));
    }
}
