package com.candronex.slice.activation.domain;

import com.candronex.slice.activation.domain.port.ProvisioningResult;

/** Service de domaine : ce que devient un service selon la réponse du réseau. */
public final class ActivationOutcomePolicy {

    public ServiceState stateAfter(ProvisioningResult result) {
        return switch (result) {
            case SUCCESS -> ServiceState.ACTIVE;
            case FAILED -> ServiceState.FAILED;
            // Le réseau a pu appliquer la configuration : ce n'est pas un échec.
            case TIMEOUT -> ServiceState.UNCONFIRMED;
        };
    }

    public AttemptResult attemptResultOf(ProvisioningResult result) {
        return switch (result) {
            case SUCCESS -> AttemptResult.SUCCESS;
            case FAILED -> AttemptResult.FAILED;
            case TIMEOUT -> AttemptResult.UNKNOWN;
        };
    }
}
