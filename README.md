# Flight Booking API ✈️

[![CI Pipeline](https://github.com/Mek3/flight-booking-api/actions/workflows/ci.yml/badge.svg)](https://github.com/Mek3/flight-booking-api/actions/workflows/ci.yml)

A RESTful API for managing flight reservations, built with Spring Boot. This project serves as the core backend engine for an airline booking system — from scheduling flights and materializing physical seat inventory, to handling reservations, concurrency-safe purchases, and role-based access control.

## 🛠️ Tech Stack
* **Framework:** Spring Boot 3.x
* **Language:** Java (using modern features like `Records`)
* **Database:** MySQL & Spring Data JPA
* **Cache:** Redis
* **Security:** Spring Security with JWT (JSON Web Tokens)
* **Distributed Scheduling:** ShedLock (multi-node safe `@Scheduled` jobs)
* **Testing:** JUnit 5, Mockito, Testcontainers, and MockMvc

## 🏆 Highlighted Proof Points

Two pieces of this project are built to be verified, not just read:

* **Zero-overbooking under real concurrency** — a multi-threaded stress test using `ExecutorService` + `CountDownLatch` mathematically proves that simultaneous purchase attempts on the same flight never oversell a seat, validating JPA Optimistic Locking (`@Version`) under actual load rather than by inspection.
  See `src/test/java/.../FlightConcurrencyIntegrationTest.java`
* **Genuinely idempotent seat generation** — a `@DataJpaTest` integration test runs the bulk seat-materialization insert twice against a real MySQL instance (Testcontainers) for the same flight and asserts the seat count doesn't change, proving idempotency at the database level rather than assuming the code is correct.
  See `src/test/java/.../SeatRepositoryJpaTest.java`

## 🚀 Current Status & Features

### 🔐 Security & Access Control
* ✅ **Advanced Security & Auth:** Stateless JWT authentication and BCrypt password encoding.
* ✅ **RBAC (Role-Based Access Control):** Method-level security (`@PreAuthorize`) enforcing strict boundaries between `ROLE_USER` and `ROLE_ADMIN`.
* ✅ **Vulnerability Mitigation:** Protection against IDOR (Insecure Direct Object Reference) by extracting user identity directly from the Security Context rather than relying on client payloads.
* ✅ **Defense in Depth Error Handling:** Unified security exception responses across both the Servlet Filter chain and MVC Controller boundaries. Custom Spring Security entry points intercept 401/403 errors outside the `DispatcherServlet`, manually serializing them to guarantee a consistent API contract.

### 🗄️ Data & Persistence
* ✅ **Core Architecture:** Domain models and relational database mapping configured.
* ✅ **Data Integrity:** Implementation of JPA Optimistic Locking (`@Version`) to prevent concurrent modification conflicts during flight updates and bookings.
* ✅ **Database Migrations:** Schema and data versioning strictly managed via Flyway.
* ✅ **Dynamic Search Engine:** Migrated rigid repository methods to Spring Data JPA Specifications (Criteria API), enabling flexible, multi-parameter flight filtering (dates, price ranges, destinations) seamlessly integrated with pagination.
* ✅ **Auditing:** Automatic JPA Auditing configured for tracking `created_at`, `updated_at`, `created_by`, and `deleted_by` fields.

### ⚙️ Background Processing & Scheduling
* ✅ **Idempotent Flight Instance Generation:** A `@Scheduled` job translates recurring `flight_schedule` blueprints into concrete `flight_instance` records for a rolling 30-day window, safe to run across multiple server nodes simultaneously. Backed by ShedLock (`@SchedulerLock`) and a database-level `UNIQUE` constraint on a generated `active_flag` column, guaranteeing zero duplicate instances even if the distributed lock itself were to fail.
* ✅ **Bulk Seat Materialization:** For every flight instance, physical seats are generated from the aircraft's layout via a custom repository using `JdbcTemplate.batchUpdate()` — bypassing JPA `saveAll()` entirely to insert hundreds of rows per flight efficiently. Idempotent by design (`INSERT IGNORE` + unique constraint), and decoupled from the flight-generation job's own schedule: it simply asks "which instances have no seats yet," making it self-healing if a prior run fails or is delayed.
* ✅ **Background Workers:** Implemented an asynchronous `@Scheduled` cron job to automatically expire abandoned reservations and release locked flight seats. Employs advanced Spring AOP proxy management and isolated transaction boundaries (`Propagation.REQUIRES_NEW`) to prevent database deadlocks during mass processing.

### 🚀 Performance & Scalability
* ✅ **Performance & Scalability:** Implemented pagination across Flight and Reservation data layers to efficiently handle large datasets and prevent memory exhaustion.
* ✅ **High-Performance Caching:** Integrated Redis to serve read-heavy flight search queries directly from RAM. Engineered a robust cache invalidation strategy (`@CacheEvict`) tied to reservation events (create, cancel, confirm, expire) to guarantee zero stale data regarding seat availability.
* ✅ **Secure Polymorphic Deserialization:** Hardened the Jackson `ObjectMapper` with a strict `BasicPolymorphicTypeValidator` to safely serialize objects to Redis without exposing the application to Remote Code Execution (RCE) vulnerabilities. Engineered a custom `RestPageImpl` wrapper to natively deserialize Spring Data's `PageImpl` interfaces from JSON.

### 🧰 API & Developer Experience
* ✅ **RESTful Conventions:** Strict adherence to industry-standard pluralized routing and proper HTTP status codes (e.g., `204 No Content` for deletions).
* ✅ **Global Exception Handling:** Centralized error management using `@RestControllerAdvice`. Standardizes all API errors (400, 403, 404, 409, 500) into a clean, immutable `ApiError` JSON payload. Strictly prevents Information Exposure by masking database constraints and raw Java stack traces from the client, while maintaining secure server-side observability via SLF4J logging.
* ✅ **Error Handling Refactor:** Upgraded the existing global exception handler by eradicating hardcoded strings and implementing an `ErrorCode` enum template system, standardizing API responses across the service layer.
* ✅ **DTO Automation:** Automated object mapping between Entities and DTOs using MapStruct interfaces.
* ✅ **API Documentation:** Auto-generated Swagger UI / OpenAPI specification with security bypass for interactive endpoint testing.
* ✅ **CORS Configuration:** Global Cross-Origin Resource Sharing filters configured for secure external frontend integration.
* ✅ **Infrastructure & Secret Management:** Eradicated hardcoded secrets and monolithic property files. Migrated to an industry-standard, multi-profile YAML configuration (`application.yml`, `application-local.yml`, `application-prod.yml`). Secured the codebase by strictly injecting database credentials and JWT signatures via environment variables, fortified by comprehensive `.gitignore` rules to prevent credential leaks.
* ✅ **Advanced Business Logic:** Engineered time-constrained cancellation flows (e.g., blocking cancellations within 24 hours of departure) and strict state machine transitions for reservations (Pending -> Confirmed / Expired).
* ✅ **Payment Simulation:** Integrated a mock payment gateway endpoint to facilitate end-to-end reservation confirmation testing without external third-party dependencies.

### 🧪 Testing & Quality
* ✅ **Enterprise Testing Architecture:** Established a robust testing baseline using Testcontainers (Docker MySQL) with a Singleton Container pattern, ensuring 100% environment parity between local development and CI pipelines while preventing database contamination across test suites.
* ✅ **Security & Integration Testing:** Implemented comprehensive `MockMvc` integration tests to strictly validate the RBAC layer, JWT authentication filters, IDOR protections, and the unified `ApiError` responses across both Servlet and MVC boundaries.
* ✅ **Unit Testing & Immutability:** Established a fast-executing Mockito test suite for complex business logic (e.g., 24-hour cancellation rules). Leveraged `ReflectionTestUtils` to safely inject mock states into strict, builder-pattern immutable entities without compromising encapsulation (no public setters).
* ✅ **Advanced Test Isolation:** Eliminated state leakage (flaky tests) in CI/CD pipelines. Refactored IDOR security tests to use out-of-bounds IDs, adopted the AAA (Arrange, Act, Assert) pattern with dynamic test data seeding to bypass MySQL auto-increment unpredictability, and replaced `@Transactional` in concurrency tests with surgical `JdbcTemplate` teardowns to validate true database-level Optimistic Locking.
* ✅ **Continuous Integration (CI):** Configured a GitHub Actions workflow to automatically provision an ephemeral environment, compile the application, and execute the entire Testcontainers suite on every Pull Request, establishing a strict quality gate against regressions.

## 🗺️ Roadmap

* ✅ **Sprint 5 — Foundations:** Static real-world topology (`airport`, `route`, `aircraft_model`, `aircraft`, `user`) and the underlying Flyway migration/JPA relationship groundwork.
* ✅ **Sprint 6 — Calendars & Physical Inventory:** The idempotent flight-instance generator and the bulk seat materialization engine documented above. *(Merged — 10/10 issues closed)*
* 🔜 **Sprint 7 — Routing & Booking Engine:** Multi-segment itineraries (`itinerary`, `flight_segment`, `booking`, `ticket`), temporal coherence validation across layovers, transactional optimistic/pessimistic seat locking (a failed second-segment lock rolls back the first), and a TTL-based reservation cart.
* 🔜 **Sprint 8 — Financial Domain:** Dynamic pricing (base fare + seat extras + baggage), `Idempotency-Key`-protected payment endpoints, and an append-only `invoice` model with asynchronous refund processing.
* 🔜 **Sprint 9 — Bulk Data Ingestion:** Spring Batch job to chunk-process large route/schedule CSV imports without locking up the system, with a dead-letter table for corrupt rows.
* 🔜 **Sprint 11 — Event-Driven Architecture:** Publishing a `BookingConfirmedEvent` on booking confirmation, consumed asynchronously by workers for ticket PDF generation and email dispatch.
* 🔜 **Sprint 12 — Observability:** Actuator + Micrometer metrics, Prometheus/Grafana dashboards for the background jobs. *(CI/CD quality gate already in place — see Testing & Quality above.)*
* 🔜 **Sprint 13 — AI Assistant:** Natural-language querying via Spring AI function calling, grounded entirely in real transactional data (no hallucinated availability).
* 🔜 **Sprint 14 — Identity Hardening:** Password-reset flow with prior-hash validation and a revoked-JWT blacklist. *(Core RBAC already in place — see Security above.)*

This reflects current intent and may be reprioritized as the project evolves.

## ⚙️ How to run locally
1. Clone the repository.
2. **Infrastructure:** Ensure **MySQL** and **Redis** are running locally.
3. **Environment Setup:**
    * The project strictly enforces environment separation. By default, it expects the `local` profile.
    * Inject your local secrets via IDE Environment Variables or your terminal: `DB_LOCAL_USER`, `DB_LOCAL_PASSWORD`, and `JWT_SECRET_LOCAL`. *(Note: Default fallbacks for DB credentials are set to 'root' for seamless onboarding, but the JWT secret must be provided).*
4. **To run the application (for manual testing via Postman/Swagger):**
    * Run the application via your IDE (ensure `Active profiles: local` is set) or using Maven: `mvn spring-boot:run -Dspring-boot.run.profiles=local`.
    * *Note: Database schema creation and initial test data injection (Users, Roles) are automatically handled upon startup via **Flyway migrations**.*
5. **To run the automated tests:**
    * Execute `mvn test`. Testcontainers will automatically intercept the execution, spin up ephemeral MySQL and Redis containers, run the suite, and tear them down with zero manual configuration required.

## 🔐 Authentication & Authorization (Testing via Postman)
The API strictly enforces role-based access.

1. Send a `POST` request to `/api/v1/auth/login` with your user credentials (e.g., admin or regular user).
2. Copy the JWT string returned in the response.
3. In your subsequent requests, go to the **Authorization** tab in Postman, select **Bearer Token**, and paste the token string.

### Access Levels:
* **Admin Role (`ROLE_ADMIN`):** Has full access to flight management endpoints (`POST`, `PUT`, `DELETE` on `/api/v1/flights`).
* **User Role (`ROLE_USER`):** Can retrieve flights and manage their own bookings (`POST` on `/api/v1/reservations`).