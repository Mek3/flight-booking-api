# Flight Booking API ✈️

[![CI Pipeline](https://github.com/Mek3/flight-booking-api/actions/workflows/ci.yml/badge.svg)](https://github.com/Mek3/flight-booking-api/actions/workflows/ci.yml)

A RESTful API for managing flight reservations, built with Spring Boot. This project serves as the core backend engine for an airline booking system, featuring advanced security, role-based access control, and concurrent data protection.

## 🛠️ Tech Stack
* **Framework:** Spring Boot 3.x
* **Language:** Java (using modern features like `Records`)
* **Database:** MySQL & Spring Data JPA
* **Security:** Spring Security with JWT (JSON Web Tokens)

## 🚀 Current Status & Features
* ✅ **Core Architecture:** Domain models and relational database mapping configured.
* ✅ **Advanced Security & Auth:** Stateless JWT authentication and BCrypt password encoding.
* ✅ **RBAC (Role-Based Access Control):** Method-level security (@PreAuthorize) enforcing strict boundaries between `ROLE_USER` and `ROLE_ADMIN`.
* ✅ **Vulnerability Mitigation:** Protection against IDOR (Insecure Direct Object Reference) by extracting user identity directly from the Security Context rather than relying on client payloads.
* ✅ **Data Integrity:** Implementation of JPA Optimistic Locking (`@Version`) to prevent concurrent modification conflicts during flight updates and bookings.
* ✅ **RESTful Conventions:** Strict adherence to industry-standard pluralized routing and proper HTTP status codes (e.g., `204 No Content` for deletions).
* ✅ **Auditing:** Automatic JPA Auditing configured for tracking `created_at`, `updated_at`, `created_by`, and `deleted_by` fields.
* ✅ **Global Exception Handling:** Centralized error management using `@RestControllerAdvice`. Standardizes all API errors (400, 403, 404, 409, 500) into a clean, immutable `ApiError` JSON payload. Strictly prevents Information Exposure by masking database constraints and raw Java stack traces from the client, while maintaining secure server-side observability via SLF4J logging.
* ✅ **Defense in Depth Error Handling:** Unified security exception responses across both the Servlet Filter chain and MVC Controller boundaries. Custom Spring Security entry points intercept 401/403 errors outside the `DispatcherServlet`, manually serializing them to guarantee a consistent API contract.
* ✅ **DTO Automation:** Automated object mapping between Entities and DTOs using MapStruct interfaces.
* ✅ **Database Migrations:** Schema and data versioning strictly managed via Flyway.
* ✅ **API Documentation:** Auto-generated Swagger UI / OpenAPI specification with security bypass for interactive endpoint testing.
* ✅ **CORS Configuration:** Global Cross-Origin Resource Sharing filters configured for secure external frontend integration.
* ✅ **Performance & Scalability:** Implemented pagination across Flight and Reservation data layers to efficiently handle large datasets and prevent memory exhaustion.
* ✅ **Advanced Business Logic:** Engineered time-constrained cancellation flows (e.g., blocking cancellations within 24 hours of departure) and strict state machine transitions for reservations (Pending -> Confirmed / Expired).
* ✅ **Background Workers:** Implemented an asynchronous `@Scheduled` cron job to automatically expire abandoned reservations and release locked flight seats. Employs advanced Spring AOP proxy management and isolated transaction boundaries (`Propagation.REQUIRES_NEW`) to prevent database deadlocks during mass processing.
* ✅ **Error Handling Refactor:** Upgraded the existing global exception handler by eradicating hardcoded strings and implementing an `ErrorCode` enum template system, standardizing API responses across the service layer.
* ✅ **Payment Simulation:** Integrated a mock payment gateway endpoint to facilitate end-to-end reservation confirmation testing without external third-party dependencies.
* ✅ **Enterprise Testing Architecture:** Established a robust testing baseline using Testcontainers (Docker MySQL) with a Singleton Container pattern, ensuring 100% environment parity between local development and CI pipelines while preventing database contamination across test suites.
* ✅ **Security & Integration Testing:** Implemented comprehensive `MockMvc` integration tests to strictly validate the RBAC layer, JWT authentication filters, IDOR protections, and the unified `ApiError` responses across both Servlet and MVC boundaries.
* ✅ **Concurrency Stress Testing:** Engineered multi-threaded stress tests utilizing `ExecutorService` and `CountDownLatch` to mathematically prove zero seat overbooking during simultaneous purchase attempts, validating the JPA Optimistic Locking (`@Version`) mechanism under heavy load.
* ✅ **Continuous Integration (CI):** Configured a GitHub Actions workflow to automatically provision an ephemeral environment, compile the application, and execute the entire Testcontainers suite on every Pull Request, establishing a strict quality gate against regressions.

## ⚙️ How to run locally
1. Clone the repository.
2. Ensure MySQL is running and update `application.properties` with your database credentials.
3. Run the application via your IDE or using Maven.
4. **Note:** Database schema creation and initial test data injection (Users, Roles, Flights) are automatically handled upon startup via **Flyway migrations** (V1 & V2), completely replacing manual data seeders.

## 🔐 Authentication & Authorization (Testing via Postman)
The API strictly enforces role-based access. 

1. Send a `POST` request to `/api/v1/auth/login` with your user credentials (e.g., admin or regular user).
2. Copy the JWT string returned in the response.
3. In your subsequent requests, go to the **Authorization** tab in Postman, select **Bearer Token**, and paste the token string.

### Access Levels:
* **Admin Role (`ROLE_ADMIN`):** Has full access to flight management endpoints (`POST`, `PUT`, `DELETE` on `/api/v1/flights`).
* **User Role (`ROLE_USER`):** Can retrieve flights and manage their own bookings (`POST` on `/api/v1/reservations`).