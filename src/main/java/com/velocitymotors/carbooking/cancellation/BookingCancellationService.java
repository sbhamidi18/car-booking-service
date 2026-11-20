package com.velocitymotors.carbooking.cancellation;

import com.velocitymotors.carbooking.booking.persistence.BookingRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingCancellationService {
    private final BookingRepository bookings;
    private final Clock clock;

    public BookingCancellationService(BookingRepository bookings, Clock clock) {
        this.bookings = bookings;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancelIfOverdue(String bookingId) {
        bookings.findByIdForUpdate(bookingId).ifPresent(booking -> {
            Instant now = clock.instant();
            booking.cancelIfOverdue(now);
        });
    }
}
