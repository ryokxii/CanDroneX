package com.candronex.slice.activation.infrastructure.network;

import com.candronex.slice.activation.domain.ServiceCharacteristic;
import com.candronex.slice.activation.domain.port.ProvisioningRequest;
import com.candronex.slice.activation.domain.port.ProvisioningResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Réseau simulé : succès, échec, délai dépassé et lecture de l'état réel, sans attente réelle. */
class MockOpen5GSAdapterTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final List<Duration> waits = new ArrayList<>();

    private MockOpen5GSAdapter network(double failureRate, double timeoutRate) {
        return new MockOpen5GSAdapter(
                new MockNetworkProperties(Duration.ofMillis(1200), failureRate, timeoutRate, TIMEOUT),
                new Random(42), waits::add);
    }

    private static ProvisioningRequest request(UUID serviceId) {
        return new ProvisioningRequest(serviceId, List.of(new ServiceCharacteristic("sst", "2")));
    }

    @Test
    @DisplayName("succès : le service est configuré, findStatus le confirme")
    void success() {
        MockOpen5GSAdapter network = network(0, 0);
        UUID serviceId = UUID.randomUUID();

        assertThat(network.provision(request(serviceId))).isEqualTo(ProvisioningResult.SUCCESS);
        assertThat(network.findStatus(serviceId)).isEqualTo(ProvisioningResult.SUCCESS);
        assertThat(waits.get(0)).isBetween(Duration.ofMillis(600), Duration.ofMillis(1800));
    }

    @Test
    @DisplayName("échec : le service n'est pas configuré")
    void failure() {
        MockOpen5GSAdapter network = network(1, 0);
        UUID serviceId = UUID.randomUUID();

        assertThat(network.provision(request(serviceId))).isEqualTo(ProvisioningResult.FAILED);
        assertThat(network.findStatus(serviceId)).isEqualTo(ProvisioningResult.FAILED);
    }

    @Test
    @DisplayName("délai dépassé : TIMEOUT après 5 s, mais la configuration a pu être appliquée")
    void timeoutButApplied() {
        MockOpen5GSAdapter network = network(0, 1);
        UUID serviceId = UUID.randomUUID();

        assertThat(network.provision(request(serviceId))).isEqualTo(ProvisioningResult.TIMEOUT);
        assertThat(waits.get(0)).isEqualTo(TIMEOUT);
        assertThat(network.findStatus(serviceId)).isEqualTo(ProvisioningResult.SUCCESS);
    }

    @Test
    @DisplayName("délai dépassé et refus : findStatus dit que le service est absent")
    void timeoutAndRefused() {
        MockOpen5GSAdapter network = network(1, 1);
        UUID serviceId = UUID.randomUUID();

        assertThat(network.provision(request(serviceId))).isEqualTo(ProvisioningResult.TIMEOUT);
        assertThat(network.findStatus(serviceId)).isEqualTo(ProvisioningResult.FAILED);
    }
}
