package com.candronex.order.application;

/** Port d'entrée d'UC-04 : commander les services de connectivité d'un drone. */
public interface PlaceOrderUseCase {

    PlaceOrderResult placeOrder(PlaceOrderCommand command);
}
