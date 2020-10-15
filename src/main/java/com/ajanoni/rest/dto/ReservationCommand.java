package com.ajanoni.rest.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.LocalDate;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.hibernate.validator.constraints.Length;

@RegisterForReflection
@Schema(name = "CreateReservationCommand", description = "Create reservation")
public class ReservationCommand {

    @NotBlank
    @Length(max = 255)
    private final String fullName;

    @NotBlank
    @Email
    @Length(max = 255)
    private final String email;

    @NotBlank
    private final LocalDate arrivalDate;

    @NotBlank
    private final LocalDate departureDate;

    @JsonCreator
    public ReservationCommand(String fullName, String email, LocalDate arrivalDate, LocalDate departureDate) {
        this.fullName = fullName;
        this.email = email;
        this.arrivalDate = arrivalDate;
        this.departureDate = departureDate;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public LocalDate getArrivalDate() {
        return arrivalDate;
    }

    public LocalDate getDepartureDate() {
        return departureDate;
    }
}
