package com.velocitymotors.carbooking.payment.banktransfer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "bank_transfer_payments")
public class BankTransferPayment {
    @Id
    @Column(name = "payment_id", nullable = false, length = 120)
    private String paymentId;
    @Column(name = "booking_id", nullable = false, length = 10)
    private String bookingId;
    @Column(name = "sender_account_number", nullable = false, length = 100)
    private String senderAccountNumber;
    @Column(name = "transaction_reference", nullable = false, length = 12)
    private String transactionReference;
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;
    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected BankTransferPayment() { }

    public BankTransferPayment(String paymentId, String bookingId, String senderAccountNumber,
            String transactionReference, BigDecimal amount, Instant processedAt) {
        this.paymentId = paymentId;
        this.bookingId = bookingId;
        this.senderAccountNumber = senderAccountNumber;
        this.transactionReference = transactionReference;
        this.amount = amount;
        this.processedAt = processedAt;
    }
    public String getPaymentId() { return paymentId; }
    public String getBookingId() { return bookingId; }
    public BigDecimal getAmount() { return amount; }
}
