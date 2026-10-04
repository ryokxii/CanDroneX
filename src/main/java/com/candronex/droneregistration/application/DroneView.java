package com.candronex.droneregistration.application;

import com.candronex.droneregistration.domain.Drone;

import java.time.Instant;

/** Projection d'un drone destinée à sortir du module. */
public record DroneView(
        String droneId,
        String clientId,
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
