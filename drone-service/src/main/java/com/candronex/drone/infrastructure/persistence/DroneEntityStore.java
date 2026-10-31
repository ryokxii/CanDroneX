package com.candronex.drone.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Accès Spring Data aux lignes de drone.drones ; détail interne de JpaDroneRepository. */
interface DroneEntityStore extends JpaRepository<DroneEntity, UUID> {

    boolean existsByImsi(String imsi);

    boolean existsByIccid(String iccid);

    Optional<DroneEntity> findByDroneIdAndClientId(UUID droneId, String clientId);

    List<DroneEntity> findAllByClientIdOrderByRegisteredAt(String clientId);
}
