package com.candronex.order.application;

import com.candronex.order.domain.DroneNotFoundException;
import com.candronex.order.domain.IdempotencyKey;
import com.candronex.order.domain.port.DroneOwnershipPort;
import com.candronex.order.domain.port.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/** Réalise PlaceOrderUseCase. */
@Service
public class PlaceOrderService implements PlaceOrderUseCase {

    private static final Logger log = LoggerFactory.getLogger(PlaceOrderService.class);

    private final OrderPlacement placement;
    private final OrderRepository orders;
    private final DroneOwnershipPort drones;
    private final SyncActivationStatusUseCase activations;

    public PlaceOrderService(OrderPlacement placement, OrderRepository orders, DroneOwnershipPort drones,
                             SyncActivationStatusUseCase activations) {
        this.placement = placement;
        this.orders = orders;
        this.drones = drones;
        this.activations = activations;
    }

    /**
     * Idempotent : un rejeu est servi avant tout appel à Drone Service ; le rejeu simultané est
     * départagé par la contrainte UNIQUE, puis relu hors de la transaction perdue. Une clé rejouée
     * avec un autre contenu est refusée.
     */
    @Override
    public PlaceOrderResult placeOrder(PlaceOrderCommand command) {
        IdempotencyKey key = IdempotencyKey.of(command.idempotencyKey());

        Optional<PlaceOrderResult> replay = replayOf(command, key);
        if (replay.isPresent()) {
            return replay.get();
        }
        if (command.orderLines() == null || command.orderLines().isEmpty()) {
            throw new IllegalArgumentException("Une commande doit comporter au moins une ligne.");
        }

        // Hors transaction : un appel REST ne garde pas de connexion à la base ouverte.
        command.orderLines().stream()
                .map(PlaceOrderCommand.OrderLine::droneId)
                .distinct()
                .forEach(droneId -> verifyOwnership(droneId, command.clientToken()));

        PlaceOrderResult result;
        try {
            result = placement.place(command, key);
        } catch (DataIntegrityViolationException concurrentReplay) {
            // Violation sans rapport avec la clé d'idempotence : l'erreur remonte.
            return replayOf(command, key).orElseThrow(() -> concurrentReplay);
        }
        if (!result.created()) {
            return result;
        }
        log.info("event=order-received orderId={} clientId={} lines={}",
                result.order().orderId(), command.clientId(), result.order().orderLines().size());
        return PlaceOrderResult.created(requestActivations(result.order(), command.clientId(), key));
    }

    /**
     * La commande est enregistrée : un échec ici ne la fait pas échouer. Les lignes restées
     * RECEIVED sont reprises par ActivationStatusPoller.
     */
    private OrderView requestActivations(OrderView placed, String clientId, IdempotencyKey key) {
        try {
            activations.sync(placed.orderId());
            return orders.findByClientIdAndIdempotencyKey(clientId, key).map(OrderView::from).orElse(placed);
        } catch (RuntimeException failure) {
            log.warn("event=activation-requested orderId={} error=deferred", placed.orderId(), failure);
            return placed;
        }
    }

    private Optional<PlaceOrderResult> replayOf(PlaceOrderCommand command, IdempotencyKey key) {
        return orders.findByClientIdAndIdempotencyKey(command.clientId(), key)
                .map(placed -> {
                    placed.verifyReplayOf(command.lineKeys());
                    log.info("event=order-replayed orderId={} clientId={}", placed.orderId(), command.clientId());
                    return PlaceOrderResult.replayed(OrderView.from(placed));
                });
    }

    private void verifyOwnership(UUID droneId, String clientToken) {
        if (!drones.isOwnedBy(droneId, clientToken)) {
            throw new DroneNotFoundException(droneId);
        }
    }
}
