package com.candronex.slice.activation.application;

import com.candronex.slice.activation.domain.ServiceCharacteristic;
import com.candronex.slice.activation.domain.ServiceNotFoundException;
import com.candronex.slice.activation.domain.ServiceState;
import com.candronex.slice.activation.domain.port.NetworkProvisioningPort;
import com.candronex.slice.activation.domain.port.ProvisioningRequest;
import com.candronex.slice.activation.domain.port.ProvisioningResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** UC-06 — activation idempotente, sans double configuration réseau. */
class ActivationServiceTest {

    private static final UUID ORDER_LINE = UUID.randomUUID();
    private static final UUID DRONE = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-10-01T14:05:00Z");

    private InMemoryServiceActivationRepository services;
    private FakeNetwork network;
    private final List<String> inventory = new ArrayList<>();
    private final List<Object> events = new ArrayList<>();
    private ActivationService activation;

    /** Réseau qui répond selon le scénario et compte ses appels. */
    static final class FakeNetwork implements NetworkProvisioningPort {
        ProvisioningResult provisionAnswer = ProvisioningResult.SUCCESS;
        ProvisioningResult statusAnswer = ProvisioningResult.FAILED;
        int provisionCalls;
        int statusCalls;

        @Override
        public ProvisioningResult provision(ProvisioningRequest request) {
            provisionCalls++;
            return provisionAnswer;
        }

        @Override
        public ProvisioningResult findStatus(UUID serviceId) {
            statusCalls++;
            return statusAnswer;
        }
    }

    @BeforeEach
    void setUp() {
        services = new InMemoryServiceActivationRepository();
        network = new FakeNetwork();
        ActivationTransitions transitions = new ActivationTransitions(services,
                (serviceId, droneId, state) -> inventory.add(state.name()), events::add);
        activation = new ActivationService(services, transitions, network, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static RequestActivationCommand command() {
        return new RequestActivationCommand(ORDER_LINE, DRONE, List.of(new ServiceCharacteristic("sst", "2")));
    }

    @Test
    @DisplayName("première demande : service PENDING, inventaire, appel au réseau planifié")
    void firstRequestCreatesPendingService() {
        RequestActivationResult result = activation.request(command());

        assertThat(result.created()).isTrue();
        assertThat(result.service().state()).isEqualTo("PENDING");
        assertThat(result.service().acceptedAt()).isEqualTo(NOW);
        assertThat(inventory).containsExactly("PENDING");
        assertThat(events).containsExactly(new ServiceAccepted(result.service().serviceId()));
        assertThat(network.provisionCalls).isZero();
    }

    @Test
    @DisplayName("demande rejouée pour la même ligne : le service existant, rien de nouveau")
    void replayReturnsExistingService() {
        RequestActivationResult first = activation.request(command());
        RequestActivationResult replay = activation.request(command());

        assertThat(replay.created()).isFalse();
        assertThat(replay.service().serviceId()).isEqualTo(first.service().serviceId());
        assertThat(services.count()).isEqualTo(1);
        assertThat(events).hasSize(1);
    }

    @Test
    @DisplayName("tentative réussie : ACTIVE dans le service et l'inventaire")
    void attemptActivates() {
        UUID serviceId = activation.request(command()).service().serviceId();

        activation.attempt(serviceId);

        assertThat(activation.findByOrderLine(ORDER_LINE).state()).isEqualTo("ACTIVE");
        assertThat(activation.findByOrderLine(ORDER_LINE).activatedAt()).isEqualTo(NOW);
        assertThat(inventory).containsExactly("PENDING", "ACTIVE");
    }

    @Test
    @DisplayName("un service déjà ACTIVE n'est jamais reconfiguré")
    void activeServiceIsNotProvisionedAgain() {
        UUID serviceId = activation.request(command()).service().serviceId();
        activation.attempt(serviceId);

        activation.attempt(serviceId);

        assertThat(network.provisionCalls).isEqualTo(1);
    }

    @Test
    @DisplayName("échec du réseau : FAILED")
    void attemptFails() {
        network.provisionAnswer = ProvisioningResult.FAILED;
        UUID serviceId = activation.request(command()).service().serviceId();

        activation.attempt(serviceId);

        assertThat(activation.findByOrderLine(ORDER_LINE).state()).isEqualTo("FAILED");
    }

    @Test
    @DisplayName("délai dépassé puis réseau configuré : ACTIVE sans second provision")
    void unconfirmedThenConfiguredNeedsNoSecondProvision() {
        network.provisionAnswer = ProvisioningResult.TIMEOUT;
        network.statusAnswer = ProvisioningResult.SUCCESS;
        UUID serviceId = activation.request(command()).service().serviceId();
        activation.attempt(serviceId);
        assertThat(activation.unconfirmed()).containsExactly(serviceId);

        activation.reconcile(serviceId);

        assertThat(activation.findByOrderLine(ORDER_LINE).state()).isEqualTo("ACTIVE");
        assertThat(network.provisionCalls).isEqualTo(1);
        assertThat(network.statusCalls).isEqualTo(1);
        assertThat(inventory).containsExactly("PENDING", "UNCONFIRMED", "ACTIVE");
    }

    @Test
    @DisplayName("délai dépassé puis absent du réseau : nouvelle tentative planifiée")
    void unconfirmedThenAbsentStartsNewAttempt() {
        network.provisionAnswer = ProvisioningResult.TIMEOUT;
        UUID serviceId = activation.request(command()).service().serviceId();
        activation.attempt(serviceId);

        activation.reconcile(serviceId);

        assertThat(activation.findByOrderLine(ORDER_LINE).state()).isEqualTo("PENDING");
        assertThat(events).containsExactly(new ServiceAccepted(serviceId), new ServiceAccepted(serviceId));
    }

    @Test
    @DisplayName("résultat arrivé après qu'un autre traitement a fait évoluer le service : ignoré")
    void lateResultIsIgnored() {
        UUID serviceId = activation.request(command()).service().serviceId();
        activation.attempt(serviceId);
        int inventoryWrites = inventory.size();

        // Le service est ACTIVE : une seconde tentative concurrente ne doit rien écrire.
        assertThat(services.saveIfStillIn(services.findById(serviceId).orElseThrow(), ServiceState.PENDING))
                .isFalse();
        assertThat(inventory).hasSize(inventoryWrites);
    }

    @Test
    @DisplayName("aucune activation pour une ligne inconnue")
    void unknownOrderLine() {
        assertThatThrownBy(() -> activation.findByOrderLine(UUID.randomUUID()))
                .isInstanceOf(ServiceNotFoundException.class);
    }
}
