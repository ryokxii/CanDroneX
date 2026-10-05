package com.candronex;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.UUID;

/**
 * Socle des tests d'intégration : un PostgreSQL 16 réel, démarré par Testcontainers, sur lequel
 * Flyway applique les migrations.
 *
 * <p>Conteneur singleton, démarré une fois pour toute la suite : le contexte Spring est mis en
 * cache entre les classes de test, il doit donc pointer vers un conteneur qui survit à chacune.
 * Ryuk le supprime à la fin de la JVM.
 */
@SpringBootTest
public abstract class PostgresIntegrationTest {

    /** Client de démonstration amorcé par la migration V4. */
    protected static final UUID DEMO_CLIENT =
            UUID.fromString("0b6f1c2e-8f4a-4d3b-9c71-5e2a7d9f3b10");

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("candronex");

    static {
        POSTGRES.start();
    }
}
