package com.candronex.slice.activation.domain;

import com.candronex.slice.activation.domain.port.ProvisioningResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** UC-06 — cycle de vie d'un service activé. */
class ServiceTest {

    private static final Instant ACCEPTED = Instant.parse("2026-10-01T14:05:00.120Z");
    private static final Instant ANSWERED = Instant.parse("2026-10-01T14:05:01.480Z");
    private static final List<ServiceCharacteristic> C2 = List.of(
            new ServiceCharacteristic("sst", "2"), new ServiceCharacteristic("dnn", "c2"));

    private static Service requested() {
        return Service.request(UUID.randomUUID(), UUID.randomUUID(), C2, ACCEPTED);
    }

    @Test
    @DisplayName("une demande crée un service PENDING et sa première tentative")
    void requestStartsPending() {
        Service service = requested();

        assertThat(service.state()).isEqualTo(ServiceState.PENDING);
        assertThat(service.attempts()).hasSize(1);
        assertThat(service.acceptedAt()).isEqualTo(ACCEPTED);
        assertThat(service.activatedAt()).isNull();
    }

    @Test
    @DisplayName("SUCCESS : ACTIVE, avec activatedAt")
    void successActivates() {
        Service service = requested();

        service.complete(ProvisioningResult.SUCCESS, ANSWERED);

        assertThat(service.state()).isEqualTo(ServiceState.ACTIVE);
        assertThat(service.activatedAt()).isEqualTo(ANSWERED);
        assertThat(service.attempts().get(0).result()).isEqualTo(AttemptResult.SUCCESS);
    }

    @Test
    @DisplayName("FAILED : échec explicite, sans activatedAt")
    void failureFails() {
        Service service = requested();

        service.complete(ProvisioningResult.FAILED, ANSWERED);

        assertThat(service.state()).isEqualTo(ServiceState.FAILED);
        assertThat(service.activatedAt()).isNull();
    }

    @Test
    @DisplayName("TIMEOUT : UNCONFIRMED, issue inconnue et non échec")
    void timeoutIsUnconfirmed() {
        Service service = requested();

        service.complete(ProvisioningResult.TIMEOUT, ANSWERED);

        assertThat(service.state()).isEqualTo(ServiceState.UNCONFIRMED);
        assertThat(service.attempts().get(0).result()).isEqualTo(AttemptResult.UNKNOWN);
    }

    @Test
    @DisplayName("un service déjà ACTIVE ne reçoit plus de résultat de provision")
    void activeServiceIsNeverProvisionedAgain() {
        Service service = requested();
        service.complete(ProvisioningResult.SUCCESS, ANSWERED);

        assertThatThrownBy(() -> service.complete(ProvisioningResult.SUCCESS, ANSWERED))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("UNCONFIRMED, réseau configuré : ACTIVE sans nouvelle tentative")
    void reconcileConfiguredServiceActivatesWithoutNewAttempt() {
        Service service = requested();
        service.complete(ProvisioningResult.TIMEOUT, ANSWERED);

        service.reconcile(ProvisioningResult.SUCCESS, ANSWERED.plusSeconds(10));

        assertThat(service.state()).isEqualTo(ServiceState.ACTIVE);
        assertThat(service.attempts()).hasSize(1);
        assertThat(service.activatedAt()).isEqualTo(ANSWERED.plusSeconds(10));
    }

    @Test
    @DisplayName("UNCONFIRMED, absent du réseau : nouvelle tentative, PENDING")
    void reconcileAbsentServiceStartsNewAttempt() {
        Service service = requested();
        service.complete(ProvisioningResult.TIMEOUT, ANSWERED);

        service.reconcile(ProvisioningResult.FAILED, ANSWERED.plusSeconds(10));

        assertThat(service.state()).isEqualTo(ServiceState.PENDING);
        assertThat(service.attempts()).extracting(ActivationAttempt::result)
                .containsExactly(AttemptResult.FAILED, null);
        assertThat(service.acceptedAt()).isEqualTo(ANSWERED.plusSeconds(10));
    }

    @Test
    @DisplayName("UNCONFIRMED, réseau toujours muet : rien ne change")
    void reconcileWithoutAnswerChangesNothing() {
        Service service = requested();
        service.complete(ProvisioningResult.TIMEOUT, ANSWERED);

        service.reconcile(ProvisioningResult.TIMEOUT, ANSWERED.plusSeconds(10));

        assertThat(service.state()).isEqualTo(ServiceState.UNCONFIRMED);
        assertThat(service.attempts()).hasSize(1);
    }
}
