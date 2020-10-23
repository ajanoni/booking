package com.ajanoni.repository;

import static java.lang.String.format;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.mysqlclient.MySQLPool;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.sqlclient.PoolOptions;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.Getter;
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
@RegisterForReflection
class DbTest {

    private static final String DB_NAME = "booking";
    private static final int DB_PORT = 3306;
    private static final String DB_USER = "usertest";
    private static final String DB_PASSWORD = "passwordtest";
    private static final String TEST_RESOURCES = "src/test/resources";
    private static final String DB_SCRIPT_NAME = "mysql_db_init.sql";
    private static final String DOCKER_IMAGE = "mysql:latest";

    @Container
    private static final MySQLContainer CONTAINER =
            new MySQLContainer(DOCKER_IMAGE)
            .withDatabaseName("booking")
            .withUsername("usertest")
            .withPassword("passwordtest");

    private Vertx vertx;

    @Getter
    private MySQLPool pool;


    DbTest() {
    }

    @BeforeAll
    void setUp() throws IOException {
        vertx = Vertx.vertx();

        MySQLConnectOptions options = new MySQLConnectOptions()
                .setPort(CONTAINER.getMappedPort(DB_PORT))
                .setHost(CONTAINER.getContainerIpAddress())
                .setDatabase(DB_NAME)
                .setUser(DB_USER)
                .setPassword(DB_PASSWORD);

        pool = MySQLPool.pool(vertx, options, new PoolOptions());

        Path initFile = Path.of("", TEST_RESOURCES).resolve(DB_SCRIPT_NAME);
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
