package com.ajanoni.rest.exception.handler;

import com.ajanoni.rest.exception.ErrorResponses;
import java.time.format.DateTimeParseException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class DateTimeParseExceptionHandler implements ExceptionMapper<DateTimeParseException> {
    @Override
    public Response toResponse(DateTimeParseException exception) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(ErrorResponses.of(exception.getMessage()))
                .build();
    }
}
