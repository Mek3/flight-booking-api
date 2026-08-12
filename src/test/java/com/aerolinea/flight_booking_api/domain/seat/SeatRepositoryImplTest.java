package com.aerolinea.flight_booking_api.domain.seat;

import com.aerolinea.flight_booking_api.repositories.SeatRepositoryImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatRepositoryImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private SeatRepositoryImpl seatRepository;

    @Test
    void shouldBuildOneBatchEntryPerRowAndLetterCombination() {
        when(jdbcTemplate.batchUpdate(anyString(), any(BatchPreparedStatementSetter.class)))
                .thenReturn(new int[]{1, 1, 1, 1, 1, 1});

        seatRepository.batchInsertSeats(42L, 3, "AB");

        ArgumentCaptor<BatchPreparedStatementSetter> setterCaptor = ArgumentCaptor.forClass(BatchPreparedStatementSetter.class);
        verify(jdbcTemplate).batchUpdate(anyString(), setterCaptor.capture());

        assertThat(setterCaptor.getValue().getBatchSize()).isEqualTo(6);
    }

    @Test
    void shouldSetCorrectValuesForFirstAndLastSeatInBatch() throws SQLException {
        when(jdbcTemplate.batchUpdate(anyString(), any(BatchPreparedStatementSetter.class)))
                .thenReturn(new int[]{1, 1, 1, 1});

        seatRepository.batchInsertSeats(42L, 2, "AB");

        ArgumentCaptor<BatchPreparedStatementSetter> setterCaptor = ArgumentCaptor.forClass(BatchPreparedStatementSetter.class);
        verify(jdbcTemplate).batchUpdate(anyString(), setterCaptor.capture());
        BatchPreparedStatementSetter setter = setterCaptor.getValue();

        PreparedStatement firstSeatStatement = mock(PreparedStatement.class);
        setter.setValues(firstSeatStatement, 0);
        verify(firstSeatStatement).setLong(1, 42L);
        verify(firstSeatStatement).setInt(2, 1);
        verify(firstSeatStatement).setString(3, "A");
        verify(firstSeatStatement).setBoolean(4, true);

        PreparedStatement lastSeatStatement = mock(PreparedStatement.class);
        setter.setValues(lastSeatStatement, 3);
        verify(lastSeatStatement).setLong(1, 42L);
        verify(lastSeatStatement).setInt(2, 2);
        verify(lastSeatStatement).setString(3, "B");
    }

    @Test
    void shouldReturnRawResultsArrayFromJdbcTemplateUnmodified() {
        int[] expected = {1, 0, 1};
        when(jdbcTemplate.batchUpdate(anyString(), any(BatchPreparedStatementSetter.class)))
                .thenReturn(expected);

        int[] result = seatRepository.batchInsertSeats(42L, 1, "ABC");

        assertThat(result).isEqualTo(expected);
    }
}