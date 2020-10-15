package com.ajanoni.service;

import com.ajanoni.model.Customer;
import com.ajanoni.model.Reservation;
import com.ajanoni.repository.ReservationRepository;
import com.ajanoni.rest.dto.AvailableResult;
import com.ajanoni.rest.dto.ReservationCommand;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class BookingService {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private final CustomerService customerService;
    private final ReservationRepository reservationRepository;

    @Inject
    public BookingService(CustomerService customerService, ReservationRepository reservationRepository) {
        this.customerService = customerService;
        this.reservationRepository = reservationRepository;
    }

    public Uni<String> createReservation(ReservationCommand reservationCommand) {
        long days = ChronoUnit.DAYS.between(reservationCommand.getArrivalDate(),
                reservationCommand.getDepartureDate());

        if(reservationCommand.getDepartureDate().isBefore(reservationCommand.getArrivalDate())) {

        }

        if (days > 3) {
            throw new IllegalArgumentException("test");
        }

        // one day ahead of arrival (arrival today + 1)
        // 1 month in advance (departureDate < today + 1 month && arrivalDate < today + 1 month

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
                return Uni.createFrom().failure(() -> new IllegalArgumentException("exception created at "
                        + "subscription time"));
            }

            return Uni.createFrom().nothing();
        });
    }
}
