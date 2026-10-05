package com.candronex.droneregistration;

import com.candronex.droneregistration.domain.Drone;
import com.candronex.droneregistration.domain.port.DroneRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Réalisation en mémoire du port DroneRepository. */
class InMemoryDroneRepository implements DroneRepository {

    private final Map<UUID, Drone> byId = new LinkedHashMap<>();

    @Override
    public boolean existsByImsi(String imsi) {
        return byId.values().stream()
                .anyMatch(drone -> drone.networkIdentity().imsi().equals(imsi));
    }

    @Override
    public Optional<Drone> findByIdAndClientId(UUID droneId, UUID clientId) {
        return Optional.ofNullable(byId.get(droneId))
                .filter(drone -> drone.belongsTo(clientId));
    }

    @Override
    public List<Drone> findAllByClientId(UUID clientId) {
        List<Drone> found = new ArrayList<>();
        byId.values().stream().filter(drone -> drone.belongsTo(clientId)).forEach(found::add);
        return found;
    }

    @Override
    public void save(Drone drone) {
        byId.put(drone.droneId(), drone);
    }

    int count() {
        return byId.size();
    }
}
