package com.ajanoni.service.booking;

import com.ajanoni.exception.ReservationRequestException;
import io.smallrye.mutiny.Uni;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import org.apache.commons.collections4.CollectionUtils;

@ApplicationScoped
public class BookingValidationHandler {

    private static final int MAX_DAYS_ALLOWED = 3;
    private static final int START_RESERVATION_OFFSET = 1;
    private static final int MONTHS_WINDOW_ALLOWED = 1;
    private static final int MAX_QUERY_MONTHS = 6;

    private static final String REQUEST_DATES_ORDER = "Departure date can not be before arrival date.";
    private static final String REQUEST_MAX_DAYS_ALLOWED = "Maximum 3 days of reservation is allowed.";
    private static final String REQUEST_START_DAY = "Arrival date must be after today.";
    private static final String REQUEST_MAX_DATE = "Maximum one month ahead allowed for arrival or departure date.";

    private static final String QUERY_MAXIMUM_PERIOD = "Maximum of 6 months between start date and end date allowed.";
    private static final String QUERY_DATES_ORDER = "The end date should be equal to or after start date.";
    private static final String QUERY_START_DAY = "Selection must start from today.";

    Uni<Void> validateQuery(LocalDate startDate, LocalDate endDate) {
        return checkDatesOrder(startDate, endDate, QUERY_DATES_ORDER).onItem().transformToUni(it -> {
                    List<String> messages = new ArrayList<>();
                    checkDatesNotBeforeToday(startDate, endDate).ifPresent(messages::add);
                    checkMaximumSelection(startDate, endDate).ifPresent(messages::add);

                    if (CollectionUtils.isNotEmpty(messages)) {
                        return Uni.createFrom().failure(() -> new ReservationRequestException(messages));
                    }
                    return Uni.createFrom().voidItem();
                }
        );
    }

    Uni<Void> validateRequest(LocalDate arrivalDate, LocalDate departureDate) {
        return checkDatesOrder(arrivalDate, departureDate, REQUEST_DATES_ORDER).onItem().transformToUni(it -> {
            List<String> messages = new ArrayList<>();
            checkStartOffset(arrivalDate).ifPresent(messages::add);
            checkMaxDays(arrivalDate, departureDate).ifPresent(messages::add);
            checkDatesBeforeLimit(arrivalDate, departureDate).ifPresent(messages::add);

            if (CollectionUtils.isNotEmpty(messages)) {
                return Uni.createFrom().failure(() -> new ReservationRequestException(messages));
            }

            return Uni.createFrom().voidItem();
        });
    }

    private Optional<String> checkMaximumSelection(LocalDate startDate, LocalDate endDate) {
        long monthsDiff = ChronoUnit.MONTHS.between(startDate, endDate);
        if (monthsDiff >= MAX_QUERY_MONTHS) {
            return Optional.of(QUERY_MAXIMUM_PERIOD);
        }

        return Optional.empty();
    }

    private Optional<String> checkDatesNotBeforeToday(LocalDate startDate, LocalDate endDate) {
        if (startDate.isBefore(LocalDate.now()) || endDate.isBefore(LocalDate.now())) {
            return Optional.of(QUERY_START_DAY);
        }

        return Optional.empty();
    }

    private Optional<String> checkDatesBeforeLimit(LocalDate arrivalDate, LocalDate departureDate) {
        LocalDate oneMonthAhead = LocalDate.now().plusMonths(MONTHS_WINDOW_ALLOWED);
        if (departureDate.isAfter(oneMonthAhead) || arrivalDate.isAfter(oneMonthAhead)) {
            return Optional.of(REQUEST_MAX_DATE);
        }

        return Optional.empty();
    }

    private Optional<String> checkStartOffset(LocalDate arrivalDate) {
        if (arrivalDate.isBefore(LocalDate.now().plusDays(START_RESERVATION_OFFSET))) {
            return Optional.of(REQUEST_START_DAY);
        }

        return Optional.empty();
    }

    private Optional<String> checkMaxDays(LocalDate arrivalDate, LocalDate departureDate) {
        long days = ChronoUnit.DAYS.between(arrivalDate, departureDate.plusDays(1));
        if (days > MAX_DAYS_ALLOWED) {
            return Optional.of(REQUEST_MAX_DAYS_ALLOWED);
        }

        return Optional.empty();
    }

    private Uni<Void> checkDatesOrder(LocalDate arrivalDate, LocalDate departureDate, String message) {
        if (departureDate.isBefore(arrivalDate)) {
            return Uni.createFrom().failure(() -> new ReservationRequestException(message));
        }

        return Uni.createFrom().voidItem();
    }
}
