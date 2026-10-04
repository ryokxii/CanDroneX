package com.candronex.droneregistration.domain.port;

import com.candronex.droneregistration.domain.Drone;

import java.util.List;
import java.util.Optional;

/** Port sortant de l'agrégat Drone. */
public interface DroneRepository {

    /** Unicité de l'IMSI sur toute la plateforme, tous clients confondus. */
    boolean existsByImsi(String imsi);

    Optional<Drone> findByIdAndClientId(String droneId, String clientId);

    List<Drone> findAllByClientId(String clientId);

    void save(Drone drone);
}
