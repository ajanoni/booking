package com.ajanoni.service;

import com.ajanoni.exception.LockAcquireException;
import com.ajanoni.exception.ReservationConflictException;
import com.ajanoni.exception.ReservationNotFoundException;
import com.ajanoni.exception.ReservationRequestException;
import com.ajanoni.model.Customer;
import com.ajanoni.model.Reservation;
import com.ajanoni.repository.ReservationRepository;
import com.ajanoni.rest.dto.AvailableResult;
import com.ajanoni.rest.dto.ReservationCommand;
import com.ajanoni.service.lock.RedissonLockHandler;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import java.time.Duration;
import java.time.LocalDate;
import java.time.chrono.ChronoLocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class BookingService {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(1);
    private static final int MAX_DAYS_ALLOWED = 3;
    private static final int START_RESERVATION_OFFSET = 1;
    private static final int MONTHS_WINDOW_ALLOWED = 1;
    private static final String RESERVATION_CONFLICT = "Other reservation conflicts with the selected dates.";

    private final CustomerService customerService;
    private final ReservationRepository reservationRepository;
    private final RedissonLockHandler lockHandler;

    @Inject
    public BookingService(CustomerService customerService, ReservationRepository reservationRepository,
            RedissonLockHandler lockHandler) {
        this.customerService = customerService;
        this.reservationRepository = reservationRepository;
        this.lockHandler = lockHandler;
    }

    public Uni<String> createReservationWithLock(ReservationCommand reservationCommand) {
        return lockHandler
                .executeWithLock(getLockDates(reservationCommand), () -> createReservation(reservationCommand))
                .onFailure(LockAcquireException.class)
                .recoverWithUni(Uni.createFrom().failure(() -> new ReservationConflictException(RESERVATION_CONFLICT)));
    }

    public Uni<String> updateReservationWithLock(String id, ReservationCommand reservationCommand) {
        return lockHandler
                .executeWithLock(getLockDates(reservationCommand), () -> updateReservation(id, reservationCommand))
                .onFailure(LockAcquireException.class)
                .recoverWithUni(Uni.createFrom().failure(() -> new ReservationConflictException(RESERVATION_CONFLICT)));
    }

    public Uni<Boolean> deleteReservation(String id) {
        return reservationRepository.delete(id);
    }

    public Multi<AvailableResult> getAvailableDates(LocalDate startDate, LocalDate endDate) {
        List<LocalDate> reservedDates = reservationRepository
                .getReservedDates(startDate, endDate)
                .collectItems().asList()
                .await().atMost(REQUEST_TIMEOUT);

        Stream<AvailableResult> resultStream = getContinuousDates(startDate, endDate).stream()
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

    private Uni<String> createReservation(ReservationCommand reservationCommand) {
        reservationValidation(reservationCommand);

        Uni<String> createReservation = customerService.updateOrCreateCustomer(reservationCommand).onItem()
                .transformToUni(id -> {
                    Reservation reservation = new Reservation(id,
                            reservationCommand.getArrivalDate(),
                            reservationCommand.getDepartureDate());

                    return reservationRepository.save(reservation);
                });

        return reserveExists(reservationCommand).onItem()
                .ifNull()
                .switchTo(createReservation);
    }

    private Uni<String> updateReservation(String id, ReservationCommand reservationCommand) {
        reservationValidation(reservationCommand);

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

        return reserveExists(id, reservationCommand).onItem()
                .ifNull()
                .switchTo(updateReservation);
    }

    private Uni<String> reserveExists(ReservationCommand reservationCommand) {
        return reserveExists("", reservationCommand);
    }

    private Uni<String> reserveExists(String id, ReservationCommand reservationCommand) {
        return reservationRepository.hasReservationBetween(id, reservationCommand.getArrivalDate(),
                reservationCommand.getDepartureDate()).onItem().ifNotNull()
                .transformToUni(hasReservation -> {
                    if (hasReservation) {
                        return Uni.createFrom().failure(() -> new ReservationConflictException(RESERVATION_CONFLICT));
                    }

                    return Uni.createFrom().nullItem();
                });
    }

    private void reservationValidation(ReservationCommand reservationCommand) {
        LocalDate arrivalDate = reservationCommand.getArrivalDate();
        LocalDate departureDate = reservationCommand.getDepartureDate();

        checkDatesOrder(arrivalDate, departureDate);
        checkMaxDays(arrivalDate, departureDate);
        checkStartOffset(arrivalDate);
        checkInsideWindow(arrivalDate, departureDate);
    }

    private void checkInsideWindow(LocalDate arrivalDate, ChronoLocalDate departureDate) {
        LocalDate oneMonthAhead = LocalDate.now().plusMonths(MONTHS_WINDOW_ALLOWED);
        if (departureDate.isAfter(oneMonthAhead) || arrivalDate.isAfter(oneMonthAhead)) {
            throw new ReservationRequestException("Maximum one month ahead allowed for arrival or departure date.");
        }
    }

    private void checkStartOffset(LocalDate arrivalDate) {
        if (arrivalDate.isBefore(LocalDate.now().plusDays(START_RESERVATION_OFFSET))) {
            throw new ReservationRequestException("Not allowed to book for today.");
        }
    }

    private void checkMaxDays(LocalDate arrivalDate, LocalDate departureDate) {
        long days = ChronoUnit.DAYS.between(arrivalDate, departureDate.plusDays(1));
        if (days > MAX_DAYS_ALLOWED) {
            throw new ReservationRequestException("Maximum 3 days of reservation is allowed.");
        }
    }

    private void checkDatesOrder(LocalDate arrivalDate, LocalDate departureDate) {
        if (departureDate.isBefore(arrivalDate)) {
            throw new ReservationRequestException("Departure date before arrival date.");
        }
    }
}
