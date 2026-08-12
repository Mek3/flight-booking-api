package com.aerolinea.flight_booking_api.repositories;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class SeatRepositoryImpl implements SeatRepositoryCustom {

    private static final String INSERT_SEAT_SQL =
            "INSERT IGNORE INTO seats (flight_instance_id, seat_row, seat_letter, is_available, created_at, created_by) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public int[] batchInsertSeats(Long flightInstanceId, Integer totalRows, String seatLetters) {
        List<Object[]> batchArgs = new ArrayList<>();

        for (int row = 1; row <= totalRows; row++) {
            for (char letter : seatLetters.toCharArray()) {
                batchArgs.add(new Object[]{
                        flightInstanceId,
                        row,
                        String.valueOf(letter),
                        Boolean.TRUE,
                        LocalDateTime.now(),
                        "system"
                });
            }
        }

        return jdbcTemplate.batchUpdate(INSERT_SEAT_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Object[] args = batchArgs.get(i);
                ps.setLong(1, (Long) args[0]);
                ps.setInt(2, (Integer) args[1]);
                ps.setString(3, (String) args[2]);
                ps.setBoolean(4, (Boolean) args[3]);
                ps.setObject(5, args[4]);
                ps.setString(6, (String) args[5]);
            }

            @Override
            public int getBatchSize() {
                return batchArgs.size();
            }
        });
    }
}