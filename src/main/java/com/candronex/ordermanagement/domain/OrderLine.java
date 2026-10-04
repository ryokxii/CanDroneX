package com.candronex.ordermanagement.domain;

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

import java.util.Objects;
import java.util.UUID;

/** Élément d'une commande : un service pour un drone. */
@Entity
@Table(schema = "orders", name = "order_lines")
public class OrderLine {

    @Id
    @Column(name = "order_line_id", nullable = false, length = 32)
    private String orderLineId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "drone_id", nullable = false, length = 32)
    private String droneId;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false, length = 24)
    private ServiceType serviceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private OrderStatus status;

    /** Requis par l'ORM ; jamais appelé par le domaine. */
    protected OrderLine() {
    }

    /** Visible de l'agrégat seul : une ligne ne se crée que par Order.addLine. */
    OrderLine(Order order, String droneId, ServiceType serviceType) {
        this.orderLineId = "OL-" + UUID.randomUUID().toString().substring(0, 8);
        this.order = order;
        this.droneId = droneId;
        this.serviceType = serviceType;
        this.status = OrderStatus.RECEIVED;
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
    boolean targets(String candidateDroneId, ServiceType candidateServiceType) {
        return droneId.equals(candidateDroneId) && serviceType == candidateServiceType;
    }

    public String orderLineId() {
        return orderLineId;
    }

    public String droneId() {
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
        return Objects.equals(orderLineId, that.orderLineId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderLineId);
    }
}
