package com.candronex.slice.inventory.application;

import com.candronex.slice.activation.domain.ServiceState;
import com.candronex.slice.inventory.domain.InventoryService;
import com.candronex.slice.inventory.domain.port.ServiceInventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/** Réalise RecordServiceStateUseCase. */
@Service
class ServiceInventoryService implements RecordServiceStateUseCase {

    private final ServiceInventoryRepository inventory;
    private final Clock clock;

    ServiceInventoryService(ServiceInventoryRepository inventory, Clock clock) {
        this.inventory = inventory;
        this.clock = clock;
    }

    // Même transaction que l'activation : l'inventaire ne diverge jamais du service.
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(UUID serviceId, UUID droneId, ServiceState state) {
        InventoryService entry = inventory.findById(serviceId)
                .map(existing -> {
                    existing.record(state, clock.instant());
                    return existing;
                })
                .orElseGet(() -> InventoryService.register(serviceId, droneId, state, clock.instant()));
        inventory.save(entry);
    }
}
