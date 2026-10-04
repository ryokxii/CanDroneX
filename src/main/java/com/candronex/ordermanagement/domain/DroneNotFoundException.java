package com.candronex.ordermanagement.domain;

/**
 * Le drone visé par une ligne de commande n'existe pas, ou n'appartient pas au client qui commande.
 */
public class DroneNotFoundException extends RuntimeException {

    public DroneNotFoundException(String droneId) {
        super("Drone introuvable : " + droneId);
    }
}
