package com.candronex.ordermanagement.application;

/** Résultat d'une prise de commande. */
public record PlaceOrderResult(OrderView order, boolean created) {

    static PlaceOrderResult created(OrderView order) {
        return new PlaceOrderResult(order, true);
    }

    static PlaceOrderResult replayed(OrderView order) {
        return new PlaceOrderResult(order, false);
    }
}
