package com.candronex.ordermanagement.application;

import com.candronex.servicecatalog.published.ServiceType;

import java.util.List;
import java.util.UUID;

/** Intention de commander des services de connectivité. */
public record PlaceOrderCommand(
        UUID clientId,
        String idempotencyKey,
        List<OrderLine> orderLines) {

    /** Une ligne demandée : un service pour un drone. */
    public record OrderLine(UUID droneId, ServiceType serviceType) {
    }
}
