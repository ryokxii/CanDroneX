package com.candronex.order.infrastructure.persistence;

import com.candronex.order.domain.IdempotencyKey;
import com.candronex.order.domain.Order;
import com.candronex.order.domain.OrderStatus;
import com.candronex.order.domain.port.OrderRepository;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Repository;

import java.util.List;
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
    public Optional<Order> findByClientIdAndIdempotencyKey(String clientId, IdempotencyKey key) {
        return orders.findByClientIdAndIdempotencyKey(clientId, key.value())
                .map(OrderEntity::toDomain);
    }

    @Override
    public Optional<Order> findByIdForUpdate(UUID orderId) {
        return orders.findByIdForUpdate(orderId).map(OrderEntity::toDomain);
    }

    @Override
    public List<UUID> findPendingOrderIds(int limit) {
        return orders.findIdsByStatusIn(List.of(OrderStatus.RECEIVED, OrderStatus.IN_PROGRESS), Limit.of(limit));
    }

    @Override
    public void save(Order order) {
        orders.save(OrderEntity.from(order));
    }
}
