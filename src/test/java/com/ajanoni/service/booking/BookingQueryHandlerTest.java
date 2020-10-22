package com.ajanoni.service.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.ajanoni.common.DateUtil;
import com.ajanoni.dto.AvailableDateResult;
import com.ajanoni.repository.ReservationRepository;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookingQueryHandlerTest {

    private static LocalDate START_DATE = LocalDate.of(2020, 1, 1);
    private static LocalDate END_DATE = LocalDate.of(2020, 2, 1);

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private BookingValidationHandler bookingRules;

    private BookingQueryHandler testInstance;

    @BeforeEach
    void setup() {
        testInstance = new BookingQueryHandler(reservationRepository, bookingRules);
    }

    @Test
    void getAvailableDates() {
        given(bookingRules.validateQuery(START_DATE, END_DATE)).willReturn(Uni.createFrom().voidItem());
        given(reservationRepository.getReservedDates(START_DATE, END_DATE)).willReturn(Multi.createFrom().empty());

        List<LocalDate> availableList = getAvailableList();

        List<LocalDate> expectedList = DateUtil.getContinuousDates(START_DATE, END_DATE);
        assertThat(availableList).containsExactlyElementsOf(expectedList);
    }

    @Test
    void getAvailableDatesWhenHasReservations() {
        given(bookingRules.validateQuery(START_DATE, END_DATE)).willReturn(Uni.createFrom().voidItem());
        LocalDate reservedDateOne = START_DATE.plusDays(1);
        LocalDate reservedDateTwo = START_DATE.plusDays(2);
        given(reservationRepository.getReservedDates(START_DATE, END_DATE))
                .willReturn(Multi.createFrom().items(reservedDateOne, reservedDateTwo));

        List<LocalDate> availableList = getAvailableList();

        List<LocalDate> expectedList = DateUtil.getContinuousDates(START_DATE, END_DATE).stream()
                .filter(date -> !reservedDateOne.isEqual(date) && !reservedDateTwo.isEqual(date))
                .collect(Collectors.toUnmodifiableList());
        assertThat(availableList).containsExactlyElementsOf(expectedList);
    }

    private List<LocalDate> getAvailableList() {
        return testInstance.getAvailableDates(START_DATE, END_DATE).collectItems()
                .asList()
                .await()
                .indefinitely()
                .stream()
                .map(AvailableDateResult::getDate)
                .collect(Collectors.toUnmodifiableList());
    }
}
