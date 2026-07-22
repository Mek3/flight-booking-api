package com.aerolinea.flight_booking_api.domain.airport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import org.springframework.test.util.ReflectionTestUtils;

import com.aerolinea.flight_booking_api.dtos.airport.AirportRequest;
import com.aerolinea.flight_booking_api.dtos.airport.AirportResponse;
import com.aerolinea.flight_booking_api.exceptions.ErrorCode;
import com.aerolinea.flight_booking_api.exceptions.ResourceNotFoundException;
import com.aerolinea.flight_booking_api.mappers.AirportMapper;
import com.aerolinea.flight_booking_api.models.Airport;
import com.aerolinea.flight_booking_api.repositories.AirportRepository;
import com.aerolinea.flight_booking_api.services.AirportServiceImpl;

@ExtendWith(MockitoExtension.class)
public class AirportServiceImplTest {

    @Mock
    AirportRepository airportRepository;

    @Mock
    private AirportMapper airportMapper;

    @InjectMocks
    AirportServiceImpl airportServiceImpl;

    private Pageable pageable;
    private Airport airport;
    private AirportRequest airportRequest;
    private AirportResponse airportResponse;

    @BeforeEach
    void setUp(){
        airport = Airport.builder()
                .code("JFL")
                .city("New York")
                .country("USA")
                .name("John F. Kennedy")
                .build();

        ReflectionTestUtils.setField(airport, "id", 1L);

        airportRequest = new AirportRequest("JFK", "John F. Kennedy", "New York", "USA");
        airportResponse = new AirportResponse(1L, "JFK", "John F. Kennedy", "New York", "USA");

        pageable = PageRequest.of(0, 10);
    }

    @Test
    void getAirportById_ShouldReturnResponse_WhenExists(){
        Long id = 1L;
        when(airportRepository.findById(id)).thenReturn(Optional.of(airport));
        when(airportMapper.toAirportResponse(airport)).thenReturn(airportResponse);

        AirportResponse airportResponseResult = airportServiceImpl.getAirportById(id);
        
        assertNotNull(airportResponseResult);
        assertThat(airportResponseResult.name()).isEqualTo("John F. Kennedy");
        assertThat(airportResponseResult.id()).isEqualTo(id); 

        verify(airportRepository).findById(id);
        verify(airportMapper).toAirportResponse(airport);
    }

    @Test
    void getAirportById_ShouldThrowException_WhenNotFound() {
        Long id = 9999L;
        when(airportRepository.findById(id)).thenReturn(Optional.empty());

        ResourceNotFoundException exception =  assertThrows(ResourceNotFoundException.class,
                         () -> airportServiceImpl.getAirportById(id));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AIRPORT_NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo(String.format(ErrorCode.AIRPORT_NOT_FOUND.getMessage(), id));

        verify(airportRepository).findById(id);
        verifyNoInteractions(airportMapper);
    }

    @Test
    void getAllAirports_ShouldReturnPaginatedResponses_WhenExists(){
        Page<Airport> airportPage = new PageImpl<>(List.of(airport));
        when(airportRepository.findAll(pageable)).thenReturn(airportPage);
        when(airportMapper.toAirportResponse(airport)).thenReturn(airportResponse);

        Page<AirportResponse> airportPageResult =  airportServiceImpl.getAllAirports(pageable);

        assertThat(airportPageResult.isEmpty()).isFalse();
        assertThat(airportPageResult.getContent()).hasSize(1);
        verify(airportRepository, times(1)).findAll(pageable);
        
    }

     @Test
    void getAllAirports_ShouldReturnPaginatedResponses_WhenIsEmpty(){
        Page<Airport> airportPage = new PageImpl<>(List.of());
        when(airportRepository.findAll(pageable)).thenReturn(airportPage);

        Page<AirportResponse> airportPageResult =  airportServiceImpl.getAllAirports(pageable);

        assertThat(airportPageResult.isEmpty()).isTrue();
        assertThat(airportPageResult.getContent()).hasSize(0);
        verify(airportRepository, times(1)).findAll(pageable);
        verifyNoInteractions(airportMapper);
    }

    @Test
    void createAirport_ShouldReturnResponse_WhenSuccessful(){
        when(airportRepository.save(airport)).thenReturn(airport);
        when(airportMapper.toAirport(airportRequest)).thenReturn(airport);
        when(airportMapper.toAirportResponse(airport)).thenReturn(airportResponse);

        AirportResponse airportResponseResult = airportServiceImpl.createAirport(airportRequest);
        
        assertNotNull(airportResponseResult);
        assertThat(airportResponseResult.id()).isEqualTo(1L);
        assertThat(airportResponseResult.code()).isEqualTo("JFK");

        verify(airportRepository).save(airport);
        verify(airportMapper).toAirportResponse(airport);
        verify(airportMapper).toAirport(airportRequest);
    }

    @Test
    void updateAirport_ShouldReturnResponse_WhenIsExists(){
        Long id = 1L;
        when(airportRepository.findById(id)).thenReturn(Optional.of(airport));
        when(airportRepository.save(airport)).thenReturn(airport);
        when(airportMapper.toAirportResponse(airport)).thenReturn(airportResponse);
        doNothing().when(airportMapper).updateAirportFromRecord(airportRequest, airport);
       
        AirportResponse airportResponseResult = airportServiceImpl.updateAirport(id, airportRequest);

        assertNotNull(airportResponseResult);
        assertThat(airportResponseResult.id()).isEqualTo(id);

        verify(airportRepository).findById(id);
        verify(airportMapper).updateAirportFromRecord(airportRequest, airport);
        verify(airportRepository).save(airport);

    }

     @Test
    void UpdateAirport_ShouldThrowException_WhenNotFound(){
        Long id = 9999L;
        when(airportRepository.findById(id)).thenReturn(Optional.empty());

       ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, 
                                             ()-> airportServiceImpl.updateAirport(id, airportRequest));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AIRPORT_NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo(String.format(ErrorCode.AIRPORT_NOT_FOUND.getMessage(), id));

        verifyNoInteractions(airportMapper);
    }

    @Test
    void deleteAirport_ShouldExecute_WhenIsExists() {
        Long id = 1L;
        when(airportRepository.existsById(id)).thenReturn(true);
        doNothing().when(airportRepository).deleteById(id);
        
        assertDoesNotThrow(() -> airportServiceImpl.deleteAirport(id));

        verify(airportRepository).existsById(id);
        verify(airportRepository).deleteById(id);
    }

    @Test
    void deleteAirport_ShouldThrowException_WhenNotFound() {
        Long id = 1L;
        when(airportRepository.existsById(id)).thenReturn(false);
        
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                                                         () -> airportServiceImpl.deleteAirport(id));

                                            
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AIRPORT_NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo(String.format(ErrorCode.AIRPORT_NOT_FOUND.getMessage(), id));
    }




    



}
