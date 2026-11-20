package com.velocitymotors.carbooking.booking.application;

import com.velocitymotors.carbooking.booking.domain.VehicleCategory;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RentalPricingServiceTest {
    private final RentalPricingService service = new RentalPricingService();
    @Test void pricesThreeDaySedan() {
        LocalDate start = LocalDate.of(2026, 12, 10);
        assertEquals(new BigDecimal("180.00"), service.quote(VehicleCategory.SEDAN, start, start.plusDays(3)));
    }
    @Test void pricesMaximumLuxuryRental() {
        LocalDate start = LocalDate.of(2026, 12, 10);
        assertEquals(new BigDecimal("3150.00"), service.quote(VehicleCategory.LUXURY, start, start.plusDays(21)));
    }
}
