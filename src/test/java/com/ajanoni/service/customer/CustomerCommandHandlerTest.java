package com.ajanoni.service.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.ajanoni.repository.CustomerRepository;
import com.ajanoni.repository.model.Customer;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerCommandHandlerTest {

    private static final String EMAIL = "email@email";
    private static final String FULL_NAME = "full name";
    private static final String FULL_NAME_UPDATED = "full name updated";
    private static final String ID = "id";

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerCommandHandler testInstance;

    @Test
    void updateOrCreateCustomerWhenExists() {
        given(customerRepository.getByEmail(EMAIL)).willReturn(Uni.createFrom().item(getCustomer()));
        mockUpdate();

        String idReturned = testInstance.updateOrCreateCustomer(EMAIL, FULL_NAME_UPDATED).await().indefinitely();

        assertThat(idReturned).isEqualTo(ID);
    }

    @Test
    void updateOrCreateCustomerWhenNotExists() {
        given(customerRepository.getByEmail(EMAIL)).willReturn(Uni.createFrom().nullItem());
        Customer customer = Customer.builder()
                .email(EMAIL)
                .fullName(FULL_NAME)
                .build();
        given(customerRepository.save(customer)).willReturn(Uni.createFrom().item(ID));

        String idReturned = testInstance.updateOrCreateCustomer(EMAIL, FULL_NAME).await().indefinitely();

        assertThat(idReturned).isEqualTo(ID);
    }

    @Test
    void updateCustomer() {
        mockUpdate();

        String idReturned = testInstance.updateCustomer(ID, EMAIL, FULL_NAME_UPDATED).await().indefinitely();

        assertThat(idReturned).isEqualTo(ID);
    }

    private void mockUpdate() {
        Customer updatedCustomer = Customer.builder()
                .id(ID)
                .email(EMAIL)
                .fullName(FULL_NAME_UPDATED)
                .build();
        given(customerRepository.update(updatedCustomer)).willReturn(Uni.createFrom().item(updatedCustomer));
    }

    private Customer getCustomer() {
        return Customer.builder()
                .id(ID)
                .email(EMAIL)
                .fullName(FULL_NAME).build();
    }

}
