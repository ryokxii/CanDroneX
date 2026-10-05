package com.candronex.ordermanagement.domain;

import java.util.UUID;

/**
 * Le drone visé par une ligne de commande n'existe pas, ou n'appartient pas au client qui commande.
 */
public class DroneNotFoundException extends RuntimeException {

    public DroneNotFoundException(UUID droneId) {
        super("Drone introuvable : " + droneId);
    }
}
