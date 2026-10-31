package com.candronex.order.application;

import java.util.List;
import java.util.UUID;

/**
 * Port d'entrée : rapprocher les lignes d'une commande de l'état de leur service dans Slice
 * Service. RECEIVED : redemander l'activation ; IN_PROGRESS : lire l'état.
 */
public interface SyncActivationStatusUseCase {

    void sync(UUID orderId);

    List<UUID> pendingOrders();
}
