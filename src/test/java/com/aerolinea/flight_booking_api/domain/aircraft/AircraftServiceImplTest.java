package com.aerolinea.flight_booking_api.domain.aircraft;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
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

import com.aerolinea.flight_booking_api.dtos.aircraft.AircraftRequest;
import com.aerolinea.flight_booking_api.dtos.aircraft.AircraftResponse;
import com.aerolinea.flight_booking_api.exceptions.BusinessRuleViolationException;
import com.aerolinea.flight_booking_api.exceptions.ErrorCode;
import com.aerolinea.flight_booking_api.exceptions.ResourceNotFoundException;
import com.aerolinea.flight_booking_api.mappers.AircraftMapper;
import com.aerolinea.flight_booking_api.models.Aircraft;
import com.aerolinea.flight_booking_api.models.AircraftModel;
import com.aerolinea.flight_booking_api.repositories.AircraftRepository;
import com.aerolinea.flight_booking_api.services.AircraftServiceImpl;

@ExtendWith(MockitoExtension.class)
public class AircraftServiceImplTest {

    @Mock
    private AircraftRepository aircraftRepository;

    @Mock
    private AircraftMapper aircraftMapper;

    @InjectMocks
    private AircraftServiceImpl aircraftService;

    private Pageable pageable;
    private Aircraft aircraft;
    private AircraftRequest aircraftRequest;
    private AircraftResponse aircraftResponse;
    private AircraftModel aircraftModel;

    @BeforeEach
    void setUp() {
        aircraftModel = AircraftModel.builder()
                .manufacturer("Boeing")
                .modelName("737 MAX")
                .maxCapacity((short) 200)
                .build();
        ReflectionTestUtils.setField(aircraftModel, "id", 10L);

        aircraft = Aircraft.builder()
                .registrationNumber("EC-LOK")
                .totalFlightHours(5000)
                .aircraftModel(aircraftModel)
                .build();
        ReflectionTestUtils.setField(aircraft, "id", 1L);

        aircraftRequest = new AircraftRequest("EC-LOK", 5000, 10L);
        aircraftResponse = new AircraftResponse(1L, "EC-LOK", 5000, 10L);

        pageable = PageRequest.of(0, 10);
    }

    @Test
    void getAircraftById_ShouldReturnResponse_WhenExists() {
        Long id = 1L;
        when(aircraftRepository.findById(id)).thenReturn(Optional.of(aircraft));
        when(aircraftMapper.toAircraftResponse(aircraft)).thenReturn(aircraftResponse);

        AircraftResponse result = aircraftService.getAircraftById(id);
        
        assertNotNull(result);
        assertThat(result.registrationNumber()).isEqualTo("EC-LOK");
        assertThat(result.id()).isEqualTo(id); 

        verify(aircraftRepository).findById(id);
        verify(aircraftMapper).toAircraftResponse(aircraft);
    }

    @Test
    void getAircraftById_ShouldThrowException_WhenNotFound() {
        Long id = 9999L;
        when(aircraftRepository.findById(id)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> aircraftService.getAircraftById(id));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AIRCRAFT_NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo(String.format(ErrorCode.AIRCRAFT_NOT_FOUND.getMessage(), id));

        verify(aircraftRepository).findById(id);
        verifyNoInteractions(aircraftMapper);
    }

    @Test
    void getAllAircraft_ShouldReturnPaginatedResponses_WhenExists() {
        Page<Aircraft> aircraftPage = new PageImpl<>(List.of(aircraft));
        when(aircraftRepository.findAll(pageable)).thenReturn(aircraftPage);
        when(aircraftMapper.toAircraftResponse(aircraft)).thenReturn(aircraftResponse);

        Page<AircraftResponse> result = aircraftService.getAllAircraft(pageable);

        assertThat(result.isEmpty()).isFalse();
        assertThat(result.getContent()).hasSize(1);
        verify(aircraftRepository, times(1)).findAll(pageable);
    }

    @Test
    void createAircraft_ShouldReturnResponse_WhenSuccessful() {
        when(aircraftRepository.existsByRegistrationNumber(aircraftRequest.registrationNumber())).thenReturn(false);
        when(aircraftMapper.toAircraft(aircraftRequest)).thenReturn(aircraft);
        when(aircraftRepository.save(aircraft)).thenReturn(aircraft);
        when(aircraftMapper.toAircraftResponse(aircraft)).thenReturn(aircraftResponse);

        AircraftResponse result = aircraftService.createAircraft(aircraftRequest);
        
        assertNotNull(result);
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.registrationNumber()).isEqualTo("EC-LOK");

        verify(aircraftRepository).existsByRegistrationNumber(aircraftRequest.registrationNumber());
        verify(aircraftRepository).save(aircraft);
    }

    @Test
    void createAircraft_ShouldThrowException_WhenRegistrationNumberAlreadyExists() {
        when(aircraftRepository.existsByRegistrationNumber(aircraftRequest.registrationNumber())).thenReturn(true);

        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class,
                () -> aircraftService.createAircraft(aircraftRequest));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AIRCRAFT_ALREADY_EXISTS);
        verify(aircraftRepository, never()).save(any());
        verifyNoInteractions(aircraftMapper);
    }

    @Test
    void updateAircraft_ShouldReturnResponse_WhenExistsAndRegistrationNotChanged() {
        Long id = 1L;
        when(aircraftRepository.findById(id)).thenReturn(Optional.of(aircraft));
        doNothing().when(aircraftMapper).updateAircraftFromRequest(aircraftRequest, aircraft);
        when(aircraftMapper.toAircraftResponse(aircraft)).thenReturn(aircraftResponse);
       
        AircraftResponse result = aircraftService.updateAircraft(id, aircraftRequest);

        assertNotNull(result);
        assertThat(result.id()).isEqualTo(id);

        verify(aircraftRepository).findById(id);
        verify(aircraftMapper).updateAircraftFromRequest(aircraftRequest, aircraft);
        verify(aircraftRepository, never()).existsByRegistrationNumber(any()); 
    }

    @Test
    void updateAircraft_ShouldThrowException_WhenChangingRegistrationToAnExistingOne() {
        Long id = 1L;
        AircraftRequest updateRequest = new AircraftRequest("NEW-REG", 6000, 10L);
        when(aircraftRepository.findById(id)).thenReturn(Optional.of(aircraft)); // La BD tiene "EC-LOK"
        when(aircraftRepository.existsByRegistrationNumber("NEW-REG")).thenReturn(true); // "NEW-REG" ya existe

        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class,
                () -> aircraftService.updateAircraft(id, updateRequest));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AIRCRAFT_ALREADY_EXISTS);
        verify(aircraftMapper, never()).updateAircraftFromRequest(any(), any());
    }

    @Test
    void deleteAircraft_ShouldExecute_WhenExists() {
        Long id = 1L;
        when(aircraftRepository.existsById(id)).thenReturn(true);
        doNothing().when(aircraftRepository).deleteById(id);
        
        assertDoesNotThrow(() -> aircraftService.deleteAircraft(id));

        verify(aircraftRepository).existsById(id);
        verify(aircraftRepository).deleteById(id);
    }

    @Test
    void deleteAircraft_ShouldThrowException_WhenNotFound() {
        Long id = 1L;
        when(aircraftRepository.existsById(id)).thenReturn(false);
        
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> aircraftService.deleteAircraft(id));
                                            
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AIRCRAFT_NOT_FOUND);
        verify(aircraftRepository, never()).deleteById(any());
    }
}