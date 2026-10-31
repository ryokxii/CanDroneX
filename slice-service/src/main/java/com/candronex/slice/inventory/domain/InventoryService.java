package com.candronex.slice.inventory.domain;

import com.candronex.slice.activation.domain.ServiceState;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Racine d'agrégat du contexte ServiceInventory (TMF638) : l'état courant d'un service d'un drone. */
public final class InventoryService {

    private static final ServiceStatePolicy STATE_POLICY = new ServiceStatePolicy();

    private final UUID serviceId;
    private final UUID droneId;
    private ServiceState state;
    private Instant updatedAt;

    private InventoryService(UUID serviceId, UUID droneId, ServiceState state, Instant updatedAt) {
        this.serviceId = Objects.requireNonNull(serviceId, "serviceId");
        this.droneId = Objects.requireNonNull(droneId, "droneId");
        this.state = Objects.requireNonNull(state, "state");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static InventoryService register(UUID serviceId, UUID droneId, ServiceState state, Instant now) {
        return new InventoryService(serviceId, droneId, state, now);
    }

    /** Reconstitue une entrée enregistrée ; réservé à l'adaptateur de persistance. */
    public static InventoryService restore(UUID serviceId, UUID droneId, ServiceState state,
                                           Instant updatedAt) {
        return new InventoryService(serviceId, droneId, state, updatedAt);
    }

    public void record(ServiceState next, Instant now) {
        if (next == state) {
            return;
        }
        if (!STATE_POLICY.allows(state, next)) {
            throw new IllegalStateException(
                    "Transition interdite pour le service " + serviceId + " : " + state + " → " + next);
        }
        this.state = next;
        this.updatedAt = now;
    }

    public UUID serviceId() {
        return serviceId;
    }

    public UUID droneId() {
        return droneId;
    }

    public ServiceState state() {
        return state;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
