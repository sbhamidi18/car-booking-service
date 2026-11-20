package com.velocitymotors.carbooking.booking.persistence;

import com.velocitymotors.carbooking.booking.domain.Booking;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") String id);

    @Query("select b.id from Booking b where b.status = com.velocitymotors.carbooking.booking.domain.BookingStatus.PENDING_PAYMENT "
            + "and b.paymentDeadline <= :now order by b.paymentDeadline, b.id")
    List<String> findOverduePendingBookingIds(@Param("now") Instant now, Pageable pageable);
}
