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
        Customer updateCustomer = Customer.builder()
                .id(customerId)
                .email(email)
                .fullName(fullName)
                .build();

        return customerRepository.update(updateCustomer).onItem()
                .transformToUni(customer -> Uni.createFrom().item(customer.id()));
    }

    private Uni<String> createCustomer(String email, String fullName) {
        Customer customer = Customer.builder()
                .email(email)
                .fullName(fullName)
                .build();

        return customerRepository.save(customer);
    }

    private Uni<String> getCustomerByEmail(String email) {
        return customerRepository.getByEmail(email).onItem()
                .ifNotNull()
                .transformToUni(customer -> Uni.createFrom().item(customer.id()));
    }

}
