package com.candronex.dronemanagement.application;

import com.candronex.dronemanagement.domain.port.DroneRepository;
import com.candronex.dronemanagement.published.DroneDirectory;
import com.candronex.dronemanagement.published.DroneSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/** Réalise l'interface publiée DroneDirectory. */
@Service
class DroneDirectoryService implements DroneDirectory {

    private final DroneRepository drones;

    DroneDirectoryService(DroneRepository drones) {
        this.drones = drones;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DroneSummary> findForClient(UUID droneId, UUID clientId) {
        return drones.findByIdAndClientId(droneId, clientId)
                .map(drone -> new DroneSummary(drone.droneId(), drone.clientId()));
    }
}
