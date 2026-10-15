package com.candronex.dronemanagement.published;

import java.util.Optional;
import java.util.UUID;

/** Interface publiée par DroneManagement. */
public interface DroneDirectory {

    /**
     * @return le drone s'il existe ET appartient à ce client ; vide sinon.
     *         Les deux cas sont volontairement indiscernables, pour ne pas
     *         révéler l'existence de la ressource d'un autre client (404).
     */
    Optional<DroneSummary> findForClient(UUID droneId, UUID clientId);
}
