package com.ajanoni.repository.model;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class Customer {

    private String id;
    private String email;
    private String fullName;

    public Customer(String id, String email, String fullName) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
    }

    public Customer(String email, String fullName) {
        id = null;
        this.email = email;
        this.fullName = fullName;
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Customer customer = (Customer) o;

        if (getId() != null ? !getId().equals(customer.getId()) : customer.getId() != null) {
            return false;
        }
        if (!getEmail().equals(customer.getEmail())) {
            return false;
        }
        return getFullName().equals(customer.getFullName());
    }

    @Override
    public int hashCode() {
        int result = getId() != null ? getId().hashCode() : 0;
        result = 31 * result + getEmail().hashCode();
        result = 31 * result + getFullName().hashCode();
        return result;
    }
}
