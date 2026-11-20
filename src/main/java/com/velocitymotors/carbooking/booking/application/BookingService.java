package com.velocitymotors.carbooking.booking.application;

import com.velocitymotors.carbooking.booking.api.CreateBookingRequest;
import com.velocitymotors.carbooking.booking.domain.*;
import com.velocitymotors.carbooking.booking.persistence.BookingRepository;
import com.velocitymotors.carbooking.vehicle.VehicleValidationService;
import com.velocitymotors.carbooking.payment.creditcard.CreditCardValidationClient;
import java.time.*;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class BookingService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Europe/Amsterdam");
    private final BookingRepository repository;
    private final BookingIdGenerator idGenerator;
    private final RentalPricingService pricing;
    private final VehicleValidationService vehicles;
    private final Clock clock;
    private final CreditCardValidationClient creditCards;

    public BookingService(BookingRepository repository, BookingIdGenerator idGenerator,
            RentalPricingService pricing, VehicleValidationService vehicles, Clock clock,
            CreditCardValidationClient creditCards) {
        this.repository = repository;
        this.idGenerator = idGenerator;
        this.pricing = pricing;
        this.vehicles = vehicles;
        this.clock = clock;
        this.creditCards = creditCards;
    }

    public Booking create(CreateBookingRequest request) {
        RentalPeriodValidator.validate(request.rentalStartDate(), request.rentalEndDate());
        vehicles.validate(request.vehicleId());
        Instant now = clock.instant();
        LocalDate today = LocalDate.ofInstant(now, BUSINESS_ZONE);
        if (request.rentalStartDate().isBefore(today)) {
            throw new IllegalArgumentException("Rental start date cannot be in the past");
        }
        Instant cutoff = request.rentalStartDate().atStartOfDay(BUSINESS_ZONE)
                .toInstant().minus(Duration.ofHours(48));
        if (request.paymentMode() == PaymentMode.BANK_TRANSFER && !now.isBefore(cutoff)) {
            throw new IllegalArgumentException("Bank-transfer payment deadline has already been reached");
        }
        BigDecimal amount = pricing.quote(request.vehicleCategory(),
                request.rentalStartDate(), request.rentalEndDate());
        if (request.paymentMode() == PaymentMode.CREDIT_CARD) {
            creditCards.requireApproved(request.paymentReference().trim());
        }
        String id = idGenerator.nextId();
        Booking booking;
        if (request.paymentMode() == PaymentMode.BANK_TRANSFER) {
            booking = Booking.pendingBankTransfer(id, request.customerName().trim(), request.vehicleId(),
                    request.vehicleCategory(), request.rentalStartDate(), request.rentalEndDate(),
                    request.paymentReference().trim(), amount, cutoff, now);
        } else if (request.paymentMode() == PaymentMode.CREDIT_CARD) {
            booking = Booking.approvedCreditCard(id, request.customerName().trim(), request.vehicleId(),
                    request.vehicleCategory(), request.rentalStartDate(), request.rentalEndDate(),
                    request.paymentReference().trim(), amount, now);
        } else {
            booking = Booking.confirmed(id, request.customerName().trim(), request.vehicleId(),
                    request.vehicleCategory(), request.rentalStartDate(), request.rentalEndDate(),
                    request.paymentMode(), request.paymentReference().trim(), amount, now);
        }
        return repository.save(booking);
    }
}
