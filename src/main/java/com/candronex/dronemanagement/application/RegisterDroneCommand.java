package com.candronex.dronemanagement.application;

import com.candronex.dronemanagement.domain.SimType;

import java.util.UUID;

/** Intention d'enregistrer un drone et son identité réseau. */
public record RegisterDroneCommand(
        UUID clientId,
        String imsi,
        String iccid,
        SimType simType) {

    /** L'IMSI et l'ICCID ne doivent jamais apparaître dans un journal. */
    @Override
    public String toString() {
        return "RegisterDroneCommand[clientId=" + clientId + ", simType=" + simType + "]";
    }
}
