package com.aerolinea.flight_booking_api.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aerolinea.flight_booking_api.dtos.aircraftmodel.AircraftModelRequest;
import com.aerolinea.flight_booking_api.dtos.aircraftmodel.AircraftModelResponse;
import com.aerolinea.flight_booking_api.exceptions.BusinessRuleViolationException;
import com.aerolinea.flight_booking_api.exceptions.ErrorCode;
import com.aerolinea.flight_booking_api.exceptions.ResourceNotFoundException;
import com.aerolinea.flight_booking_api.mappers.AircraftModelMapper;
import com.aerolinea.flight_booking_api.models.AircraftModel;
import com.aerolinea.flight_booking_api.repositories.AircraftModelRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AircraftModelServiceImpl implements AircraftModelService {

    private final AircraftModelRepository aircraftModelRepository;
    private final AircraftModelMapper aircraftModelMapper;

    @Override
    public AircraftModelResponse getAircraftModelById(Long id) {
        return aircraftModelMapper.toAircraftModelResponse(aircraftModelRepository.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException(ErrorCode.AIRCRAFT_MODEL_NOT_FOUND,
                                String.format(ErrorCode.AIRCRAFT_MODEL_NOT_FOUND.getMessage(), id))));
    }

    @Override
    public Page<AircraftModelResponse> getAllAircraftModels(Pageable pageable) {
       return aircraftModelRepository.findAll(pageable).map(aircraftModelMapper::toAircraftModelResponse);
    }

    @Override
    @Transactional
    public AircraftModelResponse createAircraftModel(AircraftModelRequest aircraftModelRequest) {
        aircraftModelExistsByManufacturerAndModelName(aircraftModelRequest);

        AircraftModel aircraftModel = aircraftModelRepository.save(aircraftModelMapper.toAircraftModel(aircraftModelRequest));

        log.info("AircraftModel successfully created. manufacturer: {} | modelName: {} | AircraftModel ID: {}",
                aircraftModel.getManufacturer(), aircraftModel.getModelName(), aircraftModel.getId());

        return aircraftModelMapper.toAircraftModelResponse(aircraftModel);
    }

    @Override
    @Transactional
    public AircraftModelResponse updateAircraftModel(Long id, AircraftModelRequest aircraftModelRequest) {

        AircraftModel aircraftModel = aircraftModelRepository.findById(id)
                                        .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.AIRCRAFT_MODEL_NOT_FOUND,
                                                        String.format(ErrorCode.AIRCRAFT_MODEL_NOT_FOUND.getMessage(), id)));

        if(!aircraftModel.getManufacturer().equals(aircraftModelRequest.manufacturer())
            || !aircraftModel.getModelName().equals(aircraftModelRequest.modelName()) ) {

            aircraftModelExistsByManufacturerAndModelName(aircraftModelRequest);
        }

       aircraftModelMapper.updateAircraftModelFromAircraftModelRequest(aircraftModelRequest, aircraftModel);

       log.info("AircraftModel successfully Updated. manufacturer: {} | modelName: {} | AircraftModel ID: {}",
                aircraftModel.getManufacturer(), aircraftModel.getModelName(), aircraftModel.getId());

        return aircraftModelMapper.toAircraftModelResponse(aircraftModel);
    }

    @Override
    @Transactional
    public void deleteAircraftModel(Long id) {
        if(!aircraftModelRepository.existsById(id)) {
            throw new ResourceNotFoundException(ErrorCode.AIRCRAFT_MODEL_NOT_FOUND,
                                String.format(ErrorCode.AIRCRAFT_MODEL_NOT_FOUND.getMessage(), id));
        }

        aircraftModelRepository.deleteById(id);

        log.info("AircraftModel successfully deleted. AircraftModel ID: {}", id);

    }

    private void aircraftModelExistsByManufacturerAndModelName(AircraftModelRequest aircraftModelRequest) {
        if(aircraftModelRepository.existsAircraftModelByManufacturerAndModelName(aircraftModelRequest.manufacturer(), aircraftModelRequest.modelName())) {
            throw new BusinessRuleViolationException(ErrorCode.AIRCRAFT_MODEL_ALREADY_EXISTS,
                             String.format(ErrorCode.AIRCRAFT_MODEL_ALREADY_EXISTS.getMessage(), 
                             aircraftModelRequest.manufacturer(), aircraftModelRequest.modelName()));
        }
    }

}
