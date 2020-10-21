package com.ajanoni.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.BDDMockito.given;

import com.ajanoni.common.DateUtil;
import com.ajanoni.dto.AvailableDateResult;
import com.ajanoni.dto.ReservationCommand;
import com.ajanoni.dto.ReservationCommandResult;
import com.ajanoni.service.booking.BookingCommandHandler;
import com.ajanoni.service.booking.BookingQueryHandler;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.http.ContentType;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

@QuarkusTest
class BookingResourceTest {

    private static final LocalDate START_DATE = LocalDate.of(2020, 01, 01);
    private static final LocalDate END_DATE = LocalDate.of(2020, 01, 10);

    @InjectMock
    private BookingCommandHandler bookingCommand;

    @InjectMock
    private BookingQueryHandler queryCommand;

    @Inject
    private ObjectMapper objectMapper;

    @Test
    void getAvailableDays() throws Exception {
        List<AvailableDateResult> sequenceDates = DateUtil.getContinuousDates(START_DATE, END_DATE).stream()
                .map(date -> new AvailableDateResult(date))
                .collect(Collectors.toUnmodifiableList());
        given(queryCommand.getAvailableDates(START_DATE, END_DATE))
                .willReturn(Multi.createFrom().iterable(sequenceDates));
        String jsonSequenceDates = objectMapper.writeValueAsString(sequenceDates);

        given()
                .queryParam("startDate", DateTimeFormatter.ISO_DATE.format(START_DATE))
                .queryParam("endDate", DateTimeFormatter.ISO_DATE.format(END_DATE))
                .when()
                .get("/booking/schedule")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("size()", is(10))
                .body(is(jsonSequenceDates));
    }

    @Test
    void createReservation() throws Exception {
        ReservationCommand command = getReservation();

        given(bookingCommand.createReservationWithLock(command))
                .willReturn(Uni.createFrom().item("reservationId"));

        ReservationCommandResult result = new ReservationCommandResult("reservationId");
        given()
                .contentType(ContentType.JSON)
                .body(command)
                .when().
                post("/booking")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(is(objectMapper.writeValueAsString(result)));
    }

    @Test
    void updateReservation() throws Exception {
        ReservationCommand command = getReservation();

        given(bookingCommand.updateReservationWithLock("reservationId", command))
                .willReturn(Uni.createFrom().item("reservationId"));

        ReservationCommandResult result = new ReservationCommandResult("reservationId");
        given()
                .contentType(ContentType.JSON)
                .body(command)
                .when()
                .put("/booking/{id}", "reservationId")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(is(objectMapper.writeValueAsString(result)));
    }

    @Test
    void deleteReservation() throws Exception {
        ReservationCommand command = getReservation();

        given(bookingCommand.deleteReservation("reservationId"))
                .willReturn(Uni.createFrom().item("reservationId"));

        ReservationCommandResult result = new ReservationCommandResult("reservationId");
        given()
                .when()
                .delete("/booking/{id}", "reservationId")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(is(objectMapper.writeValueAsString(result)));
    }

    private ReservationCommand getReservation() {
        return ReservationCommand.builder()
                .arrivalDate(START_DATE)
                .departureDate(END_DATE)
                .email("test@test.com")
                .fullName("full name")
                .build();
    }
}
