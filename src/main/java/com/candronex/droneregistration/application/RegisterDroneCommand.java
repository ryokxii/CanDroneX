package com.candronex.droneregistration.application;

import com.candronex.droneregistration.domain.SimType;

/** Intention d'enregistrer un drone. */
public record RegisterDroneCommand(
        String clientId,
        String droneId,
        String imsi,
        SimType simType) {
}
