package com.ajanoni.rest.exception.handler;

import com.ajanoni.exception.ReservationConflictException;
import com.ajanoni.rest.exception.ErrorResponses;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class ReservationConflictExceptionHandler implements ExceptionMapper<ReservationConflictException> {
    @Override
    public Response toResponse(ReservationConflictException exception) {
        return Response.status(Response.Status.CONFLICT)
                .entity(ErrorResponses.of(exception.getMessage()))
                .build();
    }
}
