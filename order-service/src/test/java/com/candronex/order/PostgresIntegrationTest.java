package com.candronex.order;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Socle des tests d'intégration : un PostgreSQL 16 réel, démarré par Testcontainers, sur lequel
 * Flyway applique les migrations.
 *
 * <p>Conteneur singleton, démarré une fois pour toute la suite : le contexte Spring est mis en
 * cache entre les classes de test, il doit donc pointer vers un conteneur qui survit à chacune.
 * Ryuk le supprime à la fin de la JVM.
 */
// Pas de tâche planifiée pendant les tests.
@SpringBootTest(properties = "candronex.activation.poll-interval=1h")
public abstract class PostgresIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("order");

    static {
        POSTGRES.start();
    }
}
