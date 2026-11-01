package com.candronex.order.catalog.domain.port;

import com.candronex.order.catalog.domain.ServiceSpecification;
import com.candronex.order.catalog.domain.ServiceSpecificationId;

import java.util.Optional;

/** Port sortant de l'agrégat ServiceSpecification. */
public interface ServiceSpecificationRepository {

    Optional<ServiceSpecification> findById(ServiceSpecificationId id);
}
