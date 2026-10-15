package com.candronex.ordermanagement.domain;

import java.util.UUID;

/** Ce qui identifie une ligne pour le métier : un service pour un drone. */
public record OrderLineKey(UUID droneId, String serviceSpecificationId) {
}
