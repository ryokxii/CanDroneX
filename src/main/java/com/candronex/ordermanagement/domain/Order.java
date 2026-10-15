package com.candronex.ordermanagement.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Racine d'agrégat du contexte OrderManagement (TMF641) : la commande de services d'un client B2B. */
public final class Order {

    private static final OrderStatusPolicy STATUS_POLICY = new OrderStatusPolicy();

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
     * spécification de service.
     */
    public OrderLine addLine(UUID droneId, OrderedService service) {
        if (droneId == null) {
            throw new IllegalArgumentException("L'identifiant du drone est obligatoire.");
        }
        if (service == null) {
            throw new IllegalArgumentException("Le service commandé est obligatoire.");
        }
        OrderLineKey key = new OrderLineKey(droneId, service.serviceSpecificationId());
        if (orderLines.stream().anyMatch(line -> line.key().equals(key))) {
            throw new DuplicateOrderLineException(droneId, service.serviceSpecificationId());
        }

        OrderLine line = OrderLine.receive(droneId, service);
        orderLines.add(line);
        this.status = STATUS_POLICY.derive(lineStatuses());
        return line;
    }

    /**
     * Fait évoluer l'état d'une ligne, puis recalcule celui de la commande. La date d'achèvement
     * n'existe que tant qu'aucune ligne n'attend de résultat : une reprise l'efface.
     */
    public void updateLineStatus(UUID orderLineId, OrderLineStatus newStatus, Instant now) {
        OrderLine line = orderLines.stream()
                .filter(candidate -> candidate.orderLineId().equals(orderLineId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Ligne inconnue dans cette commande : " + orderLineId));

        line.transitionTo(newStatus);
        this.status = STATUS_POLICY.derive(lineStatuses());
        this.completedAt = status.isPending() ? null : now;
    }

    private List<OrderLineStatus> lineStatuses() {
        return orderLines.stream().map(OrderLine::status).toList();
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

    /** Dans l'ordre de la demande ; vue non modifiable : les lignes ne s'ajoutent que par addLine. */
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
