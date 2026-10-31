package com.candronex.order;

import com.candronex.order.domain.IdempotencyKey;
import com.candronex.order.domain.Order;
import com.candronex.order.domain.port.OrderRepository;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Réalisation en mémoire du port OrderRepository. */
class InMemoryOrderRepository implements OrderRepository {

    private final Map<UUID, Order> byId = new LinkedHashMap<>();
    private boolean nextSaveViolatesUniqueKey = false;
    private int hiddenLookups = 0;

    @Override
    public Optional<Order> findByClientIdAndIdempotencyKey(String clientId, IdempotencyKey key) {
        if (hiddenLookups > 0) {
            hiddenLookups--;
            return Optional.empty();
        }
        return byId.values().stream()
                .filter(order -> order.clientId().equals(clientId))
                .filter(order -> order.idempotencyKey().equals(key))
                .findFirst();
    }

    @Override
    public void save(Order order) {
        if (nextSaveViolatesUniqueKey) {
            nextSaveViolatesUniqueKey = false;
            throw new DataIntegrityViolationException(
                    "duplicate key value violates unique constraint "
                            + "\"uq_orders_client_idempotency\"");
        }
        byId.put(order.orderId(), order);
    }

    /** Fait échouer la prochaine écriture comme le ferait la contrainte d'unicité. */
    void failNextSaveAsConcurrentReplay() {
        this.nextSaveViolatesUniqueKey = true;
    }

    /**
     * Rend aveugles les deux lectures qui précèdent l'écriture (avant l'appel au drone, puis dans la
     * transaction), comme une requête qui lit avant que la transaction concurrente ait validé.
     */
    void hideLookupsBeforeSave() {
        this.hiddenLookups = 2;
    }

    int count() {
        return byId.size();
    }
}
