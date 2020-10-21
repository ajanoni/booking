package com.ajanoni.service.booking;

import com.ajanoni.common.DateUtil;
import com.ajanoni.dto.AvailableDateResult;
import com.ajanoni.repository.ReservationRepository;
import io.smallrye.mutiny.Multi;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class BookingQueryHandler {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(3);

    private final ReservationRepository reservationRepository;
    private final BookingValidationHandler bookingRules;

    @Inject
    public BookingQueryHandler(ReservationRepository reservationRepository,
            BookingValidationHandler bookingRules) {
        this.reservationRepository = reservationRepository;
        this.bookingRules = bookingRules;
    }

    public Multi<AvailableDateResult> getAvailableDates(LocalDate startDate, LocalDate endDate) {
        LocalDate localStartDate = startDate != null ? startDate : LocalDate.now();
        LocalDate localEndDate = endDate != null ? endDate : localStartDate.plusMonths(1);

        return bookingRules.validateQuery(localStartDate, localEndDate).onItem()
                .transformToMulti(it -> {
                    List<LocalDate> reservedDates = getReservedDates(localStartDate, localEndDate);

                    Stream<AvailableDateResult> resultStream = DateUtil.getContinuousDates(localStartDate,
                            localEndDate).stream()
                            .filter(date -> !reservedDates.contains(date))
                            .map(AvailableDateResult::new);

                    return Multi.createFrom().items(resultStream);
                });
    }

    private List<LocalDate> getReservedDates(LocalDate localStartDate, LocalDate localEndDate) {
        return reservationRepository
                .getReservedDates(localStartDate, localEndDate)
                .collectItems().asList()
                .await().atMost(REQUEST_TIMEOUT);
    }

}
