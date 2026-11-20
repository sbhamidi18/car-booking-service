package com.velocitymotors.carbooking.cancellation;

import com.velocitymotors.carbooking.booking.persistence.BookingRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BookingCancellationSchedulerTest {
    @Test void scansAndCancelsOverdueBookingsInBatches() {
        var repository = mock(BookingRepository.class);
        var service = mock(BookingCancellationService.class);
        var clock = Clock.fixed(Instant.parse("2026-11-20T00:00:00Z"), ZoneOffset.UTC);
        when(repository.findOverduePendingBookingIds(any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of("BKG0000001", "BKG0000002"))
                .thenReturn(List.of());

        new BookingCancellationScheduler(repository, service, clock).cancelOverdueBookings();

        verify(service).cancelIfOverdue("BKG0000001");
        verify(service).cancelIfOverdue("BKG0000002");
        verify(repository, times(2)).findOverduePendingBookingIds(eq(clock.instant()), any(Pageable.class));
    }

    @Test void emptyScanDoesNotAttemptCancellation() {
        var repository = mock(BookingRepository.class);
        var service = mock(BookingCancellationService.class);
        when(repository.findOverduePendingBookingIds(any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of());
        new BookingCancellationScheduler(repository, service, Clock.systemUTC()).cancelOverdueBookings();
        verifyNoInteractions(service);
    }
}
