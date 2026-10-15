package com.candronex.ordermanagement.application;

import com.candronex.ordermanagement.domain.IdempotencyKey;
import com.candronex.ordermanagement.domain.Order;
import com.candronex.ordermanagement.domain.port.OrderRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

/** Point d'entrée applicatif d'UC-04 : commander les services de connectivité d'un drone. */
@Service
public class PlaceOrderService {

    private final OrderPlacement placement;
    private final OrderRepository orders;

    public PlaceOrderService(OrderPlacement placement, OrderRepository orders) {
        this.placement = placement;
        this.orders = orders;
    }

    /**
     * Idempotent : le rejeu séquentiel est intercepté par la lecture initiale, le rejeu
     * simultané par la contrainte UNIQUE, puis relu hors de la transaction perdue. Dans les deux
     * cas, une clé rejouée avec un autre contenu est refusée.
     */
    public PlaceOrderResult placeOrder(PlaceOrderCommand command) {
        IdempotencyKey key = IdempotencyKey.of(command.idempotencyKey());
        try {
            return placement.place(command, key);
        } catch (DataIntegrityViolationException concurrentReplay) {
            Order placedByOtherRequest = orders
                    .findByClientIdAndIdempotencyKey(command.clientId(), key)
                    // Violation sans rapport avec la clé d'idempotence : l'erreur remonte.
                    .orElseThrow(() -> concurrentReplay);

            placedByOtherRequest.verifyReplayOf(command.lineKeys());
            return PlaceOrderResult.replayed(OrderView.from(placedByOtherRequest));
        }
    }

    /** Une commande du client identifié. */
    @Transactional(readOnly = true)
    public OrderView findForClient(UUID orderId, UUID clientId) {
        return orders.findByIdAndClientId(orderId, clientId)
                .map(OrderView::from)
                .orElseThrow(() -> new NoSuchElementException("Commande introuvable : " + orderId));
    }
}
