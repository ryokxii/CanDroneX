package com.candronex.droneregistration;

import com.candronex.accessmanagement.published.ClientDirectory;
import com.candronex.accessmanagement.published.UnknownClientException;
import com.candronex.droneregistration.application.DroneView;
import com.candronex.droneregistration.application.RegisterDroneCommand;
import com.candronex.droneregistration.application.RegisterDroneService;
import com.candronex.droneregistration.domain.DuplicateImsiException;
import com.candronex.droneregistration.domain.InvalidNetworkIdentityException;
import com.candronex.droneregistration.domain.SimType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** UC-02 — enregistrer un drone et son identité réseau. */
class RegisterDroneServiceTest {

    private static final String CLIENT = "CLI-INSPECTRA";
    private static final String OTHER_CLIENT = "CLI-AUTRE";
    private static final String VALID_IMSI = "302720123456789";
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

    @Test
    @DisplayName("accepte l'enregistrement lorsque les informations requises sont valides")
    void registersDroneWithValidData() {
        DroneView drone = service.register(
                new RegisterDroneCommand(CLIENT, "DRN-0001", VALID_IMSI, SimType.ESIM));

        assertThat(drone.droneId()).isEqualTo("DRN-0001");
        assertThat(drone.clientId()).isEqualTo(CLIENT);
        assertThat(drone.imsi()).isEqualTo(VALID_IMSI);
        assertThat(drone.simType()).isEqualTo("ESIM");
        assertThat(drone.status()).isEqualTo("REGISTERED");
        assertThat(drone.registeredAt()).isEqualTo(NOW);
        assertThat(drones.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("refuse un enregistrement dont l'IMSI est absent")
    void rejectsMissingImsi() {
        assertThatThrownBy(() -> service.register(
                new RegisterDroneCommand(CLIENT, "DRN-0001", null, SimType.SIM)))
                .isInstanceOf(InvalidNetworkIdentityException.class);

        assertThat(drones.count()).isZero();
    }

    @Test
    @DisplayName("refuse un enregistrement dont l'IMSI est mal formé")
    void rejectsMalformedImsi() {
        assertThatThrownBy(() -> service.register(
                new RegisterDroneCommand(CLIENT, "DRN-0001", "12345", SimType.SIM)))
                .isInstanceOf(InvalidNetworkIdentityException.class);

        assertThat(drones.count()).isZero();
    }

    @Test
    @DisplayName("refuse un enregistrement sans identifiant de drone")
    void rejectsMissingDroneId() {
        assertThatThrownBy(() -> service.register(
                new RegisterDroneCommand(CLIENT, "  ", VALID_IMSI, SimType.SIM)))
                .isInstanceOf(InvalidNetworkIdentityException.class);

        assertThat(drones.count()).isZero();
    }

    @Test
    @DisplayName("refuse un IMSI déjà associé à un autre drone, même d'un autre client")
    void rejectsDuplicateImsiAcrossClients() {
        service.register(new RegisterDroneCommand(CLIENT, "DRN-0001", VALID_IMSI, SimType.ESIM));

        assertThatThrownBy(() -> service.register(
                new RegisterDroneCommand(OTHER_CLIENT, "DRN-0002", VALID_IMSI, SimType.SIM)))
                .isInstanceOf(DuplicateImsiException.class);

        // UC-02 n'est pas idempotent : le doublon est refusé.
        assertThat(drones.count()).isEqualTo(1);
        assertThat(drones.findByIdAndClientId("DRN-0001", CLIENT)).isPresent();
    }

    @Test
    @DisplayName("refuse un enregistrement demandé par un client inconnu")
    void rejectsUnknownClient() {
        assertThatThrownBy(() -> service.register(
                new RegisterDroneCommand("CLI-FANTOME", "DRN-0001", VALID_IMSI, SimType.SIM)))
                .isInstanceOf(UnknownClientException.class);

        assertThat(drones.count()).isZero();
    }

    @Test
    @DisplayName("un client ne voit que ses propres drones")
    void isolatesDronesByClient() {
        service.register(new RegisterDroneCommand(CLIENT, "DRN-0001", VALID_IMSI, SimType.ESIM));

        assertThat(service.listForClient(CLIENT)).hasSize(1);
        assertThat(service.listForClient(OTHER_CLIENT)).isEmpty();
    }
}
