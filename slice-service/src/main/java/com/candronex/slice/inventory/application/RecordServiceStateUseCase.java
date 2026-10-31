package com.candronex.slice.inventory.application;

import com.candronex.slice.activation.domain.ServiceState;

import java.util.UUID;

/** Port d'entrée de l'inventaire, appelé en mémoire par l'activation, dans sa transaction. */
public interface RecordServiceStateUseCase {

    void record(UUID serviceId, UUID droneId, ServiceState state);
}
