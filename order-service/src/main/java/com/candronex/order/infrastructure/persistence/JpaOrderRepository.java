package com.candronex.order.infrastructure.persistence;

import com.candronex.order.domain.IdempotencyKey;
import com.candronex.order.domain.Order;
import com.candronex.order.domain.port.OrderRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Adaptateur sortant : réalise le port OrderRepository et traduit l'agrégat en lignes. */
@Repository
class JpaOrderRepository implements OrderRepository {

    private final OrderEntityStore orders;

    JpaOrderRepository(OrderEntityStore orders) {
        this.orders = orders;
    }

    @Override
    public Optional<Order> findByClientIdAndIdempotencyKey(String clientId, IdempotencyKey key) {
        return orders.findByClientIdAndIdempotencyKey(clientId, key.value())
                .map(OrderEntity::toDomain);
    }

    @Override
    public void save(Order order) {
        orders.save(OrderEntity.from(order));
    }
}
