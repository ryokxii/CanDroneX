package com.candronex;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vérifie que la base se recrée de façon reproductible — critère d'acceptation du critère 4 de la
 * grille.
 */
class SchemaMigrationIT extends PostgresIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("les neuf migrations sont appliquées avec succès")
    void appliesAllMigrations() {
        List<String> applied = jdbc.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success = true ORDER BY installed_rank",
                String.class);

        assertThat(applied).containsExactly("1", "2", "3", "4", "5", "6", "7", "8", "9");
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

        assertThat(schemas).containsExactly("catalog", "customer", "drone", "orders", "public");
    }

    @Test
    @DisplayName("Hibernate n'a créé aucune table : ddl-auto est désactivé")
    void ormDoesNotTouchTheSchema() {
        List<String> tables = jdbc.queryForList(
                """
                SELECT table_schema || '.' || table_name FROM information_schema.tables
                WHERE table_schema IN ('catalog', 'customer', 'drone', 'orders')
                ORDER BY 1
                """,
                String.class);

        assertThat(tables).containsExactly(
                "catalog.characteristic_specifications",
                "catalog.service_specifications",
                "customer.api_credentials",
                "customer.clients",
                "drone.drones",
                "orders.order_line_characteristics",
                "orders.order_lines",
                "orders.service_orders");
    }

    @Test
    @DisplayName("aucune clé étrangère ne franchit la frontière d'un schéma")
    void noForeignKeyCrossesASchema() {
        Integer crossing = jdbc.queryForObject(
                """
                SELECT count(*)
                FROM pg_constraint c
                JOIN pg_class source ON source.oid = c.conrelid
                JOIN pg_class target ON target.oid = c.confrelid
                WHERE c.contype = 'f' AND source.relnamespace <> target.relnamespace
                """,
                Integer.class);

        assertThat(crossing).isZero();
    }

    @Test
    @DisplayName("l'IMSI et l'ICCID n'existent que dans le schéma drone")
    void networkIdentifiersAreConfinedToTheDroneSchema() {
        List<String> columns = jdbc.queryForList(
                """
                SELECT table_schema || '.' || table_name || '.' || column_name
                FROM information_schema.columns
                WHERE column_name IN ('imsi', 'iccid')
                  AND table_schema NOT IN ('pg_catalog', 'information_schema')
                ORDER BY 1
                """,
                String.class);

        assertThat(columns).containsExactly("drone.drones.iccid", "drone.drones.imsi");
    }

    @Test
    @DisplayName("le client de démonstration est amorcé, et lui seul")
    void seedsOnlyTheDemoClient() {
        List<UUID> clients = jdbc.queryForList(
                "SELECT client_id FROM customer.clients", UUID.class);
        assertThat(clients).containsExactly(DEMO_CLIENT);

        // Aucun drone ni commande : UC-02 et UC-04 doivent les produire.
        assertThat(jdbc.queryForObject("SELECT count(*) FROM drone.drones", Integer.class))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM orders.service_orders", Integer.class))
                .isZero();
    }

    @Test
    @DisplayName("le catalogue publie les deux services de la commande de référence")
    void seedsTheReferenceCatalog() {
        List<String> characteristics = jdbc.queryForList(
                """
                SELECT service_specification_id || '.' || name || '=' || default_value
                FROM catalog.characteristic_specifications
                WHERE name IN ('sst', 'sd', 'arp')
                ORDER BY service_specification_id, position
                """,
                String.class);

        assertThat(characteristics).containsExactly(
                "C2_URLLC.sst=2", "C2_URLLC.sd=000001", "C2_URLLC.arp=2",
                "IMAGERY_EMBB.sst=1", "IMAGERY_EMBB.sd=000002", "IMAGERY_EMBB.arp=8");
    }
}
