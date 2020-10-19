package com.ajanoni.service;

import com.ajanoni.exception.LockAcquireException;
import com.ajanoni.exception.ReservationConflictException;
import com.ajanoni.exception.ReservationNotFoundException;
import com.ajanoni.lock.LockHandler;
import com.ajanoni.model.Customer;
import com.ajanoni.model.Reservation;
import com.ajanoni.repository.ReservationRepository;
import com.ajanoni.rest.dto.AvailableResult;
import com.ajanoni.rest.dto.ReservationCommand;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class BookingService {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(3);
    private static final String RESERVATION_CONFLICT = "Other reservation conflicts with the selected dates.";

    private final CustomerService customerService;
    private final ReservationRepository reservationRepository;
    private final BookingValidationHandler bookingRules;
    private final LockHandler lockHandler;

    @Inject
    public BookingService(CustomerService customerService, ReservationRepository reservationRepository,
            LockHandler lockHandler, BookingValidationHandler bookingRules) {
        this.customerService = customerService;
        this.reservationRepository = reservationRepository;
        this.lockHandler = lockHandler;
        this.bookingRules = bookingRules;
    }

    public Uni<String> createReservationWithLock(ReservationCommand command) {
        return bookingRules.validateRequest(command.getArrivalDate(), command.getDepartureDate()).onItem()
                .transformToUni(it -> lockHandler
                        .executeWithLock(getLockDates(command), () -> create(command))
                        .onFailure(LockAcquireException.class)
                        .recoverWithUni(Uni.createFrom()
                                .failure(() -> new ReservationConflictException(RESERVATION_CONFLICT))));
    }

    public Uni<String> updateReservationWithLock(String id, ReservationCommand command) {
        return bookingRules.validateRequest(command.getArrivalDate(), command.getDepartureDate()).onItem()
                .transformToUni(it -> lockHandler
                        .executeWithLock(getLockDates(command), () -> update(id, command))
                        .onFailure(LockAcquireException.class)
                        .recoverWithUni(Uni.createFrom()
                                .failure(() -> new ReservationConflictException(RESERVATION_CONFLICT))));
    }

    public Uni<String> deleteReservation(String id) {
        return reservationRepository.getById(id).onItem()
                .ifNull().failWith(() -> new ReservationNotFoundException(id))
                .onItem().transformToUni(reservation -> delete(reservation.getId()));
    }

    public Multi<AvailableResult> getAvailableDates(LocalDate startDate, LocalDate endDate) {
        LocalDate localStartDate = startDate != null ? startDate : LocalDate.now();
        LocalDate localEndDate = endDate != null ? endDate : localStartDate.plusMonths(1);

        bookingRules.validateQuery(localStartDate, localEndDate)
                .await().atMost(REQUEST_TIMEOUT);

        List<LocalDate> reservedDates = getReservedDates(localStartDate, localEndDate);

        Stream<AvailableResult> resultStream = getContinuousDates(localStartDate, localEndDate).stream()
                .filter(date -> !reservedDates.contains(date))
                .map(AvailableResult::new);

        return Multi.createFrom().items(resultStream);
    }

    private List<String> getLockDates(ReservationCommand reservationCommand) {
        return getContinuousDates(reservationCommand.getArrivalDate(), reservationCommand.getDepartureDate()).stream()
                .map(LocalDate::toString)
                .collect(Collectors.toList());
    }

    private List<LocalDate> getContinuousDates(LocalDate startDate, LocalDate endDate) {
        return startDate.datesUntil(endDate.plusDays(1)).collect(Collectors.toList());
    }

    private Uni<String> create(ReservationCommand reservationCommand) {
        Uni<String> createReservation = customerService.updateOrCreateCustomer(reservationCommand).onItem()
                .transformToUni(id -> {
                    Reservation reservation = new Reservation(id,
                            reservationCommand.getArrivalDate(),
                            reservationCommand.getDepartureDate());

                    return reservationRepository.save(reservation);
                });

        return reservationExistsBetweenDates(reservationCommand).onItem()
                .ifNull()
                .switchTo(createReservation);
    }

    private Uni<String> update(String id, ReservationCommand reservationCommand) {
        Uni<String> updateReservation = reservationRepository.getById(id).onItem()
                .ifNull().failWith(() -> new ReservationNotFoundException(id)).onItem()
                .transformToUni(reservation -> {
                    Customer updatedCustomer = new Customer(reservation.getCustomerId(),
                            reservationCommand.getEmail(),
                            reservationCommand.getFullName());

                    Reservation updatedReservation = new Reservation(reservation.getId(),
                            updatedCustomer.getId(),
                            reservationCommand.getArrivalDate(),
                            reservationCommand.getDepartureDate());

                    return customerService.updateCustomer(updatedCustomer).onItem()
                            .transformToUni(customer ->
                                    reservationRepository.update(updatedReservation).onItem()
                                            .transformToUni(postUpdate -> Uni.createFrom().item(postUpdate.getId())));
                });

        return reservationExistsBetweenDates(id, reservationCommand).onItem()
                .ifNull()
                .switchTo(updateReservation);
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

    private List<LocalDate> getReservedDates(LocalDate localStartDate, LocalDate localEndDate) {
        return reservationRepository
                .getReservedDates(localStartDate, localEndDate)
                .collectItems().asList()
                .await().atMost(REQUEST_TIMEOUT);
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
