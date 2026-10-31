package com.candronex.order.domain.port;

import com.candronex.order.domain.IdempotencyKey;
import com.candronex.order.domain.Order;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port sortant de l'agrégat Order. Toute lecture d'une commande exige le client qui la possède. */
public interface OrderRepository {

    Optional<Order> findByClientIdAndIdempotencyKey(String clientId, IdempotencyKey key);

    /** Pour modifier la commande : verrouillée jusqu'à la fin de la transaction. */
    Optional<Order> findByIdForUpdate(UUID orderId);

    /** Commandes dont une ligne attend un résultat, les plus anciennes d'abord. */
    List<UUID> findPendingOrderIds(int limit);

    void save(Order order);
}
