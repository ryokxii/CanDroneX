package com.candronex.ordermanagement.application;

import com.candronex.ordermanagement.domain.OrderLineKey;

import java.util.List;
import java.util.UUID;

/** Intention de commander des services de connectivité. */
public record PlaceOrderCommand(
        UUID clientId,
        String idempotencyKey,
        List<OrderLine> orderLines) {

    /** Une ligne demandée : un service du catalogue pour un drone. */
    public record OrderLine(UUID droneId, String serviceSpecificationId) {

        OrderLineKey key() {
            return new OrderLineKey(droneId, serviceSpecificationId);
        }
    }

    /** Le contenu de la demande, tel que l'idempotence le compare. */
    List<OrderLineKey> lineKeys() {
        return orderLines == null ? List.of() : orderLines.stream().map(OrderLine::key).toList();
    }
}
