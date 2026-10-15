package com.candronex.ordermanagement.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Accès Spring Data aux lignes du schéma orders ; détail interne de JpaOrderRepository. */
interface OrderEntityStore extends JpaRepository<OrderEntity, UUID> {

    Optional<OrderEntity> findByClientIdAndIdempotencyKey(UUID clientId, String idempotencyKey);
}
