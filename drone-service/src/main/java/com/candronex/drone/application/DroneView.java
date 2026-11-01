package com.candronex.drone.application;

import com.candronex.drone.domain.Drone;

import java.time.Instant;
import java.util.UUID;

/** Projection d'un drone ; l'IMSI et l'ICCID n'y figurent pas. */
public record DroneView(
        UUID droneId,
        String status,
        Instant registeredAt) {

    public static DroneView from(Drone drone) {
        return new DroneView(
                drone.droneId(),
                drone.status().name(),
                drone.registeredAt());
    }
}
