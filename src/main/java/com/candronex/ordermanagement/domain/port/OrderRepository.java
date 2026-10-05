package com.candronex.ordermanagement.domain.port;

import com.candronex.ordermanagement.domain.IdempotencyKey;
import com.candronex.ordermanagement.domain.Order;

import java.util.Optional;
import java.util.UUID;

/** Port sortant de l'agrégat Order. */
public interface OrderRepository {

    Optional<Order> findByClientIdAndIdempotencyKey(UUID clientId, IdempotencyKey key);

    Optional<Order> findByIdAndClientId(UUID orderId, UUID clientId);

    void save(Order order);
}
