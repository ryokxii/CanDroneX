package com.candronex;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vérifie que la base se recrée de façon reproductible — critère d'acceptation du critère 4 de la
 * grille.
 */
class SchemaMigrationIT extends PostgresIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("les cinq migrations sont appliquées avec succès")
    void appliesAllMigrations() {
        List<String> applied = jdbc.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success = true ORDER BY installed_rank",
                String.class);

        assertThat(applied).containsExactly("1", "2", "3", "4", "5");
    }

    @Test
    @DisplayName("un schéma par module persisté, et aucun autre")
    void createsOneSchemaPerPersistedModule() {
        List<String> schemas = jdbc.queryForList(
                """
                SELECT schema_name FROM information_schema.schemata
                WHERE schema_name NOT LIKE 'pg_%' AND schema_name <> 'information_schema'
                ORDER BY schema_name
                """,
                String.class);

        // ServiceCatalog n'a pas de schéma : son offre est figée dans le code.
        assertThat(schemas).containsExactly("access", "drone", "orders", "public");
    }

    @Test
    @DisplayName("Hibernate n'a créé aucune table : ddl-auto est désactivé")
    void ormDoesNotTouchTheSchema() {
        List<String> tables = jdbc.queryForList(
                """
                SELECT table_schema || '.' || table_name FROM information_schema.tables
                WHERE table_schema IN ('access', 'drone', 'orders')
                ORDER BY 1
                """,
                String.class);

        assertThat(tables).containsExactly(
                "access.api_credentials",
                "access.clients",
                "drone.drones",
                "orders.order_lines",
                "orders.service_orders");
    }

    @Test
    @DisplayName("le client de démonstration est amorcé, et lui seul")
    void seedsOnlyTheDemoClient() {
        List<String> clients = jdbc.queryForList(
                "SELECT client_id FROM access.clients", String.class);
        assertThat(clients).containsExactly("CLI-INSPECTRA");

        // Aucun drone ni commande : UC-02 et UC-04 doivent les produire.
        assertThat(jdbc.queryForObject("SELECT count(*) FROM drone.drones", Integer.class))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM orders.service_orders", Integer.class))
                .isZero();
    }
}
