package com.aerolinea.flight_booking_api.domain.seat;

import com.aerolinea.flight_booking_api.dtos.Seat.SeatGenerationProjection;
import com.aerolinea.flight_booking_api.repositories.FlightInstanceRepository;
import com.aerolinea.flight_booking_api.services.GenerationSeatServiceImpl;
import com.aerolinea.flight_booking_api.services.SeatService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenerationSeatServiceImplTest {

    @Mock
    private FlightInstanceRepository flightInstanceRepository;

    @Mock
    private SeatService seatService;

    @InjectMocks
    private GenerationSeatServiceImpl generationSeatService;

    @Test
    void shouldGenerateSeatsForEveryProjectionInPage() {
        SeatGenerationProjection first = new SeatGenerationProjection(1L, 30, "ABCDEF");
        SeatGenerationProjection second = new SeatGenerationProjection(2L, 25, "ABC");

        Page<SeatGenerationProjection> page = new PageImpl<>(List.of(first, second), PageRequest.of(0, 1000), 2);
        Page<SeatGenerationProjection> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 1000), 0);

        when(flightInstanceRepository.findInstancesWithoutSeats(any(Pageable.class)))
                .thenReturn(page, emptyPage);

        generationSeatService.generateUpcomingSeat();

        verify(seatService, times(1)).generateSeatsForInstance(1L, 30, "ABCDEF");
        verify(seatService, times(1)).generateSeatsForInstance(2L, 25, "ABC");
    }

    @Test
    void shouldSumInsertedCountsAcrossAllProcessedProjections() {
        SeatGenerationProjection first = new SeatGenerationProjection(1L, 30, "ABCDEF");
        SeatGenerationProjection second = new SeatGenerationProjection(2L, 25, "ABC");

        Page<SeatGenerationProjection> page = new PageImpl<>(List.of(first, second), PageRequest.of(0, 1000), 2);
        Page<SeatGenerationProjection> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 1000), 0);

        when(flightInstanceRepository.findInstancesWithoutSeats(any(Pageable.class)))
                .thenReturn(page, emptyPage);
        when(seatService.generateSeatsForInstance(1L, 30, "ABCDEF")).thenReturn(180);
        when(seatService.generateSeatsForInstance(2L, 25, "ABC")).thenReturn(75);

        generationSeatService.generateUpcomingSeat();

        verify(seatService).generateSeatsForInstance(1L, 30, "ABCDEF");
        verify(seatService).generateSeatsForInstance(2L, 25, "ABC");
    }

    @Test
    void shouldContinueProcessingAfterDataIntegrityViolationOnOneInstance() {
        SeatGenerationProjection failing = new SeatGenerationProjection(1L, 30, "ABCDEF");
        SeatGenerationProjection succeeding = new SeatGenerationProjection(2L, 25, "ABC");

        Page<SeatGenerationProjection> page = new PageImpl<>(List.of(failing, succeeding), PageRequest.of(0, 1000), 2);
        Page<SeatGenerationProjection> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 1000), 0);

        when(flightInstanceRepository.findInstancesWithoutSeats(any(Pageable.class)))
                .thenReturn(page, emptyPage);
        doThrow(new DataIntegrityViolationException("duplicate"))
                .when(seatService).generateSeatsForInstance(1L, 30, "ABCDEF");
        when(seatService.generateSeatsForInstance(2L, 25, "ABC")).thenReturn(75);

        generationSeatService.generateUpcomingSeat();

        verify(seatService, times(1)).generateSeatsForInstance(2L, 25, "ABC");
    }

    @Test
    void shouldContinueProcessingAfterUnexpectedExceptionOnOneInstance() {
        SeatGenerationProjection failing = new SeatGenerationProjection(1L, 30, "ABCDEF");
        SeatGenerationProjection succeeding = new SeatGenerationProjection(2L, 25, "ABC");

        Page<SeatGenerationProjection> page = new PageImpl<>(List.of(failing, succeeding), PageRequest.of(0, 1000), 2);
        Page<SeatGenerationProjection> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 1000), 0);

        when(flightInstanceRepository.findInstancesWithoutSeats(any(Pageable.class)))
                .thenReturn(page, emptyPage);
        doThrow(new IllegalStateException("unexpected"))
                .when(seatService).generateSeatsForInstance(1L, 30, "ABCDEF");
        when(seatService.generateSeatsForInstance(2L, 25, "ABC")).thenReturn(75);

        generationSeatService.generateUpcomingSeat();

        verify(seatService, times(1)).generateSeatsForInstance(2L, 25, "ABC");
    }

    @Test
    void shouldKeepQueryingUntilNoInstancesRemain() {
        SeatGenerationProjection first = new SeatGenerationProjection(1L, 30, "ABCDEF");
        SeatGenerationProjection second = new SeatGenerationProjection(2L, 25, "ABC");
        SeatGenerationProjection third = new SeatGenerationProjection(3L, 20, "AB");

        Page<SeatGenerationProjection> firstPage = new PageImpl<>(List.of(first, second), PageRequest.of(0, 2), 3);
        Page<SeatGenerationProjection> secondPage = new PageImpl<>(List.of(third), PageRequest.of(0, 2), 1);

        when(flightInstanceRepository.findInstancesWithoutSeats(any(Pageable.class)))
                .thenReturn(firstPage, secondPage);

        generationSeatService.generateUpcomingSeat();

        verify(seatService, times(1)).generateSeatsForInstance(1L, 30, "ABCDEF");
        verify(seatService, times(1)).generateSeatsForInstance(2L, 25, "ABC");
        verify(seatService, times(1)).generateSeatsForInstance(3L, 20, "AB");
        verify(flightInstanceRepository, times(2)).findInstancesWithoutSeats(any(Pageable.class));
    }
}