CREATE TABLE bank_transfer_payments (
    payment_id VARCHAR(120) PRIMARY KEY,
    booking_id VARCHAR(10) NOT NULL REFERENCES bookings(id),
    sender_account_number VARCHAR(100) NOT NULL,
    transaction_reference VARCHAR(12) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_bank_transfer_payments_booking_id ON bank_transfer_payments(booking_id);
