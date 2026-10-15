package com.candronex.dronemanagement.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Racine d'agrégat du contexte DroneManagement : un drone d'un client et son identité réseau.
 *
 * <p>Le droneId est généré par la plateforme ; la référence que le client donne à son appareil
 * (DRN-0231) n'est pas un identifiant du système.
 */
public final class Drone {

    private final UUID droneId;
    private final UUID clientId;
    private final NetworkIdentity networkIdentity;
    private final DroneStatus status;
    private final Instant registeredAt;

    private Drone(UUID droneId, UUID clientId, NetworkIdentity networkIdentity,
                  DroneStatus status, Instant registeredAt) {
        if (droneId == null) {
            throw new IllegalArgumentException("L'identifiant du drone est obligatoire.");
        }
        if (clientId == null) {
            throw new IllegalArgumentException("L'identifiant du client est obligatoire.");
        }
        if (networkIdentity == null) {
            throw new InvalidNetworkIdentityException("L'identité réseau est obligatoire.");
        }
        this.droneId = droneId;
        this.clientId = clientId;
        this.networkIdentity = networkIdentity;
        this.status = Objects.requireNonNull(status, "status");
        this.registeredAt = Objects.requireNonNull(registeredAt, "registeredAt");
    }

    /** Enregistre un drone pour un client ; son identifiant est généré, jamais fourni. */
    public static Drone register(UUID clientId, NetworkIdentity networkIdentity,
                                 Instant registeredAt) {
        return new Drone(UUID.randomUUID(), clientId, networkIdentity,
                DroneStatus.REGISTERED, registeredAt);
    }

    /** Reconstitue un drone déjà enregistré ; réservé à l'adaptateur de persistance. */
    public static Drone restore(UUID droneId, UUID clientId, NetworkIdentity networkIdentity,
                                DroneStatus status, Instant registeredAt) {
        return new Drone(droneId, clientId, networkIdentity, status, registeredAt);
    }

    /** Un drone n'est consultable et commandable que par son propriétaire. */
    public boolean belongsTo(UUID candidateClientId) {
        return clientId.equals(candidateClientId);
    }

    public UUID droneId() {
        return droneId;
    }

    public UUID clientId() {
        return clientId;
    }

    public NetworkIdentity networkIdentity() {
        return networkIdentity;
    }

    public DroneStatus status() {
        return status;
    }

    public Instant registeredAt() {
        return registeredAt;
    }

    /** Égalité par identité : deux drones sont le même s'ils ont le même droneId. */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Drone that)) {
            return false;
        }
        return droneId.equals(that.droneId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(droneId);
    }
}
