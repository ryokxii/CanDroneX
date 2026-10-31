package com.candronex.order.infrastructure.persistence;

import com.candronex.order.domain.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Accès Spring Data aux lignes du schéma orders ; détail interne de JpaOrderRepository. */
interface OrderEntityStore extends JpaRepository<OrderEntity, UUID> {

    Optional<OrderEntity> findByClientIdAndIdempotencyKey(String clientId, String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OrderEntity o where o.orderId = :orderId")
    Optional<OrderEntity> findByIdForUpdate(UUID orderId);

    @Query("select o.orderId from OrderEntity o where o.status in :statuses order by o.placedAt")
    List<UUID> findIdsByStatusIn(Collection<OrderStatus> statuses, Limit limit);
}
