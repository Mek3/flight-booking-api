package com.aerolinea.flight_booking_api.domain.seat;

import com.aerolinea.flight_booking_api.repositories.SeatRepository;
import com.aerolinea.flight_booking_api.services.SeatServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeatServiceImplTest {

    @Mock
    private SeatRepository seatRepository;

    @InjectMocks
    private SeatServiceImpl seatService;

    @Test
    void shouldReturnInsertedCountWhenAllRowsSucceed() {
        when(seatRepository.batchInsertSeats(42L, 2, "AB")).thenReturn(new int[]{1, 1, 1, 1});

        int result = seatService.generateSeatsForInstance(42L, 2, "AB");

        assertThat(result).isEqualTo(4);
    }

    @Test
    void shouldReturnOnlyActuallyInsertedCountWhenSomeRowsAreIgnored() {
        when(seatRepository.batchInsertSeats(42L, 1, "AB")).thenReturn(new int[]{1, 0});

        int result = seatService.generateSeatsForInstance(42L, 1, "AB");

        assertThat(result)
                .as("1 of 2 rows was ignored as a duplicate — the actual count must be 1, not 2")
                .isEqualTo(1);
    }

    @Test
    void shouldReturnZeroWhenAllRowsAreIgnored() {
        when(seatRepository.batchInsertSeats(42L, 1, "AB")).thenReturn(new int[]{0, 0});

        int result = seatService.generateSeatsForInstance(42L, 1, "AB");

        assertThat(result).isZero();
    }
}
