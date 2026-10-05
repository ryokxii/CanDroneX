package com.candronex;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

/**
 * Socle des tests d'intégration : un PostgreSQL 16 réel, démarré par Testcontainers, sur lequel
 * Flyway applique les migrations.
 */
@SpringBootTest
@Testcontainers
public abstract class PostgresIntegrationTest {

    /** Client de démonstration amorcé par la migration V4. */
    protected static final UUID DEMO_CLIENT =
            UUID.fromString("0b6f1c2e-8f4a-4d3b-9c71-5e2a7d9f3b10");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("candronex");
}
