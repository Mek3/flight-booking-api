package com.aerolinea.flight_booking_api.domain.flight;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.aerolinea.flight_booking_api.dtos.FlightDTO;
import com.aerolinea.flight_booking_api.dtos.FlightSearchCriteria;
import com.aerolinea.flight_booking_api.exceptions.ErrorCode;
import com.aerolinea.flight_booking_api.exceptions.ResourceNotFoundException;
import com.aerolinea.flight_booking_api.mappers.FlightMapper;
import com.aerolinea.flight_booking_api.models.Flight;
import com.aerolinea.flight_booking_api.repositories.FlightRepository;
import com.aerolinea.flight_booking_api.services.FlightServiceImpl;

@ExtendWith(MockitoExtension.class)
public class FlightServiceImplTest {

    @Mock
    private FlightRepository flightRepository;

    @Mock
    private FlightMapper flightMapper;

    @InjectMocks
    private FlightServiceImpl flightService;

    private Flight flight;
    private FlightDTO flightDTO;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        flight = Flight.builder()
                .flightNumber("IBE-001")
                .departure("MAD")
                .departureTime(LocalDateTime.of(2026, 7, 15, 10, 0))
                .destination("JFK")
                .destinationTime(LocalDateTime.of(2026, 7, 15, 18, 0))
                .availableSeats(100)
                .price(new BigDecimal("400.00"))
                .build();

        flightDTO = new FlightDTO(
                1L,
                "IBE-001",
                "MAD",
                LocalDateTime.of(2026, 7, 15, 10, 0),
                "JFK",
                LocalDateTime.of(2026, 7, 15, 18, 0),
                100,
                new BigDecimal("400.00")
        );

        pageable = PageRequest.of(0, 10);
    }

    @Test
    void givenFlightDTO_whenSave_thenReturnSavedFlightDTO() {
        when(flightMapper.toFlight(flightDTO)).thenReturn(flight);
        when(flightRepository.save(flight)).thenReturn(flight);
        when(flightMapper.toFlightDTO(flight)).thenReturn(flightDTO);

        FlightDTO result = flightService.save(flightDTO);

        assertThat(result).isNotNull();
        assertThat(result.getFlightNumber()).isEqualTo("IBE-001");
        verify(flightRepository, times(1)).save(flight);
    }

    @Test
    void givenExistingFlightId_whenUpdateFlight_thenReturnUpdatedFlightDTO() {
        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));
        when(flightRepository.save(flight)).thenReturn(flight);
        when(flightMapper.toFlightDTO(flight)).thenReturn(flightDTO);

        FlightDTO result = flightService.updateFlight(1L, flightDTO);

        assertThat(result).isNotNull();
        verify(flightMapper, times(1)).updateFlightFromDTO(flightDTO, flight);
        verify(flightRepository, times(1)).save(flight);
    }

    @Test
    void givenNonExistingFlightId_whenUpdateFlight_thenThrowResourceNotFoundException() {
        when(flightRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> flightService.updateFlight(99L, flightDTO));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FLIGHT_NOT_FOUND);
        verify(flightRepository, never()).save(any(Flight.class));
    }

    @Test
    void givenExistingFlightId_whenFlightById_thenReturnFlightDTO() {
        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));
        when(flightMapper.toFlightDTO(flight)).thenReturn(flightDTO);

        FlightDTO result = flightService.flightById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getFlightNumber()).isEqualTo("IBE-001");
    }

    @Test
    void givenNonExistingFlightId_whenFlightById_thenThrowResourceNotFoundException() {
        when(flightRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> flightService.flightById(99L));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FLIGHT_NOT_FOUND);
    }

    @Test
    void givenPageable_whenGetFlights_thenReturnFlightDTOPage() {
        Page<Flight> flightPage = new PageImpl<>(List.of(flight));
        when(flightRepository.findAll(pageable)).thenReturn(flightPage);
        when(flightMapper.toFlightDTO(flight)).thenReturn(flightDTO);

        Page<FlightDTO> result = flightService.getFlights(pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(flightRepository, times(1)).findAll(pageable);
    }

    @Test
    void givenExistingFlightId_whenDeleteFlightById_thenExecuteDeletion() {
        when(flightRepository.existsById(1L)).thenReturn(true);

        flightService.deleteFlightById(1L);

        verify(flightRepository, times(1)).deleteById(1L);
    }

    @Test
    void givenNonExistingFlightId_whenDeleteFlightById_thenThrowResourceNotFoundException() {
        when(flightRepository.existsById(99L)).thenReturn(false);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> flightService.deleteFlightById(99L));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FLIGHT_NOT_FOUND);
        verify(flightRepository, never()).deleteById(any());
    }

    @Test
    void givenCriteriaAndPageable_whenSearchFlights_thenReturnFilteredFlightDTOPage() {
        FlightSearchCriteria criteria = new FlightSearchCriteria(
                "MAD", "JFK", new BigDecimal("100.00"), new BigDecimal("500.00"), 2, LocalDate.of(2026, 7, 15)
        );
        Page<Flight> flightPage = new PageImpl<>(List.of(flight));

        when(flightRepository.findAll((org.mockito.ArgumentMatchers.<Specification<Flight>>any()), eq(pageable))).thenReturn(flightPage);
        when(flightMapper.toFlightDTO(flight)).thenReturn(flightDTO);

        Page<FlightDTO> result = flightService.searchFlights(criteria, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(flightRepository, times(1)).findAll(org.mockito.ArgumentMatchers.<Specification<Flight>>any(), eq(pageable));
    }
}
