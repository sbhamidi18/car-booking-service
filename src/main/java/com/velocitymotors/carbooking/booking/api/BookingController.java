package com.velocitymotors.carbooking.booking.api;

import com.velocitymotors.carbooking.booking.application.BookingService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    private final BookingService service;

    public BookingController(BookingService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody CreateBookingRequest request) {
        BookingResponse response = BookingResponse.from(service.create(request));
        return ResponseEntity.created(URI.create("/api/bookings/" + response.bookingId())).body(response);
    }
}
