package com.candronex.droneregistration.application;

import com.candronex.droneregistration.domain.SimType;

import java.util.UUID;

/** Intention d'enregistrer un drone. */
public record RegisterDroneCommand(
        UUID clientId,
        String imsi,
        SimType simType) {
}
