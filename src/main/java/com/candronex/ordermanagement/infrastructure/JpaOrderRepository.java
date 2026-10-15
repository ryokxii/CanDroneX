package com.candronex.ordermanagement.infrastructure;

import com.candronex.ordermanagement.domain.IdempotencyKey;
import com.candronex.ordermanagement.domain.Order;
import com.candronex.ordermanagement.domain.port.OrderRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** Adaptateur sortant : réalise le port OrderRepository et traduit l'agrégat en lignes. */
@Repository
class JpaOrderRepository implements OrderRepository {

    private final OrderEntityStore orders;

    JpaOrderRepository(OrderEntityStore orders) {
        this.orders = orders;
    }

    @Override
    public Optional<Order> findByClientIdAndIdempotencyKey(UUID clientId, IdempotencyKey key) {
        return orders.findByClientIdAndIdempotencyKey(clientId, key.value())
                .map(OrderEntity::toDomain);
    }

    @Override
    public void save(Order order) {
        orders.save(OrderEntity.from(order));
    }
}
