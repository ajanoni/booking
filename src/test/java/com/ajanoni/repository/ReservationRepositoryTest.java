package com.ajanoni.repository;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

import com.ajanoni.common.DateUtil;
import com.ajanoni.repository.model.Reservation;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReservationRepositoryTest extends DbTest {

    private static final String CUSTOMER_ID = "customerId";
    private static final LocalDate ARRIVAL_DATE = LocalDate.of(2020, 01, 01);
    private static final LocalDate DEPARTURE_DATE = LocalDate.of(2020, 01, 10);
    private static final String ANOTHER_RESERVATION_ID = "anotherId";

    private ReservationsRepositoryImpl testInstance;

    @BeforeEach
    void setup() {
        testInstance = new ReservationsRepositoryImpl(pool);
        pool.query("DELETE FROM reservations;").executeAndAwait();
        pool.query("DELETE FROM customers;").executeAndAwait();
        pool.query(format("INSERT INTO customers VALUES ('%s', 'customerEmail', 'customerName');", CUSTOMER_ID))
                .executeAndAwait();
    }

    @Test
    void save() {
        Reservation newReservation = getReservation();
        String resultValue = testInstance.save(newReservation).await().indefinitely();
        String id = getId("reservations");

        assertThat(resultValue).isEqualTo(id);
    }

    @Test
    void update() {
        Reservation newReservation = getReservation();
        String reservationId = testInstance.save(newReservation).await().indefinitely();

        Reservation updateReservation = Reservation.builder()
                .id(reservationId)
                .customerId(CUSTOMER_ID)
                .arrivalDate(LocalDate.of(2020, 12, 12))
                .departureDate(LocalDate.of(2020, 12, 14))
                .build();

        Reservation resultReservation = testInstance.update(updateReservation).await().indefinitely();

        assertThat(resultReservation).isEqualTo(updateReservation);
    }

    @Test
    void delete() {
        Reservation newReservation = getReservation();
        String reservationId = testInstance.save(newReservation).await().indefinitely();

        boolean deleted = testInstance.delete(reservationId).await().indefinitely();

        assertThat(deleted).isTrue();
    }

    @Test
    void getById() {
        Reservation newReservation = getReservation();
        String reservationId = testInstance.save(newReservation).await().indefinitely();

        Reservation returnedReservation = testInstance.getById(reservationId).await().indefinitely();

        assertThat(newReservation)
                .extracting(Reservation::arrivalDate, Reservation::departureDate, Reservation::customerId)
                .contains(returnedReservation.arrivalDate(),
                        returnedReservation.departureDate(),
                        returnedReservation.customerId());
    }

    @Test
    void doNotGetById() {
        Reservation returnedReservation = testInstance.getById(ANOTHER_RESERVATION_ID).await().indefinitely();

        assertThat(returnedReservation).isNull();
    }

    @Test
    void hasReservationBetweenDates() {
        Reservation newReservation = getReservation();
        testInstance.save(newReservation).await().indefinitely();

        boolean hasReservation = testInstance.hasReservationBetween(ANOTHER_RESERVATION_ID, ARRIVAL_DATE,
                DEPARTURE_DATE).await().indefinitely();

        assertThat(hasReservation).isTrue();
    }

    @Test
    void doesNotHaveReservationBetweenDates() {
        Reservation newReservation = getReservation();
        String reservationId = testInstance.save(newReservation).await().indefinitely();

        boolean hasReservation = testInstance.hasReservationBetween(reservationId, ARRIVAL_DATE.plusYears(1),
                DEPARTURE_DATE.plusYears(1)).await().indefinitely();

        assertThat(hasReservation).isFalse();
    }

    @Test
    void getReservedDates() {
        Reservation newReservation = getReservation();
        testInstance.save(newReservation).await().indefinitely();

        List<LocalDate> returnedDatesList = testInstance.getReservedDates(ARRIVAL_DATE.minusYears(1),
                DEPARTURE_DATE.plusYears(1)).collectItems().asList().await().indefinitely();

        List<LocalDate> expectedDates = DateUtil.getContinuousDates(ARRIVAL_DATE, DEPARTURE_DATE);
        assertThat(returnedDatesList).containsAll(expectedDates);
    }

    private Reservation getReservation() {
        Reservation newReservation = Reservation.builder()
                .customerId(CUSTOMER_ID)
                .arrivalDate(ARRIVAL_DATE)
                .departureDate(DEPARTURE_DATE)
                .build();
        return newReservation;
    }
}
