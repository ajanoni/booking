package com.ajanoni.service;

import com.ajanoni.exception.ReservationRequestException;
import com.ajanoni.rest.dto.ReservationCommand;
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

    Uni<Void> validateQuery(LocalDate startDate, LocalDate endDate) {
        checkDatesOrder(startDate, endDate, "The endDate should be equal to or after startDate.");

        List<String> messages = new ArrayList<>();
        checkDatesNotBeforeToday(startDate, endDate).ifPresent(messages::add);
        checkMaximumSelection(startDate, endDate).ifPresent(messages::add);

        if (CollectionUtils.isNotEmpty(messages)) {
            return Uni.createFrom().failure(() -> new ReservationRequestException(messages));
        }

        return Uni.createFrom().voidItem();
    }

    Uni<Void> validateRequest(LocalDate arrivalDate, LocalDate departureDate) {
        checkDatesOrder(arrivalDate, departureDate, "Departure date before arrival date.");

        List<String> messages = new ArrayList<>();
        checkStartOffset(arrivalDate).ifPresent(messages::add);
        checkMaxDays(arrivalDate, departureDate).ifPresent(messages::add);
        checkDatesBeforeLimit(arrivalDate, departureDate).ifPresent(messages::add);

        if (CollectionUtils.isNotEmpty(messages)) {
            return Uni.createFrom().failure(() -> new ReservationRequestException(messages));
        }

        return Uni.createFrom().voidItem();
    }

    private Optional<String> checkMaximumSelection(LocalDate startDate, LocalDate endDate) {
        long monthsDiff = ChronoUnit.MONTHS.between(startDate, endDate) + 1;
        if (monthsDiff > 6) {
            return Optional.of("Maximum of 6 months between startDate and endDate allowed.");
        }

        return Optional.empty();
    }

    private Optional<String> checkDatesNotBeforeToday(LocalDate startDate, LocalDate endDate) {
        if (startDate.isBefore(LocalDate.now()) || endDate.isBefore(LocalDate.now())) {
            return Optional.of("Selection must start from today.");
        }

        return Optional.empty();
    }

    private Optional<String> checkDatesBeforeLimit(LocalDate arrivalDate, LocalDate departureDate) {
        LocalDate oneMonthAhead = LocalDate.now().plusMonths(MONTHS_WINDOW_ALLOWED);
        if (departureDate.isAfter(oneMonthAhead) || arrivalDate.isAfter(oneMonthAhead)) {
            return Optional.of("Maximum one month ahead allowed for arrival or departure date.");
        }

        return Optional.empty();
    }

    private Optional<String> checkStartOffset(LocalDate arrivalDate) {
        if (arrivalDate.isBefore(LocalDate.now().plusDays(START_RESERVATION_OFFSET))) {
            return Optional.of("Not allowed to book for today.");
        }

        return Optional.empty();
    }

    private Optional<String> checkMaxDays(LocalDate arrivalDate, LocalDate departureDate) {
        long days = ChronoUnit.DAYS.between(arrivalDate, departureDate.plusDays(1));
        if (days > MAX_DAYS_ALLOWED) {
            return Optional.of("Maximum 3 days of reservation is allowed.");
        }

        return Optional.empty();
    }

    private void checkDatesOrder(LocalDate arrivalDate, LocalDate departureDate, String message) {
        if (departureDate.isBefore(arrivalDate)) {
            throw new ReservationRequestException(message);
        }
    }
}
