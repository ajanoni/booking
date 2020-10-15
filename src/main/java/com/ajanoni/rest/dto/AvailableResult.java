package com.ajanoni.rest.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@RegisterForReflection
@Schema(name = "AvailableResult", description = "Available days for reservation")
public class AvailableResult {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private final LocalDate date;

    public AvailableResult(LocalDate date) {
        this.date = date;
    }

    public LocalDate getDate() {
        return date;
    }
}
