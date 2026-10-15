package com.candronex.dronemanagement.infrastructure;

import com.candronex.dronemanagement.domain.Drone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Interface Spring Data, détail d'infrastructure. */
interface SpringDataDroneRepository extends JpaRepository<Drone, UUID> {

    boolean existsByNetworkIdentityImsi(String imsi);

    Optional<Drone> findByDroneIdAndClientId(UUID droneId, UUID clientId);

    List<Drone> findAllByClientId(UUID clientId);
}
