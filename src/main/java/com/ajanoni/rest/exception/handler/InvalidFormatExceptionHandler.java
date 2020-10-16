package com.ajanoni.rest.exception.handler;

import static java.lang.String.format;

import com.ajanoni.rest.exception.ErrorResponses;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import java.util.List;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class InvalidFormatExceptionHandler implements ExceptionMapper<InvalidFormatException> {
    @Override
    public Response toResponse(InvalidFormatException exception) {
        List<String> messages = exception.getPath().stream()
                .map(JsonMappingException.Reference::getFieldName)
                .map(field -> format("Invalid format for field: %s", field))
                .collect(Collectors.toList());

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(ErrorResponses.of(messages))
                .build();
    }
}
