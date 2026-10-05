package com.candronex.ordermanagement.infrastructure;

import com.candronex.ordermanagement.domain.IdempotencyKey;
import com.candronex.ordermanagement.domain.Order;
import com.candronex.ordermanagement.domain.port.OrderRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** Adaptateur sortant : réalise le port OrderRepository avec l'ORM. */
@Repository
class JpaOrderRepository implements OrderRepository {

    private final SpringDataOrderRepository orders;

    JpaOrderRepository(SpringDataOrderRepository orders) {
        this.orders = orders;
    }

    @Override
    public Optional<Order> findByClientIdAndIdempotencyKey(UUID clientId, IdempotencyKey key) {
        return orders.findByClientIdAndIdempotencyKey(clientId, key);
    }

    @Override
    public Optional<Order> findByIdAndClientId(UUID orderId, UUID clientId) {
        return orders.findByOrderIdAndClientId(orderId, clientId);
    }

    @Override
    public void save(Order order) {
        orders.save(order);
    }
}
