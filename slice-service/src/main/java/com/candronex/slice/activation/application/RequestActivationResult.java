package com.candronex.slice.activation.application;

/** Résultat d'une demande d'activation : créée, ou rejouée pour une ligne déjà reçue. */
public record RequestActivationResult(ServiceView service, boolean created) {
}
