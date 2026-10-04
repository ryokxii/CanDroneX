package com.candronex.ordermanagement.infrastructure;

import com.candronex.ordermanagement.domain.IdempotencyKey;
import com.candronex.ordermanagement.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Interface Spring Data, détail d'infrastructure. */
interface SpringDataOrderRepository extends JpaRepository<Order, String> {

    Optional<Order> findByClientIdAndIdempotencyKey(String clientId, IdempotencyKey key);

    Optional<Order> findByOrderIdAndClientId(String orderId, String clientId);
}
