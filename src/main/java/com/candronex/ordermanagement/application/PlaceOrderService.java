package com.candronex.ordermanagement.application;

import com.candronex.ordermanagement.domain.IdempotencyKey;
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
     * simultané par la contrainte UNIQUE, puis relu hors de la transaction perdue.
     */
    public PlaceOrderResult placeOrder(PlaceOrderCommand command) {
        IdempotencyKey key = IdempotencyKey.of(command.idempotencyKey());
        try {
            return placement.place(command, key);
        } catch (DataIntegrityViolationException concurrentReplay) {
            return orders.findByClientIdAndIdempotencyKey(command.clientId(), key)
                    .map(OrderView::from)
                    .map(PlaceOrderResult::replayed)
                    // Violation sans rapport avec la clé d'idempotence : l'erreur remonte.
                    .orElseThrow(() -> concurrentReplay);
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
