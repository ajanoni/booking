package com.ajanoni.repository.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CustomerTest {

    private static final String ID = "id";
    private static final String EMAIL = "email";
    private static final String FULL_NAME = "fullName";

    @Test
    void constructWithThreeParameters() {
        Customer customer = new Customer(ID, EMAIL, FULL_NAME);

        assertThat(customer.getId()).isEqualTo(ID);
        assertThat(customer.getEmail()).isEqualTo(EMAIL);
        assertThat(customer.getFullName()).isEqualTo(FULL_NAME);
    }

    @Test
    void constructWithTwoParameters() {
        Customer customer = new Customer(EMAIL, FULL_NAME);

        assertThat(customer.getId()).isNull();
        assertThat(customer.getEmail()).isEqualTo(EMAIL);
        assertThat(customer.getFullName()).isEqualTo(FULL_NAME);
    }

}
