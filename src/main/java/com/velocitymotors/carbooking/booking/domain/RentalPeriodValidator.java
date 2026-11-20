
package com.velocitymotors.carbooking.booking.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class RentalPeriodValidator {

    private static final long MAX_RENTAL_DAYS = 21;

    private RentalPeriodValidator() {
    }

    public static void validate(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Rental start and end dates are required");
        }

        if (!endDate.isAfter(startDate)) {
            throw new IllegalArgumentException(
                    "Rental end date must be after the start date");
        }

        long rentalDays = ChronoUnit.DAYS.between(startDate, endDate);

        if (rentalDays > MAX_RENTAL_DAYS) {
            throw new IllegalArgumentException(
                    "Rental duration cannot exceed 21 days");
        }
    }
}
