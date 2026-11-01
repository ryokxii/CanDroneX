package com.candronex.order.domain.port;

import com.candronex.order.domain.ServiceCharacteristic;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port sortant vers Slice Service, propriétaire des activations. */
public interface ServiceActivationPort {

    /**
     * Demande l'activation du service d'une ligne ; idempotent sur orderLineId.
     *
     * @throws com.candronex.order.application.DependencyUnavailableException si Slice Service ne
     *         répond pas dans le délai
     */
    ServiceState request(UUID orderLineId, UUID droneId, List<ServiceCharacteristic> characteristics);

    /** L'état du service de cette ligne, ou vide si aucune activation n'a été reçue. */
    Optional<ServiceState> findByOrderLine(UUID orderLineId);
}
