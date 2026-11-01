package com.candronex.slice;

import com.candronex.slice.activation.application.ActivateServiceUseCase;
import com.candronex.slice.activation.application.FindActivationUseCase;
import com.candronex.slice.activation.application.RequestActivationCommand;
import com.candronex.slice.activation.application.RequestActivationResult;
import com.candronex.slice.activation.domain.ServiceCharacteristic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** UC-06 sur PostgreSQL réel : de la demande à ACTIVE, service et inventaire cohérents. */
class ActivationIT extends PostgresIntegrationTest {

    private static final UUID DRONE = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final List<ServiceCharacteristic> C2 = List.of(
            new ServiceCharacteristic("sst", "2"), new ServiceCharacteristic("sd", "000001"),
            new ServiceCharacteristic("dnn", "c2"));

    @Autowired
    private ActivateServiceUseCase activation;

    @Autowired
    private FindActivationUseCase activations;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void reset() {
        jdbc.execute("TRUNCATE TABLE activation.services, inventory.services CASCADE");
    }

    private String awaitStateOtherThan(UUID orderLineId, String state) throws InterruptedException {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(10));
        String current = activations.findByOrderLine(orderLineId).state();
        while (current.equals(state) && Instant.now().isBefore(deadline)) {
            Thread.sleep(50);
            current = activations.findByOrderLine(orderLineId).state();
        }
        return current;
    }

    @Test
    @DisplayName("la demande est acceptée PENDING, puis le worker la rend ACTIVE")
    void requestedServiceBecomesActive() throws InterruptedException {
        UUID orderLine = UUID.randomUUID();
        RequestActivationResult accepted = activation.request(new RequestActivationCommand(orderLine, DRONE, C2));
        assertThat(accepted.service().state()).isEqualTo("PENDING");

        assertThat(awaitStateOtherThan(orderLine, "PENDING")).isEqualTo("ACTIVE");

        UUID serviceId = accepted.service().serviceId();
        assertThat(jdbc.queryForObject("SELECT state FROM inventory.services WHERE service_id = ?",
                String.class, serviceId)).isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM activation.activation_attempts WHERE service_id = ? AND result = 'SUCCESS'"
                        + " AND activated_at >= accepted_at", Integer.class, serviceId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM activation.service_characteristics WHERE service_id = ?",
                Integer.class, serviceId)).isEqualTo(3);
    }

    @Test
    @DisplayName("rejeu pour la même ligne : le même service, une seule activation")
    void replayCreatesNothing() throws InterruptedException {
        UUID orderLine = UUID.randomUUID();
        RequestActivationResult first = activation.request(new RequestActivationCommand(orderLine, DRONE, C2));
        awaitStateOtherThan(orderLine, "PENDING");

        RequestActivationResult replay = activation.request(new RequestActivationCommand(orderLine, DRONE, C2));

        assertThat(replay.created()).isFalse();
        assertThat(replay.service().serviceId()).isEqualTo(first.service().serviceId());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM activation.services", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM activation.activation_attempts", Integer.class))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("la contrainte UNIQUE (order_line_id) refuse un second service pour la même ligne")
    void enforcesOneServicePerOrderLine() {
        UUID orderLine = UUID.randomUUID();
        activation.request(new RequestActivationCommand(orderLine, DRONE, C2));

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO activation.services (service_id, order_line_id, drone_id, state) VALUES (?, ?, ?, 'PENDING')",
                UUID.randomUUID(), orderLine, DRONE))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("l'UPDATE conditionnel n'écrit pas une transition sur un état déjà changé")
    void conditionalUpdateHasASingleWriter() throws InterruptedException {
        UUID orderLine = UUID.randomUUID();
        UUID serviceId = activation.request(new RequestActivationCommand(orderLine, DRONE, C2)).service().serviceId();
        awaitStateOtherThan(orderLine, "PENDING");

        assertThat(jdbc.update("UPDATE activation.services SET state = 'FAILED' WHERE service_id = ? AND state = 'PENDING'",
                serviceId)).isZero();
        assertThat(activations.findByOrderLine(orderLine).state()).isEqualTo("ACTIVE");
    }
}
