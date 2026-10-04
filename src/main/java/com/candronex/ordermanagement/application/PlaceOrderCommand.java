package com.candronex.ordermanagement.application;

import com.candronex.servicecatalog.published.ServiceType;

import java.util.List;

/** Intention de commander des services de connectivité. */
public record PlaceOrderCommand(
        String clientId,
        String idempotencyKey,
        List<OrderLine> orderLines) {

    /** Une ligne demandée : un service pour un drone. */
    public record OrderLine(String droneId, ServiceType serviceType) {
    }
}
