package com.candronex.dronemanagement.application;

import com.candronex.dronemanagement.domain.Drone;

import java.time.Instant;
import java.util.UUID;

/**
 * Projection d'un drone destinée à sortir du service applicatif. L'identité réseau n'y figure pas :
 * l'IMSI et l'ICCID restent confinés au domaine et au schéma drone.
 */
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
