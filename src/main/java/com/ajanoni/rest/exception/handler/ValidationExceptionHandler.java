package com.ajanoni.rest.exception.handler;

import com.ajanoni.rest.exception.ErrorResponses;
import java.util.List;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import org.jboss.resteasy.api.validation.ResteasyConstraintViolation;
import org.jboss.resteasy.api.validation.ResteasyViolationException;

@Provider
public class ValidationExceptionHandler implements ExceptionMapper<ResteasyViolationException> {
    @Override
    public Response toResponse(ResteasyViolationException exception) {
        List<String> messages = exception.getViolations().stream()
                .map(ResteasyConstraintViolation::getMessage)
                .collect(Collectors.toList());

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(ErrorResponses.of(messages))
                .build();
    }
}
