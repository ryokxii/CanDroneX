package com.candronex.slice.activation.application;

import java.util.List;
import java.util.UUID;

/** Port d'entrée d'UC-06 : activer le service d'une ligne de commande sur le réseau. */
public interface ActivateServiceUseCase {

    /** Idempotent sur orderLineId : une ligne déjà reçue renvoie son service, sans en créer. */
    RequestActivationResult request(RequestActivationCommand command);

    /** Appelle le réseau pour un service PENDING ; hors de la requête, par ActivationWorker. */
    void attempt(UUID serviceId);

    /** Lit l'état réel d'un service UNCONFIRMED avant toute reprise. */
    void reconcile(UUID serviceId);

    List<UUID> unconfirmed();
}
