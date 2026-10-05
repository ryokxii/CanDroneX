package com.candronex.droneregistration.api;

import com.candronex.droneregistration.application.DroneView;

import java.time.Instant;
import java.util.UUID;

/** Représentation JSON d'un drone — réduite au strict nécessaire. */
public record DroneResponse(
        UUID droneId,
        String status,
        Instant registeredAt) {

    static DroneResponse from(DroneView view) {
        return new DroneResponse(
                view.droneId(),
                view.status(),
                view.registeredAt());
    }
}
