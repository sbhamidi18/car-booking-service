package com.velocitymotors.carbooking.booking.application;

import com.velocitymotors.carbooking.booking.domain.VehicleCategory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;

@Service
public class RentalPricingService {
    public BigDecimal quote(VehicleCategory category, LocalDate start, LocalDate end) {
        long days = ChronoUnit.DAYS.between(start, end);
        BigDecimal dailyRate = switch (category) {
            case COMPACT -> new BigDecimal("40.00");
            case SEDAN -> new BigDecimal("60.00");
            case SUV -> new BigDecimal("90.00");
            case LUXURY -> new BigDecimal("150.00");
        };
        return dailyRate.multiply(BigDecimal.valueOf(days));
    }
}
