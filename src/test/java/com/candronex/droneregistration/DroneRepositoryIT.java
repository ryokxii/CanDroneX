package com.candronex.droneregistration;

import com.candronex.PostgresIntegrationTest;
import com.candronex.droneregistration.application.RegisterDroneCommand;
import com.candronex.droneregistration.application.RegisterDroneService;
import com.candronex.droneregistration.domain.Drone;
import com.candronex.droneregistration.domain.DuplicateImsiException;
import com.candronex.droneregistration.domain.NetworkIdentity;
import com.candronex.droneregistration.domain.SimType;
import com.candronex.droneregistration.domain.port.DroneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Persistance du module DroneRegistration, sur PostgreSQL réel. */
class DroneRepositoryIT extends PostgresIntegrationTest {

    private static final UUID CLIENT = DEMO_CLIENT;
    private static final UUID OTHER_CLIENT = UUID.randomUUID();
    private static final String IMSI = "302720123456789";

    @Autowired
    private DroneRepository drones;

    @Autowired
    private RegisterDroneService service;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void cleanDrones() {
        jdbc.execute("TRUNCATE TABLE drone.drones");
    }

    @Test
    @DisplayName("un drone enregistré se relit avec son identité réseau")
    void persistsAndReadsBackTheAggregate() {
        Drone drone = Drone.register(
                CLIENT,
                NetworkIdentity.of(IMSI, SimType.ESIM),
                Instant.parse("2026-10-04T14:12:03Z"));
        drones.save(drone);

        Drone reloaded = drones.findByIdAndClientId(drone.droneId(), CLIENT).orElseThrow();

        assertThat(reloaded.droneId()).isEqualTo(drone.droneId());
        assertThat(reloaded.clientId()).isEqualTo(CLIENT);
        assertThat(reloaded.networkIdentity().imsi()).isEqualTo(IMSI);
        assertThat(reloaded.networkIdentity().simType()).isEqualTo(SimType.ESIM);
        assertThat(reloaded.registeredAt()).isEqualTo(Instant.parse("2026-10-04T14:12:03Z"));
    }

    @Test
    @DisplayName("la contrainte UNIQUE (imsi) refuse un second drone, tous clients confondus")
    void enforcesGlobalImsiUniqueness() {
        drones.save(Drone.register(CLIENT,
                NetworkIdentity.of(IMSI, SimType.ESIM), Instant.now()));

        // Contourne existsByImsi : vérifie la contrainte UNIQUE de dernier recours.
        assertThatThrownBy(() -> drones.save(Drone.register(OTHER_CLIENT,
                NetworkIdentity.of(IMSI, SimType.SIM), Instant.now())))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM drone.drones", Integer.class))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("la contrainte CHECK refuse un IMSI mal formé inséré directement")
    void enforcesImsiFormatInDatabase() {
        assertThatThrownBy(() -> jdbc.update(
                """
                INSERT INTO drone.drones (drone_id, client_id, imsi, sim_type, status, created_at)
                VALUES (?, ?, '12345', 'SIM', 'REGISTERED', now())
                """, UUID.randomUUID(), CLIENT))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("un drone n'est lisible que par son client")
    void isolatesByClientAtTheSqlLevel() {
        Drone drone = Drone.register(CLIENT,
                NetworkIdentity.of(IMSI, SimType.ESIM), Instant.now());
        drones.save(drone);

        assertThat(drones.findByIdAndClientId(drone.droneId(), CLIENT)).isPresent();
        assertThat(drones.findByIdAndClientId(drone.droneId(), OTHER_CLIENT)).isEmpty();
        assertThat(drones.findAllByClientId(OTHER_CLIENT)).isEmpty();
    }

    @Test
    @DisplayName("le service applicatif refuse un IMSI déjà pris, avant d'atteindre la base")
    void applicationServiceRejectsDuplicateBeforeInsert() {
        service.register(new RegisterDroneCommand(CLIENT, IMSI, SimType.ESIM));

        assertThatThrownBy(() -> service.register(
                new RegisterDroneCommand(CLIENT, IMSI, SimType.SIM)))
                .isInstanceOf(DuplicateImsiException.class);
    }
}
