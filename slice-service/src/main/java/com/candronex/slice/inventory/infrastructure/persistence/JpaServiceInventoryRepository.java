package com.candronex.slice.inventory.infrastructure.persistence;

import com.candronex.slice.inventory.domain.InventoryService;
import com.candronex.slice.inventory.domain.port.ServiceInventoryRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** Adaptateur sortant : réalise le port ServiceInventoryRepository. */
@Repository
class JpaServiceInventoryRepository implements ServiceInventoryRepository {

    private final InventoryServiceEntityStore services;

    JpaServiceInventoryRepository(InventoryServiceEntityStore services) {
        this.services = services;
    }

    @Override
    public Optional<InventoryService> findById(UUID serviceId) {
        return services.findById(serviceId).map(InventoryServiceEntity::toDomain);
    }

    @Override
    public void save(InventoryService service) {
        services.save(InventoryServiceEntity.from(service));
    }
}
