package com.candronex.ordermanagement.api;

import com.candronex.ordermanagement.application.OrderView;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Représentation JSON d'une commande et de ses lignes. */
public record OrderResponse(
        UUID orderId,
        String status,
        Instant placedAt,
        Instant completedAt,
        List<OrderLineResponse> orderLines) {

    static OrderResponse from(OrderView view) {
        List<OrderLineResponse> orderLines = view.orderLines().stream()
                .map(line -> new OrderLineResponse(
                        line.orderLineId(),
                        line.droneId(),
                        line.serviceType(),
                        line.status()))
                .toList();

        return new OrderResponse(
                view.orderId(),
                view.status(),
                view.placedAt(),
                view.completedAt(),
                orderLines);
    }

    public record OrderLineResponse(
            UUID orderLineId,
            UUID droneId,
            String serviceType,
            String status) {
    }
}
