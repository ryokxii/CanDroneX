package com.candronex.order.domain.port;

import com.candronex.order.domain.IdempotencyKey;
import com.candronex.order.domain.Order;

import java.util.Optional;

/** Port sortant de l'agrégat Order. Toute lecture d'une commande exige le client qui la possède. */
public interface OrderRepository {

    Optional<Order> findByClientIdAndIdempotencyKey(String clientId, IdempotencyKey key);

    void save(Order order);
}
