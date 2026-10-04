package com.candronex.droneregistration.application;

import com.candronex.droneregistration.domain.port.DroneRepository;
import com.candronex.droneregistration.published.DroneDirectory;
import com.candronex.droneregistration.published.DroneSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** Réalise l'interface publiée DroneDirectory. */
@Service
class DroneDirectoryService implements DroneDirectory {

    private final DroneRepository drones;

    DroneDirectoryService(DroneRepository drones) {
        this.drones = drones;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DroneSummary> findForClient(String droneId, String clientId) {
        return drones.findByIdAndClientId(droneId, clientId)
                .map(drone -> new DroneSummary(drone.droneId(), drone.clientId()));
    }
}
