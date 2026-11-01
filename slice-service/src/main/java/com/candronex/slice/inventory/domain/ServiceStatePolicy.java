package com.candronex.slice.inventory.domain;

import com.candronex.slice.activation.domain.ServiceState;

/** Service de domaine : les transitions d'état qu'accepte l'inventaire. */
public final class ServiceStatePolicy {

    public boolean allows(ServiceState from, ServiceState to) {
        return switch (from) {
            case PENDING -> to == ServiceState.ACTIVE || to == ServiceState.FAILED
                    || to == ServiceState.UNCONFIRMED;
            case UNCONFIRMED -> to == ServiceState.ACTIVE || to == ServiceState.PENDING;
            // Reprise d'un échec (UC-10).
            case FAILED -> to == ServiceState.PENDING;
            case ACTIVE -> false;
        };
    }
}
