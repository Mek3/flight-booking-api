package com.aerolinea.flight_booking_api.dtos.aircraftmodel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AircraftModelRequest(

    @NotBlank(message = "Manufacturer is mandatory") 
    @Size(max = 50, message= "Manufacturer cannot exceed 50 characters") 
    String manufacturer,
    
    @NotBlank(message = "Model name is mandatory") 
    @Size(max = 50, message = "Model name cannot exceed 50 characters")
    String modelName,
    
    @NotNull(message = "Max capacity is mandatory")
    @Positive(message = "Max capacity must be a positive number")
    Short maxCapacity
) {

}
