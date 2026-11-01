package com.candronex.slice.activation.domain.port;

import com.candronex.slice.activation.domain.ServiceCharacteristic;

import java.util.List;
import java.util.UUID;

/** Ce que le réseau doit configurer ; l'IMSI n'y figure pas, il reste dans Drone Service. */
public record ProvisioningRequest(UUID serviceId, List<ServiceCharacteristic> characteristics) {

    public ProvisioningRequest {
        characteristics = List.copyOf(characteristics);
    }
}
