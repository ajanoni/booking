package com.ajanoni.repository;

import com.ajanoni.repository.model.Customer;
import io.smallrye.mutiny.Uni;

public interface CustomerRepository {

    Uni<String> save(Customer data);

    Uni<Customer> update(Customer data);

    Uni<Customer> getByEmail(String email);

}
