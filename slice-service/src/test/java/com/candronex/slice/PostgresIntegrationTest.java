package com.candronex.slice;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Socle des tests d'intégration : un PostgreSQL 16 réel, démarré par Testcontainers, sur lequel
 * Flyway applique les migrations. Réseau simulé sans échec ni délai dépassé, latence nulle.
 */
@SpringBootTest(properties = {
        "candronex.network.mock.latency=0",
        "candronex.network.mock.failure-rate=0",
        "candronex.network.mock.timeout-rate=0"})
public abstract class PostgresIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("slice");

    static {
        POSTGRES.start();
    }
}
