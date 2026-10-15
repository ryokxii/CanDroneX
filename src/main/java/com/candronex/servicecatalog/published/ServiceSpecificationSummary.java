package com.candronex.servicecatalog.published;

import java.util.List;

/**
 * Objet d'échange entre modules : une spécification, sa version et les valeurs de ses
 * caractéristiques, prêtes à être figées dans une commande.
 */
public record ServiceSpecificationSummary(
        String serviceSpecificationId,
        int version,
        List<Characteristic> characteristics) {

    public ServiceSpecificationSummary {
        characteristics = List.copyOf(characteristics);
    }

    /** Une caractéristique et sa valeur, sous forme de texte. */
    public record Characteristic(String name, String value) {
    }
}
