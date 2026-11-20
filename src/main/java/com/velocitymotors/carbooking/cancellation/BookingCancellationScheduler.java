package com.velocitymotors.carbooking.cancellation;

import com.velocitymotors.carbooking.booking.persistence.BookingRepository;
import java.time.Clock;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BookingCancellationScheduler {
    private static final int BATCH_SIZE = 100;
    private final BookingRepository bookings;
    private final BookingCancellationService cancellation;
    private final Clock clock;

    public BookingCancellationScheduler(BookingRepository bookings,
            BookingCancellationService cancellation, Clock clock) {
        this.bookings = bookings;
        this.cancellation = cancellation;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${booking.cancellation.fixed-delay-ms:60000}")
    public void cancelOverdueBookings() {
        // Always fetch the first page: processed rows leave the pending set.
        while (true) {
            var ids = bookings.findOverduePendingBookingIds(clock.instant(), PageRequest.of(0, BATCH_SIZE));
            if (ids.isEmpty()) return;
            for (String id : ids) {
                cancellation.cancelIfOverdue(id);
            }
            // Each scan makes progress in ordinary operation. An unexpected concurrent
            // state change can remove a row as well, which is also progress.
        }
    }
}
