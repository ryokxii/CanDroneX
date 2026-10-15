package com.candronex.ordermanagement.domain;

import java.util.Objects;
import java.util.UUID;

/** Entité de l'agrégat Order : un service commandé pour un drone, et l'état de son traitement. */
public final class OrderLine {

    private final UUID orderLineId;
    private final UUID droneId;
    private final OrderedService service;
    private OrderStatus status;

    private OrderLine(UUID orderLineId, UUID droneId, OrderedService service, OrderStatus status) {
        this.orderLineId = Objects.requireNonNull(orderLineId, "orderLineId");
        this.droneId = Objects.requireNonNull(droneId, "droneId");
        this.service = Objects.requireNonNull(service, "service");
        this.status = Objects.requireNonNull(status, "status");
    }

    /** Visible de l'agrégat seul : une ligne ne se crée que par Order.addLine. */
    static OrderLine receive(UUID droneId, OrderedService service) {
        return new OrderLine(UUID.randomUUID(), droneId, service, OrderStatus.RECEIVED);
    }

    /** Reconstitue une ligne enregistrée ; réservé à l'adaptateur de persistance. */
    public static OrderLine restore(UUID orderLineId, UUID droneId, OrderedService service,
                                    OrderStatus status) {
        return new OrderLine(orderLineId, droneId, service, status);
    }

    /** Fait évoluer l'état de la ligne. */
    void changeStatus(OrderStatus newStatus) {
        if (newStatus == OrderStatus.PARTIALLY_COMPLETED) {
            throw new IllegalArgumentException(
                    "PARTIALLY_COMPLETED ne s'applique qu'à une commande, pas à une ligne.");
        }
        this.status = newStatus;
    }

    public OrderLineKey key() {
        return new OrderLineKey(droneId, service.serviceSpecificationId());
    }

    public UUID orderLineId() {
        return orderLineId;
    }

    public UUID droneId() {
        return droneId;
    }

    public OrderedService service() {
        return service;
    }

    public OrderStatus status() {
        return status;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof OrderLine that)) {
            return false;
        }
        return orderLineId.equals(that.orderLineId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderLineId);
    }
}
