package com.candronex.dronemanagement;

import com.candronex.PostgresIntegrationTest;
import com.candronex.dronemanagement.application.RegisterDroneCommand;
import com.candronex.dronemanagement.application.RegisterDroneService;
import com.candronex.dronemanagement.domain.Drone;
import com.candronex.dronemanagement.domain.DuplicateImsiException;
import com.candronex.dronemanagement.domain.NetworkIdentity;
import com.candronex.dronemanagement.domain.SimType;
import com.candronex.dronemanagement.domain.port.DroneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.UUID;

import static com.candronex.dronemanagement.NetworkIdentityTest.VALID_ICCID;
import static com.candronex.dronemanagement.NetworkIdentityTest.VALID_IMSI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Persistance du module DroneManagement, sur PostgreSQL réel. */
class DroneRepositoryIT extends PostgresIntegrationTest {

    private static final UUID CLIENT = DEMO_CLIENT;
    private static final UUID OTHER_CLIENT = UUID.randomUUID();
    private static final String OTHER_IMSI = "999700000010232";
    private static final String OTHER_ICCID = "8999700000000010247";

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

    private static NetworkIdentity identity(String imsi, String iccid) {
        return NetworkIdentity.of(imsi, iccid, SimType.ESIM);
    }

    @Test
    @DisplayName("un drone enregistré se relit avec son identité réseau complète")
    void persistsAndReadsBackTheAggregate() {
        Drone drone = Drone.register(CLIENT, identity(VALID_IMSI, VALID_ICCID),
                Instant.parse("2026-10-04T14:12:03Z"));
        drones.save(drone);

        Drone reloaded = drones.findByIdAndClientId(drone.droneId(), CLIENT).orElseThrow();

        assertThat(reloaded.droneId()).isEqualTo(drone.droneId());
        assertThat(reloaded.clientId()).isEqualTo(CLIENT);
        assertThat(reloaded.networkIdentity()).isEqualTo(identity(VALID_IMSI, VALID_ICCID));
        assertThat(reloaded.registeredAt()).isEqualTo(Instant.parse("2026-10-04T14:12:03Z"));
    }

    @Test
    @DisplayName("les contraintes UNIQUE refusent un IMSI ou un ICCID déjà pris, tous clients confondus")
    void enforcesGlobalUniquenessOfNetworkIdentifiers() {
        drones.save(Drone.register(CLIENT, identity(VALID_IMSI, VALID_ICCID), Instant.now()));

        // Contourne existsByImsi et existsByIccid : vérifie les contraintes de dernier recours.
        assertThatThrownBy(() -> drones.save(Drone.register(OTHER_CLIENT,
                identity(VALID_IMSI, OTHER_ICCID), Instant.now())))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> drones.save(Drone.register(OTHER_CLIENT,
                identity(OTHER_IMSI, VALID_ICCID), Instant.now())))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM drone.drones", Integer.class))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("les contraintes CHECK refusent un IMSI ou un ICCID mal formé inséré directement")
    void enforcesFormatsInDatabase() {
        String insert = """
                INSERT INTO drone.drones
                    (drone_id, client_id, imsi, iccid, sim_type, status, registered_at)
                VALUES (?, ?, ?, ?, 'SIM', 'REGISTERED', now())
                """;

        assertThatThrownBy(() -> jdbc.update(insert, UUID.randomUUID(), CLIENT, "12345", VALID_ICCID))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(insert, UUID.randomUUID(), CLIENT, VALID_IMSI, "1234"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("un drone n'est lisible que par son client")
    void isolatesByClientAtTheSqlLevel() {
        Drone drone = Drone.register(CLIENT, identity(VALID_IMSI, VALID_ICCID), Instant.now());
        drones.save(drone);

        assertThat(drones.findByIdAndClientId(drone.droneId(), CLIENT)).isPresent();
        assertThat(drones.findByIdAndClientId(drone.droneId(), OTHER_CLIENT)).isEmpty();
        assertThat(drones.findAllByClientId(OTHER_CLIENT)).isEmpty();
    }

    @Test
    @DisplayName("le service applicatif refuse un IMSI déjà pris, avant d'atteindre la contrainte")
    void applicationServiceRejectsDuplicateBeforeInsert() {
        service.register(new RegisterDroneCommand(CLIENT, VALID_IMSI, VALID_ICCID, SimType.ESIM));

        assertThatThrownBy(() -> service.register(
                new RegisterDroneCommand(CLIENT, VALID_IMSI, OTHER_ICCID, SimType.SIM)))
                .isInstanceOf(DuplicateImsiException.class);
    }
}
