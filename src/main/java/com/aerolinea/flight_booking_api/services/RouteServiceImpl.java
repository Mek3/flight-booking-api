package com.aerolinea.flight_booking_api.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aerolinea.flight_booking_api.dtos.route.RouteRequest;
import com.aerolinea.flight_booking_api.dtos.route.RouteResponse;
import com.aerolinea.flight_booking_api.exceptions.BusinessRuleViolationException;
import com.aerolinea.flight_booking_api.exceptions.ErrorCode;
import com.aerolinea.flight_booking_api.exceptions.ResourceNotFoundException;
import com.aerolinea.flight_booking_api.mappers.RouteMapper;
import com.aerolinea.flight_booking_api.models.Route;
import com.aerolinea.flight_booking_api.repositories.RouteRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RouteServiceImpl implements RouteService {

    private final RouteRepository routeRepository;
    private final RouteMapper routeMapper;

    @Override
    public RouteResponse getRouteById(Long id) {
        return routeMapper.toRouteResponse(routeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ROUTE_NOT_FOUND,
                        String.format(ErrorCode.ROUTE_NOT_FOUND.getMessage(), id))));
    }

    @Override
    public Page<RouteResponse> getAllRoutes(Pageable pageable) {
        return routeRepository.findAll(pageable).map(routeMapper::toRouteResponse);
    }

    @Override
    @Transactional
    public RouteResponse createRoute(RouteRequest routeRequest) {
        validateDifferentAirports(routeRequest);
        validateRouteDoesNotExist(routeRequest);

        Route route = routeRepository.save(routeMapper.toRoute(routeRequest));

        log.info("Route successfully created. Origin ID: {} | Destination ID: {} | Route ID: {}",
                routeRequest.originAirportId(), routeRequest.destinationAirportId(), route.getId());

        return routeMapper.toRouteResponse(route);
    }

    @Override
    @Transactional
    public RouteResponse updateRoute(Long id, RouteRequest routeRequest) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ROUTE_NOT_FOUND,
                        String.format(ErrorCode.ROUTE_NOT_FOUND.getMessage(), id)));

        boolean isOriginChanged = !route.getOriginAirport().getId().equals(routeRequest.originAirportId());
        boolean isDestinationChanged = !route.getDestinationAirport().getId().equals(routeRequest.destinationAirportId());

        if (isOriginChanged || isDestinationChanged) {
            validateDifferentAirports(routeRequest);
            validateRouteDoesNotExist(routeRequest);
        }

        routeMapper.updateRouteFromRequest(routeRequest, route);

        log.info("Route successfully updated. Origin ID: {} | Destination ID: {} | Route ID: {}",
                routeRequest.originAirportId(), routeRequest.destinationAirportId(), route.getId());

        return routeMapper.toRouteResponse(route);
    }

    @Override
    @Transactional
    public void deleteRoute(Long id) {
        if (!routeRepository.existsById(id)) {
            throw new ResourceNotFoundException(ErrorCode.ROUTE_NOT_FOUND,
                    String.format(ErrorCode.ROUTE_NOT_FOUND.getMessage(), id));
        }

        routeRepository.deleteById(id);

        log.info("Route successfully deleted. Route ID: {}", id);
    }


    private void validateDifferentAirports(RouteRequest routeRequest) {
        if (routeRequest.originAirportId().equals(routeRequest.destinationAirportId())) {
            throw new BusinessRuleViolationException(ErrorCode.ROUTE_SAME_ORIGIN_DESTINATION,
                    String.format(ErrorCode.ROUTE_SAME_ORIGIN_DESTINATION.getMessage(), routeRequest.originAirportId()));
        }
    }

    private void validateRouteDoesNotExist(RouteRequest routeRequest) {
        if (routeRepository.existsByOriginAirportIdAndDestinationAirportId(
                routeRequest.originAirportId(), routeRequest.destinationAirportId())) {
            throw new BusinessRuleViolationException(ErrorCode.ROUTE_ALREADY_EXISTS,
                    String.format(ErrorCode.ROUTE_ALREADY_EXISTS.getMessage(),
                            routeRequest.originAirportId(), routeRequest.destinationAirportId()));
        }
    }
}