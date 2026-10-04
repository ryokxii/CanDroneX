package com.candronex.ordermanagement.domain;

import com.candronex.servicecatalog.published.ServiceType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Racine d'agrégat du contexte OrderManagement : la commande de services d'un client B2B. */
@Entity
@Table(schema = "orders", name = "service_orders")
public class Order {

    @Id
    @Column(name = "order_id", nullable = false, length = 32)
    private String orderId;

    @Column(name = "client_id", nullable = false, length = 32)
    private String clientId;

    @Embedded
    private IdempotencyKey idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private OrderStatus status;

    @Column(name = "placed_at", nullable = false)
    private Instant placedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    /** Chargement immédiat assumé : on charge un agrégat entier ou pas du tout. */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderLine> orderLines = new ArrayList<>();

    /** Requis par l'ORM ; jamais appelé par le domaine. */
    protected Order() {
    }

    private Order(String orderId, String clientId, IdempotencyKey idempotencyKey,
                  Instant placedAt) {
        this.orderId = orderId;
        this.clientId = clientId;
        this.idempotencyKey = idempotencyKey;
        this.placedAt = placedAt;
        this.status = OrderStatus.RECEIVED;
    }

    /** Ouvre une commande pour un client. */
    public static Order place(String clientId, IdempotencyKey idempotencyKey,
                              Instant placedAt) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("L'identifiant du client est obligatoire.");
        }
        if (idempotencyKey == null) {
            throw new IllegalArgumentException("La clé d'idempotence est obligatoire.");
        }
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8);
        return new Order(orderId, clientId, idempotencyKey, placedAt);
    }

    /**
     * Ajoute une ligne et applique l'invariant de l'agrégat : au plus une ligne par couple drone +
     * type de service.
     */
    public OrderLine addLine(String droneId, ServiceType serviceType) {
        if (droneId == null || droneId.isBlank()) {
            throw new IllegalArgumentException("L'identifiant du drone est obligatoire.");
        }
        if (serviceType == null) {
            throw new IllegalArgumentException("Le type de service est obligatoire.");
        }
        boolean alreadyRequested = orderLines.stream()
                .anyMatch(line -> line.targets(droneId, serviceType));
        if (alreadyRequested) {
            throw new DuplicateOrderLineException(droneId, serviceType);
        }

        OrderLine line = new OrderLine(this, droneId, serviceType);
        orderLines.add(line);
        this.status = deriveStatus();
        return line;
    }

    /** Dérive l'état global des lignes ; la première règle qui s'applique l'emporte. */
    public OrderStatus deriveStatus() {
        if (orderLines.isEmpty()) {
            return OrderStatus.RECEIVED;
        }
        if (allLinesAre(OrderStatus.RECEIVED)) {
            return OrderStatus.RECEIVED;
        }
        boolean someStillRunning = orderLines.stream()
                .anyMatch(line -> !line.status().isTerminal());
        if (someStillRunning) {
            return OrderStatus.IN_PROGRESS;
        }
        if (allLinesAre(OrderStatus.COMPLETED)) {
            return OrderStatus.COMPLETED;
        }
        if (allLinesAre(OrderStatus.CANCELLED)) {
            return OrderStatus.CANCELLED;
        }
        boolean someCompleted = orderLines.stream()
                .anyMatch(line -> line.status() == OrderStatus.COMPLETED);
        return someCompleted ? OrderStatus.PARTIALLY_COMPLETED : OrderStatus.FAILED;
    }

    /** Fait évoluer l'état d'une ligne, puis recalcule celui de la commande. */
    public void updateLineStatus(String orderLineId, OrderStatus newStatus, Instant now) {
        OrderLine line = orderLines.stream()
                .filter(candidate -> candidate.orderLineId().equals(orderLineId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Ligne inconnue dans cette commande : " + orderLineId));

        line.changeStatus(newStatus);
        this.status = deriveStatus();
        this.completedAt = this.status.isTerminal() || this.status == OrderStatus.PARTIALLY_COMPLETED
                ? now
                : null;
    }

    private boolean allLinesAre(OrderStatus expected) {
        return orderLines.stream().allMatch(line -> line.status() == expected);
    }

    /** Une commande n'a de sens qu'avec au moins une ligne. */
    public boolean hasLines() {
        return !orderLines.isEmpty();
    }

    public boolean belongsTo(String candidateClientId) {
        return clientId.equals(candidateClientId);
    }

    public String orderId() {
        return orderId;
    }

    public String clientId() {
        return clientId;
    }

    public IdempotencyKey idempotencyKey() {
        return idempotencyKey;
    }

    public OrderStatus status() {
        return status;
    }

    public Instant placedAt() {
        return placedAt;
    }

    public Instant completedAt() {
        return completedAt;
    }

    /** Vue non modifiable : les lignes ne s'ajoutent que par addLine. */
    public List<OrderLine> orderLines() {
        return Collections.unmodifiableList(orderLines);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Order that)) {
            return false;
        }
        return Objects.equals(orderId, that.orderId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId);
    }
}
