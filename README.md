# Flight Booking API ✈️

A RESTful API for managing flight reservations, built with Spring Boot. This project serves as the core backend engine for an airline booking system.

## Tech Stack
* **Framework:** Spring Boot 3.x
* **Language:** Java
* **Database:** MySQL & Spring Data JPA
* **Security:** Spring Security (In Progress)

## Current Status
* ✅ Core domain models and database relationships configured.
* ✅ Reservation engine and End-to-End E2E logic implemented.
* 🏗️ Security and JWT authentication currently in development.

## How to run locally
1. Clone the repository.
2. Ensure MySQL is running and update `application.properties` with your credentials.
3. Run the application via your IDE or using Maven.
4. Note: A `DataSeeder` is configured to automatically inject initial test data (Users and Flights) upon startup to facilitate local testing.