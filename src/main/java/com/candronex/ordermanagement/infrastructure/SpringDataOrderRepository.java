package com.candronex.ordermanagement.infrastructure;

import com.candronex.ordermanagement.domain.IdempotencyKey;
import com.candronex.ordermanagement.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Interface Spring Data, détail d'infrastructure. */
interface SpringDataOrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByClientIdAndIdempotencyKey(UUID clientId, IdempotencyKey key);

    Optional<Order> findByOrderIdAndClientId(UUID orderId, UUID clientId);
}
