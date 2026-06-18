# Flight Booking API ✈️

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

## ⚙️ How to run locally
1. Clone the repository.
2. Ensure MySQL is running and update `application.properties` with your database credentials.
3. Run the application via your IDE or using Maven.
4. **Note:** A `DataSeeder` is configured to automatically inject initial test data (Users, Roles, and Flights) upon startup to facilitate local testing.

## 🔐 Authentication & Authorization (Testing via Postman)
The API strictly enforces role-based access. 

1. Send a `POST` request to `/api/v1/auth/login` with your user credentials (e.g., admin or regular user).
2. Copy the JWT string returned in the response.
3. In your subsequent requests, go to the **Authorization** tab in Postman, select **Bearer Token**, and paste the token string.

### Access Levels:
* **Admin Role (`ROLE_ADMIN`):** Has full access to flight management endpoints (`POST`, `PUT`, `DELETE` on `/api/v1/flights`).
* **User Role (`ROLE_USER`):** Can retrieve flights and manage their own bookings (`POST` on `/api/v1/reservations`).