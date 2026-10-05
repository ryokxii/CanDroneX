package com.candronex.droneregistration.published;

import java.util.UUID;

/** Objet simple d'échange entre modules. */
public record DroneSummary(UUID droneId, UUID clientId) {
}
