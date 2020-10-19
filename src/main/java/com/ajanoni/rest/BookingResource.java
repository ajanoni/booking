package com.ajanoni.rest;

import com.ajanoni.dto.AvailableDatesResult;
import com.ajanoni.dto.ReservationCommand;
import com.ajanoni.dto.ReservationCommandResult;
import com.ajanoni.service.booking.BookingCommandHandler;
import com.ajanoni.service.booking.BookingQueryHandler;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import java.time.LocalDate;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("/booking")
public class BookingResource {

    private final BookingCommandHandler bookingCommand;
    private final BookingQueryHandler queryCommand;

    public BookingResource(BookingCommandHandler bookingCommand, BookingQueryHandler queryCommand) {
        this.bookingCommand = bookingCommand;
        this.queryCommand = queryCommand;
    }

    @GET
    @Path("hello")
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "hello";
    }

    @GET
    @Path("/schedule")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Multi<AvailableDatesResult> getAvailableDays(@QueryParam("startDate") LocalDate startDate,
            @QueryParam("endDate") LocalDate endDate) {
        return queryCommand.getAvailableDates(startDate, endDate);
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<ReservationCommandResult> createReservation(@Valid ReservationCommand reservationCommand) {
        return bookingCommand
                .createReservationWithLock(reservationCommand)
                .map(ReservationCommandResult::new);
    }

    @PUT
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<ReservationCommandResult> updateReservation(@PathParam("id") String id,
            @Valid ReservationCommand reservationCommand) {
        return bookingCommand
                .updateReservationWithLock(id, reservationCommand)
                .map(ReservationCommandResult::new);
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<ReservationCommandResult> updateReservation(@PathParam("id") String id) {
        return bookingCommand
                .deleteReservation(id)
                .map(ReservationCommandResult::new);
    }
}
