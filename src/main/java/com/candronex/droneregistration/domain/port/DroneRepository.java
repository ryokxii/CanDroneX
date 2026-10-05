package com.candronex.droneregistration.domain.port;

import com.candronex.droneregistration.domain.Drone;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port sortant de l'agrégat Drone. */
public interface DroneRepository {

    /** Unicité de l'IMSI sur toute la plateforme, tous clients confondus. */
    boolean existsByImsi(String imsi);

    Optional<Drone> findByIdAndClientId(UUID droneId, UUID clientId);

    List<Drone> findAllByClientId(UUID clientId);

    void save(Drone drone);
}
