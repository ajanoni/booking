package com.ajanoni.rest.exception.handler;

import com.ajanoni.exception.ReservationRequestException;
import com.ajanoni.rest.exception.ErrorResponses;
import java.util.List;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class ReservationRequestExceptionHandler implements ExceptionMapper<ReservationRequestException> {
    @Override
    public Response toResponse(ReservationRequestException exception) {
        List<String> messages = exception.getMessages().stream()
                .collect(Collectors.toList());

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(ErrorResponses.of(messages))
                .build();
    }
}
