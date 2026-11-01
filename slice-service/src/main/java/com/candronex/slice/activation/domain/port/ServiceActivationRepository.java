package com.candronex.slice.activation.domain.port;

import com.candronex.slice.activation.domain.Service;
import com.candronex.slice.activation.domain.ServiceState;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port sortant de l'agrégat Service. */
public interface ServiceActivationRepository {

    Optional<Service> findById(UUID serviceId);

    Optional<Service> findByOrderLineId(UUID orderLineId);

    List<Service> findAllByState(ServiceState state);

    /** Enregistre un nouveau service ; la contrainte UNIQUE (order_line_id) refuse un doublon. */
    void add(Service service);

    /**
     * Enregistre une transition, seulement si le service est encore dans l'état attendu : un seul
     * écrivain par transition.
     *
     * @return faux si un autre traitement a déjà fait évoluer le service
     */
    boolean saveIfStillIn(Service service, ServiceState expected);
}
