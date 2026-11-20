package com.velocitymotors.carbooking.payment.banktransfer;

import com.velocitymotors.carbooking.booking.domain.Booking;
import com.velocitymotors.carbooking.booking.persistence.BookingRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BankTransferPaymentService {
    private final BookingRepository bookings;
    private final BankTransferPaymentRepository payments;
    private final Clock clock;

    public BankTransferPaymentService(BookingRepository bookings,
            BankTransferPaymentRepository payments, Clock clock) {
        this.bookings = bookings;
        this.payments = payments;
        this.clock = clock;
    }

    @Transactional
    public void process(BankTransferEvent event) {
        BankTransferEvent.ParsedDetails details = event.validate();
        // Duplicate check must also be repeated after acquiring the booking lock.
        if (payments.existsById(event.paymentId())) return;
        Booking booking = bookings.findByIdForUpdate(details.bookingId())
                .orElseThrow(() -> new UnknownBookingException(details.bookingId()));
        if (payments.existsById(event.paymentId())) return;
        Instant processedAt = clock.instant();
        booking.receiveBankTransfer(event.paymentAmount(), processedAt);
        payments.saveAndFlush(new BankTransferPayment(event.paymentId(), booking.getId(),
                event.senderAccountNumber(), details.transactionReference(),
                event.paymentAmount(), processedAt));
        // Managed booking changes are committed atomically with the payment insert.
    }
}
