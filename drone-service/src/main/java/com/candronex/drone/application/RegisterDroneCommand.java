package com.candronex.drone.application;

import com.candronex.drone.domain.SimType;

/** Intention d'enregistrer un drone et son identité réseau. */
public record RegisterDroneCommand(
        String clientId,
        String imsi,
        String iccid,
        SimType simType) {

    /** L'IMSI et l'ICCID ne doivent jamais apparaître dans un journal. */
    @Override
    public String toString() {
        return "RegisterDroneCommand[clientId=" + clientId + ", simType=" + simType + "]";
    }
}
