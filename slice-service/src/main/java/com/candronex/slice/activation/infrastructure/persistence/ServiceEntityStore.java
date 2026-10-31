package com.candronex.slice.activation.infrastructure.persistence;

import com.candronex.slice.activation.domain.ServiceState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Accès Spring Data aux lignes de activation.services. */
interface ServiceEntityStore extends JpaRepository<ServiceEntity, UUID> {

    Optional<ServiceEntity> findByOrderLineId(UUID orderLineId);

    List<ServiceEntity> findAllByState(ServiceState state);

    /** UPDATE conditionnel : 0 si le service n'est plus dans l'état attendu. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update ServiceEntity s set s.state = :next where s.serviceId = :serviceId and s.state = :expected")
    int updateState(@Param("serviceId") UUID serviceId, @Param("expected") ServiceState expected,
                    @Param("next") ServiceState next);
}
