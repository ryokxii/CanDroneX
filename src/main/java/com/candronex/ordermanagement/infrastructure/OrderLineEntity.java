package com.candronex.ordermanagement.infrastructure;

import com.candronex.ordermanagement.domain.OrderLine;
import com.candronex.ordermanagement.domain.OrderStatus;
import com.candronex.servicecatalog.published.ServiceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

/** Correspondance relationnelle d'une ligne de l'agrégat Order. */
@Entity
@Table(schema = "orders", name = "order_lines")
class OrderLineEntity {

    @Id
    @Column(name = "order_line_id", nullable = false, updatable = false)
    private UUID orderLineId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, updatable = false)
    private OrderEntity order;

    @Column(name = "drone_id", nullable = false, updatable = false)
    private UUID droneId;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false, updatable = false, length = 24)
    private ServiceType serviceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private OrderStatus status;

    /** Requis par l'ORM. */
    protected OrderLineEntity() {
    }

    static OrderLineEntity from(OrderLine line, OrderEntity order) {
        OrderLineEntity entity = new OrderLineEntity();
        entity.orderLineId = line.orderLineId();
        entity.order = order;
        entity.droneId = line.droneId();
        entity.serviceType = line.serviceType();
        entity.status = line.status();
        return entity;
    }

    OrderLine toDomain() {
        return OrderLine.restore(orderLineId, droneId, serviceType, status);
    }
}
