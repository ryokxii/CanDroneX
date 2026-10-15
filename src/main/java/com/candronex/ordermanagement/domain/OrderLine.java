package com.candronex.ordermanagement.domain;

import com.candronex.servicecatalog.published.ServiceType;

import java.util.Objects;
import java.util.UUID;

/** Entité de l'agrégat Order : un service pour un drone, et l'état de son traitement. */
public final class OrderLine {

    private final UUID orderLineId;
    private final UUID droneId;
    private final ServiceType serviceType;
    private OrderStatus status;

    private OrderLine(UUID orderLineId, UUID droneId, ServiceType serviceType, OrderStatus status) {
        this.orderLineId = Objects.requireNonNull(orderLineId, "orderLineId");
        this.droneId = Objects.requireNonNull(droneId, "droneId");
        this.serviceType = Objects.requireNonNull(serviceType, "serviceType");
        this.status = Objects.requireNonNull(status, "status");
    }

    /** Visible de l'agrégat seul : une ligne ne se crée que par Order.addLine. */
    static OrderLine receive(UUID droneId, ServiceType serviceType) {
        return new OrderLine(UUID.randomUUID(), droneId, serviceType, OrderStatus.RECEIVED);
    }

    /** Reconstitue une ligne enregistrée ; réservé à l'adaptateur de persistance. */
    public static OrderLine restore(UUID orderLineId, UUID droneId, ServiceType serviceType,
                                    OrderStatus status) {
        return new OrderLine(orderLineId, droneId, serviceType, status);
    }

    /** Fait évoluer l'état de la ligne. */
    void changeStatus(OrderStatus newStatus) {
        if (newStatus == OrderStatus.PARTIALLY_COMPLETED) {
            throw new IllegalArgumentException(
                    "PARTIALLY_COMPLETED ne s'applique qu'à une commande, pas à une ligne.");
        }
        this.status = newStatus;
    }

    /** Une ligne vise un couple drone + type de service, qui l'identifie métier. */
    boolean targets(UUID candidateDroneId, ServiceType candidateServiceType) {
        return droneId.equals(candidateDroneId) && serviceType == candidateServiceType;
    }

    public UUID orderLineId() {
        return orderLineId;
    }

    public UUID droneId() {
        return droneId;
    }

    public ServiceType serviceType() {
        return serviceType;
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
