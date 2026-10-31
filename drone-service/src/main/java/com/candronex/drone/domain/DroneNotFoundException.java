package com.candronex.drone.domain;

import java.util.UUID;

/** Le drone n'existe pas ou appartient à un autre client ; les deux cas sont indiscernables. */
public class DroneNotFoundException extends RuntimeException {

    public DroneNotFoundException(UUID droneId) {
        super("Aucun drone ne correspond à l'identifiant " + droneId + ".");
    }
}
