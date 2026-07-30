package com.aerolinea.flight_booking_api.domain.route;

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

import com.aerolinea.flight_booking_api.dtos.airport.AirportResponse;
import com.aerolinea.flight_booking_api.dtos.route.RouteRequest;
import com.aerolinea.flight_booking_api.dtos.route.RouteResponse;
import com.aerolinea.flight_booking_api.exceptions.BusinessRuleViolationException;
import com.aerolinea.flight_booking_api.exceptions.ErrorCode;
import com.aerolinea.flight_booking_api.exceptions.ResourceNotFoundException;
import com.aerolinea.flight_booking_api.mappers.RouteMapper;
import com.aerolinea.flight_booking_api.models.Airport;
import com.aerolinea.flight_booking_api.models.Route;
import com.aerolinea.flight_booking_api.repositories.RouteRepository;
import com.aerolinea.flight_booking_api.services.RouteServiceImpl;

@ExtendWith(MockitoExtension.class)
public class RouteServiceImplTest {

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private RouteMapper routeMapper;

    @InjectMocks
    private RouteServiceImpl routeService;

    private Pageable pageable;
    private Airport originAirport;
    private Airport destinationAirport;
    private Route route;
    private RouteRequest routeRequest;
    private RouteResponse routeResponse;

    @BeforeEach
    void setUp() {
        originAirport = Airport.builder()
                .code("MAD")
                .name("Adolfo Suárez")
                .city("Madrid")
                .country("Spain")
                .build();
        ReflectionTestUtils.setField(originAirport, "id", 10L);

        destinationAirport = Airport.builder()
                .code("JFK")
                .name("John F. Kennedy")
                .city("New York")
                .country("USA")
                .build();
        ReflectionTestUtils.setField(destinationAirport, "id", 20L);

        route = Route.builder()
                .originAirport(originAirport)
                .destinationAirport(destinationAirport)
                .build();
        ReflectionTestUtils.setField(route, "id", 1L);

        routeRequest = new RouteRequest(10L, 20L);
        
        AirportResponse originResponse = new AirportResponse(10L, "MAD", "Adolfo Suárez", "Madrid", "Spain");
        AirportResponse destinationResponse = new AirportResponse(20L, "JFK", "John F. Kennedy", "New York", "USA");
        routeResponse = new RouteResponse(1L, originResponse, destinationResponse);

        pageable = PageRequest.of(0, 10);
    }

    @Test
    void getRouteById_ShouldReturnResponse_WhenExists() {
        Long id = 1L;
        when(routeRepository.findById(id)).thenReturn(Optional.of(route));
        when(routeMapper.toRouteResponse(route)).thenReturn(routeResponse);

        RouteResponse result = routeService.getRouteById(id);
        
        assertNotNull(result);
        assertThat(result.id()).isEqualTo(id);
        assertThat(result.originAirport().code()).isEqualTo("MAD");
        assertThat(result.destinationAirport().code()).isEqualTo("JFK");

        verify(routeRepository).findById(id);
        verify(routeMapper).toRouteResponse(route);
    }

    @Test
    void getRouteById_ShouldThrowException_WhenNotFound() {
        Long id = 9999L;
        when(routeRepository.findById(id)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> routeService.getRouteById(id));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ROUTE_NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo(String.format(ErrorCode.ROUTE_NOT_FOUND.getMessage(), id));

        verify(routeRepository).findById(id);
        verifyNoInteractions(routeMapper);
    }

    @Test
    void getAllRoutes_ShouldReturnPaginatedResponses_WhenExists() {
        Page<Route> routePage = new PageImpl<>(List.of(route));
        when(routeRepository.findAll(pageable)).thenReturn(routePage);
        when(routeMapper.toRouteResponse(route)).thenReturn(routeResponse);

        Page<RouteResponse> result = routeService.getAllRoutes(pageable);

        assertThat(result.isEmpty()).isFalse();
        assertThat(result.getContent()).hasSize(1);
        verify(routeRepository, times(1)).findAll(pageable);
    }

