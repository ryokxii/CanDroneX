package com.candronex.order.domain.port;

import java.util.UUID;

/** Port sortant vers Drone Service, propriétaire des drones. */
public interface DroneOwnershipPort {

    /**
     * Vrai si le drone existe et appartient au client du jeton ; faux sinon, sans distinguer les
     * deux cas.
     *
     * @throws com.candronex.order.application.DependencyUnavailableException si Drone Service ne
     *         répond pas dans le délai
     */
    boolean isOwnedBy(UUID droneId, String clientToken);
}
