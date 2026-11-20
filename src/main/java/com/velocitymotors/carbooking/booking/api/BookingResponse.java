package com.velocitymotors.carbooking.booking.api;

import com.velocitymotors.carbooking.booking.domain.Booking;
import com.velocitymotors.carbooking.booking.domain.BookingStatus;
import java.math.BigDecimal;

public record BookingResponse(String bookingId, BookingStatus bookingStatus,
                              BigDecimal amountDue, String currency) {
    public static BookingResponse from(Booking booking) {
        return new BookingResponse(booking.getId(), booking.getStatus(),
                booking.getAmountDue(), booking.getCurrency());
    }
}
