package com.aerolinea.flight_booking_api.domain.aircraftmodel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

import com.aerolinea.flight_booking_api.dtos.aircraftmodel.AircraftModelRequest;
import com.aerolinea.flight_booking_api.dtos.aircraftmodel.AircraftModelResponse;
import com.aerolinea.flight_booking_api.exceptions.BusinessRuleViolationException;
import com.aerolinea.flight_booking_api.exceptions.ErrorCode;
import com.aerolinea.flight_booking_api.exceptions.ResourceNotFoundException;
import com.aerolinea.flight_booking_api.mappers.AircraftModelMapper;
import com.aerolinea.flight_booking_api.models.AircraftModel;
import com.aerolinea.flight_booking_api.repositories.AircraftModelRepository;
import com.aerolinea.flight_booking_api.services.AircraftModelServiceImpl;

@ExtendWith(MockitoExtension.class)
class AircraftModelServiceImplTest {

    @Mock
    private AircraftModelRepository aircraftModelRepository;

    @Mock
    private AircraftModelMapper aircraftModelMapper;

    @InjectMocks
    private AircraftModelServiceImpl aircraftModelService;

    private AircraftModel mockAircraftModel;
    private AircraftModelRequest mockRequest;
    private AircraftModelResponse mockResponse;

    @BeforeEach
    void setUp() {
        mockAircraftModel = AircraftModel.builder()
                .manufacturer("Airbus")
                .modelName("A320neo")
                .maxCapacity((short) 195)
                .build();

        mockRequest = new AircraftModelRequest("Airbus", "A320neo", (short) 195);
        mockResponse = new AircraftModelResponse(1L, "Airbus", "A320neo", (short) 195);
    }

    @Test
    void shouldReturnAircraftModelWhenIdExists() {
        Long id = 1L;
        when(aircraftModelRepository.findById(id)).thenReturn(Optional.of(mockAircraftModel));
        when(aircraftModelMapper.toAircraftModelResponse(mockAircraftModel)).thenReturn(mockResponse);

        AircraftModelResponse result = aircraftModelService.getAircraftModelById(id);

        assertThat(result).isNotNull();
        assertThat(result.manufacturer()).isEqualTo("Airbus");
        assertThat(result.modelName()).isEqualTo("A320neo");
        verify(aircraftModelRepository).findById(id);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {
        Long invalidId = 99L;
        when(aircraftModelRepository.findById(invalidId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, 
                                                    () -> aircraftModelService.getAircraftModelById(invalidId));
                
        verify(aircraftModelMapper, never()).toAircraftModelResponse(any());
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AIRCRAFT_MODEL_NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo(String.format(ErrorCode.AIRCRAFT_MODEL_NOT_FOUND.getMessage(), invalidId));
    }

    @Test
    void shouldReturnPagedAircraftModels() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<AircraftModel> mockPage = new PageImpl<>(List.of(mockAircraftModel));
        when(aircraftModelRepository.findAll(pageable)).thenReturn(mockPage);
        when(aircraftModelMapper.toAircraftModelResponse(mockAircraftModel)).thenReturn(mockResponse);

        Page<AircraftModelResponse> result = aircraftModelService.getAllAircraftModels(pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).manufacturer()).isEqualTo("Airbus");
        verify(aircraftModelRepository).findAll(pageable);
    }

    @Test
    void shouldCreateAircraftModelWhenNotDuplicate() {
        when(aircraftModelRepository.existsAircraftModelByManufacturerAndModelName(
                mockRequest.manufacturer(), mockRequest.modelName())).thenReturn(false);
        when(aircraftModelMapper.toAircraftModel(mockRequest)).thenReturn(mockAircraftModel);
        when(aircraftModelRepository.save(mockAircraftModel)).thenReturn(mockAircraftModel);
        when(aircraftModelMapper.toAircraftModelResponse(mockAircraftModel)).thenReturn(mockResponse);

        AircraftModelResponse result = aircraftModelService.createAircraftModel(mockRequest);

        assertThat(result).isNotNull();
        assertThat(result.manufacturer()).isEqualTo("Airbus");
        assertThat(result.modelName()).isEqualTo("A320neo");
        verify(aircraftModelRepository).save(any(AircraftModel.class));
    }

    @Test
    void shouldThrowExceptionWhenCreatingDuplicateAircraftModel() {
        when(aircraftModelRepository.existsAircraftModelByManufacturerAndModelName(
                mockRequest.manufacturer(), mockRequest.modelName())).thenReturn(true);

        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class, 
                () -> aircraftModelService.createAircraftModel(mockRequest));
                
