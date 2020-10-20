package com.ajanoni.repository.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ReservationTest {

    private static final String ID = "id";
    private static final String CUSTOMER_ID = "customerId";
    private static final LocalDate arrivalDate = LocalDate.of(2020,01,01);
    private static final LocalDate departurelDate = LocalDate.of(2020,01,03);

    @Test
    void constructWithThreeParameters() {
        Reservation reservation = new Reservation(CUSTOMER_ID, arrivalDate, departurelDate);

        assertThat(reservation.getId()).isNull();
        assertThat(reservation.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(reservation.getArrivalDate()).isEqualTo(arrivalDate);
        assertThat(reservation.getDepartureDate()).isEqualTo(departurelDate);
    }

    @Test
    void constructWithFourParameters() {
        Reservation reservation = new Reservation(ID, CUSTOMER_ID, arrivalDate, departurelDate);

        assertThat(reservation.getId()).isEqualTo(ID);
        assertThat(reservation.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(reservation.getArrivalDate()).isEqualTo(arrivalDate);
        assertThat(reservation.getDepartureDate()).isEqualTo(departurelDate);
    }

}
