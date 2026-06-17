# Flight Booking API ✈️

A RESTful API for managing flight reservations, built with Spring Boot. This project serves as the core backend engine for an airline booking system.

## Tech Stack
* **Framework:** Spring Boot 3.x
* **Language:** Java
* **Database:** MySQL & Spring Data JPA
* **Security:** Spring Security with JWT (JSON Web Tokens)

## Current Status
* ✅ Core domain models and database relationships configured.
* ✅ Reservation engine and End-to-End (E2E) logic implemented.
* ✅ Security architecture completed: JWT authentication, BCrypt password encoding, and protected endpoints.
* ✅ Automatic JPA Auditing configured (automatic population of `created_by` and `updated_by` tracking fields).

## How to run locally
1. Clone the repository.
2. Ensure MySQL is running and update `application.properties` with your credentials.
3. Run the application via your IDE or using Maven.
4. **Note:** A `DataSeeder` is configured to automatically inject initial test data (Users, Roles, and Flights) upon startup to facilitate local testing.

## Authentication (Testing via Postman)
To access protected endpoints (e.g., creating a reservation):
1. Send a `POST` request to `/api/v1/auth/login` with your user credentials.
2. Copy the JWT string returned in the response.
3. In your subsequent requests, go to the **Authorization** tab in Postman, select **Bearer Token**, and paste the token string.