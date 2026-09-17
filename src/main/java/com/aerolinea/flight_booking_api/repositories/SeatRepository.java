package com.aerolinea.flight_booking_api.repositories;

import com.aerolinea.flight_booking_api.models.Seat;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long>, SeatRepositoryCustom  {
    long countByFlightInstanceId(Long flightInstanceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.id IN :ids ORDER BY s.id")
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")})
    List<Seat> lockSeats(@Param("ids") List<Long> ids);

    @Query("""
            SELECT COUNT(s) 
            FROM Seat s
            WHERE s.flightInstance.id = :flightInstanceId
              AND s.id IN :seatIds
        """)
    long countSeatsByFlightInstanceIdAndSeatIds(@Param("flightInstanceId") Long flightInstanceId, @Param("seatIds") List<Long> seatIds);
}
