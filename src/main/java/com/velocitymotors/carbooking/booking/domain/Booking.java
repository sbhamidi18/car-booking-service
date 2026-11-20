package com.velocitymotors.carbooking.booking.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "bookings")
public class Booking {
    @Id
    @Column(name = "id", length = 10, nullable = false)
    private String id;

    @Column(name = "customer_name", length = 150, nullable = false)
    private String customerName;

    @Column(name = "vehicle_id", length = 50, nullable = false)
    private String vehicleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_category", length = 20, nullable = false)
    private VehicleCategory vehicleCategory;

    @Column(name = "rental_start_date", nullable = false)
    private LocalDate rentalStartDate;

    @Column(name = "rental_end_date", nullable = false)
    private LocalDate rentalEndDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", length = 30, nullable = false)
    private PaymentMode paymentMode;

    @Column(name = "payment_reference", length = 100, nullable = false)
    private String paymentReference;

    @Column(name = "amount_due", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountDue;

    @Column(name = "amount_received", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountReceived;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private BookingStatus status;

    @Column(name = "payment_deadline")
    private Instant paymentDeadline;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Booking() {
    }

    private Booking(String id, String customerName, String vehicleId, VehicleCategory vehicleCategory,
            LocalDate rentalStartDate, LocalDate rentalEndDate, PaymentMode paymentMode,
            String paymentReference, BigDecimal amountDue, BookingStatus status,
            Instant paymentDeadline, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.customerName = Objects.requireNonNull(customerName);
        this.vehicleId = Objects.requireNonNull(vehicleId);
        this.vehicleCategory = Objects.requireNonNull(vehicleCategory);
        this.rentalStartDate = Objects.requireNonNull(rentalStartDate);
        this.rentalEndDate = Objects.requireNonNull(rentalEndDate);
        this.paymentMode = Objects.requireNonNull(paymentMode);
        this.paymentReference = Objects.requireNonNull(paymentReference);
        this.amountDue = Objects.requireNonNull(amountDue).setScale(2);
        this.amountReceived = new BigDecimal("0.00");
        this.currency = "EUR";
        this.status = Objects.requireNonNull(status);
        this.paymentDeadline = paymentDeadline;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = createdAt;
        RentalPeriodValidator.validate(rentalStartDate, rentalEndDate);
        if (amountDue.signum() <= 0)
            throw new IllegalArgumentException("Amount due must be positive");
        if (paymentMode == PaymentMode.BANK_TRANSFER &&
                (status != BookingStatus.PENDING_PAYMENT || paymentDeadline == null)) {
            throw new IllegalArgumentException("Bank transfer requires pending status and deadline");
        }
        if (paymentMode != PaymentMode.BANK_TRANSFER &&
                (status != BookingStatus.CONFIRMED || paymentDeadline != null)) {
            throw new IllegalArgumentException("Immediate booking must be confirmed without deadline");
        }
    }

    public static Booking pendingBankTransfer(String id, String customerName, String vehicleId,
            VehicleCategory category, LocalDate startDate, LocalDate endDate, String paymentReference,
            BigDecimal amountDue, Instant deadline, Instant createdAt) {
        return new Booking(id, customerName, vehicleId, category, startDate, endDate,
                PaymentMode.BANK_TRANSFER, paymentReference, amountDue,
                BookingStatus.PENDING_PAYMENT, Objects.requireNonNull(deadline), createdAt);
    }

    public static Booking confirmed(String id, String customerName, String vehicleId,
            VehicleCategory category, LocalDate startDate, LocalDate endDate, PaymentMode paymentMode,
            String paymentReference, BigDecimal amountDue, Instant createdAt) {
        if (paymentMode == PaymentMode.BANK_TRANSFER || paymentMode == PaymentMode.CREDIT_CARD) {
            throw new IllegalArgumentException("This factory does not establish external payment approval");
        }
        return new Booking(id, customerName, vehicleId, category, startDate, endDate,
                paymentMode, paymentReference, amountDue, BookingStatus.CONFIRMED, null, createdAt);
    }

    /**
     * Called only after the application service verifies APPROVED with the payment
     * provider.
     */
    public static Booking approvedCreditCard(String id, String customerName, String vehicleId,
            VehicleCategory category, LocalDate startDate, LocalDate endDate,
            String paymentReference, BigDecimal amountDue, Instant createdAt) {
        return new Booking(id, customerName, vehicleId, category, startDate, endDate,
                PaymentMode.CREDIT_CARD, paymentReference, amountDue,
                BookingStatus.CONFIRMED, null, createdAt);
    }

    public String getId() {
        return id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public VehicleCategory getVehicleCategory() {
        return vehicleCategory;
    }

    public LocalDate getRentalStartDate() {
        return rentalStartDate;
    }

    public LocalDate getRentalEndDate() {
        return rentalEndDate;
    }

    public PaymentMode getPaymentMode() {
        return paymentMode;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public BigDecimal getAmountDue() {
        return amountDue;
    }

    public BigDecimal getAmountReceived() {
        return amountReceived;
    }

    public String getCurrency() {
        return currency;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public Instant getPaymentDeadline() {
        return paymentDeadline;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void receiveBankTransfer(BigDecimal amount, Instant processedAt) {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(processedAt, "processedAt");
        if (paymentMode != PaymentMode.BANK_TRANSFER || status != BookingStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("Booking is not awaiting bank-transfer payment");
        }
        if (!processedAt.isBefore(paymentDeadline)) {
            throw new IllegalStateException("Payment deadline has passed");
        }
        if (amount.signum() <= 0 || amount.scale() > 2) {
            throw new IllegalArgumentException("Payment must be positive with at most 2 decimal places");
        }
        BigDecimal newTotal = amountReceived.add(amount);
        if (newTotal.precision() - newTotal.scale() > 10) {
            throw new IllegalArgumentException("Received amount exceeds supported monetary range");
        }
        amountReceived = newTotal;
        if (amountReceived.compareTo(amountDue) >= 0)
            status = BookingStatus.CONFIRMED;
        updatedAt = processedAt;
    }

    public void cancelIfOverdue(Instant now) {
        Objects.requireNonNull(now);
        if (status == BookingStatus.PENDING_PAYMENT && !now.isBefore(paymentDeadline)) {
            status = BookingStatus.CANCELLED;
            updatedAt = now;
        }
    }
}
