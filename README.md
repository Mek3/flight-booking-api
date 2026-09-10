# Flight Booking API ✈️

[![CI Pipeline](https://github.com/Mek3/flight-booking-api/actions/workflows/ci.yml/badge.svg)](https://github.com/Mek3/flight-booking-api/actions/workflows/ci.yml)

The backend engine of an airline booking system: recurring flight schedules projected into dated instances, physical seats materialised per aircraft, and bookings built from multi-segment itineraries with temporal validation and concurrency-safe seat inventory.

Built to explore the problems that make airline booking genuinely hard — not to be another CRUD demo.

**Stack:** Java 17 · Spring Boot 3.5 · MySQL 8 · Redis · Spring Security (JWT) · Flyway · ShedLock · Testcontainers

📖 **[Detailed feature documentation in the Wiki](https://github.com/Mek3/flight-booking-api/wiki)**

---

## The domain model

```
User ──books──> Reservation
                    │  code, status, passengers, total price
                    │
                    └── 1..N Itinerary            sequenceOrder: outbound = 1, return = 2
                             │
                             └── 1..N FlightSegment        segmentOrder: 1, 2, 3...
                                      │
                                      └── FlightInstance   a flight on a concrete date
                                               │
                                               ├── FlightSchedule ──> Airport ×2, AircraftLayout
                                               └── Aircraft

Seat ── belongs to one FlightInstance
  └── 0..1 active SeatReservation ──> FlightSegment
```

**The rule that shapes everything:** an itinerary groups segments that continue the same
journey. A round trip is *two* itineraries under one reservation — not four segments in
one — because the days between an outbound flight and its return are not a layover, and
treating them as one would reject perfectly valid bookings.

A direct flight is an itinerary with a single segment. Not a special case: the degenerate
case of the same structure.

---

## Booking a round trip with a connection

```http
POST /api/v1/reservations
Authorization: Bearer <token>
```

```json
{
  "numberOfPassengers": 2,
  "itineraries": [
    { "flightInstanceIds": [101, 205] },
    { "flightInstanceIds": [412] }
  ]
}
```

Two itineraries: the outbound connects through a second flight, the return is direct.
Segment order comes from list position, so a client cannot submit a contradictory
sequence.

```json
{
  "id": 88,
  "reservationCode": "3EA31A83",
  "status": "PENDING",
  "numberOfPassengers": 2,
  "totalPrice": 1500.00,
  "itineraries": [
    {
      "sequenceOrder": 1,
      "segments": [
        { "segmentOrder": 1, "flightNumber": "IBE-101",
          "departureAirport": "ALC", "departureAt": "2026-10-15T08:00:00",
          "arrivalAirport": "MAD",   "arrivalAt": "2026-10-15T09:00:00" },
        { "segmentOrder": 2, "flightNumber": "IBE-205",
          "departureAirport": "MAD", "departureAt": "2026-10-15T10:30:00",
          "arrivalAirport": "JFK",   "arrivalAt": "2026-10-15T18:30:00" }
      ]
    },
    {
      "sequenceOrder": 2,
      "segments": [
        { "segmentOrder": 1, "flightNumber": "IBE-412",
          "departureAirport": "JFK", "departureAt": "2026-10-22T21:00:00",
          "arrivalAirport": "ALC",   "arrivalAt": "2026-10-23T11:00:00" }
      ]
    }
  ]
}
```

Times are resolved through the flight instance, never stored on the segment — a
rescheduled flight is reflected without touching booking data. The return flight lands the
following day, which the schedule's arrival day offset makes explicit rather than leaving
to inference.

Submit an itinerary whose second segment departs before the first lands, or from an
airport the passenger never reaches, and the request fails before a single row is written.

---

## 🏆 Three things built to be verified, not read

**Genuinely idempotent seat generation**
A `@DataJpaTest` runs the bulk seat-materialization insert twice against a real MySQL
instance and asserts the seat count doesn't change. Idempotency proven at the database
level rather than assumed from reading the code.
→ `SeatRepositoryJpaTest.java`

**Routing rules with no framework attached**
Temporal coherence, airport continuity and layover validation operate on a plain record,
not on JPA entities. The entire rule set is unit-testable with no Spring context and no
database: sixteen cases in milliseconds, including a layover sitting exactly on the
minimum connection time and an overnight flight crossing midnight.
→ `RoutingValidatorTest.java`

**A uniqueness constraint that survives soft deletes**
Seat reservations are guarded by a `UNIQUE` index over a generated column that evaluates
to `NULL` for released holds — and MySQL does not treat `NULL`s as colliding. An expired
hold frees its seat for a new reservation while the original row survives as an audit
trail. The intuitive constraint on `(seat, segment)` would have been wrong: two bookings
on the same flight produce two different segment rows, so it would have permitted selling
the same seat twice.
→ `V*__add_seat_reservation.sql`

---

## Design decisions worth arguing about

**Derived over stored.** Seat availability, layover times and total travel time are
computed from the operational record, never persisted. Nothing goes stale because nothing
is duplicated — a delayed flight propagates to every derived value for free.

**The database is the last line of defence.** Every idempotency and uniqueness guarantee
is enforced by a constraint, not only by application logic: `INSERT IGNORE` plus a unique
index for seat generation, a generated `active_flag` for flight instances, ShedLock for
scheduled jobs. If the application logic were bypassed or a distributed lock failed, the
data would still be correct.

**Business rules kept away from the framework.** Routing validation receives a record and
returns a verdict. It knows nothing about JPA, Spring or repositories, which is why its
tests need none of them.

**Errors have one contract, wherever they originate.** Security failures happen in the
filter chain, before the `DispatcherServlet`, so `@RestControllerAdvice` cannot see them.
A custom entry point (401) and access-denied handler (403) intercept there and serialise
through the same responder and the same configured `ObjectMapper` — so a client cannot
tell from the payload shape whether an error came from a filter or a controller.

---

## 🗺️ Roadmap

* ✅ **Sprint 5 — Foundations:** static topology (`airport`, `route`, `aircraft_model`, `aircraft`, `user`), Flyway and JPA groundwork.
* ✅ **Sprint 6 — Calendars & Physical Inventory:** idempotent flight-instance generator and bulk seat materialization.
* 🚧 **Sprint 7 — Routing & Booking Engine:** itinerary model, routing validation and the seat reservation model are merged. Remaining: transactional seat locking across segments, and the TTL reservation cart.
* 🔜 **Sprint 8 — Spring Batch:** chunked ingestion and export of large CSV/XML datasets, with a dead-letter table for corrupt rows.
* 🔜 **Sprint 9 — Event-Driven Architecture:** `BookingConfirmedEvent` consumed by a separately deployed service, orchestrated with Docker Compose.
* 🔜 **Sprint 10 — Minimal Frontend:** three Angular screens consuming this API.

### Consciously out of scope

Documented rather than hidden — deciding where to stop is part of the design.

* **Ticket and coupon model, and the financial domain** (invoices, refunds, baggage, dynamic pricing). The booking engine is the interesting problem; billing is well-trodden ground.
* **`Passenger` as a distinct entity.** Bookings carry a passenger count, not per-traveller identity. Seat locking contends over seats, so a count produces the same contention; what is deferred is nominative ticket issuance.
* **Token revocation, login rate limiting and refresh tokens.** All three require shared storage and therefore reintroduce the state the stateless design was chosen to avoid. Deliberate trade-offs, not omissions.
* **Observability stack and an AI assistant.** Good additions; not what this project is demonstrating.

---

## ⚙️ Running it

**Requirements:** JDK 17, MySQL and Redis running locally, Docker for the tests.

```bash
# Secrets via environment variables — nothing hardcoded, nothing committed
export DB_LOCAL_USER=root DB_LOCAL_PASSWORD=root JWT_SECRET_LOCAL=<your-secret>

mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Schema and seed data (users, roles) are applied on startup by Flyway.

```bash
mvn test
```

Testcontainers provisions ephemeral MySQL and Redis containers, runs the suite and tears
them down. No manual setup, and identical behaviour locally and in CI.

**API documentation:** Swagger UI at `/swagger-ui.html` once running.

### Authentication

`POST /api/v1/auth/register` or `/api/v1/auth/login`, then send the returned JWT as a
Bearer token. `ROLE_ADMIN` manages infrastructure (airports, aircraft, routes, schedules);
`ROLE_USER` searches flights and manages their own bookings.