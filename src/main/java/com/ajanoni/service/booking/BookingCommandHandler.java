package com.ajanoni.service.booking;

import com.ajanoni.service.customer.CustomerCommandHandler;
import com.ajanoni.common.DateUtil;
import com.ajanoni.exception.LockAcquireException;
import com.ajanoni.exception.ReservationConflictException;
import com.ajanoni.exception.ReservationNotFoundException;
import com.ajanoni.lock.LockHandler;
import com.ajanoni.repository.model.Reservation;
import com.ajanoni.repository.ReservationRepository;
import com.ajanoni.dto.ReservationCommand;
import io.smallrye.mutiny.Uni;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class BookingCommandHandler {

    private static final String RESERVATION_CONFLICT = "Other reservation conflicts with the selected dates.";

    private final CustomerCommandHandler customerService;
    private final ReservationRepository reservationRepository;
    private final BookingValidationHandler bookingRules;
    private final LockHandler lockHandler;

    @Inject
    public BookingCommandHandler(CustomerCommandHandler customerService, ReservationRepository reservationRepository,
            LockHandler lockHandler, BookingValidationHandler bookingRules) {
        this.customerService = customerService;
        this.reservationRepository = reservationRepository;
        this.lockHandler = lockHandler;
        this.bookingRules = bookingRules;
    }

    public Uni<String> createReservationWithLock(ReservationCommand command) {
        return bookingRules.validateRequest(command.getArrivalDate(), command.getDepartureDate()).onItem()
                .transformToUni(it -> reservationExistsBetweenDates(command).onItem()
                        .ifNull()
                        .switchTo(createWithLock(command)));
    }

    public Uni<String> updateReservationWithLock(String id, ReservationCommand command) {
        return bookingRules.validateRequest(command.getArrivalDate(), command.getDepartureDate()).onItem()
                .transformToUni(it -> reservationRepository.getById(id).onItem()
                        .ifNull()
                        .failWith(() -> new ReservationNotFoundException(id)).onItem()
                        .ifNotNull()
                        .transformToUni(reservation -> reservationExistsBetweenDates(id, command)
                                .onItem()
                                .ifNull()
                                .switchTo(updateWithLock(reservation, command))));
    }

    public Uni<String> deleteReservation(String id) {
        return reservationRepository.getById(id).onItem()
                .ifNull().failWith(() -> new ReservationNotFoundException(id))
                .onItem().transformToUni(reservation -> delete(reservation.id()));
    }

    private List<String> getLockDates(ReservationCommand command) {
        return DateUtil.getContinuousDates(command.getArrivalDate(), command.getDepartureDate()).stream()
                .map(LocalDate::toString)
                .collect(Collectors.toList());
    }

    private Uni<String> createWithLock(ReservationCommand command) {
        return lockHandler
                .executeWithLock(getLockDates(command), () -> create(command))
                .onFailure(LockAcquireException.class)
                .recoverWithUni(Uni.createFrom()
                        .failure(() -> new ReservationConflictException(RESERVATION_CONFLICT)));
    }

    private Uni<String> updateWithLock(Reservation reservation, ReservationCommand command) {
        return lockHandler
                .executeWithLock(getLockDates(command), () -> update(reservation, command))
                .onFailure(LockAcquireException.class)
                .recoverWithUni(Uni.createFrom()
                        .failure(() -> new ReservationConflictException(
                                RESERVATION_CONFLICT)));
    }

    private Uni<String> create(ReservationCommand command) {
        return customerService.updateOrCreateCustomer(command.getEmail(), command.getFullName()).onItem()
                .transformToUni(id -> {
                    Reservation reservation = Reservation.builder()
                            .id(id)
                            .arrivalDate(command.getArrivalDate())
                            .departureDate(command.getDepartureDate())
                            .build();

                    return reservationRepository.save(reservation);
                });
    }

    private Uni<String> update(Reservation reservation, ReservationCommand reservationCommand) {
        Reservation updatedReservation = Reservation.builder()
                .id(reservation.id())
                .customerId(reservation.customerId())
                .arrivalDate(reservationCommand.getArrivalDate())
                .departureDate(reservationCommand.getDepartureDate())
                .build();

        return customerService.updateCustomer(reservation.customerId(), reservationCommand.getEmail(),
                reservationCommand.getFullName()).onItem()
                .transformToUni(customer ->
                        reservationRepository.update(updatedReservation).onItem()
                                .transformToUni(postUpdate -> Uni.createFrom().item(postUpdate.id())));
    }

    private Uni<? extends String> delete(String id) {
        return reservationRepository.delete(id).onItem()
                .transformToUni(deleted -> {
                    if (deleted) {
                        return Uni.createFrom().item(id);
                    }

                    return Uni.createFrom()
                            .failure(() -> new IllegalStateException("Error on deleting reservation."));
                });
    }

    private Uni<String> reservationExistsBetweenDates(ReservationCommand reservationCommand) {
        return reservationExistsBetweenDates("", reservationCommand);
    }

    private Uni<String> reservationExistsBetweenDates(String id, ReservationCommand reservationCommand) {
        return reservationRepository.hasReservationBetween(id, reservationCommand.getArrivalDate(),
                reservationCommand.getDepartureDate()).onItem().ifNotNull()
                .transformToUni(hasReservation -> {
                    if (hasReservation) {
                        return Uni.createFrom().failure(() -> new ReservationConflictException(RESERVATION_CONFLICT));
                    }

                    return Uni.createFrom().nullItem();
                });
    }
}
