package com.ajanoni.rest.exception.handler;

import com.ajanoni.rest.exception.ErrorResponses;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class ExceptionHandler implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {
        exception.printStackTrace();
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(ErrorResponses.of("Something unexpected happened."))
                .build();
    }
}
