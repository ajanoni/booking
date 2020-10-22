package com.ajanoni.service.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.ajanoni.common.DateUtil;
import com.ajanoni.dto.ReservationCommand;
import com.ajanoni.lock.LockHandler;
import com.ajanoni.repository.ReservationRepository;
import com.ajanoni.repository.model.Reservation;
import com.ajanoni.service.customer.CustomerCommandHandler;
import io.smallrye.mutiny.Uni;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookingCommandHandlerTest {

    private static final LocalDate START_DATE = LocalDate.now().plusDays(1);
    private static final LocalDate END_DATE = START_DATE.plusDays(2);
    private static final String EMAIL = "email@email";
    private static final String FULL_NAME = "full name";
    private static final String CUSTOMER_ID = "customerId";

    private static final ReservationCommand command = ReservationCommand.builder()
            .email(EMAIL)
            .fullName(FULL_NAME)
            .arrivalDate(START_DATE)
            .departureDate(END_DATE)
            .build();

    private static final Reservation reservation = Reservation.builder()
            .customerId(CUSTOMER_ID)
            .arrivalDate(command.getArrivalDate())
            .departureDate(command.getDepartureDate())
            .build();
    private static final String ID = "id";

    @Mock
    private CustomerCommandHandler customerService;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private BookingValidationHandler bookingRules;

    @Mock
    private LockHandler lockHandler;

    @Captor
    private ArgumentCaptor<Supplier<Uni<String>>> suplierCaptor;

    @InjectMocks
    private BookingCommandHandler testInstance;

    @BeforeEach
    void setup() {
        given(bookingRules.validateRequest(START_DATE, END_DATE)).willReturn(Uni.createFrom().voidItem());
    }

    @Test
    void createReservationWithLock() {
        given(reservationRepository.hasReservationBetween("", command.getArrivalDate(),
                command.getDepartureDate())).willReturn(Uni.createFrom().item(false));
        given(lockHandler.executeWithLock(eq(getLockIds()), suplierCaptor.capture()))
                .willReturn(Uni.createFrom().item(ID));
        given(customerService.updateOrCreateCustomer(EMAIL, FULL_NAME))
                .willReturn(Uni.createFrom().item(CUSTOMER_ID));
        given(reservationRepository.save(reservation))
                .willReturn(Uni.createFrom().item(ID));

        String returnedId = testInstance.createReservationWithLock(command).await().indefinitely();
        String lambdaId = suplierCaptor.getValue().get().await().indefinitely();

        assertThat(returnedId).isEqualTo(lambdaId);
    }

    @Test
    void updateReservationWithLock() {

        testInstance.createReservationWithLock(command);
    }

    @Test
    void deleteReservation() {

        testInstance.createReservationWithLock(command);
    }

    private List<String> getLockIds() {
        List<String> lockDates =
                DateUtil.getContinuousDates(command.getArrivalDate(), command.getDepartureDate()).stream()
                        .map(LocalDate::toString)
                        .collect(Collectors.toList());
        return lockDates;
    }

}
