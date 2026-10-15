package com.candronex.dronemanagement.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Accès Spring Data aux lignes de drone.drones ; détail interne de JpaDroneRepository. */
interface DroneEntityStore extends JpaRepository<DroneEntity, UUID> {

    boolean existsByImsi(String imsi);

    boolean existsByIccid(String iccid);

    Optional<DroneEntity> findByDroneIdAndClientId(UUID droneId, UUID clientId);

    List<DroneEntity> findAllByClientIdOrderByRegisteredAt(UUID clientId);
}
