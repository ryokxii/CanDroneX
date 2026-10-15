package com.candronex.servicecatalog.domain.port;

import com.candronex.servicecatalog.domain.ServiceSpecification;
import com.candronex.servicecatalog.domain.ServiceSpecificationId;

import java.util.Optional;

/** Port sortant de l'agrégat ServiceSpecification. */
public interface ServiceSpecificationRepository {

    Optional<ServiceSpecification> findById(ServiceSpecificationId id);
}
