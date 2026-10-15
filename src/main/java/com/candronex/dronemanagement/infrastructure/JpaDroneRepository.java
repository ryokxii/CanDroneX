package com.candronex.dronemanagement.infrastructure;

import com.candronex.dronemanagement.domain.Drone;
import com.candronex.dronemanagement.domain.port.DroneRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptateur sortant : réalise le port DroneRepository et traduit l'agrégat en lignes. */
@Repository
class JpaDroneRepository implements DroneRepository {

    private final DroneEntityStore drones;

    JpaDroneRepository(DroneEntityStore drones) {
        this.drones = drones;
    }

    @Override
    public boolean existsByImsi(String imsi) {
        return drones.existsByImsi(imsi);
    }

    @Override
    public Optional<Drone> findByIdAndClientId(UUID droneId, UUID clientId) {
        return drones.findByDroneIdAndClientId(droneId, clientId).map(DroneEntity::toDomain);
    }

    @Override
    public List<Drone> findAllByClientId(UUID clientId) {
        return drones.findAllByClientIdOrderByRegisteredAt(clientId).stream()
                .map(DroneEntity::toDomain)
                .toList();
    }

    @Override
    public void save(Drone drone) {
        drones.save(DroneEntity.from(drone));
    }
}
