package com.ajanoni.service;

import com.ajanoni.model.Customer;
import com.ajanoni.repository.CustomerRepository;
import com.ajanoni.rest.dto.ReservationCommand;
import io.smallrye.mutiny.Uni;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Inject
    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    Uni<String> updateOrCreateCustomer(ReservationCommand reservationCommand) {
        return getCustomerByEmail(reservationCommand).onItem()
                .ifNull()
                .switchTo(() -> createCustomer(reservationCommand))
                .onItem()
                .ifNotNull()
                .transformToUni(id -> updateCustomer(id, reservationCommand));
    }

    private Uni<String> createCustomer(ReservationCommand reservationCommand) {
        Customer newCustomer = new Customer(reservationCommand.getEmail(),
                reservationCommand.getFullName());

        return customerRepository.save(newCustomer);
    }

    Uni<Customer> updateCustomer(Customer customer) {
        return customerRepository.update(customer);
    }

    private Uni<String> updateCustomer(String customerId, ReservationCommand reservationCommand) {
        Customer updateCustomer = new Customer(customerId,
                reservationCommand.getEmail(),
                reservationCommand.getFullName());

        return customerRepository.update(updateCustomer).onItem()
                .transformToUni(customer -> Uni.createFrom().item(customer.getId()));
    }

    private Uni<String> getCustomerByEmail(ReservationCommand reservationCommand) {
        return customerRepository.getByEmail(reservationCommand.getEmail()).onItem()
                .transformToUni(customer -> Uni.createFrom().item(customer.getId()));
    }

}
