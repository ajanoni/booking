package com.ajanoni.service.customer;

import com.ajanoni.repository.model.Customer;
import com.ajanoni.repository.CustomerRepository;
import io.smallrye.mutiny.Uni;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class CustomerCommandHandler {

    private final CustomerRepository customerRepository;

    @Inject
    public CustomerCommandHandler(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Uni<String> updateOrCreateCustomer(String email, String fullName) {
        return getCustomerByEmail(email).onItem()
                .ifNull()
                .switchTo(() -> createCustomer(email, fullName))
                .onItem()
                .ifNotNull()
                .transformToUni(id -> updateCustomer(id, email, fullName));
    }

    public Uni<String> updateCustomer(String customerId, String email, String fullName) {
        Customer updateCustomer = new Customer(customerId, email, fullName);

        return customerRepository.update(updateCustomer).onItem()
                .transformToUni(customer -> Uni.createFrom().item(customer.getId()));
    }

    private Uni<String> createCustomer(String email, String fullName) {
        Customer newCustomer = new Customer(email, fullName);

        return customerRepository.save(newCustomer);
    }

    private Uni<String> getCustomerByEmail(String email) {
        return customerRepository.getByEmail(email).onItem()
                .ifNotNull()
                .transformToUni(customer -> Uni.createFrom().item(customer.getId()));
    }

}
