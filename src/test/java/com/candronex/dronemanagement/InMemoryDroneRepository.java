package com.candronex.dronemanagement;

import com.candronex.dronemanagement.domain.Drone;
import com.candronex.dronemanagement.domain.Iccid;
import com.candronex.dronemanagement.domain.Imsi;
import com.candronex.dronemanagement.domain.port.DroneRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Réalisation en mémoire du port DroneRepository. */
class InMemoryDroneRepository implements DroneRepository {

    private final Map<UUID, Drone> byId = new LinkedHashMap<>();

    @Override
    public boolean existsByImsi(Imsi imsi) {
        return byId.values().stream()
                .anyMatch(drone -> drone.networkIdentity().imsi().equals(imsi));
    }

    @Override
    public boolean existsByIccid(Iccid iccid) {
        return byId.values().stream()
                .anyMatch(drone -> drone.networkIdentity().iccid().equals(iccid));
    }

    @Override
    public Optional<Drone> findByIdAndClientId(UUID droneId, UUID clientId) {
        return Optional.ofNullable(byId.get(droneId))
                .filter(drone -> drone.belongsTo(clientId));
    }

    @Override
    public List<Drone> findAllByClientId(UUID clientId) {
        return byId.values().stream().filter(drone -> drone.belongsTo(clientId)).toList();
    }

    @Override
    public void save(Drone drone) {
        byId.put(drone.droneId(), drone);
    }

    int count() {
        return byId.size();
    }
}
