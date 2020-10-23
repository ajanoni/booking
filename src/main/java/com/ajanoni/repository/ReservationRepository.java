package com.ajanoni.repository;

import com.ajanoni.repository.model.Reservation;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import java.time.LocalDate;

public interface ReservationRepository {

    Uni<String> save(Reservation data);

    Uni<Reservation> update(Reservation data);

    Uni<Boolean> delete(String id);

    Uni<Reservation> getById(String id);

    Multi<LocalDate> getReservedDates(LocalDate startDate, LocalDate endDate);

    Uni<Boolean> hasReservationBetween(String reservationId, LocalDate startDate, LocalDate endDate);

}
