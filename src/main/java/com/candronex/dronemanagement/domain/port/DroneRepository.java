package com.candronex.dronemanagement.domain.port;

import com.candronex.dronemanagement.domain.Drone;
import com.candronex.dronemanagement.domain.Iccid;
import com.candronex.dronemanagement.domain.Imsi;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port sortant de l'agrégat Drone. Toute lecture d'un drone exige le client qui le possède. */
public interface DroneRepository {

    /** Unicité de l'IMSI sur toute la plateforme, tous clients confondus. */
    boolean existsByImsi(Imsi imsi);

    /** Unicité de l'ICCID sur toute la plateforme, tous clients confondus. */
    boolean existsByIccid(Iccid iccid);

    Optional<Drone> findByIdAndClientId(UUID droneId, UUID clientId);

    List<Drone> findAllByClientId(UUID clientId);

    void save(Drone drone);
}
