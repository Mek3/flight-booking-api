package com.aerolinea.flight_booking_api.services;

import com.aerolinea.flight_booking_api.repositories.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatServiceImpl implements  SeatService {

    private final SeatRepository seatRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public int generateSeatsForInstance(Long flightInstanceId, Integer totalRows, String seatLetters) {
        List<Object[]> batchArgs = new ArrayList<>();

        for (int row = 1; row <= totalRows; row++) {
            for (char letter : seatLetters.toCharArray()) {
                batchArgs.add(new Object[]{
                        flightInstanceId,
                        row,
                        String.valueOf(letter),
                        Boolean.TRUE,
                        LocalDateTime.now(),
                        "SYSTEM"
                });
            }
        }

        int[] results = seatRepository.batchInsertSeats(flightInstanceId, totalRows, seatLetters);

        int inserted = 0;
        int ignored = 0;
        for (int result : results) {
            if (result > 0) {
                inserted++;
            } else if (result == 0) {
                ignored++;
            }
        }

        if (ignored > 0) {
            log.warn("FlightInstance {}: {} of {} rows skipped by INSERT IGNORE — verify whether these are expected duplicates or a different cause",
                    flightInstanceId, ignored, batchArgs.size());
        } else {
            log.info("FlightInstance {}: {} seats inserted successfully", flightInstanceId, inserted);
        }

        return inserted;
    }
}
