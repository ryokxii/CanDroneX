package com.candronex.ordermanagement;

import com.candronex.ordermanagement.domain.IdempotencyKey;
import com.candronex.ordermanagement.domain.Order;
import com.candronex.ordermanagement.domain.port.OrderRepository;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Réalisation en mémoire du port OrderRepository. */
class InMemoryOrderRepository implements OrderRepository {

    private final Map<UUID, Order> byId = new LinkedHashMap<>();
    private boolean nextSaveViolatesUniqueKey = false;
    private boolean nextLookupSeesNothing = false;

    @Override
    public Optional<Order> findByClientIdAndIdempotencyKey(UUID clientId, IdempotencyKey key) {
        if (nextLookupSeesNothing) {
            nextLookupSeesNothing = false;
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
     * Rend la prochaine lecture aveugle, comme l'est une requête qui consulte la base avant que la
     * transaction concurrente ait validé.
     */
    void hideNextLookup() {
        this.nextLookupSeesNothing = true;
    }

    int count() {
        return byId.size();
    }
}
