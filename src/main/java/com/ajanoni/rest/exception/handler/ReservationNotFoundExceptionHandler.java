package com.ajanoni.rest.exception.handler;

import com.ajanoni.exception.ReservationNotFoundException;
import com.ajanoni.exception.ReservationRequestException;
import com.ajanoni.rest.exception.ErrorResponses;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class ReservationNotFoundExceptionHandler implements ExceptionMapper<ReservationNotFoundException> {
    @Override
    public Response toResponse(ReservationNotFoundException exception) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(ErrorResponses.of(exception.getMessage()))
                .build();
    }
}
