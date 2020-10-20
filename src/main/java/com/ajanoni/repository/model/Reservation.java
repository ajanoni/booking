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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Reservation that = (Reservation) o;

        if (getId() != null ? !getId().equals(that.getId()) : that.getId() != null) {
            return false;
        }
        if (!getCustomerId().equals(that.getCustomerId())) {
            return false;
        }
        if (!getArrivalDate().equals(that.getArrivalDate())) {
            return false;
        }
        return getDepartureDate().equals(that.getDepartureDate());
    }

    @Override
    public int hashCode() {
        int result = getId() != null ? getId().hashCode() : 0;
        result = 31 * result + getCustomerId().hashCode();
        result = 31 * result + getArrivalDate().hashCode();
        result = 31 * result + getDepartureDate().hashCode();
        return result;
    }
}
