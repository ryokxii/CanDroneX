package com.candronex.drone.domain.port;

import com.candronex.drone.domain.Drone;
import com.candronex.drone.domain.Iccid;
import com.candronex.drone.domain.Imsi;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port sortant de l'agrégat Drone. Toute lecture d'un drone exige le client qui le possède. */
public interface DroneRepository {

    /** Unicité de l'IMSI sur toute la plateforme, tous clients confondus. */
    boolean existsByImsi(Imsi imsi);

    /** Unicité de l'ICCID sur toute la plateforme, tous clients confondus. */
    boolean existsByIccid(Iccid iccid);

    Optional<Drone> findByIdAndClientId(UUID droneId, String clientId);

    List<Drone> findAllByClientId(String clientId);

    void save(Drone drone);
}
