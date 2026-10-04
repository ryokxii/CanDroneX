package com.candronex.droneregistration.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

/** Racine d'agrégat du contexte DroneRegistration. */
@Entity
@Table(schema = "drone", name = "drones")
public class Drone {

    @Id
    @Column(name = "drone_id", nullable = false, length = 32)
    private String droneId;

    @Column(name = "client_id", nullable = false, length = 32)
    private String clientId;

    @Embedded
    private NetworkIdentity networkIdentity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private DroneStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant registeredAt;

    /** Requis par l'ORM ; jamais appelé par le domaine. */
    protected Drone() {
    }

    private Drone(String droneId, String clientId, NetworkIdentity networkIdentity,
                  DroneStatus status, Instant registeredAt) {
        this.droneId = droneId;
        this.clientId = clientId;
        this.networkIdentity = networkIdentity;
        this.status = status;
        this.registeredAt = registeredAt;
    }

    /** Enregistre un drone pour un client. */
    public static Drone register(String droneId, String clientId,
                                 NetworkIdentity networkIdentity, Instant registeredAt) {
        if (droneId == null || droneId.isBlank()) {
            throw new InvalidNetworkIdentityException("L'identifiant du drone est obligatoire.");
        }
        if (clientId == null || clientId.isBlank()) {
            throw new InvalidNetworkIdentityException("L'identifiant du client est obligatoire.");
        }
        if (networkIdentity == null) {
            throw new InvalidNetworkIdentityException("L'identité réseau est obligatoire.");
        }
        return new Drone(droneId, clientId, networkIdentity,
                DroneStatus.REGISTERED, registeredAt);
    }

    /** Un drone n'est consultable et commandable que par son propriétaire. */
    public boolean belongsTo(String candidateClientId) {
        return clientId.equals(candidateClientId);
    }

    public String droneId() {
        return droneId;
    }

    public String clientId() {
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
        return Objects.equals(droneId, that.droneId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(droneId);
    }
}
