package com.aerolinea.flight_booking_api.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.aerolinea.flight_booking_api.dtos.booking.BookingRequest;
import com.aerolinea.flight_booking_api.models.*;
import com.aerolinea.flight_booking_api.models.enums.FlightStatus;
import com.aerolinea.flight_booking_api.repositories.*;
import com.aerolinea.flight_booking_api.services.BookingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Fills the local database with enough realistic data to browse the API
 * manually — airports, a couple of routes (including a round trip with a
 * connection), materialised seats, a demo user and one live booking.
 *
 * Runs only under the "local" profile and only if the database looks empty,
 * so it is safe to leave in place and restart the app as many times as you
 * like without piling up duplicate data.
 */
@Component
@Profile("local")
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final AirportRepository airportRepository;
    private final AircraftModelRepository aircraftModelRepository;
    private final AircraftLayoutRepository aircraftLayoutRepository;
    private final FlightScheduleRepository flightScheduleRepository;
    private final FlightInstanceRepository flightInstanceRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BookingService bookingService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.enabled:true}")
    private boolean enabled;

    @Override
    public void run(String... args) {
        if (!enabled) {
            return;
        }
        if (airportRepository.count() > 0) {
            log.info("Seed skipped — airports table is not empty.");
            return;
        }

        log.info("Seeding local database with demo data...");

        // BaseEntity.createdBy is not nullable and is normally filled by Spring
        // Data JPA Auditing from the authenticated user. There is no logged-in
        // user yet at startup, so authenticate as a synthetic "system" user for
        // everything the seeder creates itself.
        authenticateAs("system");

        Airport alc = airport("ALC", "Alicante-Elche", "Alicante", "ES");
        Airport mad = airport("MAD", "Adolfo Suárez Madrid-Barajas", "Madrid", "ES");
        Airport jfk = airport("JFK", "John F. Kennedy", "New York", "US");
        Airport bcn = airport("BCN", "Barcelona-El Prat", "Barcelona", "ES");

        AircraftModel model = aircraftModelRepository.save(
                AircraftModel.builder()
                        .manufacturer("Airbus")
                        .modelName("A320")
                        .maxCapacity((short) 30)
                        .build());

        AircraftLayout layout = aircraftLayoutRepository.save(
                AircraftLayout.builder()
                        .aircraftModel(model)
                        .cabinClass("ECONOMY") // adjust to your real type/value if this is an enum
                        .totalRows(5)
                        .seatLetters("ABCDEF")
                        .seatCapacity(30)
                        .build());

        LocalDate today = LocalDate.now();

        // Outbound with a connection: ALC -> MAD -> JFK
        FlightSchedule outboundLeg1 = schedule("IBE-101", alc, mad,
                LocalTime.of(8, 0), LocalTime.of(9, 0), 0, layout, "100.00");
        FlightSchedule outboundLeg2 = schedule("IBE-205", mad, jfk,
                LocalTime.of(10, 30), LocalTime.of(18, 30), 0, layout, "400.00");

        // Direct return, overnight: JFK -> ALC
        FlightSchedule returnLeg = schedule("IBE-412", jfk, alc,
                LocalTime.of(21, 0), LocalTime.of(11, 0), 1, layout, "380.00");

        // A domestic hop for variety: ALC -> BCN
        FlightSchedule domestic = schedule("VLG-330", alc, bcn,
                LocalTime.of(14, 0), LocalTime.of(15, 15), 0, layout, "65.00");

        FlightInstance instanceLeg1 = null;
        FlightInstance instanceLeg2 = null;

        for (int day = 1; day <= 7; day++) {
            LocalDate date = today.plusDays(day);

            FlightInstance i1 = instance(outboundLeg1, date);
            FlightInstance i2 = instance(outboundLeg2, date);
            instance(returnLeg, date.plusDays(7));
            instance(domestic, date);

            if (day == 1) {
                instanceLeg1 = i1;
                instanceLeg2 = i2;
            }
        }

        Role userRole = roleRepository.findByName("ROLE_USER");
        if (userRole == null) {
            log.warn("ROLE_USER not found — skipping demo user and booking. " +
                    "Make sure your role seed migration ran.");
            printSummary();
            return;
        }

        User demoUser = User.builder()
                .username("demo")
                .email("demo@example.com")
                .password(passwordEncoder.encode("demo1234"))
                .name("Demo")
                .surname("User")
                .build();
        demoUser.addRole(userRole);
        userRepository.save(demoUser);

        createDemoBooking(demoUser, instanceLeg1, instanceLeg2);

        SecurityContextHolder.clearContext();
        printSummary();
    }

    private void authenticateAs(String username) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(username, null, List.of()));
        SecurityContextHolder.setContext(context);
    }

    private Airport airport(String code, String name, String city, String country) {
        return airportRepository.save(Airport.builder()
                .code(code)
                .name(name)
                .city(city)
                .country(country)
                .build());
    }

    private FlightSchedule schedule(String flightNumber, Airport from, Airport to,
                                    LocalTime departureTime, LocalTime arrivalTime,
                                    int arrivalDayOffset, AircraftLayout layout, String basePrice) {
        return flightScheduleRepository.save(FlightSchedule.builder()
                .flightNumber(flightNumber)
                .departureAirport(from)
                .arrivalAirport(to)
                .departureTime(departureTime)
                .arrivalTime(arrivalTime)
                .arrivalDayOffset(arrivalDayOffset)
                .daysOfWeekMask(127)
                .aircraftLayout(layout)
                .basePrice(new BigDecimal(basePrice))
                .build());
    }

    private FlightInstance instance(FlightSchedule schedule, LocalDate date) {
        FlightInstance saved = flightInstanceRepository.save(FlightInstance.builder()
                .flightSchedule(schedule)
                .departureDate(date)
                .status(FlightStatus.SCHEDULED)
                .build());

        seatRepository.batchInsertSeats(saved.getId(), 5, "ABCDEF");

        return saved;
    }

    private void createDemoBooking(User user, FlightInstance leg1, FlightInstance leg2) {
        if (leg1 == null || leg2 == null) {
            return;
        }

        Long seat1 = firstFreeSeatId(leg1.getId());
        Long seat2 = firstFreeSeatId(leg2.getId());

        if (seat1 == null || seat2 == null) {
            log.warn("Could not find seats to build the demo booking — skipping.");
            return;
        }

        authenticateAs(user.getUsername());

        try {
            BookingRequest request = new BookingRequest(1, List.of(
                    new BookingRequest.ItineraryRequest(List.of(
                            new BookingRequest.FlightSegmentRequest(leg1.getId(), List.of(seat1)),
                            new BookingRequest.FlightSegmentRequest(leg2.getId(), List.of(seat2))
                    ))
            ));
            bookingService.createBooking(request);
        } catch (Exception e) {
            log.warn("Could not create the demo booking: {}", e.getMessage());
        } finally {
            authenticateAs("system");
        }
    }

    private Long firstFreeSeatId(Long flightInstanceId) {
        return seatRepository.findAll().stream()
                .filter(seat -> seat.getFlightInstance().getId().equals(flightInstanceId))
                .findFirst()
                .map(Seat::getId)
                .orElse(null);
    }

    private void printSummary() {
        log.info("""

                ── Seed complete ─────────────────────────────
                  Login:    demo / demo1234
                  Search:   GET /api/v1/flights/search?departure=ALC&destination=JFK
                  Routes:   ALC↔MAD↔JFK (connection), ALC↔BCN (domestic)
                  Window:   7 upcoming days per route
                ───────────────────────────────────────────────
                """);
    }
}