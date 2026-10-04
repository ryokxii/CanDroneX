package com.candronex.droneregistration.published;

import java.util.Optional;

/** Interface publiée par DroneRegistration. */
public interface DroneDirectory {

    /**
     * @return le drone s'il existe ET appartient à ce client ; vide sinon.
     *         Les deux cas sont volontairement indiscernables, pour ne pas
     *         révéler l'existence de la ressource d'un autre client (404).
     */
    Optional<DroneSummary> findForClient(String droneId, String clientId);
}
