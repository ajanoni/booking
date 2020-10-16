package com.ajanoni.rest.exception.handler;

import com.ajanoni.rest.exception.ErrorResponses;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class MismatchedInputExceptionHandler implements ExceptionMapper<MismatchedInputException> {
    @Override
    public Response toResponse(final MismatchedInputException exception) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(ErrorResponses.of("Cannot parse JSON"))
                .build();
    }
}
