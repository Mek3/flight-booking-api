package com.aerolinea.flight_booking_api.services;

import com.aerolinea.flight_booking_api.dtos.Seat.SeatGenerationProjection;
import com.aerolinea.flight_booking_api.repositories.FlightInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GenerationSeatServiceImpl implements GenerationSeatService {

    private final SeatService seatService;
    private final FlightInstanceRepository flightInstanceRepository;

    @Override
    public void generateUpcomingSeat() {
        Pageable pageable = PageRequest.of(0, 1000);

        Page<SeatGenerationProjection> page;
        int totalGenerated = 0;

        do {
            page = flightInstanceRepository.findInstancesWithoutSeats(pageable);

            for (SeatGenerationProjection projection : page.getContent()) {

                try {
                    totalGenerated += seatService.generateSeatsForInstance(
                            projection.flightInstanceId(), projection.totalRows(), projection.seatLetters());

                } catch (DataIntegrityViolationException e) {
                    log.warn("FlightInstance {}: constraint violation during seat generation, skipping",  projection.flightInstanceId());
                } catch (Exception e) {
                    log.error("FlightInstance {}: unexpected error during seat generation",  projection.flightInstanceId(), e);
                }
            }

        } while (page.hasNext());

        log.info("Seat generation completed. {} seats inserted for flights", totalGenerated);
    }


}