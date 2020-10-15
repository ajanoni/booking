package com.ajanoni.service;

import com.ajanoni.model.Customer;
import com.ajanoni.repository.CustomerRepository;
import com.ajanoni.rest.dto.ReservationCommand;
import io.smallrye.mutiny.Uni;
import java.time.Duration;

public class CustomerService {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Uni<String> updateOrCreateCustomer(ReservationCommand reservationCommand) {
        return updateCustomer(reservationCommand).onItem().transformToUni(customer ->
                Uni.createFrom().item(customer.getId()))
                .ifNoItem().after(REQUEST_TIMEOUT)
                .recoverWithUni(() -> customerRepository.save(new Customer(reservationCommand.getEmail(),
                        reservationCommand.getFullName())));
    }

    public Uni<Customer> updateCustomer(Customer customer) {
            return customerRepository.update(customer);
    }

    private Uni<Customer> updateCustomer(ReservationCommand reservationCommand) {
        return customerRepository.getByEmail(reservationCommand.getEmail()).onItem().transformToUni(customer -> {
            Customer updateCustomer = new Customer(customer.getId(), reservationCommand.getEmail(),
                    reservationCommand.getFullName());
            return customerRepository.update(updateCustomer);
        });
    }

}
