package com.candronex.ordermanagement.domain.port;

import com.candronex.ordermanagement.domain.IdempotencyKey;
import com.candronex.ordermanagement.domain.Order;

import java.util.Optional;

/** Port sortant de l'agrégat Order. */
public interface OrderRepository {

    Optional<Order> findByClientIdAndIdempotencyKey(String clientId, IdempotencyKey key);

    Optional<Order> findByIdAndClientId(String orderId, String clientId);

    void save(Order order);
}
