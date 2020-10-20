package com.ajanoni.repository.model;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

@Value
@Builder
@Accessors(fluent = true)
@RegisterForReflection
public class Reservation {

    private final String id;
    private final String customerId;
    private final LocalDate arrivalDate;
    private final LocalDate departureDate;

}
