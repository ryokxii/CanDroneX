package com.candronex.drone.application;

import com.candronex.drone.domain.Drone;
import com.candronex.drone.domain.DroneNotFoundException;
import com.candronex.drone.domain.DuplicateIccidException;
import com.candronex.drone.domain.DuplicateImsiException;
import com.candronex.drone.domain.NetworkIdentity;
import com.candronex.drone.domain.port.DroneRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/** Service applicatif d'UC-02 : enregistrer un drone et son identité réseau. */
@Service
public class RegisterDroneService {

    private static final Logger log = LoggerFactory.getLogger(RegisterDroneService.class);

    private final DroneRepository drones;
    private final Clock clock;

    public RegisterDroneService(DroneRepository drones, Clock clock) {
        this.drones = drones;
        this.clock = clock;
    }

    /** Enregistre un drone pour le client identifié. */
    @Transactional
    public DroneView register(RegisterDroneCommand command) {
        // Format d'abord : une identité mal formée est refusée sans interroger la base.
        NetworkIdentity networkIdentity =
                NetworkIdentity.of(command.imsi(), command.iccid(), command.simType());

        // Unicité globale ; les contraintes UNIQUE en base départagent les requêtes concurrentes.
        if (drones.existsByImsi(networkIdentity.imsi())) {
            throw new DuplicateImsiException();
        }
        if (drones.existsByIccid(networkIdentity.iccid())) {
            throw new DuplicateIccidException();
        }

        Drone drone = Drone.register(command.clientId(), networkIdentity, clock.instant());
        drones.save(drone);
        log.info("event=drone-registered clientId={} droneId={}", command.clientId(), drone.droneId());
        return DroneView.from(drone);
    }

    /** Les drones du client identifié, et eux seuls. */
    @Transactional(readOnly = true)
    public List<DroneView> listForClient(String clientId) {
        return drones.findAllByClientId(clientId).stream()
                .map(DroneView::from)
                .toList();
    }

    /** Un drone du client identifié ; un drone d'un autre client est introuvable. */
    @Transactional(readOnly = true)
    public DroneView findForClient(UUID droneId, String clientId) {
        return drones.findByIdAndClientId(droneId, clientId)
                .map(DroneView::from)
                .orElseThrow(() -> new DroneNotFoundException(droneId));
    }
}
