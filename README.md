# Car Booking Service

Spring Boot implementation of the Velocity Motors car-rental booking assignment.

## Overview

The service accepts rental booking requests and manages confirmation based on payment mode:

- **Digital wallet:** confirms immediately (assumed already paid).
- **Cash:** confirms immediately as a documented compatibility assumption because the assignment's payment-mode list conflicts with its digital-wallet rule.
- **Credit card:** queries the supplied credit-card payment-status API; creates a confirmed booking only for `APPROVED`.
- **Bank transfer:** creates a `PENDING_PAYMENT` booking, consumes Kafka payment events, accumulates valid payments and confirms on full payment before the deadline.

A scheduled job cancels unpaid bank-transfer bookings when the payment deadline has been reached. The database protects payment processing using transactions, row locks and unique payment identifiers.

## Stack

Java 21, Spring Boot 4.1.2, Maven, PostgreSQL 17, Flyway, Spring Data JPA, Spring Kafka, Spring `RestClient`, JUnit 5.

## Run locally

Prerequisites: JDK 21, Docker Desktop, Docker Compose, and either Maven or the included Maven Wrapper.

```bash
docker compose up -d postgres kafka
./mvnw clean verify
./mvnw spring-boot:run
```

Application: `http://localhost:8080`.

The credit-card status service is **external** and must be available at the configured URL for credit-card booking requests. By default the client uses `http://localhost:9090/host/credit-card-payment-api`; no real service is bundled. Tests use a simulated HTTP server. Configuration overrides: `CREDIT_CARD_BASE_URL`, `DB_USERNAME`, `DB_PASSWORD`, `KAFKA_BOOTSTRAP_SERVERS`. The local `application.properties` contains development-only defaults; never reuse these credentials in production.

Spring Boot is run on the host machine; the example Kafka broker advertises `localhost:9092` for host-side clients. This Compose setup is intended for single-machine development, not deployment.

## Create a booking

`POST /api/bookings` with JSON:

```json
{
  "customerName": "Example Customer",
  "vehicleId": "VEH001",
  "rentalStartDate": "2026-11-28",
  "rentalEndDate": "2026-11-30",
  "vehicleCategory": "SEDAN",
  "paymentMode": "BANK_TRANSFER",
  "paymentReference": "REF001"
}
```

The success response uses `201 Created` and returns `bookingId`, `bookingStatus`, `amountDue` and `currency`. A bank-transfer booking begins `PENDING_PAYMENT`. The exact booking ID is generated as `BKG` plus seven digits.

Accepted vehicle IDs (mock): `VEH001`, `VEH002`, `VEH003`, `VEH004`. Allowed categories: `COMPACT`, `SEDAN`, `SUV`, `LUXURY`. Supported payment modes: `DIGITAL_WALLET`, `CASH`, `CREDIT_CARD`, `BANK_TRANSFER`.

## Kafka event contract

Topic: `bank-transfer-payment-events`. JSON event:

```json
{
  "paymentId": "PAY000001",
  "senderAccountNumber": "NL91ABNA0417164300",
  "paymentAmount": 100.00,
  "transactionDetails": "TXN987654321 BKG0000001"
}
```

`transactionDetails` contains a 12-character alphanumeric transaction reference, one space and a ten-character booking ID. Use the real booking ID returned by the REST call. Unique payment IDs prevent duplicate amounts being counted twice. An error handler is configured to route failed payment events to `bank-transfer-payment-events-dlt` after its retry policy. Dead-letter delivery was manually verified using a malformed Kafka message.

## Credit-card status API

The supplied OpenAPI contract specifies `POST /payment-status` relative to the configured base URL, a JSON `paymentReference` request and `status` (`APPROVED`/`REJECTED`) response. The original specification contains formatting mistakes in the URL, enum and date-time declaration. The client follows the intended operation and JSON field names. The endpoint checks status; it does **not** charge a card.

## Assumptions and limitations

- Date range duration is `endDate - startDate` days (return date exclusive); maximum 21 days.
- EUR mock daily prices: Compact 40, Sedan 60, SUV 90, Luxury 150. Amount due is snapshotted on booking creation; the assignment did not specify price or currency.
- Rental starts at midnight in `Europe/Amsterdam`; bank-transfer payment deadline is 48 elapsed hours before that instant. Processing time, not bank settlement time, determines payment eligibility because Kafka events have no settlement timestamp.
- Bank-transfer payments must be processed strictly before the deadline. Payments arriving exactly at or after the deadline are rejected. New bank-transfer bookings at or after the deadline are also rejected. The scheduler cancels eligible unpaid bookings at or after the cutoff, although actual execution may occur slightly later.
- Vehicle validation is a fixed mock catalog; overlapping reservations, stock allocation, refunds and payment reconciliation are not part of the stated assignment.
- Cash confirmation is an explicit assumption due to the PDF's contradictory wallet/cash modes; no cash settlement is verified.
- Kafka/Compose setup is single-broker and development-focused. There is no distributed production deployment configuration or production-grade secrets management.
- Rejected/late/unknown payment events require operational reconciliation; a dead-letter topic is not a refund mechanism.

## Testing

Run the configured suite with:

```bash
./mvnw clean verify
```

Integration tests need a reachable local PostgreSQL server. The project includes unit tests, HTTP-client contract tests, persistence tests and additional database-backed concurrency tests. Live manual checks included bank-transfer booking creation, partial and full payment aggregation, duplicate delivery, automatic cancellation, late-payment rejection, and credit-card approval (201) and rejection (422) through a temporary local Python mock. The latest local verification completed successfully: **44 tests, 0 failures, 0 errors, 0 skipped**. This includes four database-backed concurrency integration tests. This is the observed local result, not a claim of exhaustive behavior coverage.

## Design notes

See `docs/engineering-decisions.md` for implementation decisions, assumptions, transaction boundaries, trade-offs and verification evidence.