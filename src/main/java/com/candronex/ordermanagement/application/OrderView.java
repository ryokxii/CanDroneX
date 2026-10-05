package com.candronex.ordermanagement.application;

import com.candronex.ordermanagement.domain.Order;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Projection d'une commande destinée à sortir du module. */
public record OrderView(
        UUID orderId,
        UUID clientId,
        String status,
        Instant placedAt,
        Instant completedAt,
        List<OrderLineView> orderLines) {

    public static OrderView from(Order order) {
        List<OrderLineView> lineViews = order.orderLines().stream()
                .map(line -> new OrderLineView(
                        line.orderLineId(),
                        line.droneId(),
                        line.serviceType().name(),
                        line.status().name()))
                .toList();

        return new OrderView(
                order.orderId(),
                order.clientId(),
                order.status().name(),
                order.placedAt(),
                order.completedAt(),
                lineViews);
    }

    /** L'état par ligne : c'est lui qui fait autorité. */
    public record OrderLineView(
            UUID orderLineId,
            UUID droneId,
            String serviceType,
            String status) {
    }
}
