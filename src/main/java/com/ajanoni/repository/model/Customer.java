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

}
