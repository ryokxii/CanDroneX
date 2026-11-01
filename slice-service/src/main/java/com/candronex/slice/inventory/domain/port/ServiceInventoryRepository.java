package com.candronex.slice.inventory.domain.port;

import com.candronex.slice.inventory.domain.InventoryService;

import java.util.Optional;
import java.util.UUID;

/** Port sortant de l'inventaire. */
public interface ServiceInventoryRepository {

    Optional<InventoryService> findById(UUID serviceId);

    void save(InventoryService service);
}