    @Test
    void createRoute_ShouldReturnResponse_WhenSuccessful() {
        when(routeRepository.existsByOriginAirportIdAndDestinationAirportId(10L, 20L)).thenReturn(false);
        when(routeMapper.toRoute(routeRequest)).thenReturn(route);
        when(routeRepository.save(route)).thenReturn(route);
        when(routeMapper.toRouteResponse(route)).thenReturn(routeResponse);

        RouteResponse result = routeService.createRoute(routeRequest);
        
        assertNotNull(result);
        assertThat(result.id()).isEqualTo(1L);

        verify(routeRepository).existsByOriginAirportIdAndDestinationAirportId(10L, 20L);
        verify(routeRepository).save(route);
    }

    @Test
    void createRoute_ShouldThrowException_WhenOriginAndDestinationAreTheSame() {
        RouteRequest invalidRequest = new RouteRequest(10L, 10L);

        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class,
                () -> routeService.createRoute(invalidRequest));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ROUTE_SAME_ORIGIN_DESTINATION);
        verify(routeRepository, never()).save(any());
        verifyNoInteractions(routeMapper);
    }

    @Test
    void createRoute_ShouldThrowException_WhenRouteAlreadyExists() {
        when(routeRepository.existsByOriginAirportIdAndDestinationAirportId(10L, 20L)).thenReturn(true);

        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class,
                () -> routeService.createRoute(routeRequest));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ROUTE_ALREADY_EXISTS);
        verify(routeRepository, never()).save(any());
        verifyNoInteractions(routeMapper);
    }

    @Test
    void updateRoute_ShouldReturnResponse_WhenExistsAndAirportsNotChanged() {
        Long id = 1L;
        when(routeRepository.findById(id)).thenReturn(Optional.of(route));
        doNothing().when(routeMapper).updateRouteFromRequest(routeRequest, route);
        when(routeMapper.toRouteResponse(route)).thenReturn(routeResponse);
       
        RouteResponse result = routeService.updateRoute(id, routeRequest);

        assertNotNull(result);
        assertThat(result.id()).isEqualTo(id);

        verify(routeRepository).findById(id);
        verify(routeMapper).updateRouteFromRequest(routeRequest, route);
        verify(routeRepository, never()).existsByOriginAirportIdAndDestinationAirportId(any(), any()); 
    }

    @Test
    void updateRoute_ShouldThrowException_WhenChangingToSameAirports() {
        Long id = 1L;
        RouteRequest updateRequest = new RouteRequest(10L, 10L); 
        when(routeRepository.findById(id)).thenReturn(Optional.of(route));

        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class,
                () -> routeService.updateRoute(id, updateRequest));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ROUTE_SAME_ORIGIN_DESTINATION);
        verify(routeMapper, never()).updateRouteFromRequest(any(), any());
    }

    @Test
    void updateRoute_ShouldThrowException_WhenChangingToExistingRoute() {
        Long id = 1L;
        RouteRequest updateRequest = new RouteRequest(10L, 30L); 
        when(routeRepository.findById(id)).thenReturn(Optional.of(route));
        when(routeRepository.existsByOriginAirportIdAndDestinationAirportId(10L, 30L)).thenReturn(true); 

        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class,
                () -> routeService.updateRoute(id, updateRequest));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ROUTE_ALREADY_EXISTS);
        verify(routeMapper, never()).updateRouteFromRequest(any(), any());
    }

    @Test
    void deleteRoute_ShouldExecute_WhenExists() {
        Long id = 1L;
        when(routeRepository.existsById(id)).thenReturn(true);
        doNothing().when(routeRepository).deleteById(id);
        
        assertDoesNotThrow(() -> routeService.deleteRoute(id));

        verify(routeRepository).existsById(id);
        verify(routeRepository).deleteById(id);
    }

    @Test
    void deleteRoute_ShouldThrowException_WhenNotFound() {
        Long id = 1L;
        when(routeRepository.existsById(id)).thenReturn(false);
        
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> routeService.deleteRoute(id));
                                                            
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ROUTE_NOT_FOUND);
        verify(routeRepository, never()).deleteById(any());
    }
}