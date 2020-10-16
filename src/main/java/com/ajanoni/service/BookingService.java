package com.ajanoni.service;

import com.ajanoni.exception.ReservationRequestException;
import com.ajanoni.model.Customer;
import com.ajanoni.model.Reservation;
import com.ajanoni.repository.ReservationRepository;
import com.ajanoni.rest.dto.AvailableResult;
import com.ajanoni.rest.dto.ReservationCommand;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import java.time.Duration;
import java.time.LocalDate;
import java.time.chrono.ChronoLocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class BookingService {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
    private static final int MAX_DAYS_ALLOWED = 3;
    private static final int START_RESERVATION_OFFSET = 1;
    private static final int MONTHS_WINDOW_ALLOWED = 1;
    private static final String RESERVATION_CONFLICT = "Another reservation has been "
            + "made for your selected dates.";

    private final CustomerService customerService;
    private final ReservationRepository reservationRepository;

    @Inject
    public BookingService(CustomerService customerService, ReservationRepository reservationRepository) {
        this.customerService = customerService;
        this.reservationRepository = reservationRepository;
    }

    public Uni<String> createReservation(ReservationCommand reservationCommand) {
        reservationValidation(reservationCommand);

        Uni<String> createReservation =
                customerService.updateOrCreateCustomer(reservationCommand).onItem().transformToUni(id -> {
                    Reservation reservation = new Reservation(id, reservationCommand.getArrivalDate(),
                            reservationCommand.getDepartureDate());
                    return reservationRepository.save(reservation);
                });

        Uni<String> reserveExists = reserveExists(reservationCommand);
        return reserveExists.ifNoItem().after(REQUEST_TIMEOUT).recoverWithUni(createReservation);
    }

    public Uni<String> updateReservation(String id, ReservationCommand reservationCommand) {
        reservationValidation(reservationCommand);

        Uni<String> updateReservation = reservationRepository.getById(id).onItem().transformToUni(reservation -> {
            Customer updatedCustomer = new Customer(reservation.getId(),
                    reservationCommand.getEmail(),
                    reservationCommand.getFullName());
            customerService.updateCustomer(updatedCustomer).await().atMost(REQUEST_TIMEOUT);

            Reservation updatedReservation = new Reservation(reservation.getId(),
                    reservation.getCustomerId(),
                    reservation.getArrivalDate(),
                    reservation.getDepartureDate());

            return reservationRepository.update(updatedReservation).onItem()
                    .transformToUni(postUpdate -> Uni.createFrom().item(postUpdate.getId()));
        });

        Uni<String> reserveExists = reserveExists(reservationCommand);
        return reserveExists.ifNoItem().after(REQUEST_TIMEOUT).recoverWithUni(updateReservation);
    }

    public Uni<Boolean> deleteReservation(String id) {
        return reservationRepository.delete(id);
    }

    public Multi<AvailableResult> getAvailableDates(LocalDate startDate, LocalDate endDate) {
        List<LocalDate> availableDates = startDate.datesUntil(endDate.plusDays(1)).collect(Collectors.toList());
        List<LocalDate> reservedDates = reservationRepository.getReservedDates(startDate, endDate).collectItems()
                .asList().await().atMost(REQUEST_TIMEOUT);

        Stream<AvailableResult> resultStream = availableDates.stream()
                .filter(date -> !reservedDates.contains(date))
                .map(AvailableResult::new);

        return Multi.createFrom().items(resultStream);
    }


    private Uni<String> reserveExists(ReservationCommand reservationCommand) {
        return reservationRepository.hasReservationBetween(reservationCommand.getArrivalDate(),
                reservationCommand.getDepartureDate()).flatMap(hasReservation -> {
            if (hasReservation) {
                return Uni.createFrom().failure(() -> new ReservationRequestException(RESERVATION_CONFLICT));
            }

            return Uni.createFrom().nothing();
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

    private void checkInsideWindow(ChronoLocalDate arrivalDate, ChronoLocalDate departureDate) {
        LocalDate oneMonthAhead = LocalDate.now().plusMonths(MONTHS_WINDOW_ALLOWED);
        if(departureDate.isAfter(oneMonthAhead) || arrivalDate.isAfter(oneMonthAhead)) {
            throw new ReservationRequestException("Maximum one month ahead allowed for arrival or departure date.");
        }
    }

    private void checkStartOffset(ChronoLocalDate arrivalDate) {
        if(arrivalDate.isBefore(LocalDate.now().plusDays(START_RESERVATION_OFFSET))) {
            throw new ReservationRequestException("Not allowed to book for today.");
        }
    }

    private void checkMaxDays(Temporal arrivalDate, Temporal departureDate) {
        long days = ChronoUnit.DAYS.between(arrivalDate, departureDate);
        if (days > MAX_DAYS_ALLOWED) {
            throw new ReservationRequestException("Maximum 3 days of reservation is allowed.");
        }
    }

    private void checkDatesOrder(ChronoLocalDate arrivalDate, ChronoLocalDate departureDate) {
        if (departureDate.isBefore(arrivalDate)) {
            throw new ReservationRequestException("Departure date before arrival date.");
        }
    }
}
