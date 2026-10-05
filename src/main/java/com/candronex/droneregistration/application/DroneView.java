package com.candronex.droneregistration.application;

import com.candronex.droneregistration.domain.Drone;

import java.time.Instant;
import java.util.UUID;

/** Projection d'un drone destinée à sortir du module. */
public record DroneView(
        UUID droneId,
        UUID clientId,
        String imsi,
        String simType,
        String status,
        Instant registeredAt) {

    public static DroneView from(Drone drone) {
        return new DroneView(
                drone.droneId(),
                drone.clientId(),
                drone.networkIdentity().imsi(),
                drone.networkIdentity().simType().name(),
                drone.status().name(),
                drone.registeredAt());
    }
}
