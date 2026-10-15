package com.candronex.dronemanagement;

import com.candronex.customermanagement.published.ClientDirectory;
import com.candronex.customermanagement.published.UnknownClientException;
import com.candronex.dronemanagement.application.DroneView;
import com.candronex.dronemanagement.application.RegisterDroneCommand;
import com.candronex.dronemanagement.application.RegisterDroneService;
import com.candronex.dronemanagement.domain.DuplicateIccidException;
import com.candronex.dronemanagement.domain.DuplicateImsiException;
import com.candronex.dronemanagement.domain.InvalidNetworkIdentityException;
import com.candronex.dronemanagement.domain.SimType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.NoSuchElementException;
import java.util.UUID;

import static com.candronex.dronemanagement.NetworkIdentityTest.VALID_ICCID;
import static com.candronex.dronemanagement.NetworkIdentityTest.VALID_IMSI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** UC-02 — enregistrer un drone et son identité réseau. */
class RegisterDroneServiceTest {

    private static final UUID CLIENT = UUID.randomUUID();
    private static final UUID OTHER_CLIENT = UUID.randomUUID();
    private static final String OTHER_IMSI = "999700000010232";
    private static final String OTHER_ICCID = "8999700000000010247";
    private static final Instant NOW = Instant.parse("2026-10-04T14:12:03Z");

    private InMemoryDroneRepository drones;
    private RegisterDroneService service;

    @BeforeEach
    void setUp() {
        drones = new InMemoryDroneRepository();
        ClientDirectory knownClients = clientId ->
                CLIENT.equals(clientId) || OTHER_CLIENT.equals(clientId);
        service = new RegisterDroneService(
                drones, knownClients, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static RegisterDroneCommand command(UUID clientId, String imsi, String iccid) {
        return new RegisterDroneCommand(clientId, imsi, iccid, SimType.ESIM);
    }

    @Test
    @DisplayName("accepte l'enregistrement lorsque les informations requises sont valides")
    void registersDroneWithValidData() {
        DroneView drone = service.register(command(CLIENT, VALID_IMSI, VALID_ICCID));

        assertThat(drone.droneId()).isNotNull();
        assertThat(drone.status()).isEqualTo("REGISTERED");
        assertThat(drone.registeredAt()).isEqualTo(NOW);
        assertThat(drones.findByIdAndClientId(drone.droneId(), CLIENT)).isPresent();
    }

    @Test
    @DisplayName("la vue retournée ne porte aucune identité d'abonné")
    void viewDoesNotExposeNetworkIdentity() {
        DroneView drone = service.register(command(CLIENT, VALID_IMSI, VALID_ICCID));

        assertThat(drone.toString()).doesNotContain(VALID_IMSI).doesNotContain(VALID_ICCID);
    }

    @Test
    @DisplayName("refuse un enregistrement dont l'IMSI est absent ou mal formé")
    void rejectsInvalidImsi() {
        assertThatThrownBy(() -> service.register(command(CLIENT, null, VALID_ICCID)))
                .isInstanceOf(InvalidNetworkIdentityException.class);
        assertThatThrownBy(() -> service.register(command(CLIENT, "12345", VALID_ICCID)))
                .isInstanceOf(InvalidNetworkIdentityException.class);

        assertThat(drones.count()).isZero();
    }

    @Test
    @DisplayName("refuse un enregistrement dont l'ICCID est absent ou invalide")
    void rejectsInvalidIccid() {
        assertThatThrownBy(() -> service.register(command(CLIENT, VALID_IMSI, null)))
                .isInstanceOf(InvalidNetworkIdentityException.class);
        assertThatThrownBy(() -> service.register(command(CLIENT, VALID_IMSI, "8999700000000010238")))
                .isInstanceOf(InvalidNetworkIdentityException.class);

        assertThat(drones.count()).isZero();
    }

    @Test
    @DisplayName("génère un identifiant distinct pour chaque drone enregistré")
    void generatesDistinctDroneIds() {
        DroneView first = service.register(command(CLIENT, VALID_IMSI, VALID_ICCID));
        DroneView second = service.register(command(CLIENT, OTHER_IMSI, OTHER_ICCID));

        assertThat(first.droneId()).isNotNull().isNotEqualTo(second.droneId());
    }

    @Test
    @DisplayName("refuse un IMSI déjà associé à un autre drone, même d'un autre client")
    void rejectsDuplicateImsiAcrossClients() {
        DroneView registered = service.register(command(CLIENT, VALID_IMSI, VALID_ICCID));

        assertThatThrownBy(() -> service.register(command(OTHER_CLIENT, VALID_IMSI, OTHER_ICCID)))
                .isInstanceOf(DuplicateImsiException.class)
                .hasMessageNotContaining(VALID_IMSI);

        // UC-02 n'est pas idempotent : le doublon est refusé.
        assertThat(drones.count()).isEqualTo(1);
        assertThat(drones.findByIdAndClientId(registered.droneId(), CLIENT)).isPresent();
    }

    @Test
    @DisplayName("refuse un ICCID déjà associé à un autre drone, même d'un autre client")
    void rejectsDuplicateIccidAcrossClients() {
        service.register(command(CLIENT, VALID_IMSI, VALID_ICCID));

        assertThatThrownBy(() -> service.register(command(OTHER_CLIENT, OTHER_IMSI, VALID_ICCID)))
                .isInstanceOf(DuplicateIccidException.class)
                .hasMessageNotContaining(VALID_ICCID);

        assertThat(drones.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("refuse un enregistrement demandé par un client inconnu")
    void rejectsUnknownClient() {
        assertThatThrownBy(() -> service.register(command(UUID.randomUUID(), VALID_IMSI, VALID_ICCID)))
                .isInstanceOf(UnknownClientException.class);

        assertThat(drones.count()).isZero();
    }

    @Test
    @DisplayName("un client ne voit que ses propres drones")
    void isolatesDronesByClient() {
        DroneView mine = service.register(command(CLIENT, VALID_IMSI, VALID_ICCID));

        assertThat(service.listForClient(CLIENT)).hasSize(1);
        assertThat(service.listForClient(OTHER_CLIENT)).isEmpty();
        assertThat(service.findForClient(mine.droneId(), CLIENT).droneId()).isEqualTo(mine.droneId());
        assertThatThrownBy(() -> service.findForClient(mine.droneId(), OTHER_CLIENT))
                .isInstanceOf(NoSuchElementException.class);
    }
}
