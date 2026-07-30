package com.aerolinea.flight_booking_api.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.aerolinea.flight_booking_api.dtos.route.RouteRequest;
import com.aerolinea.flight_booking_api.dtos.route.RouteResponse;

public interface RouteService {
    RouteResponse getRouteById(Long id);
    Page<RouteResponse> getAllRoutes(Pageable pageable);
    RouteResponse createRoute(RouteRequest routeRequest);
    RouteResponse updateRoute(Long id, RouteRequest routeRequest);
    void deleteRoute(Long id);
}