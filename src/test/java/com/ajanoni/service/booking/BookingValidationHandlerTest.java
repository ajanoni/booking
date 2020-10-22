package com.ajanoni.service.booking;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ajanoni.exception.ReservationRequestException;
import io.smallrye.mutiny.Uni;
import java.time.LocalDate;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookingValidationHandlerTest {

    private BookingValidationHandler testInstance;

    @BeforeEach
    void setup() {
        testInstance = new BookingValidationHandler();
    }

    @Test
    void queryValidationIsOk() {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusMonths(6).minusDays(1);

        assertThatNoException().isThrownBy(() -> testInstance.validateQuery(startDate, endDate).await().indefinitely());
    }

    @Test
    void queryCheckDatesOrder() {
        LocalDate startDate = LocalDate.of(2020,01,01);
        LocalDate endDate = LocalDate.of(2020,01,02);

        Uni<Void> validation = testInstance.validateQuery(endDate, startDate);

        assertException(validation, "The end date should be equal to or after start date.");
    }

    @Test
    void queryCheckStartDateNotBeforeToday() {
        LocalDate startDate = LocalDate.MIN;
        LocalDate endDate = LocalDate.now();

        Uni<Void> validation = testInstance.validateQuery(startDate, endDate);

        assertException(validation, "Selection must start from today.");
    }

    @Test
    void queryCheckMaximumSelection() {
        LocalDate startDate = LocalDate.now();
        LocalDate endDateSevenMonthsFromStart = startDate.plusMonths(7).minusDays(1);

        Uni<Void> validation = testInstance.validateQuery(startDate, endDateSevenMonthsFromStart);

        assertException(validation, "Maximum of 6 months between start date and end date allowed.");
    }

    @Test
    void validRequestDates() {
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = startDate.plusDays(2);

        assertThatNoException().isThrownBy(() -> testInstance
                .validateRequest(startDate, endDate).await().indefinitely());
    }

    @Test
    void requestCheckDatesOrder() {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(1);

        Uni<Void> validation = testInstance.validateRequest(endDate, startDate);

        assertException(validation, "Departure date can not be before arrival date.");
    }

    @Test
    void requestCheckArrivalOffset() {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(2);

        Uni<Void> validation = testInstance.validateRequest(startDate, endDate);

        assertException(validation, "Arrival date must be after today.");
    }

    @Test
    void requestCheckMaxDays() {
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = startDate.plusDays(3);

        Uni<Void> validation = testInstance.validateRequest(startDate, endDate);

        assertException(validation, "Maximum 3 days of reservation is allowed.");
    }

    @Test
    void requestArrivalDateWithinOneMonth() {
        LocalDate startDate = LocalDate.now().plusMonths(1);
        LocalDate endDate = startDate.plusDays(2);

        Uni<Void> validation = testInstance.validateRequest(startDate, endDate);

        assertException(validation, "Maximum one month ahead allowed for arrival or departure date.");
    }

    @Test
    void requestDepartureDateWithinOneMonth() {
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = startDate.plusMonths(1);

        Uni<Void> validation = testInstance.validateRequest(startDate, endDate);

        assertException(validation, "Maximum one month ahead allowed for arrival or departure date.");
    }

    private void assertException(Uni<Void> validation, String message) {
        assertThatThrownBy(() -> validation.await().indefinitely())
                .asInstanceOf(InstanceOfAssertFactories.type(ReservationRequestException.class))
                .extracting(ReservationRequestException::getMessages)
                .asList()
                .contains(message);
    }
}
