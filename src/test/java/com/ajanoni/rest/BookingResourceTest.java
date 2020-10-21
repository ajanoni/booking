package com.ajanoni.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.hasItems;
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
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

@QuarkusTest
class BookingResourceTest {

    private static final LocalDate START_DATE = LocalDate.of(2020, 01, 01);
    private static final LocalDate END_DATE = LocalDate.of(2020, 01, 10);
    private static final String RESERVATION_ID = "reservationId";
    private static final String EMAIL = "test@test.com";
    private static final String FULL_NAME = "full name";

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
    void getAvailableDaysBadDate() throws Exception {
        given()
                .queryParam("startDate", "invalid")
                .queryParam("endDate", "2020/01/2")
                .when()
                .get("/booking/schedule")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body("errors.message[0]", is("Invalid date format."));
    }

    @Test
    void createReservation() throws Exception {
        ReservationCommand command = getReservation();

        given(bookingCommand.createReservationWithLock(command))
                .willReturn(Uni.createFrom().item(RESERVATION_ID));

        ReservationCommandResult result = new ReservationCommandResult(RESERVATION_ID);
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
    void reservationRequiredFields() throws Exception {
        ReservationCommand command = ReservationCommand.builder()
                .arrivalDate(null)
                .departureDate(null)
                .email(null)
                .fullName(null)
                .build();

        List<String> messages = List.of("Field email is required.", "Field fullName is required.",
                "Field arrivalDate is required.", "Field departureDate is required.");
        given()
                .contentType(ContentType.JSON)
                .body(command)
                .when().
                post("/booking")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body("errors.message", hasItems(messages.toArray()));
    }

    @Test
    void invalidDate() throws Exception {
        given()
                .contentType(ContentType.JSON)
                .body("{ \"arrivalDate\": \"20204141\" }")
                .when().
                post("/booking")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body("errors.message[0]", is("Invalid format for field: arrivalDate"));
    }

    @Test
    void invalidEmail() throws Exception {
        ReservationCommand command = ReservationCommand.builder()
                .arrivalDate(START_DATE)
                .departureDate(END_DATE)
                .email("invalid email")
                .fullName(FULL_NAME)
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(command)
                .when().
                post("/booking")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body("errors.message[0]", is("Invalid email address."));
    }

    @Test
    void invalidInputLength() throws Exception {
        ReservationCommand command = ReservationCommand.builder()
                .arrivalDate(START_DATE)
                .departureDate(END_DATE)
                .email(EMAIL)
                .fullName(StringUtils.repeat("x", 300))
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(command)
                .when().
                post("/booking")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body("errors.message[0]", is("length must be between 0 and 255"));
    }

    @Test
    void updateReservation() throws Exception {
        ReservationCommand command = getReservation();

        given(bookingCommand.updateReservationWithLock(RESERVATION_ID, command))
                .willReturn(Uni.createFrom().item(RESERVATION_ID));

        ReservationCommandResult result = new ReservationCommandResult(RESERVATION_ID);
        given()
                .contentType(ContentType.JSON)
                .body(command)
                .when()
                .put("/booking/{id}", RESERVATION_ID)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(is(objectMapper.writeValueAsString(result)));
    }

    @Test
    void deleteReservation() throws Exception {
        ReservationCommand command = getReservation();

        given(bookingCommand.deleteReservation(RESERVATION_ID))
                .willReturn(Uni.createFrom().item(RESERVATION_ID));

        ReservationCommandResult result = new ReservationCommandResult(RESERVATION_ID);
        given()
                .when()
                .delete("/booking/{id}", RESERVATION_ID)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(is(objectMapper.writeValueAsString(result)));
    }

    private ReservationCommand getReservation() {
        return ReservationCommand.builder()
                .arrivalDate(START_DATE)
                .departureDate(END_DATE)
                .email(EMAIL)
                .fullName(FULL_NAME)
                .build();
    }
}
