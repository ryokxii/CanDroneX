package com.candronex.drone.infrastructure.persistence;

import com.candronex.drone.domain.Drone;
import com.candronex.drone.domain.Iccid;
import com.candronex.drone.domain.Imsi;
import com.candronex.drone.domain.port.DroneRepository;
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
    public boolean existsByImsi(Imsi imsi) {
        return drones.existsByImsi(imsi.value());
    }

    @Override
    public boolean existsByIccid(Iccid iccid) {
        return drones.existsByIccid(iccid.value());
    }

    @Override
    public Optional<Drone> findByIdAndClientId(UUID droneId, String clientId) {
        return drones.findByDroneIdAndClientId(droneId, clientId).map(DroneEntity::toDomain);
    }

    @Override
    public List<Drone> findAllByClientId(String clientId) {
        return drones.findAllByClientIdOrderByRegisteredAt(clientId).stream()
                .map(DroneEntity::toDomain)
                .toList();
    }

    @Override
    public void save(Drone drone) {
        drones.saveAndFlush(DroneEntity.from(drone));
    }
}
