package com.candronex.dronemanagement.infrastructure;

import com.candronex.dronemanagement.domain.Drone;
import com.candronex.dronemanagement.domain.DroneStatus;
import com.candronex.dronemanagement.domain.NetworkIdentity;
import com.candronex.dronemanagement.domain.SimType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Correspondance relationnelle de l'agrégat Drone ; ne sort jamais de l'adaptateur. */
@Entity
@Table(schema = "drone", name = "drones")
class DroneEntity {

    @Id
    @Column(name = "drone_id", nullable = false, updatable = false)
    private UUID droneId;

    @Column(name = "client_id", nullable = false, updatable = false)
    private UUID clientId;

    @Column(name = "imsi", nullable = false, length = 15)
    private String imsi;

    @Enumerated(EnumType.STRING)
    @Column(name = "sim_type", nullable = false, length = 8)
    private SimType simType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private DroneStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant registeredAt;

    /** Requis par l'ORM. */
    protected DroneEntity() {
    }

    static DroneEntity from(Drone drone) {
        DroneEntity entity = new DroneEntity();
        entity.droneId = drone.droneId();
        entity.clientId = drone.clientId();
        entity.imsi = drone.networkIdentity().imsi();
        entity.simType = drone.networkIdentity().simType();
        entity.status = drone.status();
        entity.registeredAt = drone.registeredAt();
        return entity;
    }

    Drone toDomain() {
        return Drone.restore(droneId, clientId,
                NetworkIdentity.of(imsi, simType), status, registeredAt);
    }
}
