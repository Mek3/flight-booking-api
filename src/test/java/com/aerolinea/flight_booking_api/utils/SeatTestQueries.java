package com.aerolinea.flight_booking_api.utils;

import org.springframework.jdbc.core.JdbcTemplate;

public class SeatTestQueries {

    private final JdbcTemplate jdbcTemplate;

    public SeatTestQueries(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Integer countSeatsFor(Long flightInstanceId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM seats WHERE flight_instance_id = ?", Integer.class, flightInstanceId);
    }

    public boolean seatExists(Long flightInstanceId, Integer rowNumber, String seatLetter) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM seats WHERE flight_instance_id = ? AND seat_row = ? AND seat_letter = ?",
                Integer.class, flightInstanceId, rowNumber, seatLetter);
        return count != null && count > 0;
    }
}