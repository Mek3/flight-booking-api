package com.aerolinea.flight_booking_api.services;

import com.aerolinea.flight_booking_api.dtos.booking.BookingRequest;
import com.aerolinea.flight_booking_api.dtos.booking.BookingDTO;

public interface BookingService {
    BookingDTO createBooking(BookingRequest request);
}
