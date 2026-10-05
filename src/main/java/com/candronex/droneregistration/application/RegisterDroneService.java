package com.candronex.droneregistration.application;

import com.candronex.accessmanagement.published.ClientDirectory;
import com.candronex.accessmanagement.published.UnknownClientException;
import com.candronex.droneregistration.domain.Drone;
import com.candronex.droneregistration.domain.DuplicateImsiException;
import com.candronex.droneregistration.domain.NetworkIdentity;
import com.candronex.droneregistration.domain.port.DroneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/** Service applicatif d'UC-02 : enregistrer un drone et son identité réseau. */
@Service
public class RegisterDroneService {

    private final DroneRepository drones;
    private final ClientDirectory clients;
    private final Clock clock;

    public RegisterDroneService(DroneRepository drones, ClientDirectory clients, Clock clock) {
        this.drones = drones;
        this.clients = clients;
        this.clock = clock;
    }

    /** Enregistre un drone pour le client identifié. */
    @Transactional
    public DroneView register(RegisterDroneCommand command) {
        if (!clients.exists(command.clientId())) {
            throw new UnknownClientException(command.clientId());
        }

        // Unicité globale de l'IMSI ; la contrainte UNIQUE en base départage les requêtes concurrentes.
        if (drones.existsByImsi(command.imsi())) {
            throw new DuplicateImsiException();
        }

        NetworkIdentity networkIdentity =
                NetworkIdentity.of(command.imsi(), command.simType());

        Drone drone = Drone.register(
                command.clientId(),
                networkIdentity,
                clock.instant());

        drones.save(drone);
        return DroneView.from(drone);
    }

    /** Les drones du client identifié, et eux seuls. */
    @Transactional(readOnly = true)
    public List<DroneView> listForClient(UUID clientId) {
        return drones.findAllByClientId(clientId).stream()
                .map(DroneView::from)
                .toList();
    }

    /** Un drone du client identifié. */
    @Transactional(readOnly = true)
    public DroneView findForClient(UUID droneId, UUID clientId) {
        return drones.findByIdAndClientId(droneId, clientId)
                .map(DroneView::from)
                .orElseThrow(() -> new NoSuchElementException("Drone introuvable : " + droneId));
    }
}
