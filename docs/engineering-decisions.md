# Engineering Decisions

This document describes the implementation decisions, assumptions, trade-offs, and verification evidence for the car booking service.

## 1. Assignment requirements and implementation traceability

| Requirement | Implementation | Evidence / status |
|---|---|---|
| POST booking confirmation with required fields | `BookingController`, `CreateBookingRequest`, `BookingService` | Manual bank-transfer POST returned 201; request validation tests |
| Valid categories / vehicle ID | Enums and `VehicleValidationService` mock | Unit tests; invalid vehicle returned 400 manually |
| End date after start, max 21 days | `RentalPeriodValidator`; DB check constraint | Seven rental validation tests passed |
| Wallet confirms immediately | `BookingService`, `Booking.confirmed` | Booking service unit tests |
| Cash listed in request options | `PaymentMode.CASH` and immediate-confirmation assumption | Documented immediate-confirmation assumption |
| Credit card queries supplied API and approves only `APPROVED` | `CreditCardValidationClient`, `BookingService`, dedicated `Booking.approvedCreditCard` | HTTP client tests passed; manual credit-card approval (201) and rejection (422) verified |
| Bank transfer initially pending | `Booking.pendingBankTransfer`, `BookingService` | 201/PENDING_PAYMENT manually observed |
| Kafka topic & event shape | `BankTransferEventConsumer`, `BankTransferEvent` | Live Kafka partial/full payment tested |
| Full payment confirmation | `BankTransferPaymentService`, `Booking.receiveBankTransfer` | €100 + €80 produced CONFIRMED / €180 |
| Duplicate payment ID | `BankTransferPaymentRepository` plus PostgreSQL payment ID primary key | Duplicate live event no extra credit; concurrent duplicate test passed |
| Cancel at missed 48-hour cutoff | `BookingCancellationScheduler`, `BookingCancellationService`, `Booking.cancelIfOverdue` | Local overdue booking auto-cancelled |
| Late payment cannot reopen cancelled booking | Domain transition method and transactional payment service | Live cancelled booking remained CANCELLED, €0 received |

## 2. Core decisions and trade-offs

### A. Layered organization, not elaborate distributed architecture
- **Decision:** Keep controller/application/domain/repository modules, with Kafka and HTTP adapters.
- **Rationale:** One service with a narrow workflow; useful unit boundaries without framework ceremony.
- **Alternative:** Extensive hexagonal interfaces, CQRS/event sourcing or multiple deployable services.
- **Trade-off:** Some classes depend on Spring/JPA; acceptable for this take-home.
- **Complexity:** Payment lookup by booking primary key and payment by unique ID; no distributed coordination service.

### B. PostgreSQL + Flyway
- **Decision:** Use a durable relational DB, JPA and SQL migrations V1–V3. Preserve applied migration history.
- **Rationale:** Booking and payment records need atomic updates and uniqueness constraints.
- **Alternative:** In-memory records or automatic schema generation.
- **Trade-off:** Local PostgreSQL required for application-context and integration tests.
- **Concurrency:** Row-level pessimistic lock on booking before payment total changes; DB primary key on payment ID.
- **Tests**: Repository tests and four database-backed concurrency integration tests passed. The complete suite passed all 44 tests.

### C. Pricing / money
- **Decision:** Mock daily EUR category rates 40/60/90/150; compute `daysBetween(start,end) * rate`, store `BigDecimal` amount due.
- **Evidence:** The PDF demands full bank-transfer payment but does not define rental price.
- **Alternative:** Add price to client request (untrusted), or external pricing service (not supplied).
- **Trade-off:** Mock rates are not market quotes and have no rate-versioning.
- **Edge cases:** 21-day maximum, no zero/negative total; precision 12, scale 2 in database.

### D. Modes: DIGITAL_WALLET vs CASH
- **Decision:** Accept both, treat cash as immediately confirmed for assignment compatibility.
- **Evidence:** Narrative requires wallet; request field list names cash instead.
- **Alternative:** Reject cash or treat cash as an alias; either conflicts with one part of PDF.
- **Limitation:** Cash payment is assumed settled, not independently confirmed.

### E. Deadlines and dates
- **Decision:** Use `LocalDate`, local midnight in `Europe/Amsterdam`, then subtract 48 elapsed hours; persist `Instant`.
- **Rationale:** No times or timezone given in request. Absolute instants make comparisons reliable across DST changes.
- **Boundary:** Bank-transfer payments must be processed strictly before the cutoff. Payments at or after the cutoff are rejected. Pending bookings become eligible for cancellation at the cutoff.
- **Limitation:** Kafka lacks trusted settlement time, so service processing time determines eligibility.
- **Tests:** Exact-deadline payment rejection, cancellation boundary conditions, and overdue cancellation were verified.

### F. Booking IDs
- **Decision:** `BKG` + 7-digit database sequence ID.
- **Evidence:** Event format requires booking ID length 10.
- **Alternative:** UUID (not compatible with documented event format).
- **Trade-off:** Finite ID space; explicit exhaustion check.

### G. Kafka payment idempotency and locking
- **Decision:** Unique payment ID; pessimistically lock booking; re-check duplicate after locking; record payment and update amount in one transaction.
- **Rationale:** Kafka may redeliver; concurrent payments must not overwrite each other.
- **Alternative:** Java `synchronized` (unsafe across service processes), distributed locks (unnecessary), optimistic locking (more retry complexity).
- **Edge cases:** Duplicate, partial, out-of-order, invalid/unknown booking, cancelled booking, invalid amount.
- **Tests performed:** Live partial/full, duplicate and late-payment flows; two real-DB payment concurrency tests passed.
- **Verification:** Duplicate payment handling and malformed-event delivery to bank-transfer-payment-events-dlt were verified.

### H. Cancellation
- **Decision:** Periodically scan overdue pending IDs, then lock and re-check each booking in a short transaction.
- **Rationale:** Scheduler timing is approximate; domain operation enforces state rules.
- **Trade-off:** 60-second default cadence means physical cancellation may follow cutoff; consumer independently refuses overdue payments.
- **Tests performed:** Live overdue record cancelled; two real-DB payment/cancellation tests passed.

### I. Credit-card HTTP integration
- **Decision:** `RestClient` uses configured base URL, POST `/payment-status`, `paymentReference`; only `APPROVED` permits creation.
- **Evidence:** Provided OpenAPI YAML; schema includes APPROVED/REJECTED and HTTP 400/404/500.
- **Workaround:** Treat malformed server URL, enum and date-time declaration as spec errors while retaining endpoint/body semantics.
- **Failure mapping:** Rejected/not-found = unsuccessful payment; timeout/provider errors = unavailable validation; no confirmed booking.
- **Tests:** HTTP client tests passed. Manual integration tests confirmed successful booking creation for approved payments (201) and rejection of declined payments (422).

## 3. State machine

- New + wallet/cash assumption -> `CONFIRMED`.
- New + approved credit card -> `CONFIRMED`; rejected/unavailable validation -> no confirmed booking created.
- New + bank transfer -> `PENDING_PAYMENT`.
- `PENDING_PAYMENT` + full valid payment before deadline -> `CONFIRMED`.
- `PENDING_PAYMENT` + cutoff elapsed without full payment -> `CANCELLED`.
- `CONFIRMED` and `CANCELLED` are terminal for this assignment.
- Extra funds after confirmation, late funds after cancellation and refunds are not automatically reconciled.
