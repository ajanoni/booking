package com.ajanoni.repository.model;

import java.time.LocalDate;

public class Reservation {

    private final String id;
    private final String customerId;
    private final LocalDate arrivalDate;
    private final LocalDate departureDate;

    public Reservation(String id, String customerId, LocalDate arrivalDate, LocalDate departureDate) {
        this.id = id;
        this.customerId = customerId;
        this.arrivalDate = arrivalDate;
        this.departureDate = departureDate;
    }

    public Reservation(String customerId, LocalDate arrivalDate, LocalDate departureDate) {
        id = null;
        this.customerId = customerId;
        this.arrivalDate = arrivalDate;
        this.departureDate = departureDate;
    }

    public String getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public LocalDate getArrivalDate() {
        return arrivalDate;
    }

    public LocalDate getDepartureDate() {
        return departureDate;
    }
}
