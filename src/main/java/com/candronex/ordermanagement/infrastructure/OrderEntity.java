package com.candronex.ordermanagement.infrastructure;

import com.candronex.ordermanagement.domain.IdempotencyKey;
import com.candronex.ordermanagement.domain.Order;
import com.candronex.ordermanagement.domain.OrderStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Correspondance relationnelle de l'agrégat Order ; ne sort jamais de l'adaptateur. */
@Entity
@Table(schema = "orders", name = "service_orders")
class OrderEntity {

    @Id
    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Column(name = "client_id", nullable = false, updatable = false)
    private UUID clientId;

    @Column(name = "idempotency_key", nullable = false, updatable = false, length = 64)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private OrderStatus status;

    @Column(name = "placed_at", nullable = false, updatable = false)
    private Instant placedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    /** Chargement immédiat assumé : on charge un agrégat entier ou pas du tout. */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderLineEntity> orderLines = new ArrayList<>();

    /** Requis par l'ORM. */
    protected OrderEntity() {
    }

    static OrderEntity from(Order order) {
        OrderEntity entity = new OrderEntity();
        entity.orderId = order.orderId();
        entity.clientId = order.clientId();
        entity.idempotencyKey = order.idempotencyKey().value();
        entity.status = order.status();
        entity.placedAt = order.placedAt();
        entity.completedAt = order.completedAt();
        order.orderLines().forEach(line -> entity.orderLines.add(OrderLineEntity.from(line, entity)));
        return entity;
    }

    Order toDomain() {
        return Order.restore(
                orderId,
                clientId,
                IdempotencyKey.of(idempotencyKey),
                placedAt,
                orderLines.stream().map(OrderLineEntity::toDomain).toList(),
                status,
                completedAt);
    }
}
