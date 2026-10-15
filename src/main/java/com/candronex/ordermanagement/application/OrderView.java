package com.candronex.ordermanagement.application;

import com.candronex.ordermanagement.domain.Order;
import com.candronex.ordermanagement.domain.OrderLine;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Projection d'une commande destinée à sortir du service applicatif. */
public record OrderView(
        UUID orderId,
        String status,
        Instant placedAt,
        Instant completedAt,
        List<OrderLineView> orderLines) {

    public static OrderView from(Order order) {
        return new OrderView(
                order.orderId(),
                order.status().name(),
                order.placedAt(),
                order.completedAt(),
                order.orderLines().stream().map(OrderView::lineOf).toList());
    }

    private static OrderLineView lineOf(OrderLine line) {
        return new OrderLineView(
                line.orderLineId(),
                line.droneId(),
                line.service().serviceSpecificationId(),
                line.service().serviceSpecificationVersion(),
                line.service().characteristics().stream()
                        .map(characteristic -> new CharacteristicView(
                                characteristic.name(), characteristic.value()))
                        .toList(),
                line.status().name());
    }

    /** L'état par ligne : c'est lui qui fait autorité. */
    public record OrderLineView(
            UUID orderLineId,
            UUID droneId,
            String serviceSpecificationId,
            int serviceSpecificationVersion,
            List<CharacteristicView> serviceCharacteristics,
            String status) {
    }

    /** Une caractéristique commandée, telle que figée à la soumission. */
    public record CharacteristicView(String name, String value) {
    }
}
