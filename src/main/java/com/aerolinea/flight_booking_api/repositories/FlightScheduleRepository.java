package com.aerolinea.flight_booking_api.repositories;

import com.aerolinea.flight_booking_api.models.FlightSchedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FlightScheduleRepository extends JpaRepository<FlightSchedule, Long> {

    @Query(
            value ="SELECT * FROM flight_schedules  WHERE (days_of_week_mask & :daysOfWeekMask) = :daysOfWeekMask",
            countQuery = "SELECT COUNT(*) FROM flight_schedules  WHERE (days_of_week_mask & :daysOfWeekMask) = :daysOfWeekMask",
            nativeQuery = true)
    Page<FlightSchedule> findAllByDaysOfWeekMask(Pageable pageable, @Param("daysOfWeekMask") Integer daysOfWeekMask);
}
