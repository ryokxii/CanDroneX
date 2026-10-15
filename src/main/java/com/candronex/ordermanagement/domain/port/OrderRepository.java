package com.candronex.ordermanagement.domain.port;

import com.candronex.ordermanagement.domain.IdempotencyKey;
import com.candronex.ordermanagement.domain.Order;

import java.util.Optional;
import java.util.UUID;

/** Port sortant de l'agrégat Order. Toute lecture d'une commande exige le client qui la possède. */
public interface OrderRepository {

    Optional<Order> findByClientIdAndIdempotencyKey(UUID clientId, IdempotencyKey key);

    void save(Order order);
}
