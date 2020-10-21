package com.ajanoni.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.hibernate.validator.constraints.Length;

@RegisterForReflection
@Value
@Builder
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
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private final LocalDate arrivalDate;

    @NotNull(message = "Field departureDate is required.")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private final LocalDate departureDate;

    @JsonCreator
    public ReservationCommand(@JsonProperty("fullName") String fullName, @JsonProperty("email") String email,
            @JsonProperty("arrivalDAte") LocalDate arrivalDate,
            @JsonProperty("departureDate") LocalDate departureDate) {
        this.fullName = fullName;
        this.email = email;
        this.arrivalDate = arrivalDate;
        this.departureDate = departureDate;
    }

}
