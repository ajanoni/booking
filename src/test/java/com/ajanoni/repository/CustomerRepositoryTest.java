package com.ajanoni.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ajanoni.repository.model.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CustomerRepositoryTest extends DbTest {

    private static final String EMAIL = "email@email.com";
    private static final String FULL_NAME = "full name";

    private CustomerRepositoryImpl testInstance;
    private Customer customer;

    @BeforeEach
    void setup() {
        testInstance = new CustomerRepositoryImpl(getPool());
        customer = Customer.builder()
                .email(EMAIL)
                .fullName(FULL_NAME)
                .build();
        getPool().query("DELETE FROM customers;").executeAndAwait();
    }

    @Test
    void save() {
        String resultValue = testInstance.save(customer).await().indefinitely();
        String id = getId("customers");

        assertThat(resultValue).isEqualTo(id);
    }

    @Test
    void update() {
        String customerId = testInstance.save(customer).await().indefinitely();

        Customer updateCustomer = Customer.builder()
                .id(customerId)
                .email("new email")
                .fullName("new fullname")
                .build();

        Customer resultCustomer = testInstance.update(updateCustomer).await().indefinitely();

        assertThat(resultCustomer).isEqualTo(updateCustomer);
    }

    @Test
    void getByEmail() {
        testInstance.save(customer).await().indefinitely();
        Customer resultCustomer = testInstance.getByEmail(EMAIL).await().indefinitely();

        assertThat(resultCustomer.email()).isEqualTo(customer.email());
        assertThat(resultCustomer.fullName()).isEqualTo(customer.fullName());
    }

    @Test
    void getByEmailNotFound() {
        Customer resultCustomer = testInstance.getByEmail("not found").await().indefinitely();

        assertThat(resultCustomer).isNull();
    }

}
