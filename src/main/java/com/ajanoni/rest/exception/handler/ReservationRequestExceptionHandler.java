package com.ajanoni.rest.exception.handler;

import com.ajanoni.exception.ReservationRequestException;
import com.ajanoni.rest.exception.ErrorResponses;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class ReservationRequestExceptionHandler implements ExceptionMapper<ReservationRequestException> {
    @Override
    public Response toResponse(ReservationRequestException exception) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(ErrorResponses.of(exception.getMessage()))
                .build();
    }
}
