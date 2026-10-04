package com.candronex.droneregistration.infrastructure;

import com.candronex.droneregistration.domain.Drone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Interface Spring Data, détail d'infrastructure. */
interface SpringDataDroneRepository extends JpaRepository<Drone, String> {

    boolean existsByNetworkIdentityImsi(String imsi);

    Optional<Drone> findByDroneIdAndClientId(String droneId, String clientId);

    List<Drone> findAllByClientId(String clientId);
}
