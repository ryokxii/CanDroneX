package com.candronex.droneregistration.infrastructure;

import com.candronex.droneregistration.domain.Drone;
import com.candronex.droneregistration.domain.port.DroneRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptateur sortant : réalise le port DroneRepository avec l'ORM. */
@Repository
class JpaDroneRepository implements DroneRepository {

    private final SpringDataDroneRepository drones;

    JpaDroneRepository(SpringDataDroneRepository drones) {
        this.drones = drones;
    }

    @Override
    public boolean existsByImsi(String imsi) {
        return drones.existsByNetworkIdentityImsi(imsi);
    }

    @Override
    public Optional<Drone> findByIdAndClientId(UUID droneId, UUID clientId) {
        return drones.findByDroneIdAndClientId(droneId, clientId);
    }

    @Override
    public List<Drone> findAllByClientId(UUID clientId) {
        return drones.findAllByClientId(clientId);
    }

    @Override
    public void save(Drone drone) {
        drones.save(drone);
    }
}
