package com.candronex.dronemanagement.application;

import com.candronex.dronemanagement.domain.SimType;

import java.util.UUID;

/** Intention d'enregistrer un drone. */
public record RegisterDroneCommand(
        UUID clientId,
        String imsi,
        SimType simType) {
}
