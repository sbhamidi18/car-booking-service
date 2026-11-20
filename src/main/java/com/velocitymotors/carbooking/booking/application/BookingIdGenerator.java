package com.velocitymotors.carbooking.booking.application;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

@Component
public class BookingIdGenerator {
    private final EntityManager entityManager;

    public BookingIdGenerator(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public String nextId() {
        long value = ((Number) entityManager.createNativeQuery(
                "SELECT nextval('booking_number_seq')").getSingleResult()).longValue();
        if (value > 9_999_999L) {
            throw new IllegalStateException("10-character booking ID range exhausted");
        }
        return "BKG" + String.format(java.util.Locale.ROOT, "%07d", value);
    }
}
