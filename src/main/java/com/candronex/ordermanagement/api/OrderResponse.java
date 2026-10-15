package com.candronex.ordermanagement.api;

import com.candronex.ordermanagement.application.OrderView;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Représentation JSON d'une commande et de ses lignes. Ni le client ni la clé n'y figurent. */
public record OrderResponse(
        UUID orderId,
        String status,
        Instant placedAt,
        Instant completedAt,
        List<OrderLineResponse> orderLines) {

    static OrderResponse from(OrderView view) {
        return new OrderResponse(
                view.orderId(),
                view.status(),
                view.placedAt(),
                view.completedAt(),
                view.orderLines().stream().map(OrderResponse::lineOf).toList());
    }

    private static OrderLineResponse lineOf(OrderView.OrderLineView line) {
        return new OrderLineResponse(
                line.orderLineId(),
                line.droneId(),
                line.serviceSpecificationId(),
                line.serviceSpecificationVersion(),
                line.serviceCharacteristics().stream()
                        .map(characteristic -> new CharacteristicResponse(
                                characteristic.name(), characteristic.value()))
                        .toList(),
                line.status());
    }

    public record OrderLineResponse(
            UUID orderLineId,
            UUID droneId,
            String serviceSpecificationId,
            int serviceSpecificationVersion,
            List<CharacteristicResponse> serviceCharacteristics,
            String status) {
    }

    public record CharacteristicResponse(String name, String value) {
    }
}
