package com.candronex.slice.activation.domain;

import com.candronex.slice.activation.domain.port.ProvisioningResult;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Racine d'agrégat du contexte ServiceActivation (TMF640) : le service d'une ligne de commande,
 * activé sur le réseau pour un drone. Une ligne n'a qu'un service : orderLineId est unique.
 */
public final class Service {

    private static final ActivationOutcomePolicy OUTCOME_POLICY = new ActivationOutcomePolicy();

    private final UUID serviceId;
    private final UUID orderLineId;
    private final UUID droneId;
    private final List<ServiceCharacteristic> characteristics;
    private final List<ActivationAttempt> attempts;
    private ServiceState state;

    private Service(UUID serviceId, UUID orderLineId, UUID droneId,
                    List<ServiceCharacteristic> characteristics, List<ActivationAttempt> attempts,
                    ServiceState state) {
        if (characteristics == null || characteristics.isEmpty()) {
            throw new IllegalArgumentException("Le service porte au moins une caractéristique.");
        }
        this.serviceId = Objects.requireNonNull(serviceId, "serviceId");
        this.orderLineId = Objects.requireNonNull(orderLineId, "orderLineId");
        this.droneId = Objects.requireNonNull(droneId, "droneId");
        this.characteristics = List.copyOf(characteristics);
        this.attempts = new ArrayList<>(attempts);
        this.state = Objects.requireNonNull(state, "state");
    }

    /** Accepte la demande d'activation : PENDING, avec sa première tentative. */
    public static Service request(UUID orderLineId, UUID droneId,
                                  List<ServiceCharacteristic> characteristics, Instant now) {
        return new Service(UUID.randomUUID(), orderLineId, droneId, characteristics,
                List.of(ActivationAttempt.accept(now)), ServiceState.PENDING);
    }

    /** Reconstitue un service enregistré ; réservé à l'adaptateur de persistance. */
    public static Service restore(UUID serviceId, UUID orderLineId, UUID droneId,
                                  List<ServiceCharacteristic> characteristics,
                                  List<ActivationAttempt> attempts, ServiceState state) {
        return new Service(serviceId, orderLineId, droneId, characteristics, attempts, state);
    }

    /** Résultat de provision : seul un service PENDING en reçoit un. */
    public void complete(ProvisioningResult result, Instant now) {
        requireState(ServiceState.PENDING);
        currentAttempt().finish(OUTCOME_POLICY.attemptResultOf(result), now);
        this.state = OUTCOME_POLICY.stateAfter(result);
    }

    /**
     * État réel lu après un délai dépassé : configuré, le service devient ACTIVE sans nouvel appel
     * à provision ; absent, une nouvelle tentative commence. Sans réponse, rien ne change.
     */
    public void reconcile(ProvisioningResult actualStatus, Instant now) {
        requireState(ServiceState.UNCONFIRMED);
        switch (actualStatus) {
            case SUCCESS -> {
                currentAttempt().finish(AttemptResult.SUCCESS, now);
                this.state = ServiceState.ACTIVE;
            }
            case FAILED -> {
                currentAttempt().finish(AttemptResult.FAILED, now);
                attempts.add(ActivationAttempt.accept(now));
                this.state = ServiceState.PENDING;
            }
            case TIMEOUT -> {
            }
        }
    }

    private void requireState(ServiceState expected) {
        if (state != expected) {
            throw new IllegalStateException(
                    "Le service " + serviceId + " est " + state + ", " + expected + " attendu.");
        }
    }

    private ActivationAttempt currentAttempt() {
        ActivationAttempt last = attempts.get(attempts.size() - 1);
        if (!last.isOpen()) {
            throw new IllegalStateException("Aucune tentative en cours pour le service " + serviceId + ".");
        }
        return last;
    }

    public UUID serviceId() {
        return serviceId;
    }

    public UUID orderLineId() {
        return orderLineId;
    }

    public UUID droneId() {
        return droneId;
    }

    public List<ServiceCharacteristic> characteristics() {
        return characteristics;
    }

    /** Dans l'ordre chronologique ; vue non modifiable. */
    public List<ActivationAttempt> attempts() {
        return Collections.unmodifiableList(attempts);
    }

    public ServiceState state() {
        return state;
    }

    /** Acceptation de la tentative courante. */
    public Instant acceptedAt() {
        return attempts.get(attempts.size() - 1).acceptedAt();
    }

    /** Nul tant que le service n'est pas ACTIVE. */
    public Instant activatedAt() {
        return attempts.stream()
                .filter(attempt -> attempt.result() == AttemptResult.SUCCESS)
                .map(ActivationAttempt::activatedAt)
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Service that)) {
            return false;
        }
        return serviceId.equals(that.serviceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceId);
    }
}
