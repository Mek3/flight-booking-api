package com.aerolinea.flight_booking_api.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aerolinea.flight_booking_api.dtos.aircraft.AircraftRequest;
import com.aerolinea.flight_booking_api.dtos.aircraft.AircraftResponse;
import com.aerolinea.flight_booking_api.exceptions.BusinessRuleViolationException;
import com.aerolinea.flight_booking_api.exceptions.ErrorCode;
import com.aerolinea.flight_booking_api.exceptions.ResourceNotFoundException;
import com.aerolinea.flight_booking_api.mappers.AircraftMapper;
import com.aerolinea.flight_booking_api.models.Aircraft;
import com.aerolinea.flight_booking_api.repositories.AircraftRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AircraftServiceImpl implements AircraftService {

    private final AircraftRepository aircraftRepository;
    private final AircraftMapper aircraftMapper;

    @Override
    public AircraftResponse getAircraftById(Long id) {
        return aircraftMapper.toAircraftResponse(aircraftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.AIRCRAFT_NOT_FOUND,
                        String.format(ErrorCode.AIRCRAFT_NOT_FOUND.getMessage(), id))));
    }

    @Override
    public Page<AircraftResponse> getAllAircraft(Pageable pageable) {
        return aircraftRepository.findAll(pageable).map(aircraftMapper::toAircraftResponse);
    }

    @Override
    @Transactional
    public AircraftResponse createAircraft(AircraftRequest aircraftRequest) {
        aircraftExistsByRegistrationNumber(aircraftRequest.registrationNumber());

        Aircraft aircraft = aircraftRepository.save(aircraftMapper.toAircraft(aircraftRequest));

        log.info("Aircraft successfully created. Registration Number: {} | Aircraft ID: {}",
                aircraft.getRegistrationNumber(), aircraft.getId());

        return aircraftMapper.toAircraftResponse(aircraft);
    }

    @Override
    @Transactional
    public AircraftResponse updateAircraft(Long id, AircraftRequest aircraftRequest) {
        Aircraft aircraft = aircraftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.AIRCRAFT_NOT_FOUND,
                        String.format(ErrorCode.AIRCRAFT_NOT_FOUND.getMessage(), id)));

        if (!aircraft.getRegistrationNumber().equals(aircraftRequest.registrationNumber())) {
            aircraftExistsByRegistrationNumber(aircraftRequest.registrationNumber());
        }

        aircraftMapper.updateAircraftFromRequest(aircraftRequest, aircraft);

        log.info("Aircraft successfully updated. Registration Number: {} | Aircraft ID: {}",
                aircraft.getRegistrationNumber(), aircraft.getId());

        return aircraftMapper.toAircraftResponse(aircraft);
    }

    @Override
    @Transactional
    public void deleteAircraft(Long id) {
        if (!aircraftRepository.existsById(id)) {
            throw new ResourceNotFoundException(ErrorCode.AIRCRAFT_NOT_FOUND,
                    String.format(ErrorCode.AIRCRAFT_NOT_FOUND.getMessage(), id));
        }

        aircraftRepository.deleteById(id);

        log.info("Aircraft successfully deleted. Aircraft ID: {}", id);
    }

    private void aircraftExistsByRegistrationNumber(String registrationNumber) {
        if (aircraftRepository.existsByRegistrationNumber(registrationNumber)) {
            throw new BusinessRuleViolationException(ErrorCode.AIRCRAFT_ALREADY_EXISTS,
                    String.format(ErrorCode.AIRCRAFT_ALREADY_EXISTS.getMessage(), registrationNumber));
        }
    }
}
