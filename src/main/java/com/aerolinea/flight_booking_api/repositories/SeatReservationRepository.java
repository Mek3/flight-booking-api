package com.aerolinea.flight_booking_api.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.aerolinea.flight_booking_api.models.SeatReservation;
import com.aerolinea.flight_booking_api.models.enums.SeatReservationStatus;

public interface SeatReservationRepository extends JpaRepository<SeatReservation, Long> {

    Optional<SeatReservation> findBySeatIdAndStatusIn(Long seatId, List<SeatReservationStatus> statuses);

    List<SeatReservation> findByFlightSegmentId(Long flightSegmentId);

    @Query("""
            SELECT sr.id FROM SeatReservation sr
            WHERE sr.status = :status
              AND sr.heldUntil < :moment
            """)
    List<Long> findExpiredHoldIds(@Param("status") SeatReservationStatus status,
                                  @Param("moment") LocalDateTime moment);

    @Query("""
            SELECT COUNT(sr) FROM SeatReservation sr
            WHERE sr.seat.flightInstance.id = :flightInstanceId
              AND sr.status IN :statuses
            """)
    long countOccupiedSeats(@Param("flightInstanceId") Long flightInstanceId,
                            @Param("statuses") List<SeatReservationStatus> statuses);
}