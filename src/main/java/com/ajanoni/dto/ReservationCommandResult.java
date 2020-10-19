package com.ajanoni.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@RegisterForReflection
@Schema(name = "ReservationSaveResult", description = "Saved booking id")
public class ReservationCommandResult {

    private final String id;

    public ReservationCommandResult(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
