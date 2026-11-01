package com.candronex.slice.activation.application;

import java.util.UUID;

/** Port d'entrée : l'état du service d'une ligne de commande. */
public interface FindActivationUseCase {

    ServiceView findByOrderLine(UUID orderLineId);
}