        verify(aircraftModelMapper, never()).toAircraftModelResponse(any());
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AIRCRAFT_MODEL_ALREADY_EXISTS);
        assertThat(exception.getMessage()).isEqualTo(String.format(ErrorCode.AIRCRAFT_MODEL_ALREADY_EXISTS.getMessage(), 
                                                        mockRequest.manufacturer(), mockRequest.modelName()));

        verify(aircraftModelRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentId() {
        Long invalidId = 99L;
        when(aircraftModelRepository.findById(invalidId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, 
                                                    () -> aircraftModelService.updateAircraftModel(invalidId, mockRequest));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AIRCRAFT_MODEL_NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo(String.format(ErrorCode.AIRCRAFT_MODEL_NOT_FOUND.getMessage(), invalidId));

        verify(aircraftModelMapper, never()).updateAircraftModelFromAircraftModelRequest(any(), any());
    }

    @Test
    void shouldUpdateWithoutDuplicateCheckWhenNameAndManufacturerAreUnchanged() {
        Long id = 1L;
        when(aircraftModelRepository.findById(id)).thenReturn(Optional.of(mockAircraftModel));
        when(aircraftModelMapper.toAircraftModelResponse(mockAircraftModel)).thenReturn(mockResponse);

        AircraftModelResponse result = aircraftModelService.updateAircraftModel(id, mockRequest);

        assertThat(result).isNotNull();
        verify(aircraftModelRepository, never()).existsAircraftModelByManufacturerAndModelName(any(), any());
        verify(aircraftModelMapper).updateAircraftModelFromAircraftModelRequest(mockRequest, mockAircraftModel);
    }

    @Test
    void shouldUpdateAndPerformDuplicateCheckWhenNameIsChangedAndNotDuplicate() {
        Long id = 1L;
        AircraftModelRequest updatedRequest = new AircraftModelRequest("Airbus", "A321XLR", (short) 220);
        when(aircraftModelRepository.findById(id)).thenReturn(Optional.of(mockAircraftModel));
        when(aircraftModelRepository.existsAircraftModelByManufacturerAndModelName("Airbus", "A321XLR")).thenReturn(false);
        when(aircraftModelMapper.toAircraftModelResponse(mockAircraftModel)).thenReturn(mockResponse);

        AircraftModelResponse result = aircraftModelService.updateAircraftModel(id, updatedRequest);

        assertThat(result).isNotNull();
        verify(aircraftModelRepository).existsAircraftModelByManufacturerAndModelName("Airbus", "A321XLR");
        verify(aircraftModelMapper).updateAircraftModelFromAircraftModelRequest(updatedRequest, mockAircraftModel);
    }

    @Test
    void shouldThrowExceptionWhenUpdateCausesDuplicateCollision() {
        Long id = 1L;
        AircraftModelRequest updatedRequest = new AircraftModelRequest("Airbus", "A321XLR", (short) 220);
        when(aircraftModelRepository.findById(id)).thenReturn(Optional.of(mockAircraftModel));
        when(aircraftModelRepository.existsAircraftModelByManufacturerAndModelName("Airbus", "A321XLR")).thenReturn(true);

        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class, 
                                                    () -> aircraftModelService.updateAircraftModel(id, updatedRequest));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AIRCRAFT_MODEL_ALREADY_EXISTS);
        assertThat(exception.getMessage()).isEqualTo(String.format(ErrorCode.AIRCRAFT_MODEL_ALREADY_EXISTS.getMessage(), "Airbus", "A321XLR"));

        verify(aircraftModelMapper, never()).updateAircraftModelFromAircraftModelRequest(any(), any());
    }


    @Test
    void shouldDeleteWhenIdExists() {
        Long id = 1L;
        when(aircraftModelRepository.existsById(id)).thenReturn(true);

        aircraftModelService.deleteAircraftModel(id);

        verify(aircraftModelRepository).deleteById(id);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentId() {
        Long invalidId = 99L;
        when(aircraftModelRepository.existsById(invalidId)).thenReturn(false);
       
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, 
                                                    () -> aircraftModelService.deleteAircraftModel(invalidId));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AIRCRAFT_MODEL_NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo(String.format(ErrorCode.AIRCRAFT_MODEL_NOT_FOUND.getMessage(), invalidId));

        verify(aircraftModelRepository, never()).deleteById(any());
    }
}