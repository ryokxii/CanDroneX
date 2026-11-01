package com.candronex.slice.inventory.infrastructure.persistence;

import com.candronex.slice.activation.domain.ServiceState;
import com.candronex.slice.inventory.domain.InventoryService;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Une ligne de inventory.services. */
@Entity
@Table(schema = "inventory", name = "services")
class InventoryServiceEntity {

    @Id
    @Column(name = "service_id", nullable = false, updatable = false)
    private UUID serviceId;

    @Column(name = "drone_id", nullable = false, updatable = false)
    private UUID droneId;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 16)
    private ServiceState state;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Requis par l'ORM. */
    protected InventoryServiceEntity() {
    }

    static InventoryServiceEntity from(InventoryService service) {
        InventoryServiceEntity entity = new InventoryServiceEntity();
        entity.serviceId = service.serviceId();
        entity.droneId = service.droneId();
        entity.state = service.state();
        entity.updatedAt = service.updatedAt();
        return entity;
    }

    InventoryService toDomain() {
        return InventoryService.restore(serviceId, droneId, state, updatedAt);
    }
}
