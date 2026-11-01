package com.candronex.slice.inventory.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Accès Spring Data aux lignes de inventory.services. */
interface InventoryServiceEntityStore extends JpaRepository<InventoryServiceEntity, UUID> {
}
