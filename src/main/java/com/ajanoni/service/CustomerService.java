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
        return updateCustomer(reservationCommand).onItem()
                .ifNotNull()
                .transformToUni(customer -> Uni.createFrom().item(customer.getId())).onItem()
                .transformToUni(customer -> {
                    Customer newCustomer = new Customer(reservationCommand.getEmail(),
                            reservationCommand.getFullName());

                    return customerRepository.save(newCustomer);
                });
    }

    Uni<Customer> updateCustomer(Customer customer) {
        return customerRepository.update(customer);
    }

    private Uni<Customer> updateCustomer(ReservationCommand reservationCommand) {
        return customerRepository.getByEmail(reservationCommand.getEmail()).onItem()
                .ifNotNull().transformToUni(customer -> {
                    Customer updateCustomer = new Customer(customer.getId(),
                            reservationCommand.getEmail(),
                            reservationCommand.getFullName());

                    return customerRepository.update(updateCustomer);
                });
    }

}
