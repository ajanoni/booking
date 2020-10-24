package com.ajanoni.service.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.ajanoni.common.DateUtil;
import com.ajanoni.dto.ReservationCommand;
import com.ajanoni.exception.ReservationConflictException;
import com.ajanoni.lock.LockHandler;
import com.ajanoni.repository.ReservationRepository;
import com.ajanoni.repository.model.Reservation;
import com.ajanoni.service.customer.CustomerCommandHandler;
import io.smallrye.mutiny.Uni;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
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

    private static final String ID = "id";
    private static final LocalDate START_DATE = LocalDate.now().plusDays(1);
    private static final LocalDate END_DATE = START_DATE.plusDays(2);
    private static final LocalDate UPDATED_START_DATE = LocalDate.now().plusDays(2);
    private static final LocalDate UPDATED_END_DATE = START_DATE.plusDays(3);
    private static final String EMAIL = "email@email";
    private static final String FULL_NAME = "full name";
    private static final String CUSTOMER_ID = "customerId";

    private static final ReservationCommand INSERT_COMMAND = ReservationCommand.builder()
            .email(EMAIL)
            .fullName(FULL_NAME)
            .arrivalDate(START_DATE)
            .departureDate(END_DATE)
            .build();

    private static final ReservationCommand UPDATE_COMMAND = ReservationCommand.builder()
            .email(EMAIL)
            .fullName(FULL_NAME)
            .arrivalDate(UPDATED_START_DATE)
            .departureDate(UPDATED_END_DATE)
            .build();

    private static final Reservation RESERVATION = Reservation.builder()
            .customerId(CUSTOMER_ID)
            .arrivalDate(START_DATE)
            .departureDate(END_DATE)
            .build();

    private static final Reservation UPDATED_RESERVATION = Reservation.builder()
            .id(ID)
            .customerId(CUSTOMER_ID)
            .arrivalDate(UPDATED_START_DATE)
            .departureDate(UPDATED_END_DATE)
            .build();

    @Mock
    private CustomerCommandHandler customerService;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private BookingValidationHandler bookingRules;

    @Mock
    private LockHandler lockHandler;

    @Captor
    private ArgumentCaptor<Supplier<Uni<String>>> supplierCaptor;

    @InjectMocks
    private BookingCommandHandler testInstance;

    @BeforeEach
    void setup() {

    }

    @Test
    void createReservationWithLock() {
        given(bookingRules.validateRequest(START_DATE, END_DATE)).willReturn(Uni.createFrom().voidItem());
        given(reservationRepository.hasReservationBetween(StringUtils.EMPTY, INSERT_COMMAND.getArrivalDate(),
                INSERT_COMMAND.getDepartureDate())).willReturn(Uni.createFrom().item(false));
        given(lockHandler.executeWithLock(eq(getLockIdsInsert()), supplierCaptor.capture()))
                .willReturn(Uni.createFrom().item(ID));
        given(customerService.updateOrCreateCustomer(EMAIL, FULL_NAME)).willReturn(Uni.createFrom().item(CUSTOMER_ID));
        given(reservationRepository.save(RESERVATION))
                .willReturn(Uni.createFrom().item(ID));

        String returnedId = testInstance.createReservationWithLock(INSERT_COMMAND).await().indefinitely();
        String lambdaId = supplierCaptor.getValue().get().await().indefinitely();

        assertThat(returnedId).isEqualTo(ID);
        assertThat(lambdaId).isEqualTo(ID);
    }

    @Test
    void notCreatedWhenConflict() {
        given(bookingRules.validateRequest(START_DATE, END_DATE)).willReturn(Uni.createFrom().voidItem());
        given(reservationRepository.hasReservationBetween(StringUtils.EMPTY, START_DATE,
                END_DATE)).willReturn(Uni.createFrom().item(true));

        assertThatThrownBy(() -> testInstance.createReservationWithLock(INSERT_COMMAND).await().indefinitely())
            .isInstanceOf(ReservationConflictException.class)
            .hasMessage("Other reservation conflicts with the selected dates.");

        verifyNoInteractions(customerService);
        verifyNoMoreInteractions(reservationRepository);
    }

    @Test
    void updateReservationWithLock() {
        mockUpdateValidation();
        given(reservationRepository.hasReservationBetween(ID, UPDATE_COMMAND.getArrivalDate(),
                UPDATE_COMMAND.getDepartureDate())).willReturn(Uni.createFrom().item(false));
        given(lockHandler.executeWithLock(eq(getLockIdsUpdate()), supplierCaptor.capture()))
                .willReturn(Uni.createFrom().item(ID));
        given(customerService.updateCustomer(CUSTOMER_ID, UPDATE_COMMAND.getEmail(),
                UPDATE_COMMAND.getFullName())).willReturn(Uni.createFrom().item(CUSTOMER_ID));
        given(reservationRepository.update(UPDATED_RESERVATION))
                .willReturn(Uni.createFrom().item(UPDATED_RESERVATION));

        String returnedId = testInstance.updateReservationWithLock(ID, UPDATE_COMMAND).await().indefinitely();
        String lambdaId = supplierCaptor.getValue().get().await().indefinitely();

        assertThat(returnedId).isEqualTo(ID);
        assertThat(lambdaId).isEqualTo(ID);
    }

    @Test
    void notUpdateWhenConflict() {
        mockUpdateValidation();
        given(reservationRepository.hasReservationBetween(ID, UPDATED_START_DATE, UPDATED_END_DATE))
                .willReturn(Uni.createFrom().item(true));

        assertThatThrownBy(() -> testInstance.updateReservationWithLock(ID, UPDATE_COMMAND).await().indefinitely())
                .isInstanceOf(ReservationConflictException.class)
                .hasMessage("Other reservation conflicts with the selected dates.");

        verifyNoInteractions(customerService);
        verifyNoMoreInteractions(reservationRepository);
    }

    @Test
    void deleteReservation() {
        given(reservationRepository.getById(ID)).willReturn(Uni.createFrom().item(UPDATED_RESERVATION));
        given(reservationRepository.delete(ID)).willReturn(Uni.createFrom().item(true));

        String deletedId = testInstance.deleteReservation(ID).await().indefinitely();

        assertThat(deletedId).isEqualTo(ID);
    }

    @Test
    void deleteReservationFail() {
        given(reservationRepository.getById(ID)).willReturn(Uni.createFrom().item(UPDATED_RESERVATION));
        given(reservationRepository.delete(ID)).willReturn(Uni.createFrom().item(false));

        assertThatThrownBy(() -> testInstance.deleteReservation(ID).await().indefinitely())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Error on deleting reservation.");
    }

    private List<String> getLockIdsInsert() {
        List<String> lockDates =
                DateUtil.getContinuousDates(INSERT_COMMAND.getArrivalDate(), INSERT_COMMAND.getDepartureDate()).stream()
                        .map(LocalDate::toString)
                        .collect(Collectors.toList());
        return lockDates;
    }

    private List<String> getLockIdsUpdate() {
        List<String> lockDates =
                DateUtil.getContinuousDates(UPDATE_COMMAND.getArrivalDate(), UPDATE_COMMAND.getDepartureDate()).stream()
                        .map(LocalDate::toString)
                        .collect(Collectors.toList());
        return lockDates;
    }

    private void mockUpdateValidation() {
        given(bookingRules.validateRequest(UPDATED_START_DATE, UPDATED_END_DATE))
                .willReturn(Uni.createFrom().voidItem());
        given(reservationRepository.getById(ID)).willReturn(Uni.createFrom().item(UPDATED_RESERVATION));
    }
}
