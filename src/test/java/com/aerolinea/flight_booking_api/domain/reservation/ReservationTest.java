package com.aerolinea.flight_booking_api.domain.reservation;

import com.aerolinea.flight_booking_api.exceptions.BusinessRuleViolationException;
import com.aerolinea.flight_booking_api.exceptions.ErrorCode;
import com.aerolinea.flight_booking_api.models.Reservation;
import com.aerolinea.flight_booking_api.models.enums.ReservationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class ReservationTest {

    private Reservation reservationIn(ReservationStatus status) {
        return Reservation.builder()
                .reservationCode("RES-12345")
                .status(status)
                .numberOfPassengers(1)
                .totalPrice(new BigDecimal("100.00"))
                .build();
    }

    @Test
    @DisplayName("A pending reservation can be confirmed")
    void pendingReservationCanBeConfirmed() {
        Reservation reservation = reservationIn(ReservationStatus.PENDING);

        reservation.confirmReservation();

        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
    }

    @ParameterizedTest
    @EnumSource(value = ReservationStatus.class, names = {"EXPIRED", "CANCELLED", "COMPLETED"})
    @DisplayName("A closed reservation cannot be confirmed and keeps its status")
    void closedReservationCannotBeConfirmed(ReservationStatus status) {
        Reservation reservation = reservationIn(status);

        BusinessRuleViolationException exception = assertThrows(
                BusinessRuleViolationException.class,
                reservation::confirmReservation);

        assertEquals(ErrorCode.RESERVATION_INVALID_TRANSITION, exception.getErrorCode());
        assertEquals(status, reservation.getStatus());
    }

    @Test
    @DisplayName("A confirmed reservation cannot be confirmed again")
    void confirmedReservationCannotBeConfirmedAgain() {
        Reservation reservation = reservationIn(ReservationStatus.CONFIRMED);

        BusinessRuleViolationException exception = assertThrows(
                BusinessRuleViolationException.class,
                reservation::confirmReservation);

        assertEquals(ErrorCode.RESERVATION_ALREADY_CONFIRMED, exception.getErrorCode());
    }


    @Test
    void confirmedReservationCanBeCancelled() {
        Reservation reservation = reservationIn(ReservationStatus.CONFIRMED);

        reservation.cancelReservation();

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @ParameterizedTest
    @EnumSource(value = ReservationStatus.class, names = {"EXPIRED", "COMPLETED"})
    void closedReservationCannotBeCancelled(ReservationStatus status) {
        Reservation reservation = reservationIn(status);

        assertThatThrownBy(reservation::cancelReservation)
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESERVATION_INVALID_TRANSITION);
        assertThat(reservation.getStatus()).isEqualTo(status);
    }

    @ParameterizedTest
    @EnumSource(value = ReservationStatus.class, names = {"CONFIRMED", "CANCELLED", "COMPLETED"})
    void onlyPendingReservationCanExpire(ReservationStatus status) {
        Reservation reservation = reservationIn(status);

        assertThatThrownBy(reservation::expireReservation)
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESERVATION_INVALID_TRANSITION);
        assertThat(reservation.getStatus()).isEqualTo(status);
    }
}