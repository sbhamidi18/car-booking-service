
CREATE SEQUENCE booking_number_seq
    START WITH 1
    INCREMENT BY 1;

CREATE TABLE bookings (
    id VARCHAR(10) PRIMARY KEY,

    customer_name VARCHAR(150) NOT NULL,
    vehicle_id VARCHAR(50) NOT NULL,
    vehicle_category VARCHAR(20) NOT NULL,

    rental_start_date DATE NOT NULL,
    rental_end_date DATE NOT NULL,

    payment_mode VARCHAR(30) NOT NULL,
    payment_reference VARCHAR(100) NOT NULL,

    amount_due NUMERIC(12, 2) NOT NULL,
    amount_received NUMERIC(12, 2) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'EUR',

    status VARCHAR(30) NOT NULL,
    payment_deadline TIMESTAMP WITH TIME ZONE,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT chk_rental_dates
        CHECK (
            rental_end_date > rental_start_date
            AND rental_end_date <= rental_start_date + 21
        ),

    CONSTRAINT chk_amounts
        CHECK (
            amount_due > 0
            AND amount_received >= 0
        ),

    CONSTRAINT chk_booking_status
        CHECK (
            status IN (
                'PENDING_PAYMENT',
                'CONFIRMED',
                'CANCELLED'
            )
        ),

    CONSTRAINT chk_payment_mode
        CHECK (
            payment_mode IN (
                'DIGITAL_WALLET',
                'CREDIT_CARD',
                'BANK_TRANSFER',
                'CASH'
            )
        )
);
