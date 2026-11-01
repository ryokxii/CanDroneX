package com.candronex.order.application;

import com.candronex.order.domain.OrderLineKey;

import java.util.List;
import java.util.UUID;

/** Intention de commander des services de connectivité ; le jeton est relayé à Drone Service. */
public record PlaceOrderCommand(
        String clientId,
        String clientToken,
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

    /** Le jeton ne doit jamais apparaître dans un journal. */
    @Override
    public String toString() {
        return "PlaceOrderCommand[clientId=" + clientId + ", idempotencyKey=" + idempotencyKey
                + ", orderLines=" + orderLines + "]";
    }
}
