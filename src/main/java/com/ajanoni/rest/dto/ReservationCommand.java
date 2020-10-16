package com.ajanoni.rest.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.LocalDate;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.hibernate.validator.constraints.Length;

@RegisterForReflection
@Schema(name = "CreateReservationCommand", description = "Create reservation")
public class ReservationCommand {

    @NotBlank(message = "Field fullName is required.")
    @Length(max = 255)
    private final String fullName;

    @NotBlank(message = "Field email is required.")
    @Email(message = "Invalid email address.")
    @Length(max = 255)
    private final String email;

    @NotNull(message = "Field arrivalDate is required.")
    private final LocalDate arrivalDate;

    @NotNull(message = "Field departureDate is required.")
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
