package com.candronex.ordermanagement.domain;

import com.candronex.servicecatalog.published.ServiceType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Racine d'agrégat du contexte OrderManagement : la commande de services d'un client B2B. */
public final class Order {

    private final UUID orderId;
    private final UUID clientId;
    private final IdempotencyKey idempotencyKey;
    private final Instant placedAt;
    private final List<OrderLine> orderLines;
    private OrderStatus status;
    private Instant completedAt;

    private Order(UUID orderId, UUID clientId, IdempotencyKey idempotencyKey, Instant placedAt,
                  List<OrderLine> orderLines, OrderStatus status, Instant completedAt) {
        if (clientId == null) {
            throw new IllegalArgumentException("L'identifiant du client est obligatoire.");
        }
        if (idempotencyKey == null) {
            throw new IllegalArgumentException("La clé d'idempotence est obligatoire.");
        }
        this.orderId = Objects.requireNonNull(orderId, "orderId");
        this.clientId = clientId;
        this.idempotencyKey = idempotencyKey;
        this.placedAt = Objects.requireNonNull(placedAt, "placedAt");
        this.orderLines = new ArrayList<>(orderLines);
        this.status = Objects.requireNonNull(status, "status");
        this.completedAt = completedAt;
    }

    /** Ouvre une commande pour un client ; son identifiant est généré, jamais fourni. */
    public static Order place(UUID clientId, IdempotencyKey idempotencyKey, Instant placedAt) {
        return new Order(UUID.randomUUID(), clientId, idempotencyKey, placedAt,
                List.of(), OrderStatus.RECEIVED, null);
    }

    /** Reconstitue une commande enregistrée ; réservé à l'adaptateur de persistance. */
    public static Order restore(UUID orderId, UUID clientId, IdempotencyKey idempotencyKey,
                                Instant placedAt, List<OrderLine> orderLines,
                                OrderStatus status, Instant completedAt) {
        return new Order(orderId, clientId, idempotencyKey, placedAt,
                orderLines, status, completedAt);
    }

    /**
     * Ajoute une ligne et applique l'invariant de l'agrégat : au plus une ligne par couple drone +
     * type de service.
     */
    public OrderLine addLine(UUID droneId, ServiceType serviceType) {
        if (droneId == null) {
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

        OrderLine line = OrderLine.receive(droneId, serviceType);
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
    public void updateLineStatus(UUID orderLineId, OrderStatus newStatus, Instant now) {
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

    public boolean belongsTo(UUID candidateClientId) {
        return clientId.equals(candidateClientId);
    }

    public UUID orderId() {
        return orderId;
    }

    public UUID clientId() {
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
        return orderId.equals(that.orderId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId);
    }
}
