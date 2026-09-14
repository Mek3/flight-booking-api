# Flight Booking API ✈️

[![CI Pipeline](https://github.com/Mek3/flight-booking-api/actions/workflows/ci.yml/badge.svg)](https://github.com/Mek3/flight-booking-api/actions/workflows/ci.yml)

This is a backend API for an airline booking system.

It does three main things. First, it takes flight schedules that repeat every week and creates flights on specific dates. Second, it creates the seats for each flight. Third, it handles bookings that can have one or more flights, checks that the times are correct, and makes sure two people cannot book the same seat.

I built this project to work on the difficult parts of airline booking. I did not want to build another simple CRUD example.

**Stack:** Java 17 · Spring Boot 3.5 · MySQL 8 · Spring Security (JWT) · Flyway · ShedLock · Testcontainers

📖 **[More documentation in the Wiki](https://github.com/Mek3/flight-booking-api/wiki)**

---

## Domain model

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

**The main rule of the model:** one itinerary has the flights of one trip.

A round trip has *two* itineraries in the same reservation. It does not have four segments in one itinerary. The reason is simple: the days between the outbound flight and the return flight are not a layover. If I put them together, the validation would reject bookings that are correct.

A direct flight is an itinerary with only one segment. It is not a special case. It is the same structure with one element.

---

## Example: a round trip with a connection

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

This request has two itineraries. The outbound trip has a connection. The return is a direct flight.

The order of the segments comes from the position in the list. So the client cannot send an order that is wrong.

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

The segment does not store the times. It reads them from the flight instance. So if a flight changes its time, I do not need to change the booking data.

The return flight lands the next day. The schedule stores this as an arrival day offset, so the API does not have to guess it.

The API also checks the trip before it saves anything. For example, if the second segment departs before the first one lands, or from an airport where the passenger never arrives, the request fails. Nothing is written to the database.

---

## 🏆 Three parts with tests

**Idempotent seat generation**

A `@DataJpaTest` runs the bulk seat insert two times against a real MySQL instance. Then it checks that the number of seats is the same. So the test proves idempotency in the database, not only in the code.
→ `SeatRepositoryJpaTest.java`

**Routing rules without the framework**

The validation of times, airports and layovers works on a plain Java record. It does not work on JPA entities.

Because of this, I can test all the rules without a Spring context and without a database. There are sixteen test cases and they run in milliseconds. Two examples: a layover exactly on the minimum connection time, and a flight that crosses midnight.
→ `RoutingValidatorTest.java`

**A unique constraint that works with soft deletes**

Seat reservations have a `UNIQUE` index on a generated column. This column is `NULL` when the seat is released. MySQL does not see two `NULL` values as a collision.

So when a hold expires, the seat is free for a new reservation, but the old row stays in the table as history.

The obvious solution, a constraint on `(seat, segment)`, does not work. Two bookings on the same flight create two different segment rows. So that constraint would allow selling the same seat two times.
→ `V*__add_seat_reservation.sql`

---

## Design decisions

**Calculate, do not store.** I do not save seat availability, layover times or total travel time in the database. I calculate them from the flight data. Nothing becomes old, because nothing is duplicated. If a flight is delayed, every calculated value is correct.

**The database also protects the data.** Every idempotency and uniqueness rule has a database constraint, not only application code. I use `INSERT IGNORE` with a unique index for the seats, a generated `active_flag` for flight instances, and ShedLock for the scheduled jobs. If somebody skips the application code, or if a lock fails, the data is still correct.

**Business rules are separate from the framework.** The routing validator receives a record and returns a result. It knows nothing about JPA, Spring or repositories. This is why its tests do not need them.

**All errors have the same format.** Security errors happen in the filter chain, before the `DispatcherServlet`. So `@RestControllerAdvice` cannot catch them.

I added a custom entry point for 401 errors and an access denied handler for 403 errors. Both use the same responder and the same `ObjectMapper` as the controllers. The client cannot see from the response if the error comes from a filter or from a controller.

---

## 🗺️ Roadmap

* ✅ **Sprint 5 — Foundations:** static data (`airport`, `route`, `aircraft_model`, `aircraft`, `user`), plus Flyway and JPA setup.
* ✅ **Sprint 6 — Calendars and seats:** idempotent flight instance generator and bulk seat creation.
* 🚧 **Sprint 7 — Routing and booking engine:** the itinerary model, the routing validation and the seat reservation model are done. Still to do: transactional seat locking between segments, and the reservation cart with TTL.
* 🔜 **Sprint 8 — Spring Batch:** import and export of large CSV and XML files in chunks, with a dead letter table for invalid rows.
* 🔜 **Sprint 9 — Event driven architecture:** a `BookingConfirmedEvent` and a separate service that consumes it. Both run with Docker Compose.
* 🔜 **Sprint 10 — Minimal frontend:** three Angular screens that use this API.

### Not included on purpose

I prefer to write these decisions here instead of hiding them. Deciding where to stop is also part of the design.

* **Tickets, coupons and the financial domain** (invoices, refunds, baggage, dynamic pricing). The booking engine is the interesting problem in this project. Billing is a problem that many people have solved before.
* **`Passenger` as a separate entity.** A booking stores the number of passengers, not the name of each one. Seat locking competes for seats, so a number creates the same contention. What is missing is tickets with names.
* **Token revocation, login rate limiting and refresh tokens.** All three need shared storage. That brings back the state that a stateless design tries to avoid. These are decisions, not mistakes.
* **Observability stack and an AI assistant.** Both are useful, but they are not the objective of this project.

---

## ⚙️ How to run it

**You need:** JDK 17, MySQL running on your machine, and Docker for the tests.

```bash
# Secrets are environment variables. Nothing is in the code, nothing is committed.
export DB_LOCAL_USER=root DB_LOCAL_PASSWORD=root JWT_SECRET_LOCAL=<your-secret>

mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Flyway creates the schema and the seed data (users and roles) when the application starts.

```bash
mvn test
```

Testcontainers creates temporary MySQL containers, runs the tests, and deletes them. You do not need any manual setup, and the tests work the same on your machine and in CI.

**API documentation:** Swagger UI at `/swagger-ui.html` when the application is running.

### Authentication

Use `POST /api/v1/auth/register` or `/api/v1/auth/login`. Then send the JWT as a Bearer token.

`ROLE_ADMIN` manages the infrastructure: airports, aircraft, routes and schedules. `ROLE_USER` searches flights and manages their own bookings.