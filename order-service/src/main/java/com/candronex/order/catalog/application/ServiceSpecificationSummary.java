package com.candronex.order.catalog.application;

import java.util.List;

/** Une spécification et les valeurs de ses caractéristiques, prêtes à être figées dans une commande. */
public record ServiceSpecificationSummary(
        String serviceSpecificationId,
        List<Characteristic> characteristics) {

    public ServiceSpecificationSummary {
        characteristics = List.copyOf(characteristics);
    }

    /** Une caractéristique et sa valeur, sous forme de texte. */
    public record Characteristic(String name, String value) {
    }
}
