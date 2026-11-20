
package com.velocitymotors.carbooking.booking.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RentalPeriodValidatorTest {

    private static final LocalDate START_DATE = LocalDate.of(2026, 11, 21);

    @Test
    void acceptsValidRentalPeriod() {
        assertDoesNotThrow(() ->
                RentalPeriodValidator.validate(START_DATE, START_DATE.plusDays(5)));
    }

    @Test
    void acceptsMaximumRentalDuration() {
        assertDoesNotThrow(() ->
                RentalPeriodValidator.validate(START_DATE, START_DATE.plusDays(21)));
    }

    @Test
    void rejectsRentalDurationExceeding21Days() {
        assertThrows(IllegalArgumentException.class, () ->
                RentalPeriodValidator.validate(START_DATE, START_DATE.plusDays(22)));
    }

    @Test
    void rejectsEqualStartAndEndDates() {
        assertThrows(IllegalArgumentException.class, () ->
                RentalPeriodValidator.validate(START_DATE, START_DATE));
    }

    @Test
    void rejectsEndDateBeforeStartDate() {
        assertThrows(IllegalArgumentException.class, () ->
                RentalPeriodValidator.validate(START_DATE, START_DATE.minusDays(1)));
    }

    @Test
    void rejectsMissingStartDate() {
        assertThrows(IllegalArgumentException.class, () ->
                RentalPeriodValidator.validate(null, START_DATE.plusDays(1)));
    }

    @Test
    void rejectsMissingEndDate() {
        assertThrows(IllegalArgumentException.class, () ->
                RentalPeriodValidator.validate(START_DATE, null));
    }
}
