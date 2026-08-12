package com.aerolinea.flight_booking_api.repositories;

import com.aerolinea.flight_booking_api.dtos.Seat.SeatGenerationProjection;
import com.aerolinea.flight_booking_api.models.FlightInstance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FlightInstanceRepository  extends JpaRepository<FlightInstance, Long> {

    @Query("SELECT new  com.aerolinea.flight_booking_api.dtos.Seat.SeatGenerationProjection(" +
            "fi.id, al.totalRows, al.seatLetters) " +
            "FROM FlightInstance fi " +
            "JOIN fi.flightSchedule fs " +
            "JOIN fs.aircraftLayout al " +
            "WHERE NOT EXISTS (SELECT 1 FROM Seat s WHERE s.flightInstance = fi)")
    Page<SeatGenerationProjection>  findInstancesWithoutSeats(Pageable pageable);
}
