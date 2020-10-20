package com.ajanoni.repository;

import static java.lang.String.format;

import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.mysqlclient.MySQLPool;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.sqlclient.PoolOptions;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.After;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@ExtendWith(MockitoExtension.class)
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DbTest {

    @Container
    private static final MySQLContainer CONTAINER = new MySQLContainer()
            .withDatabaseName("booking")
            .withUsername("usertest")
            .withPassword("passwordtest");

    Vertx vertx;
    MySQLPool pool;

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

    String getId(String table) {
        return pool.query(format("SELECT ID FROM %s;", table)).executeAndAwait()
                .iterator()
                .next()
                .getString("ID");
    }
}
