package com.ajanoni.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ajanoni.repository.model.Customer;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.mysqlclient.MySQLPool;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.sqlclient.PoolOptions;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.After;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@ExtendWith(MockitoExtension.class)
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CustomerRepositoryTest {

    private static final String EMAIL = "email@email.com";
    private static final String FULL_NAME = "full name";

    @Container
    private static final MySQLContainer CONTAINER = new MySQLContainer()
            .withDatabaseName("booking")
            .withUsername("usertest")
            .withPassword("passwordtest");

    private CustomerRepositoryImpl testInstance;
    private Customer customer;
    private Vertx vertx;
    private MySQLPool pool;

    @BeforeAll
    void setUp() throws IOException {
        vertx = Vertx.vertx();

        MySQLConnectOptions options = new MySQLConnectOptions()
                .setPort(CONTAINER.getMappedPort(3306))
                .setHost(CONTAINER.getContainerIpAddress())
                .setDatabase("booking")
                .setUser("usertest")
                .setPassword("passwordtest");

        pool = MySQLPool.pool(vertx, options, new PoolOptions());

        Path initFile = Path.of("", "src/test/resources").resolve("mysql_db_init.sql");
        String dbInit = Files.readString(initFile);

        pool.query(dbInit).executeAndAwait();
    }

    @After
    public void tearDown() {
        pool.close();
        vertx.closeAndAwait();
    }

    @BeforeEach
    void setup() {
        testInstance = new CustomerRepositoryImpl(pool);
        customer = new Customer(EMAIL, FULL_NAME);
        pool.query("DELETE FROM customers;").executeAndAwait();
    }

    @Test
    void save() {
        String resultValue = testInstance.save(customer).await().indefinitely();
        String id = pool.query("SELECT ID FROM customers;").executeAndAwait()
                .iterator()
                .next()
                .getString("ID");

        assertThat(resultValue).isEqualTo(id);
    }

    @Test
    void update() {
        String resultValue = testInstance.save(customer).await().indefinitely();

        Customer updateCustomer = new Customer(resultValue, "new email", "new fullname");
        Customer resultCustomer = testInstance.update(updateCustomer).await().indefinitely();

        assertThat(resultCustomer).isEqualTo(updateCustomer);
    }

    @Test
    void getByEmail() {
        testInstance.save(customer).await().indefinitely();
        Customer resultCustomer = testInstance.getByEmail(EMAIL).await().indefinitely();

        assertThat(resultCustomer.getEmail()).isEqualTo(customer.getEmail());
        assertThat(resultCustomer.getFullName()).isEqualTo(customer.getFullName());
    }

    @Test
    void getByEmailNotFound() {
        Customer resultCustomer = testInstance.getByEmail("not found").await().indefinitely();

        assertThat(resultCustomer).isNull();
    }

}
