package com.candronex.slice.activation.domain.port;

import java.util.UUID;

/** Port sortant vers le réseau 5G ; MockOpen5GSAdapter en Phase 2, Open5GS plus tard. */
public interface NetworkProvisioningPort {

    /** Applique la configuration ; même serviceId, même configuration. */
    ProvisioningResult provision(ProvisioningRequest request);

    /** État réel sur le réseau : SUCCESS si le service y est configuré, FAILED s'il en est absent. */
    ProvisioningResult findStatus(UUID serviceId);
}
