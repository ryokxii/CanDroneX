package com.candronex.slice.activation.domain.port;

/** Réponse du réseau : succès, refus, ou aucune réponse dans le délai. */
public enum ProvisioningResult {
    SUCCESS,
    FAILED,
    TIMEOUT
}
