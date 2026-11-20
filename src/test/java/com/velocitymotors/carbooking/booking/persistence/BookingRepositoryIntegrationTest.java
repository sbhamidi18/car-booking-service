package com.velocitymotors.carbooking.booking.persistence;

import com.velocitymotors.carbooking.booking.domain.*;
import java.math.BigDecimal;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class BookingRepositoryIntegrationTest {
    @Autowired BookingRepository repository;

    @Test void persistsBookingWithAmountsAndEnums() {
        String id = "BKG9999999";
        Booking booking = Booking.pendingBankTransfer(id, "Customer", "VEH001", VehicleCategory.SEDAN,
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 4), "REF001",
                new BigDecimal("180.00"), Instant.parse("2026-11-28T23:00:00Z"),
                Instant.parse("2026-11-18T12:00:00Z"));
        repository.saveAndFlush(booking);
        Booking loaded = repository.findById(id).orElseThrow();
        assertEquals(BookingStatus.PENDING_PAYMENT, loaded.getStatus());
        assertEquals(new BigDecimal("180.00"), loaded.getAmountDue());
        assertEquals(new BigDecimal("0.00"), loaded.getAmountReceived());
        assertEquals(PaymentMode.BANK_TRANSFER, loaded.getPaymentMode());
    }
}
