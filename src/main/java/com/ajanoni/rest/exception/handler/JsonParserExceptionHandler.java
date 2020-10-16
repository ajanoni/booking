package com.ajanoni.rest.exception.handler;

import static java.lang.String.format;

import com.ajanoni.rest.exception.ErrorResponses;
import com.fasterxml.jackson.core.JsonParseException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class JsonParserExceptionHandler implements ExceptionMapper<JsonParseException> {
    @Override
    public Response toResponse(JsonParseException exception) {
        String message = format("Error on parsing json at line %d and column %d. Message: %s",
                exception.getLocation().getLineNr(), exception.getLocation().getColumnNr(),
                exception.getOriginalMessage());
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(ErrorResponses.of(message))
                .build();
    }
}
